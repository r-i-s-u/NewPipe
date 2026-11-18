package us.shandian.giga.ui.common;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * Round badge in the middle of a download thumbnail: arrow while downloading, pause bars
 * while paused or queued, with a thin progress ring around it.
 */
public class DownloadBadgeDrawable extends Drawable {
    private static final int RING_COLOR = 0xFF3EA6FF;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    private int mode;
    private float progress;

    public DownloadBadgeDrawable() {
        fill.setColor(0xB3000000);
        track.setStyle(Paint.Style.STROKE);
        track.setColor(0x40FFFFFF);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(RING_COLOR);
        ring.setStrokeCap(Paint.Cap.ROUND);
        icon.setStyle(Paint.Style.STROKE);
        icon.setColor(0xFFFFFFFF);
        icon.setStrokeCap(Paint.Cap.ROUND);
        icon.setStrokeJoin(Paint.Join.ROUND);
    }

    /** @param newMode 0 = hidden, 1 = downloading, 2 = paused or queued */
    public void update(final int newMode, final double value) {
        float p = (float) value;
        if (Float.isNaN(p)) {
            p = 0f;
        }
        p = Math.max(0f, Math.min(1f, p));
        if (newMode != mode || p != progress) {
            mode = newMode;
            progress = p;
            invalidateSelf();
        }
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        if (mode == 0) {
            return;
        }
        final Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        final float cx = b.exactCenterX();
        final float cy = b.exactCenterY();
        final float r = Math.min(b.width(), b.height()) * 0.21f;
        final float stroke = r * 0.09f;

        canvas.drawCircle(cx, cy, r, fill);

        track.setStrokeWidth(stroke);
        ring.setStrokeWidth(stroke);
        oval.set(cx - r + stroke, cy - r + stroke, cx + r - stroke, cy + r - stroke);
        canvas.drawArc(oval, 0, 360, false, track);
        if (progress > 0f) {
            canvas.drawArc(oval, -90, 360f * progress, false, ring);
        }

        icon.setStrokeWidth(r * 0.14f);
        path.reset();
        if (mode == 1) {
            path.moveTo(cx, cy - r * 0.38f);
            path.lineTo(cx, cy + r * 0.32f);
            path.moveTo(cx - r * 0.3f, cy + r * 0.04f);
            path.lineTo(cx, cy + r * 0.34f);
            path.lineTo(cx + r * 0.3f, cy + r * 0.04f);
        } else {
            path.moveTo(cx - r * 0.17f, cy - r * 0.3f);
            path.lineTo(cx - r * 0.17f, cy + r * 0.3f);
            path.moveTo(cx + r * 0.17f, cy - r * 0.3f);
            path.lineTo(cx + r * 0.17f, cy + r * 0.3f);
        }
        canvas.drawPath(path, icon);
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
