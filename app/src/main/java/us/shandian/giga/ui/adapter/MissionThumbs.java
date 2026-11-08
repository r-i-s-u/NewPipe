package us.shandian.giga.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.download.MiniPlayerSheet;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import us.shandian.giga.get.FinishedMission;
import us.shandian.giga.get.Mission;

/** Loads cover art (or a video frame) and the uploader name of finished downloads. */
final class MissionThumbs {
    private static final int MAX_SIDE = 512;

    private static final class Meta {
        final Bitmap bmp;
        final String artist;

        Meta(final Bitmap bmp, final String artist) {
            this.bmp = bmp;
            this.artist = artist;
        }
    }

    private static final LruCache<String, Meta> CACHE =
            new LruCache<String, Meta>((int) (Runtime.getRuntime().maxMemory() / 1024 / 12)) {
                @Override
                protected int sizeOf(final String key, final Meta value) {
                    return value.bmp == null ? 1 : value.bmp.getByteCount() / 1024 + 1;
                }
            };
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private MissionThumbs() {
    }

    static void load(final Context context, final Mission mission, final ImageView view,
                     final TextView subtitle) {
        int[] pad = (int[]) view.getTag(R.id.tag_thumb_pad);
        if (pad == null) {
            pad = new int[]{view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), view.getPaddingBottom()};
            view.setTag(R.id.tag_thumb_pad, pad);
        }
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setPadding(pad[0], pad[1], pad[2], pad[3]);
        view.setTag(R.id.tag_thumb_key, null);
        subtitle.setText("");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            final android.graphics.drawable.Drawable fg = view.getForeground();
            if (fg instanceof PendingOverlayDrawable) {
                ((PendingOverlayDrawable) fg).stop();
            }
            view.setForeground(null);
        }
        if (mission instanceof us.shandian.giga.get.DownloadMission) {
            loadRemote((us.shandian.giga.get.DownloadMission) mission, view, subtitle);
            return;
        }

        if (!(mission instanceof FinishedMission) || mission.storage == null
                || mission.storage.isInvalid()) {
            return;
        }

        final Uri uri;
        final String key;
        final boolean audio;
        try {
            uri = mission.storage.getUri();
            key = uri + "#" + mission.length;
            audio = MiniPlayerSheet.isAudio(mission.storage);
        } catch (final Exception e) {
            return;
        }
        if (MISSING.contains(key)) {
            return;
        }

        final Meta cached = CACHE.get(key);
        if (cached != null) {
            apply(view, subtitle, cached);
            return;
        }

        view.setTag(R.id.tag_thumb_key, key);
        final Context app = context.getApplicationContext();
        POOL.execute(() -> {
            final Meta meta = withStored(app, mission.source, uri,
                    extract(app, uri, audio));
            if (meta == null) {
                MISSING.add(key);
                return;
            }
            CACHE.put(key, meta);
            MAIN.post(() -> {
                if (key.equals(view.getTag(R.id.tag_thumb_key))) {
                    apply(view, subtitle, meta);
                }
            });
        });
    }

    private static void loadRemote(final us.shandian.giga.get.DownloadMission mission,
                                   final ImageView view, final TextView subtitle) {
        boolean audio = false;
        try {
            audio = mission.storage != null && MiniPlayerSheet.isAudio(mission.storage);
        } catch (final Exception ignored) {
            // treat as video
        }
        final String url = mission.psAlgorithm == null
                ? null : mission.psAlgorithm.thumbnailUrl(audio);
        if (url == null) {
            return;
        }
        final String key = "remote#" + url;
        if (MISSING.contains(key)) {
            return;
        }
        final Meta cached = CACHE.get(key);
        if (cached != null) {
            apply(view, subtitle, cached);
            return;
        }
        view.setTag(R.id.tag_thumb_key, key);
        POOL.execute(() -> {
            final Bitmap bmp = fetchRemote(url);
            if (bmp == null) {
                MISSING.add(key);
                return;
            }
            final Meta meta = new Meta(bmp, null);
            CACHE.put(key, meta);
            MAIN.post(() -> {
                if (key.equals(view.getTag(R.id.tag_thumb_key))) {
                    apply(view, subtitle, meta);
                }
            });
        });
    }

    private static Meta withStored(final Context ctx, final String source, final Uri uri,
                                   final Meta found) {
        final String[] st = us.shandian.giga.get.MissionMeta.get(ctx, source);
        if (st == null || hasEmbedded(ctx, uri)) {
            return found;
        }
        final Bitmap thumb = st[1] == null ? null : fetchRemote(st[1]);
        final Bitmap bmp = thumb != null ? thumb : (found == null ? null : found.bmp);
        final String artist = st[0] != null ? st[0] : (found == null ? null : found.artist);
        return bmp == null && artist == null ? found : new Meta(bmp, artist);
    }

    private static boolean hasEmbedded(final Context ctx, final Uri uri) {
        final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(ctx, uri);
            return mmr.getEmbeddedPicture() != null;
        } catch (final Exception e) {
            return false;
        } finally {
            try {
                mmr.release();
            } catch (final Exception ignored) {
                // nothing to do
            }
        }
    }

    private static Bitmap fetchRemote(final String url) {
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
            return decode(bytes.toByteArray());
        } catch (final Exception e) {
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    static void setOverlay(final ImageView view, final Mission mission) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || !(mission instanceof us.shandian.giga.get.DownloadMission)) {
            return;
        }
        final us.shandian.giga.get.DownloadMission m = (us.shandian.giga.get.DownloadMission) mission;
        final float p = m.isPsRunning() || !m.isInitialized() || m.unknownLength || m.length <= 0
                ? -1f : (float) m.done / (float) m.length;
        final android.graphics.drawable.Drawable fg = view.getForeground();
        final PendingOverlayDrawable o;
        if (fg instanceof PendingOverlayDrawable) {
            o = (PendingOverlayDrawable) fg;
        } else {
            o = new PendingOverlayDrawable();
            view.setForeground(o);
            view.setClipToOutline(true);
        }
        o.setColor(androidx.core.content.ContextCompat.getColor(view.getContext(),
                R.color.dl_accent));
        o.update(p, m.running || m.isPsRunning());
    }

    private static void apply(final ImageView view, final TextView subtitle, final Meta meta) {
        if (meta.bmp != null) {
            view.setPadding(0, 0, 0, 0);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setClipToOutline(true);
            view.setImageBitmap(meta.bmp);
        }
        if (meta.artist != null) {
            subtitle.setText(meta.artist);
        }
    }

    private static Meta extract(final Context ctx, final Uri uri, final boolean audio) {
        final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        Bitmap bmp = null;
        String artist = null;
        try {
            mmr.setDataSource(ctx, uri);
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            final byte[] pic = mmr.getEmbeddedPicture();
            if (pic != null) {
                bmp = decode(pic);
            }
            if (!audio && bmp != null && bmp.getWidth() * 10 < bmp.getHeight() * 14) {
                bmp = null;
            }
            if (bmp == null && !audio) {
                Bitmap frame = mmr.getFrameAtTime(1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (frame == null) {
                    frame = mmr.getFrameAtTime();
                }
                if (frame != null) {
                    bmp = scaleDown(frame);
                }
            }
        } catch (final Exception ignored) {
            // unreadable file: keep the icon
        } finally {
            try {
                mmr.release();
            } catch (final Exception ignored) {
                // nothing to do
            }
        }
        if (artist != null) {
            artist = artist.trim();
            if (artist.isEmpty()) {
                artist = null;
            }
        }
        return bmp == null && artist == null ? null : new Meta(bmp, artist);
    }

    private static Bitmap decode(final byte[] data) {
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
    }

    private static Bitmap scaleDown(final Bitmap src) {
        final int max = Math.max(src.getWidth(), src.getHeight());
        if (max <= MAX_SIDE) {
            return src;
        }
        final float f = (float) MAX_SIDE / max;
        return Bitmap.createScaledBitmap(src, Math.round(src.getWidth() * f),
                Math.round(src.getHeight() * f), true);
    }
}
