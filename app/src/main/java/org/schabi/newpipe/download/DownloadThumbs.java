package org.schabi.newpipe.download;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Remembers title, uploader and thumbnail of the downloads started in this session. */
public final class DownloadThumbs {
    private static final int MAX_SIDE = 320;

    public static final class Info {
        public final String title;
        public final String uploader;
        final String thumbUrl;
        volatile Bitmap bitmap;
        volatile boolean loading;

        Info(final String title, final String uploader, final String thumbUrl) {
            this.title = title;
            this.uploader = uploader;
            this.thumbUrl = thumbUrl;
        }
    }

    private static final Map<String, Info> MAP = Collections.synchronizedMap(
            new LinkedHashMap<String, Info>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, Info> eldest) {
                    return size() > 24;
                }
            });

    private DownloadThumbs() {
    }

    public static void register(final String fileName, final StreamInfo info) {
        if (fileName == null || info == null) {
            return;
        }
        String url = null;
        int best = -2;
        try {
            for (final var image : info.getThumbnails()) {
                if (image.getUrl() != null && image.getHeight() > best) {
                    best = image.getHeight();
                    url = image.getUrl();
                }
            }
        } catch (final Exception ignored) {
            // no thumbnail
        }
        MAP.put(fileName, new Info(info.getName(), info.getUploaderName(), url));
    }

    public static Info get(final String fileName) {
        return fileName == null ? null : MAP.get(fileName);
    }

    /** Returns the thumbnail if it is already loaded, otherwise starts loading it. */
    public static Bitmap bitmap(final String fileName) {
        final Info i = get(fileName);
        if (i == null) {
            return null;
        }
        if (i.bitmap == null && i.thumbUrl != null && !i.loading) {
            i.loading = true;
            new Thread(() -> i.bitmap = download(i.thumbUrl), "download-thumb").start();
        }
        return i.bitmap;
    }

    private static Bitmap download(final String url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (con.getResponseCode() != 200) {
                return null;
            }
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = con.getInputStream()) {
                final byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    bytes.write(buf, 0, n);
                    if (bytes.size() > 3_000_000) {
                        return null;
                    }
                }
            }
            final byte[] data = bytes.toByteArray();
            final BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= MAX_SIDE) {
                sample *= 2;
            }
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o);
        } catch (final Exception e) {
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
