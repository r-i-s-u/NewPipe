package us.shandian.giga.ui.common;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/** An ImageView whose height always equals its width. */
public class SquareImageView extends AppCompatImageView {
    public SquareImageView(final Context context) {
        super(context);
    }

    public SquareImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(final Context context, final AttributeSet attrs, final int style) {
        super(context, attrs, style);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
