package us.shandian.giga.ui.adapter;

import android.content.Context;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.schabi.newpipe.R;

public final class MissionMenuSheet {
    private MissionMenuSheet() { }

    public static void show(final Context ctx, final Menu menu,
                            final MenuItem.OnMenuItemClickListener listener) {
        final BottomSheetDialog d = new BottomSheetDialog(ctx);
        final LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        final int pad = (int) (12 * ctx.getResources().getDisplayMetrics().density);
        box.setPadding(0, pad, 0, pad * 2);

        for (int i = 0; i < menu.size(); i++) {
            final MenuItem item = menu.getItem(i);
            if (!item.isVisible() || item.getTitle() == null) {
                continue;
            }
            final TextView row = new TextView(ctx, null, 0, R.style.MissionMenuRow);
            row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (56 * ctx.getResources().getDisplayMetrics().density)));
            final boolean checked = item.isCheckable() && item.isChecked();
            row.setText(checked ? "\u2713  " + item.getTitle() : item.getTitle());
            row.setOnClickListener(v -> {
                d.dismiss();
                listener.onMenuItemClick(item);
            });
            box.addView(row);
        }
        d.setContentView(box);

        d.setOnShowListener(x -> {
            final View sheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundResource(R.drawable.sheet_glass_bg);
            }
            final Window w = d.getWindow();
            if (w != null) {
                w.setDimAmount(0.3f);
            }
        });
        d.show();
    }
}
