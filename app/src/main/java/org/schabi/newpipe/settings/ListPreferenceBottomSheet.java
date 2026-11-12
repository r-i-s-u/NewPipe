package org.schabi.newpipe.settings;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.schabi.newpipe.R;

/**
 * Shows the options of any {@link ListPreference} in a bottom sheet
 * (drag handle, title, close button, radio list) instead of the default dialog.
 * Colors come from the app theme, so it follows light/dark/black themes.
 */
public class ListPreferenceBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "ListPreferenceBottomSheet";
    private static final float CORNER_RADIUS_DP = 10f;

    private ListPreference preference;

    public static void show(@NonNull final FragmentManager fragmentManager,
                            @NonNull final ListPreference preference) {
        final ListPreferenceBottomSheet sheet = new ListPreferenceBottomSheet();
        sheet.preference = preference;
        sheet.show(fragmentManager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        // Inflate with the activity theme so text/radio colors match the app theme
        final View root = LayoutInflater.from(requireActivity())
                .inflate(R.layout.bottom_sheet_list_preference, container, false);
        root.setBackground(createBackground());
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (preference == null
                || preference.getEntries() == null
                || preference.getEntryValues() == null) {
            // Preference is lost after process/config recreation
            dismissAllowingStateLoss();
            return;
        }

        final TextView title = view.findViewById(R.id.lpbs_title);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            title.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT, 600, false));
        } else {
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        title.setText(preference.getDialogTitle() != null
                ? preference.getDialogTitle() : preference.getTitle());

        final ImageButton close = view.findViewById(R.id.lpbs_close);
        ImageViewCompat.setImageTintList(close, title.getTextColors());
        close.setOnClickListener(v -> dismiss());

        final LinearLayout options = view.findViewById(R.id.lpbs_options);
        final LayoutInflater inflater = LayoutInflater.from(requireActivity());
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        final String current = preference.getValue();

        for (int i = 0; i < entries.length; i++) {
            final String value = values[i].toString();
            final View row = inflater.inflate(R.layout.item_bottom_sheet_option, options, false);
            ((TextView) row.findViewById(R.id.lpbs_option_title)).setText(entries[i]);
            ((CompoundButton) row.findViewById(R.id.lpbs_radio))
                    .setChecked(value.equals(current));
            row.setOnClickListener(v -> {
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                }
                dismiss();
            });
            options.addView(row);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            final BottomSheetDialog sheetDialog = (BottomSheetDialog) dialog;
            sheetDialog.setCanceledOnTouchOutside(false);
            final View sheet = sheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                // Our root draws the themed, rounded background
                sheet.setBackgroundColor(Color.TRANSPARENT);
            }
            final android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            final int sbId = getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            final int sbH = sbId > 0 ? getResources().getDimensionPixelSize(sbId) : 0;
            final int sheetH = dm.heightPixels - sbH - (int) (48 * dm.density);
            if (sheet != null && sheet.getLayoutParams() != null) {
                sheet.getLayoutParams().height = sheetH;
                sheet.requestLayout();
            }
            final View content = getView();
            if (content != null) {
                if (content.getLayoutParams() != null) {
                    content.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                }
                final View opts = content.findViewById(R.id.lpbs_options);
                if (opts != null && opts.getParent() instanceof View) {
                    final View scroller = (View) opts.getParent();
                    final ViewGroup.LayoutParams slp = scroller.getLayoutParams();
                    if (slp instanceof LinearLayout.LayoutParams) {
                        slp.height = 0;
                        ((LinearLayout.LayoutParams) slp).weight = 1f;
                        scroller.setLayoutParams(slp);
                    }
                }
            }
            sheetDialog.getBehavior().setSkipCollapsed(true);
            sheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private GradientDrawable createBackground() {
        final float radius = CORNER_RADIUS_DP * getResources().getDisplayMetrics().density;
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(resolveBackgroundColor());
        drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        return drawable;
    }

    private int resolveBackgroundColor() {
        if (!org.schabi.newpipe.util.ThemeHelper.isLightThemeSelected(requireActivity())) {
            return 0xFF0F0F0F;
        }
        final Resources.Theme theme = requireActivity().getTheme();
        final TypedValue value = new TypedValue();
        final int[] attrs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? new int[]{android.R.attr.colorBackground}
                : new int[]{android.R.attr.colorBackground};
        for (final int attr : attrs) {
            if (theme.resolveAttribute(attr, value, true)
                    && value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
        }
        return Color.parseColor("#222222");
    }
}
