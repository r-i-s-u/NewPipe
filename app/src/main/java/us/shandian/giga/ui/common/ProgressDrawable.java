package us.shandian.giga.ui.common;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class ProgressDrawable extends Drawable {
    private static final int MARQUEE_INTERVAL = 150;

    private float mProgress;
    private int mBackgroundColor, mForegroundColor;
    private Handler mMarqueeHandler;
    private float mMarqueeProgress;
    private Path mMarqueeLine;
    private int mMarqueeSize;
    private long mMarqueeNext;

    public ProgressDrawable() {
        mMarqueeLine = null;// marquee disabled
        mMarqueeProgress = 0.0f;
        mMarqueeSize = 0;
        mMarqueeNext = 0;
    }

    public void setColors(@ColorInt int background, @ColorInt int foreground) {
        mBackgroundColor = background;
        mForegroundColor = foreground;
    }

    public void setProgress(double progress) {
        mProgress = (float) progress;
        invalidateSelf();
    }

    public void setMarquee(boolean marquee) {
        if (marquee == (mMarqueeLine != null)) {
            return;
        }
        mMarqueeLine = marquee ? new Path() : null;
        mMarqueeHandler = marquee ? new Handler(Looper.getMainLooper()) : null;
        mMarqueeSize = 0;
        mMarqueeNext = 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
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
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public void onBoundsChange(Rect rect) {
        if (mMarqueeLine != null) setupMarquee(rect.width(), rect.height());
    }

    private void setupMarquee(int width, int height) {
        mMarqueeSize = (int) ((width * 10.0f) / 100.0f);// the size is 10% of the width

        mMarqueeLine.rewind();
        mMarqueeLine.moveTo(-mMarqueeSize, -mMarqueeSize);
        mMarqueeLine.lineTo(-mMarqueeSize * 4, height + mMarqueeSize);
        mMarqueeLine.close();
    }
}
