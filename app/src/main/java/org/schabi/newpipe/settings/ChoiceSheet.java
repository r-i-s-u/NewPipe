package org.schabi.newpipe.settings;

import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.schabi.newpipe.R;

/** Bottom sheet replacement for ListPreference dialogs (YouTube Studio style). */
public class ChoiceSheet extends BottomSheetDialogFragment {
    private static final String KEY = "pref_key";

    public static ChoiceSheet newInstance(final String key) {
        final ChoiceSheet sheet = new ChoiceSheet();
        final Bundle args = new Bundle();
        args.putString(KEY, key);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.DownloadSheetTheme);
    }

    @Nullable
    private ListPreference preference() {
        final Fragment parent = getParentFragment();
        final String key = getArguments() == null ? null : getArguments().getString(KEY);
        if (key != null && parent instanceof PreferenceFragmentCompat) {
            final Preference p = ((PreferenceFragmentCompat) parent).findPreference(key);
            if (p instanceof ListPreference) {
                return (ListPreference) p;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.sheet_choice, container, false);
        final ListPreference pref = preference();
        final CharSequence[] entries = pref == null ? null : pref.getEntries();
        final CharSequence[] values = pref == null ? null : pref.getEntryValues();
        if (pref == null || entries == null || values == null) {
            root.post(this::dismissAllowingStateLoss);
            return root;
        }

        final CharSequence title = pref.getDialogTitle() != null
                ? pref.getDialogTitle() : pref.getTitle();
        ((TextView) root.findViewById(R.id.choice_title)).setText(title);
        root.findViewById(R.id.choice_close)
                .setOnClickListener(v -> dismissAllowingStateLoss());

        final String current = pref.getValue();
        final LinearLayout list = root.findViewById(R.id.choice_list);
        final int n = Math.min(entries.length, values.length);
        final RadioButton[] radios = new RadioButton[n];

        for (int i = 0; i < n; i++) {
            final View row = inflater.inflate(R.layout.sheet_choice_row, list, false);
            ((TextView) row.findViewById(R.id.choice_text)).setText(entries[i]);

            final RadioButton radio = row.findViewById(R.id.choice_radio);
            radio.setChecked(values[i].toString().equals(current));
            radios[i] = radio;

            final int index = i;
            row.setOnClickListener(v -> {
                for (final RadioButton r : radios) {
                    r.setChecked(r == radio);
                }

                final String value = values[index].toString();
                if (pref.callChangeListener(value)) {
                    pref.setValue(value);
                }

                v.postDelayed(() -> {
                    if (isAdded()) {
                        dismissAllowingStateLoss();
                    }
                }, 140);
            });
            list.addView(row);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final int bottom = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bottom);
            return insets;
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() instanceof BottomSheetDialog) {
            final BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            final View sheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);

            if (sheet != null) {
                sheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;

                final float radius = 28
                        * getResources().getDisplayMetrics().density;
                final GradientDrawable background = new GradientDrawable();
                background.setColor(
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(), R.color.dl_bottom_bg));
                background.setCornerRadii(new float[]{
                        radius, radius, radius, radius,
                        0, 0, 0, 0
                });

                sheet.setBackground(background);
                sheet.setClipToOutline(true);
                sheet.requestLayout();
            }

            final boolean portrait = getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT;
            final float density = getResources().getDisplayMetrics().density;

            dialog.getBehavior().setFitToContents(false);
            dialog.getBehavior().setExpandedOffset((int) (48 * density));
            dialog.getBehavior().setState(
                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
            dialog.getBehavior().setMaxWidth(-1);

            final Window window = dialog.getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.5f);
            }
        }
    }
}
