package org.schabi.newpipe.util;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Flat, thumbless seek bar track (YouTube Music style): thin at rest,
 * thicker while touched.
 */
public final class FlatSeekBarDrawable extends Drawable {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float restPx;
    private final float pressedPx;
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private float thick;
    private ValueAnimator anim;

    public FlatSeekBarDrawable(final int color, final float restPx, final float pressedPx) {
        this.restPx = restPx;
        this.pressedPx = pressedPx;
        this.thick = restPx;
        fill.setColor(color);
        track.setColor(color);
        track.setAlpha(0x40);
    }

    public void setPressedState(final boolean pressed) {
        if (anim != null) {
            anim.cancel();
        }
        anim = ValueAnimator.ofFloat(thick, pressed ? pressedPx : restPx);
        anim.setDuration(pressed ? 120 : 180);
        anim.addUpdateListener(a -> {
            thick = (float) a.getAnimatedValue();
            invalidateSelf();
        });
        anim.start();
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        final Rect b = getBounds();
        final float cy = b.exactCenterY();
        rect.set(b.left, cy - thick / 2f, b.right, cy + thick / 2f);
        final float radius = thick / 2f;
        canvas.drawRoundRect(rect, radius, radius, track);
        final float frac = getLevel() / 10000f;
        if (frac > 0f) {
            canvas.save();
            clip.reset();
            clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
            canvas.clipPath(clip);
            canvas.drawRect(rect.left, rect.top, rect.left + rect.width() * frac,
                    rect.bottom, fill);
            canvas.restore();
        }
    }

    @Override
    protected boolean onLevelChange(final int level) {
        invalidateSelf();
        return true;
    }

    @Override
    public void setAlpha(final int alpha) {
        fill.setAlpha(alpha);
        track.setAlpha(alpha * 0x40 / 0xFF);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable final ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
        track.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
