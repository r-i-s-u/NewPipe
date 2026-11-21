package us.shandian.giga.ui.common;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;

import org.schabi.newpipe.R;

/** White rounded snackbar with a blue action, like the YouTube app. */
public final class SnackbarStyle {
    private static final int TEXT = 0xFF0F0F0F;
    private static final int BLUE = 0xFF065FD4;

    private SnackbarStyle() {
    }

    private static int nightColor(final Context ctx, final int resId) {
        final Configuration cfg = new Configuration(ctx.getResources().getConfiguration());
        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | Configuration.UI_MODE_NIGHT_YES;
        return ContextCompat.getColor(ctx.createConfigurationContext(cfg), resId);
    }

    public static void apply(final Snackbar sb) {
        final View v = sb.getView();
        final boolean dark = (v.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        final Context ctx = v.getContext();
        final int bgColor = dark ? Color.WHITE : ContextCompat.getColor(ctx, R.color.black);
        final int textColor = dark ? TEXT : ContextCompat.getColor(ctx, R.color.white);
        final int actionColor = dark ? BLUE : nightColor(ctx, R.color.dl_accent);
        final float d = v.getResources().getDisplayMetrics().density;
        final GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
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
        sb.setTextColor(textColor);
        sb.setActionTextColor(actionColor);
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
