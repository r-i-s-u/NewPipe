package org.schabi.newpipe.download;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.schabi.newpipe.R;
import org.schabi.newpipe.streams.io.StoredFileHelper;

import java.util.Locale;

public final class MiniPlayerSheet {
    private static final String[] AUDIO_EXT =
            {".m4a", ".opus", ".mp3", ".ogg", ".aac", ".flac", ".wav", ".weba"};

    private MiniPlayerSheet() {
    }

    public static boolean isAudio(final StoredFileHelper storage) {
        if (storage == null) {
            return false;
        }
        final String type = storage.getType();
        if (type != null && type.startsWith("audio")) {
            return true;
        }
        final String name = storage.getName();
        if (name == null) {
            return false;
        }
        final String lower = name.toLowerCase(Locale.ROOT);
        for (final String ext : AUDIO_EXT) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLightTheme(final Context ctx) {
        final TypedValue tv = new TypedValue();
        if (!ctx.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true)) {
            return false;
        }
        final int color = tv.resourceId != 0
                ? ContextCompat.getColor(ctx, tv.resourceId) : tv.data;
        return ColorUtils.calculateLuminance(color) < 0.5;
    }

    private static String format(final int ms) {
        final int sec = Math.max(0, (ms + 999) / 1000);
        return String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
    }

    /** @return true if the player sheet was shown. */
    public static boolean show(final Context context, final StoredFileHelper storage) {
        final MediaPlayer player = new MediaPlayer();
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            player.setDataSource(context, storage.getUri());
            player.prepare();
        } catch (final Exception e) {
            player.release();
            return false;
        }

        String title = null;
        String artist = null;
        Bitmap cover = null;
        final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(context, storage.getUri());
            title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            final byte[] pic = mmr.getEmbeddedPicture();
            if (pic != null) {
                cover = BitmapFactory.decodeByteArray(pic, 0, pic.length);
            }
        } catch (final Exception ignored) {
            // tags are optional
        } finally {
            try {
                mmr.release();
            } catch (final Exception ignored) {
                // nothing to do
            }
        }

        if (title == null || title.trim().isEmpty()) {
            final String name = storage.getName();
            final int dot = name == null ? -1 : name.lastIndexOf('.');
            title = name == null ? "" : (dot > 0 ? name.substring(0, dot) : name);
        }
        final int duration = Math.max(player.getDuration(), 1);

        final boolean light = isLightTheme(context);
        final BottomSheetDialog dialog = new BottomSheetDialog(context,
                light ? R.style.DownloadSheetThemeLight : R.style.DownloadSheetTheme);
        dialog.setContentView(R.layout.mini_player);

        final ImageView coverView = dialog.findViewById(R.id.mini_cover);
        final TextView titleView = dialog.findViewById(R.id.mini_title);
        final TextView subtitleView = dialog.findViewById(R.id.mini_subtitle);
        final ImageView playView = dialog.findViewById(R.id.mini_play);
        final SeekBar seek = dialog.findViewById(R.id.mini_seek);
        final TextView remaining = dialog.findViewById(R.id.mini_remaining);

        if (cover != null) {
            coverView.setImageBitmap(cover);
            coverView.setClipToOutline(true);
        } else {
            coverView.setVisibility(View.GONE);
        }
        titleView.setText(title);
        final String total = format(duration);
        subtitleView.setText(artist == null || artist.trim().isEmpty()
                ? total : artist.trim() + " \u2022 " + total);
        seek.setMax(duration);

        final Controller controller = new Controller(player, seek, remaining, playView, duration);
        seek.setOnSeekBarChangeListener(controller);
        playView.setOnClickListener(v -> controller.toggle());
        player.setOnCompletionListener(mp -> controller.onCompleted());
        player.setOnErrorListener((mp, what, extra) -> {
            controller.onCompleted();
            return true;
        });
        dialog.setOnDismissListener(d -> controller.release());

        dialog.show();
        final BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        player.start();
        controller.refresh();
        return true;
    }

    private static final class Controller implements Runnable, SeekBar.OnSeekBarChangeListener {
        private final MediaPlayer player;
        private final SeekBar seek;
        private final TextView remaining;
        private final ImageView playButton;
        private final int duration;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean dragging = false;
        private boolean released = false;

        Controller(final MediaPlayer player, final SeekBar seek, final TextView remaining,
                   final ImageView playButton, final int duration) {
            this.player = player;
            this.seek = seek;
            this.remaining = remaining;
            this.playButton = playButton;
            this.duration = duration;
        }

        void toggle() {
            if (released) {
                return;
            }
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.start();
            }
            refresh();
        }

        void refresh() {
            if (released) {
                return;
            }
            playButton.setImageResource(
                    player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            handler.removeCallbacks(this);
            handler.post(this);
        }

        @Override
        public void run() {
            if (released) {
                return;
            }
            final int pos = Math.min(player.getCurrentPosition(), duration);
            if (!dragging) {
                seek.setProgress(pos);
                remaining.setText("-" + format(duration - pos));
            }
            if (player.isPlaying()) {
                handler.postDelayed(this, 250);
            }
        }

        void onCompleted() {
            if (released) {
                return;
            }
            handler.removeCallbacks(this);
            player.seekTo(0);
            seek.setProgress(0);
            remaining.setText("-" + format(duration));
            playButton.setImageResource(R.drawable.ic_play_arrow);
        }

        void release() {
            released = true;
            handler.removeCallbacks(this);
            try {
                player.release();
            } catch (final Exception ignored) {
                // nothing to do
            }
        }

        @Override
        public void onProgressChanged(final SeekBar bar, final int progress,
                                      final boolean fromUser) {
            if (fromUser) {
                remaining.setText("-" + format(duration - progress));
            }
        }

        @Override
        public void onStartTrackingTouch(final SeekBar bar) {
            dragging = true;
        }

        @Override
        public void onStopTrackingTouch(final SeekBar bar) {
            dragging = false;
            if (!released) {
                player.seekTo(bar.getProgress());
                refresh();
            }
        }
    }
}
