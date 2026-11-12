package org.schabi.newpipe.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * Shows dialogs with the app look: for two buttons, equal width pills.
 */
public final class AppDialogs {
    private AppDialogs() {
    }

    public static AlertDialog show(@NonNull final AlertDialog.Builder builder) {
        final AlertDialog dialog = builder.create();
        dialog.show();
        style(dialog);
        return dialog;
    }

    public static void style(@NonNull final AlertDialog dialog) {
        final Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        final Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        final Button neu = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neg == null || pos == null || neg.getVisibility() != View.VISIBLE
                || pos.getVisibility() != View.VISIBLE
                || (neu != null && neu.getVisibility() == View.VISIBLE)) {
            return;
        }
        if (!(pos.getParent() instanceof LinearLayout) || neg.getParent() != pos.getParent()) {
            return;
        }
        final LinearLayout bar = (LinearLayout) pos.getParent();
        final float dens = dialog.getContext().getResources().getDisplayMetrics().density;
        for (int i = 0; i < bar.getChildCount(); i++) {
            final View child = bar.getChildAt(i);
            if (child != neg && child != pos) {
                child.setVisibility(View.GONE);
            }
        }
        for (final Button b : new Button[]{neg, pos}) {
            final ViewGroup.LayoutParams lp = b.getLayoutParams();
            if (lp instanceof LinearLayout.LayoutParams) {
                lp.width = 0;
                ((LinearLayout.LayoutParams) lp).weight = 1f;
                b.setLayoutParams(lp);
            }
        }
        bar.setPadding(bar.getPaddingLeft(), (int) (12 * dens),
                bar.getPaddingRight(), (int) (24 * dens));
    }
}
