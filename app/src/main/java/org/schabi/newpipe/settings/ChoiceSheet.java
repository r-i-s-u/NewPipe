package org.schabi.newpipe.settings;

import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import org.schabi.newpipe.R;

/** Bottom sheet replacement for ListPreference dialogs (YouTube Studio style). */
public class ChoiceSheet extends BottomSheetDialogFragment {
    private static final String KEY = "pref_key";
    private static final float CORNER_DP = 24f;
    private static final float TOP_OFFSET_DP = 48f;

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
        setStyle(STYLE_NORMAL, R.style.ChoiceSheetTheme);
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
            final int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bottom);
            return insets;
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!(getDialog() instanceof BottomSheetDialog)) {
            return;
        }
        final BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        final float density = getResources().getDisplayMetrics().density;

        final View sheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;

            final float radius = CORNER_DP * density;
            final MaterialShapeDrawable background = new MaterialShapeDrawable(
                    new ShapeAppearanceModel().toBuilder()
                            .setTopLeftCornerSize(radius)
                            .setTopRightCornerSize(radius)
                            .setBottomLeftCornerSize(0)
                            .setBottomRightCornerSize(0)
                            .build());
            background.setFillColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.dl_bottom_bg)));

            sheet.setBackground(background);
            sheet.setClipToOutline(true);
            sheet.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (v.getBackground() != background) {
                    v.setBackground(background);
                }
            });
            sheet.requestLayout();
        }

        final BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setFitToContents(false);
        behavior.setExpandedOffset((int) (TOP_OFFSET_DP * density));
        behavior.setSkipCollapsed(true);
        behavior.setMaxWidth(-1);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        final Window window = dialog.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
    }
}
