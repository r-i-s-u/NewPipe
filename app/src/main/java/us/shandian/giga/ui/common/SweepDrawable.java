package us.shandian.giga.ui.common;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/** Dark overlay that sweeps over the thumbnail from left to right as the download progresses. */
public class SweepDrawable extends Drawable {
    private final Paint paint = new Paint();
    private float progress;

    public SweepDrawable() {
        paint.setColor(0x99000000);
    }

    public void setProgress(final double value) {
        float p = (float) value;
        if (Float.isNaN(p)) {
            p = 0f;
        }
        p = Math.max(0f, Math.min(1f, p));
        if (p != progress) {
            progress = p;
            invalidateSelf();
        }
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        if (progress <= 0f || progress >= 1f) {
            return;
        }
        final Rect b = getBounds();
        canvas.drawRect(b.left, b.top, b.left + b.width() * progress, b.bottom, paint);
    }

    @Override
    public void setAlpha(final int alpha) {
        // Unsupported
    }

    @Override
    public void setColorFilter(final ColorFilter filter) {
        // Unsupported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
