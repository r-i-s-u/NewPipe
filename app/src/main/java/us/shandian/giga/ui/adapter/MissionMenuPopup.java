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
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.schabi.newpipe.R;

public final class MissionMenuPopup {
    private MissionMenuPopup() { }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.S)
    private static void setBlur(final View v, final float r) {
        v.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                r, r, android.graphics.Shader.TileMode.CLAMP));
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.S)
    private static void blurBackground(final Context ctx, final PopupWindow pw) {
        android.app.Activity act = null;
        Context c = ctx;
        while (c instanceof android.content.ContextWrapper) {
            if (c instanceof android.app.Activity) {
                act = (android.app.Activity) c;
                break;
            }
            c = ((android.content.ContextWrapper) c).getBaseContext();
        }
        if (act == null) {
            return;
        }
        final View target = act.getWindow().getDecorView();
        final float max = dp(ctx, 10);
        final android.animation.ValueAnimator in =
                android.animation.ValueAnimator.ofFloat(0.01f, max);
        in.setDuration(200);
        in.addUpdateListener(an -> setBlur(target, (float) an.getAnimatedValue()));
        in.start();
        pw.setOnDismissListener(() -> {
            in.cancel();
            final android.animation.ValueAnimator out =
                    android.animation.ValueAnimator.ofFloat(max, 0.01f);
            out.setDuration(150);
            out.addUpdateListener(an -> setBlur(target, (float) an.getAnimatedValue()));
            out.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final android.animation.Animator a) {
                    target.setRenderEffect(null);
                }
            });
            out.start();
        });
    }

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

        boolean anyIcon = false;
        for (int i = 0; i < menu.size(); i++) {
            final MenuItem it = menu.getItem(i);
            if (it.isVisible() && it.getIcon() != null) {
                anyIcon = true;
            }
        }

        final PopupWindow pw = new PopupWindow(ctx);
        final LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(ctx, 14), 0, dp(ctx, 14));
        box.setMinimumWidth(dp(ctx, 160));

        for (int i = 0; i < menu.size(); i++) {
            final MenuItem item = menu.getItem(i);
            if (!item.isVisible() || item.getTitle() == null) {
                continue;
            }
            final LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 16), 0, dp(ctx, 20), 0);
            row.setMinimumHeight(dp(ctx, 48));
            row.setBackgroundResource(rip.resourceId);

            final Drawable ic = item.getIcon();
            if (ic != null) {
                final ImageView iv = new ImageView(ctx);
                iv.setImageDrawable(ic.mutate());
                iv.setColorFilter(fg);
                row.addView(iv, new LinearLayout.LayoutParams(dp(ctx, 24), dp(ctx, 24)));
            } else if (anyIcon) {
                row.addView(new View(ctx), new LinearLayout.LayoutParams(dp(ctx, 24), 1));
            }
            final boolean checked = item.isCheckable() && item.isChecked();
            final TextView t = new TextView(ctx);
            t.setText(checked ? "\u2713  " + item.getTitle() : item.getTitle());
            t.setTextColor(fg);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(anyIcon ? dp(ctx, 16) : 0);
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
        bg.setCornerRadius(dp(ctx, 20));

        final ScrollView scroll = new ScrollView(ctx);
        scroll.setBackground(bg);
        scroll.setClipToOutline(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        scroll.addView(box);

        // position: kone se 16dp door, jagah na ho to upar flip
        final View root = anchor.getRootView();
        final int sw = root.getWidth();
        final int sh = root.getHeight();
        final int margin = dp(ctx, 16);
        final WindowInsetsCompat wi = ViewCompat.getRootWindowInsets(anchor);
        final int topIn = wi != null
                ? wi.getInsets(WindowInsetsCompat.Type.systemBars()).top : 0;
        final int botIn = wi != null
                ? wi.getInsets(WindowInsetsCompat.Type.systemBars()).bottom : 0;
        scroll.measure(
                View.MeasureSpec.makeMeasureSpec(sw - 2 * margin, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(sh - topIn - botIn - 2 * margin,
                        View.MeasureSpec.AT_MOST));
        final int w = scroll.getMeasuredWidth();
        final int h = scroll.getMeasuredHeight();
        final int[] loc = new int[2];
        anchor.getLocationInWindow(loc);

        int x = loc[0] + anchor.getWidth() - w;
        x = Math.max(margin, Math.min(x, sw - w - margin));
        final boolean below = loc[1] + anchor.getHeight() + h <= sh - botIn - margin;
        int y = below ? loc[1] + anchor.getHeight() : loc[1] - h;
        y = Math.max(topIn + margin, Math.min(y, sh - botIn - h - margin));

        pw.setContentView(scroll);
        pw.setWidth(w);
        pw.setHeight(h);
        pw.setFocusable(true);
        pw.setOutsideTouchable(true);
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // spread animation: three-dot ki taraf se phailta hai (neeche ya upar)
        scroll.setPivotX(Math.max(0f, Math.min(w, loc[0] + anchor.getWidth() / 2f - x)));
        scroll.setPivotY(below ? 0f : h);
        scroll.setAlpha(0f);
        scroll.setScaleX(0.6f);
        scroll.setScaleY(0.3f);
        pw.showAtLocation(anchor, Gravity.TOP | Gravity.START, x, y);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            blurBackground(ctx, pw);
        }
        scroll.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
                .setInterpolator(new DecelerateInterpolator()).start();
    }
}
