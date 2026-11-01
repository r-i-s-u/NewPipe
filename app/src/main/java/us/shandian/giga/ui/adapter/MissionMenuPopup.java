package us.shandian.giga.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.schabi.newpipe.R;

public final class MissionMenuPopup {
    private MissionMenuPopup() { }

    private static int dp(final Context c, final float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static void show(final View anchor, final Menu menu,
                            final MenuItem.OnMenuItemClickListener listener) {
        final Context ctx = anchor.getContext();
        final TypedValue fgv = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.textColorPrimary, fgv, true);
        final int fg = fgv.resourceId != 0
                ? ContextCompat.getColor(ctx, fgv.resourceId) : fgv.data;
        final TypedValue rip = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rip, true);

        final PopupWindow pw = new PopupWindow(ctx);
        final LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(ctx, 8), 0, dp(ctx, 8));
        box.setMinimumWidth(dp(ctx, 220));

        for (int i = 0; i < menu.size(); i++) {
            final MenuItem item = menu.getItem(i);
            if (!item.isVisible() || item.getTitle() == null) {
                continue;
            }
            final LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 20), 0, dp(ctx, 28), 0);
            row.setMinimumHeight(dp(ctx, 48));
            row.setBackgroundResource(rip.resourceId);

            final Drawable ic = item.getIcon();
            if (ic != null) {
                final ImageView iv = new ImageView(ctx);
                iv.setImageDrawable(ic.mutate());
                iv.setColorFilter(fg);
                row.addView(iv, new LinearLayout.LayoutParams(dp(ctx, 24), dp(ctx, 24)));
            }
            final boolean checked = item.isCheckable() && item.isChecked();
            final TextView t = new TextView(ctx);
            t.setText(checked ? "\u2713  " + item.getTitle() : item.getTitle());
            t.setTextColor(fg);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(ic != null ? dp(ctx, 16) : 0);
            row.addView(t, lp);

            row.setOnClickListener(v -> {
                pw.dismiss();
                listener.onMenuItemClick(item);
            });
            box.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        final GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(ctx, R.color.mission_popup_bg));
        bg.setCornerRadius(dp(ctx, 24));
        bg.setStroke(dp(ctx, 1), (fg & 0x00FFFFFF) | 0x1F000000);

        final ScrollView scroll = new ScrollView(ctx);
        scroll.setBackground(bg);
        scroll.setClipToOutline(true);
        scroll.addView(box);

        pw.setContentView(scroll);
        pw.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        pw.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        pw.setFocusable(true);
        pw.setOutsideTouchable(true);
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.showAsDropDown(anchor, -dp(ctx, 8), 0, Gravity.END);
    }
}
