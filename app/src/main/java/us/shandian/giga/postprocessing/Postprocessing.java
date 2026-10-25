package us.shandian.giga.postprocessing;

import android.util.Log;

import androidx.annotation.NonNull;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.streams.io.SharpStream;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import us.shandian.giga.get.DownloadMission;
import us.shandian.giga.io.ChunkFileInputStream;
import us.shandian.giga.io.CircularFileWriter;
import us.shandian.giga.io.CircularFileWriter.OffsetChecker;
import us.shandian.giga.io.ProgressReport;

import static us.shandian.giga.get.DownloadMission.ERROR_NOTHING;
import static us.shandian.giga.get.DownloadMission.ERROR_POSTPROCESSING;
import static us.shandian.giga.get.DownloadMission.ERROR_POSTPROCESSING_HOLD;

public abstract class Postprocessing implements Serializable {

    static transient final byte OK_RESULT = ERROR_NOTHING;

    public transient static final String ALGORITHM_TTML_CONVERTER = "ttml";
    public transient static final String ALGORITHM_WEBM_MUXER = "webm";
    public transient static final String ALGORITHM_MP4_FROM_DASH_MUXER = "mp4D-mp4";
    public transient static final String ALGORITHM_M4A_NO_DASH = "mp4D-m4a";
    public transient static final String ALGORITHM_OGG_FROM_WEBM_DEMUXER = "webm-ogg-d";

    public static Postprocessing getAlgorithm(@NonNull String algorithmName, String[] args,
                                              StreamInfo streamInfo) {
        Postprocessing instance;

        switch (algorithmName) {
            case ALGORITHM_TTML_CONVERTER:
                instance = new TtmlConverter();
                break;
            case ALGORITHM_WEBM_MUXER:
                instance = new WebMMuxer();
                break;
            case ALGORITHM_MP4_FROM_DASH_MUXER:
                instance = new Mp4FromDashMuxer();
                break;
            case ALGORITHM_M4A_NO_DASH:
                instance = new M4aNoDash();
                break;
            case ALGORITHM_OGG_FROM_WEBM_DEMUXER:
                instance = new OggFromWebmDemuxer();
                break;
            /*case "example-algorithm":
                instance = new ExampleAlgorithm();*/
            default:
                throw new UnsupportedOperationException("Unimplemented post-processing algorithm: " + algorithmName);
        }

        instance.args = args;
        instance.streamInfo = streamInfo;
        return instance;
    }

    /**
     * Get a boolean value that indicate if the given algorithm work on the same
     * file
     */
    public boolean worksOnSameFile;

    /**
     * Indicates whether the selected algorithm needs space reserved at the beginning of the file
     */
    public boolean reserveSpace;

    /**
     * Gets the given algorithm short name
     */
    private final String name;

    private String[] args;
    protected StreamInfo streamInfo;

    private transient DownloadMission mission;

    private transient File tempFile;

    Postprocessing(boolean reserveSpace, boolean worksOnSameFile, String algorithmName) {
        this.reserveSpace = reserveSpace;
        this.worksOnSameFile = worksOnSameFile;
        this.name = algorithmName;// for debugging only
    }

    public void setTemporalDir(@NonNull File directory) {
        long rnd = (int) (Math.random() * 100000.0f);
        tempFile = new File(directory, rnd + "_" + System.nanoTime() + ".tmp");
    }

