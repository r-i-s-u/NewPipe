package us.shandian.giga.ui.common;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;

/** White rounded snackbar with a blue action, like the YouTube app. */
public final class SnackbarStyle {
    private static final int TEXT = 0xFF0F0F0F;
    private static final int BLUE = 0xFF065FD4;

    private SnackbarStyle() {
    }

    public static void apply(final Snackbar sb) {
        final View v = sb.getView();
        final float d = v.getResources().getDisplayMetrics().density;
        final GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(12 * d);
        v.setBackground(bg);
        ViewCompat.setElevation(v, 6 * d);
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            final int m = (int) (16 * d);
            lp.setMargins(m, lp.topMargin, m, m);
            v.setLayoutParams(lp);
        }
        sb.setTextColor(TEXT);
        sb.setActionTextColor(BLUE);
        final TextView text = v.findViewById(com.google.android.material.R.id.snackbar_text);
        if (text != null) {
            text.setTextSize(14);
            text.setMaxLines(2);
            text.setEllipsize(TextUtils.TruncateAt.END);
        }
        final TextView action = v.findViewById(com.google.android.material.R.id.snackbar_action);
        if (action != null) {
            action.setAllCaps(false);
            action.setTextSize(14);
            action.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
    }
}
