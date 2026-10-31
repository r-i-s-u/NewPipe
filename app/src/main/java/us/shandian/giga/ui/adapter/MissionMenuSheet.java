package us.shandian.giga.ui.adapter;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.schabi.newpipe.R;

public final class MissionMenuSheet {
    private MissionMenuSheet() { }

    public static void show(final Context context, final Menu menu,
                            final MenuItem.OnMenuItemClickListener listener) {
        final BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.DownloadSheetTheme);
        final Context sheetContext = dialog.getContext();
        final float density = sheetContext.getResources().getDisplayMetrics().density;
        final LinearLayout box = new LinearLayout(sheetContext);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, Math.round(8 * density));

        final View handle = new View(sheetContext);
        final LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
                Math.round(36 * density), Math.round(4 * density));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = Math.round(8 * density);
        handleParams.bottomMargin = Math.round(8 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackgroundResource(R.drawable.bg_handle);
        box.addView(handle);

        final ScrollView scroll = new ScrollView(sheetContext);
        scroll.setFillViewport(false);
        final LinearLayout rows = new LinearLayout(sheetContext);
        rows.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < menu.size(); i++) {
            final MenuItem item = menu.getItem(i);
            if (!item.isVisible() || item.getTitle() == null) {
                continue;
            }
            final TextView row = new TextView(sheetContext, null, 0, R.style.MissionMenuRow);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setMinHeight(Math.round(48 * density));
            row.setPaddingRelative(Math.round(24 * density), Math.round(8 * density),
                    Math.round(24 * density), Math.round(8 * density));
            final boolean checked = item.isCheckable() && item.isChecked();
            row.setText(checked ? "\u2713  " + item.getTitle() : item.getTitle());
            row.setEnabled(item.isEnabled());
            row.setOnClickListener(v -> {
                dialog.dismiss();
                listener.onMenuItemClick(item);
            });
            rows.addView(row);
        }
        scroll.addView(rows);
        box.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(box);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.show();
    }
}
