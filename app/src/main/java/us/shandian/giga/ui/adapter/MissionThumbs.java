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

import org.schabi.newpipe.R;
import org.schabi.newpipe.download.MiniPlayerSheet;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import us.shandian.giga.get.FinishedMission;
import us.shandian.giga.get.Mission;

/** Loads cover art (audio) or a video frame for finished downloads. */
final class MissionThumbs {
    private static final int MAX_SIDE = 512;
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 12)) {
                @Override
                protected int sizeOf(final String key, final Bitmap value) {
                    return value.getByteCount() / 1024;
                }
            };
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private MissionThumbs() {
    }

    static void load(final Context context, final Mission mission, final ImageView view) {
        int[] pad = (int[]) view.getTag(R.id.tag_thumb_pad);
        if (pad == null) {
            pad = new int[]{view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), view.getPaddingBottom()};
            view.setTag(R.id.tag_thumb_pad, pad);
        }
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setPadding(pad[0], pad[1], pad[2], pad[3]);
        view.setTag(R.id.tag_thumb_key, null);

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

        final Bitmap cached = CACHE.get(key);
        if (cached != null) {
            showThumb(view, cached);
            return;
        }

        view.setTag(R.id.tag_thumb_key, key);
        final Context app = context.getApplicationContext();
        POOL.execute(() -> {
            final Bitmap bmp = extract(app, uri, audio);
            if (bmp == null) {
                MISSING.add(key);
                return;
            }
            CACHE.put(key, bmp);
            MAIN.post(() -> {
                if (key.equals(view.getTag(R.id.tag_thumb_key))) {
                    showThumb(view, bmp);
                }
            });
        });
    }

    private static void showThumb(final ImageView view, final Bitmap bmp) {
        view.setPadding(0, 0, 0, 0);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setClipToOutline(true);
        view.setImageBitmap(bmp);
    }

    private static Bitmap extract(final Context ctx, final Uri uri, final boolean audio) {
        final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(ctx, uri);
            final byte[] pic = mmr.getEmbeddedPicture();
            if (pic != null) {
                final Bitmap b = decode(pic);
                if (b != null) {
                    return b;
                }
            }
            if (!audio) {
                Bitmap frame = mmr.getFrameAtTime(1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (frame == null) {
                    frame = mmr.getFrameAtTime();
                }
                return frame == null ? null : scaleDown(frame);
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
        return null;
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
