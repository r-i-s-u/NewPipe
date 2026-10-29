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
            final Meta meta = extract(app, uri, audio);
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

    private static void apply(final ImageView view, final TextView subtitle, final Meta meta) {
        if (meta.bmp != null) {
            view.setPadding(0, 0, 0, 0);
            view.setScaleType(ImageView.ScaleType.FIT_CENTER);
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
