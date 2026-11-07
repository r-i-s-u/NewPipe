package us.shandian.giga.ui.adapter;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Dim scrim with a progress ring and percentage drawn over a pending download thumbnail. */
final class PendingOverlayDrawable extends Drawable {
    private final Paint scrim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private ValueAnimator progressAnim;
    private ValueAnimator loop;
    private boolean first = true;
    private boolean indeterminate;
    private boolean running;
    private float shown;
    private float target;
    private float phase;
    private int color = Color.WHITE;

    void setColor(final int c) {
        color = c;
    }

    void update(final float p, final boolean isRunning) {
        indeterminate = p < 0f || Float.isNaN(p);
        running = isRunning;
        if (!indeterminate) {
            final float t = Math.max(0f, Math.min(1f, p));
            if (first) {
                shown = t;
                target = t;
            } else if (Math.abs(t - target) > 0.0005f) {
                target = t;
                if (progressAnim != null) {
                    progressAnim.cancel();
                }
                progressAnim = ValueAnimator.ofFloat(shown, target);
                progressAnim.setDuration(350);
                progressAnim.setInterpolator(new DecelerateInterpolator());
                progressAnim.addUpdateListener(an -> {
                    if (getCallback() == null) {
                        an.cancel();
                        return;
                    }
                    shown = (float) an.getAnimatedValue();
                    invalidateSelf();
                });
                progressAnim.start();
            }
        }
        first = false;
        if (running) {
            startLoop();
        } else {
            stopLoop();
        }
        invalidateSelf();
    }

    private void startLoop() {
        if (loop != null && loop.isRunning()) {
            return;
        }
        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(1200);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(an -> {
            if (getCallback() == null) {
                an.cancel();
                return;
            }
            phase = (float) an.getAnimatedValue();
            invalidateSelf();
        });
        loop.start();
    }

    private void stopLoop() {
        if (loop != null) {
            loop.cancel();
            loop = null;
        }
    }

    void stop() {
        stopLoop();
        if (progressAnim != null) {
            progressAnim.cancel();
            progressAnim = null;
        }
    }

    @Override
    public void draw(@NonNull final Canvas c) {
        final Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        scrim.setColor(0x66000000);
        c.drawRect(b, scrim);
        final float cx = b.exactCenterX();
        final float cy = b.exactCenterY();
        final float r = Math.min(b.width(), b.height()) * 0.225f;
        final float sw = Math.max(3f, r * 0.16f);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(sw);
        track.setColor(0x55FFFFFF);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(sw);
        arc.setStrokeCap(Paint.Cap.ROUND);
        oval.set(cx - r, cy - r, cx + r, cy + r);
        c.drawCircle(cx, cy, r, track);
        if (indeterminate) {
            arc.setColor(color);
            c.drawArc(oval, -90f + 360f * phase, 100f, false, arc);
            return;
        }
        final int alpha = running
                ? (int) (255 * (0.75f + 0.25f * Math.sin(phase * 2 * Math.PI))) : 255;
        arc.setColor(Color.argb(alpha, Color.red(color), Color.green(color),
                Color.blue(color)));
        c.drawArc(oval, -90f, 360f * shown, false, arc);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(r * 0.62f);
        final Paint.FontMetrics fm = text.getFontMetrics();
        c.drawText((int) (shown * 100f) + "%", cx, cy - (fm.ascent + fm.descent) / 2f, text);
    }

    @Override
    public void setAlpha(final int alpha) {
        // fixed alpha
    }

    @Override
    public void setColorFilter(@Nullable final ColorFilter colorFilter) {
        // no filter
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
