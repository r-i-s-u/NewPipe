package us.shandian.giga.ui.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import org.schabi.newpipe.R;

/** Reserves selection space while keeping video covers in a 16:9 box. */
public final class DownloadItemLayout extends LinearLayout {
    private boolean audio;
    private boolean selecting;
    private float selectionFraction;
    private ValueAnimator selectionAnimator;

    public DownloadItemLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAudio(final boolean value) {
        if (audio != value) {
            audio = value;
            requestLayout();
        }
    }

    public void setSelecting(final boolean value, final boolean animate) {
        if (animate && selecting == value) {
            return;
        }
        selecting = value;
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
            selectionAnimator = null;
        }
        final float target = value ? 1f : 0f;
        if (!animate || !isLaidOut() || selectionFraction == target) {
            setSelectionFraction(target);
            return;
        }
        selectionAnimator = ValueAnimator.ofFloat(selectionFraction, target);
        selectionAnimator.setDuration(220);
        selectionAnimator.setInterpolator(new DecelerateInterpolator());
        selectionAnimator.addUpdateListener(animation ->
                setSelectionFraction((float) animation.getAnimatedValue()));
        selectionAnimator.start();
    }

    private void setSelectionFraction(final float value) {
        selectionFraction = value;
        requestLayout();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final View icon = findViewById(R.id.item_icon);
        final View check = findViewById(R.id.item_check);
        if (icon != null && check != null
                && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            final float density = getResources().getDisplayMetrics().density;
            final int checkWidth = Math.round(40 * density * selectionFraction);
            final LayoutParams checkParams = (LayoutParams) check.getLayoutParams();
            checkParams.width = checkWidth;
            check.setAlpha(selectionFraction);
            check.setVisibility(selectionFraction > 0 ? View.VISIBLE : View.INVISIBLE);

            final LayoutParams iconParams = (LayoutParams) icon.getLayoutParams();
            final int available = MeasureSpec.getSize(widthMeasureSpec)
                    - getPaddingLeft() - getPaddingRight() - checkWidth
                    - Math.round(12 * density);
            iconParams.width = Math.max(1, Math.min(Math.round((audio ? 84 : 188) * density),
                    available / 2));
            iconParams.height = audio ? iconParams.width : Math.round(iconParams.width * 9f / 16f);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
            selectionAnimator = null;
        }
        setSelectionFraction(selecting ? 1f : 0f);
        super.onDetachedFromWindow();
    }
}
