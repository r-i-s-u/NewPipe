package org.schabi.newpipe.player.notification;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_ONE;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_CLOSE;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_FAST_FORWARD;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_FAST_REWIND;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_PLAY_NEXT;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_PLAY_PAUSE;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_PLAY_PREVIOUS;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_REPEAT;
import static org.schabi.newpipe.player.notification.NotificationConstants.ACTION_SHUFFLE;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.R;
import org.schabi.newpipe.player.Player;

import java.util.Objects;

public final class NotificationActionData {

    @NonNull
    private final String action;
    @NonNull
    private final String name;
    @DrawableRes
    private final int icon;


    public NotificationActionData(@NonNull final String action, @NonNull final String name,
                                  @DrawableRes final int icon) {
        this.action = action;
        this.name = name;
        this.icon = icon;
    }

    @NonNull
    public String action() {
        return action;
    }

    @NonNull
    public String name() {
        return name;
    }

    @DrawableRes
    public int icon() {
        return icon;
    }


    @SuppressLint("PrivateResource") // we currently use Exoplayer's internal strings and icons
    @Nullable
    public static NotificationActionData fromNotificationActionEnum(
            @NonNull final Player player,
            @NotificationConstants.Action final int selectedAction
    ) {

        final int baseActionIcon = NotificationConstants.ACTION_ICONS[selectedAction];
        final Context ctx = player.getContext();

        switch (selectedAction) {
            case NotificationConstants.PREVIOUS:
                return new NotificationActionData(ACTION_PLAY_PREVIOUS,
                        ctx.getString(com.google.android.exoplayer2.ui.R.string
                                .exo_controls_previous_description), baseActionIcon);

            case NotificationConstants.NEXT:
                return new NotificationActionData(ACTION_PLAY_NEXT,
                        ctx.getString(com.google.android.exoplayer2.ui.R.string
                                .exo_controls_next_description), baseActionIcon);

            case NotificationConstants.REWIND:
                return new NotificationActionData(ACTION_FAST_REWIND,
                        ctx.getString(com.google.android.exoplayer2.ui.R.string
                                .exo_controls_rewind_description), baseActionIcon);

            case NotificationConstants.FORWARD:
                return new NotificationActionData(ACTION_FAST_FORWARD,
                        ctx.getString(com.google.android.exoplayer2.ui.R.string
                                .exo_controls_fastforward_description), baseActionIcon);

            case NotificationConstants.SMART_REWIND_PREVIOUS:
                if (player.getPlayQueue() != null && player.getPlayQueue().size() > 1) {
                    return new NotificationActionData(ACTION_PLAY_PREVIOUS,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_previous_description),
                            0);
                } else {
                    return new NotificationActionData(ACTION_FAST_REWIND,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_rewind_description),
                            0);
                }

            case NotificationConstants.SMART_FORWARD_NEXT:
                if (player.getPlayQueue() != null && player.getPlayQueue().size() > 1) {
                    return new NotificationActionData(ACTION_PLAY_NEXT,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_next_description),
                            0);
                } else {
                    return new NotificationActionData(ACTION_FAST_FORWARD,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_fastforward_description),
                            0);
                }

            case NotificationConstants.PLAY_PAUSE_BUFFERING:
                if (player.getCurrentState() == Player.STATE_PREFLIGHT
                        || player.getCurrentState() == Player.STATE_BLOCKED
                        || player.getCurrentState() == Player.STATE_BUFFERING) {
                    return new NotificationActionData(ACTION_PLAY_PAUSE,
                            ctx.getString(R.string.notification_action_buffering),
                            0);
                }

                // fallthrough
            case NotificationConstants.PLAY_PAUSE:
                if (player.getCurrentState() == Player.STATE_COMPLETED) {
                    return new NotificationActionData(ACTION_PLAY_PAUSE,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_pause_description),
                            0);
                } else if (player.isPlaying()
                        || player.getCurrentState() == Player.STATE_PREFLIGHT
                        || player.getCurrentState() == Player.STATE_BLOCKED
                        || player.getCurrentState() == Player.STATE_BUFFERING) {
                    return new NotificationActionData(ACTION_PLAY_PAUSE,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_pause_description),
                            0);
                } else {
                    return new NotificationActionData(ACTION_PLAY_PAUSE,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_play_description),
                            0);
                }

            case NotificationConstants.REPEAT:
                if (player.getRepeatMode() == REPEAT_MODE_ALL) {
                    return new NotificationActionData(ACTION_REPEAT,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_repeat_all_description),
                            0);
                } else if (player.getRepeatMode() == REPEAT_MODE_ONE) {
                    return new NotificationActionData(ACTION_REPEAT,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_repeat_one_description),
                            0);
                } else /* player.getRepeatMode() == REPEAT_MODE_OFF */ {
                    return new NotificationActionData(ACTION_REPEAT,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_repeat_off_description),
                            0);
                }

            case NotificationConstants.SHUFFLE:
                if (player.getPlayQueue() != null && player.getPlayQueue().isShuffled()) {
                    return new NotificationActionData(ACTION_SHUFFLE,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_shuffle_on_description),
                            0);
                } else {
                    return new NotificationActionData(ACTION_SHUFFLE,
                            ctx.getString(com.google.android.exoplayer2.ui.R.string
                                    .exo_controls_shuffle_off_description),
                            0);
                }

            case NotificationConstants.CLOSE:
                return new NotificationActionData(ACTION_CLOSE, ctx.getString(R.string.close),
                        0);

            case NotificationConstants.NOTHING:
            default:
                // do nothing
                return null;
        }
    }


    @Override
    public boolean equals(@Nullable final Object obj) {
        return (obj instanceof NotificationActionData other)
                && this.action.equals(other.action)
                && this.name.equals(other.name)
                && this.icon == other.icon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, name, icon);
    }
}
