package us.shandian.giga.ui.common;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/** A thin rounded progress bar (determinate, or a moving segment while the size is unknown). */
public class ProgressDrawable extends Drawable {
    private static final int MARQUEE_PERIOD = 1200;
    private static final long FRAME_DELAY = 40;

    private float mProgress;
    private int mBackgroundColor;
    private int mForegroundColor;
    private boolean mMarquee = false;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final Runnable mTick = this::invalidateSelf;

    public ProgressDrawable() {
    }

    public void setColors(@ColorInt int background, @ColorInt int foreground) {
        mBackgroundColor = background;
        mForegroundColor = foreground;
        invalidateSelf();
    }

    public void setProgress(double progress) {
        float p = (float) progress;
        if (Float.isNaN(p)) {
            p = 0f;
        }
        mProgress = Math.max(0f, Math.min(1f, p));
        invalidateSelf();
    }

    public void setMarquee(boolean marquee) {
        if (mMarquee == marquee) {
            return;
        }
        mMarquee = marquee;
        if (!marquee) {
            unscheduleSelf(mTick);
        }
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        final float radius = b.height() / 2f;

        mPaint.setColor(mBackgroundColor);
        mRect.set(b);
        canvas.drawRoundRect(mRect, radius, radius, mPaint);

        mPaint.setColor(mForegroundColor);
        if (mMarquee) {
            final float seg = b.width() * 0.3f;
            final float t = (SystemClock.uptimeMillis() % MARQUEE_PERIOD) / (float) MARQUEE_PERIOD;
            final float start = b.left - seg + (b.width() + seg) * t;
            canvas.save();
            mRect.set(b);
            canvas.clipRect(mRect);
            mRect.set(start, b.top, start + seg, b.bottom);
            canvas.drawRoundRect(mRect, radius, radius, mPaint);
            canvas.restore();
            scheduleSelf(mTick, SystemClock.uptimeMillis() + FRAME_DELAY);
        } else if (mProgress > 0f) {
            final float w = Math.max(b.height(), b.width() * mProgress);
            mRect.set(b.left, b.top, b.left + w, b.bottom);
            canvas.drawRoundRect(mRect, radius, radius, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // Unsupported
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        // Unsupported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