    public void cleanupTemporalDir() {
        if (tempFile != null && tempFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            } catch (Exception e) {
                // nothing to do
            }
        }
    }


    public void run(DownloadMission target) throws IOException {
        this.mission = target;

        int result;
        long finalLength = -1;

        mission.done = 0;

        long length = mission.storage.length() - mission.offsets[0];
        mission.length = Math.max(length, mission.nearLength);

        final ProgressReport readProgress = (long position) -> {
            position -= mission.offsets[0];
            if (position > mission.done) mission.done = position;
        };

        if (worksOnSameFile) {
            ChunkFileInputStream[] sources = new ChunkFileInputStream[mission.urls.length];
            try {
                for (int i = 0, j = 1; i < sources.length; i++, j++) {
                    SharpStream source = mission.storage.getStream();
                    long end = j < sources.length ? mission.offsets[j] : source.length();

                    sources[i] = new ChunkFileInputStream(source, mission.offsets[i], end, readProgress);
                }

                if (test(sources)) {
                    for (SharpStream source : sources) source.rewind();

                    OffsetChecker checker = () -> {
                        for (ChunkFileInputStream source : sources) {
                            /*
                             * WARNING: never use rewind() in any chunk after any writing (especially on first chunks)
                             *          or the CircularFileWriter can lead to unexpected results
                             */
                            if (source.isClosed() || source.available() < 1) {
                                continue;// the selected source is not used anymore
                            }

                            return source.getFilePointer() - 1;
                        }

                        return -1;
                    };

                    try (CircularFileWriter out = new CircularFileWriter(
                            mission.storage.getStream(), tempFile, checker)) {
                        out.onProgress = (long position) -> mission.done = position;

                        out.onWriteError = err -> {
                            mission.psState = 3;
                            mission.notifyError(ERROR_POSTPROCESSING_HOLD, err);

                            try {
                                synchronized (this) {
                                    while (mission.psState == 3)
                                        wait();
                                }
                            } catch (InterruptedException e) {
                                // nothing to do
                                Log.e(getClass().getSimpleName(), "got InterruptedException");
                            }

                            return mission.errCode == ERROR_NOTHING;
                        };

                        result = process(out, sources);

                        if (result == OK_RESULT)
                            finalLength = out.finalizeFile();
                    }
                } else {
                    result = OK_RESULT;
                }
            } finally {
                for (SharpStream source : sources) {
                    if (source != null && !source.isClosed()) {
                        source.close();
                    }
                }
                if (tempFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                    tempFile = null;
                }
            }
        } else {
            result = test() ? process(null) : OK_RESULT;
        }

        if (result == OK_RESULT) {
            if (finalLength != -1) {
                mission.length = finalLength;
            }
        } else {
            mission.errCode = ERROR_POSTPROCESSING;
            mission.errObject = new RuntimeException("post-processing algorithm returned " + result);
        }

        if (result != OK_RESULT && worksOnSameFile) mission.storage.delete();

        this.mission = null;
    }

    /**
     * Test if the post-processing algorithm can be skipped
     *
     * @param sources files to be processed
     * @return {@code true} if the post-processing is required, otherwise, {@code false}
     * @throws IOException if an I/O error occurs.
     */
    boolean test(SharpStream... sources) throws IOException {
        return true;
    }

    /**
     * Abstract method to execute the post-processing algorithm
     *
     * @param out     output stream
     * @param sources files to be processed
     * @return an error code, {@code OK_RESULT} means the operation was successful
     * @throws IOException if an I/O error occurs.
     */
    abstract int process(SharpStream out, SharpStream... sources) throws IOException;

    void applyMetadata(final org.schabi.newpipe.streams.Mp4FromDashWriter muxer) {
        if (streamInfo != null) {
            muxer.setMetadata(streamInfo.getName(), streamInfo.getUploaderName(),
                    fetchThumbnail());
        }
    }

    private byte[] fetchThumbnail() {
        final java.util.ArrayList<String> urls = new java.util.ArrayList<>();
        try {
            if (streamInfo.getServiceId() == 0 && streamInfo.getId() != null) {
                final String base = "https://i.ytimg.com/vi/" + streamInfo.getId() + "/";
                urls.add(base + "maxresdefault.jpg");
                urls.add(base + "hq720.jpg");
            }
            String best = null;
            int bestHeight = -2;
            for (final var image : streamInfo.getThumbnails()) {
                if (image.getUrl() != null && image.getHeight() > bestHeight) {
                    bestHeight = image.getHeight();
                    best = image.getUrl();
                }
            }
            if (best != null) {
                urls.add(best);
            }
        } catch (final Exception ignored) {
            // use whatever was collected
        }

        for (final String url : urls) {
            final byte[] data = downloadImage(url);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private static boolean isDark(final int px) {
        return ((px >> 16) & 0xFF) < 10 && ((px >> 8) & 0xFF) < 10 && (px & 0xFF) < 10;
    }

    private static boolean rowDark(final android.graphics.Bitmap b, final int y, final int x0,
                                   final int x1, final int[] buf) {
        final int n = x1 - x0;
        b.getPixels(buf, 0, n, x0, y, n, 1);
        int bad = 0;
        for (int i = 0; i < n; i++) {
            if (!isDark(buf[i])) {
                bad++;
            }
        }
        return bad * 200 <= n;
    }

    private static boolean colDark(final android.graphics.Bitmap b, final int x, final int y0,
                                   final int y1, final int[] buf) {
        final int n = y1 - y0;
        b.getPixels(buf, 0, 1, x, y0, 1, n);
        int bad = 0;
        for (int i = 0; i < n; i++) {
            if (!isDark(buf[i])) {
                bad++;
            }
        }
        return bad * 200 <= n;
    }

    /** Removes black bars and returns a centred square crop (like album art). */
    private static android.graphics.Bitmap squareCover(final android.graphics.Bitmap src) {
        try {
            final int w = src.getWidth();
            final int h = src.getHeight();
            final int[] buf = new int[Math.max(w, h)];
            int top = 0;
            int bottom = h;
            int left = 0;
            int right = w;
            while (top < bottom - 1 && rowDark(src, top, 0, w, buf)) {
                top++;
            }
            while (bottom > top + 1 && rowDark(src, bottom - 1, 0, w, buf)) {
                bottom--;
            }
            while (left < right - 1 && colDark(src, left, top, bottom, buf)) {
                left++;
            }
            while (right > left + 1 && colDark(src, right - 1, top, bottom, buf)) {
                right--;
            }
            int cw = right - left;
            int ch = bottom - top;
            if ((long) cw * ch < (long) w * h / 10) {
                // almost everything is black: keep the original picture
                left = 0;
                top = 0;
                cw = w;
                ch = h;
            }
            final int side = Math.min(cw, ch);
            final int x = left + (cw - side) / 2;
            final int y = top + (ch - side) / 2;
            if (side == w && side == h) {
                return src;
            }
            return android.graphics.Bitmap.createBitmap(src, x, y, side, side);
        } catch (final Throwable t) {
            return src;
        }
    }

    private static byte[] downloadImage(final String url) {
        java.net.HttpURLConnection con = null;
        try {
            con = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (con.getResponseCode() != 200) {
                return null;
            }
            final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream in = con.getInputStream()) {
                final byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    bytes.write(buf, 0, n);
                    if (bytes.size() > 4_000_000) {
                        return null;
                    }
                }
            }
            final byte[] data = bytes.toByteArray();
            if (data.length < 1024) {
                return null;
            }
            final boolean jpeg = (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
            final boolean png = (data[0] & 0xFF) == 0x89 && data[1] == 'P';
            final android.graphics.Bitmap bmp =
                    android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bmp == null) {
                return (jpeg || png) ? data : null;
            }
            final android.graphics.Bitmap cover = squareCover(bmp);
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            cover.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out);
            return out.toByteArray();
        } catch (final Exception e) {
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    byte[] metadataBox() {
        if (streamInfo == null) {
            return null;
        }
        return org.schabi.newpipe.streams.Mp4Tagger.buildUdta(
                streamInfo.getName(), streamInfo.getUploaderName(), fetchThumbnail());
    }

    String getArgumentAt(int index, String defaultValue) {
        if (args == null || index >= args.length) {
            return defaultValue;
        }

        return args[index];
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        str.append("{ name=").append(name).append('[');

        if (args != null) {
            for (String arg : args) {
                str.append(", ");
                str.append(arg);
            }
            str.delete(0, 1);
        }

        return str.append("] }").toString();
    }
}
