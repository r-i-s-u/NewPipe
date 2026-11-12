package org.schabi.newpipe.settings;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.ThemeHelper;

/**
 * Shows an {@link EditTextPreference} in a bottom sheet with an outlined,
 * labelled field and a tick button instead of the default dialog.
 */
public class EditPreferenceBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "EditPreferenceBottomSheet";
    private static final float CORNER_RADIUS_DP = 10f;

    private EditTextPreference preference;

    public static void show(@NonNull final FragmentManager fragmentManager,
                            @NonNull final EditTextPreference preference) {
        final EditPreferenceBottomSheet sheet = new EditPreferenceBottomSheet();
        sheet.preference = preference;
        sheet.show(fragmentManager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View root = LayoutInflater.from(requireActivity())
                .inflate(R.layout.bottom_sheet_simple, container, false);
        root.setBackground(createBackground());
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (preference == null) {
            dismissAllowingStateLoss();
            return;
        }
        final float dens = getResources().getDisplayMetrics().density;

        final TextView title = view.findViewById(R.id.sheet_title);
        final CharSequence titleText = preference.getDialogTitle() != null
                ? preference.getDialogTitle() : preference.getTitle();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            title.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT, 600, false));
        } else {
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        title.setText(titleText);

        final ColorStateList primary = title.getTextColors();
        final ColorStateList secondary = ColorStateList.valueOf(
                (primary.getDefaultColor() & 0x00FFFFFF) | 0x99000000);

        final TextView label = new TextView(requireActivity());
        label.setText(titleText);
        label.setTextSize(12);
        label.setTextColor(primary);

        final EditText input = new EditText(requireActivity());
        input.setBackground(null);
        input.setPadding(0, (int) (2 * dens), 0, 0);
        input.setTextSize(16);
        input.setTextColor(primary);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        final String current = preference.getText() == null ? "" : preference.getText();
        input.setText(current);
        input.setSelection(current.length());

        final LinearLayout field = new LinearLayout(requireActivity());
        field.setOrientation(LinearLayout.VERTICAL);
        field.setBackgroundResource(R.drawable.bg_edit_field);
        field.setPadding((int) (16 * dens), (int) (8 * dens), (int) (16 * dens),
                (int) (10 * dens));
        field.addView(label);
        field.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        field.setActivated(true);
        field.setOnClickListener(v -> input.requestFocus());
        input.setOnFocusChangeListener((v, hasFocus) -> {
            field.setActivated(hasFocus);
            label.setTextColor(hasFocus ? primary : secondary);
        });

        final FrameLayout box = new FrameLayout(requireActivity());
        box.setPadding((int) (16 * dens), (int) (8 * dens), (int) (16 * dens),
                (int) (8 * dens));
        box.addView(field);
        ((ViewGroup) view.findViewById(R.id.sheet_content)).addView(box);

        final View tick = view.findViewById(R.id.sheet_tick);
        tick.setVisibility(View.VISIBLE);
        tick.setOnClickListener(v -> save(input.getText().toString()));
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save(input.getText().toString());
                return true;
            }
            return false;
        });

        input.postDelayed(() -> {
            input.requestFocus();
            final InputMethodManager imm = (InputMethodManager) input.getContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, 0);
            }
        }, 250);
    }

    private void save(@NonNull final String text) {
        if (preference != null && preference.callChangeListener(text)) {
            preference.setText(text);
        }
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            final BottomSheetDialog sheetDialog = (BottomSheetDialog) dialog;
            sheetDialog.setCanceledOnTouchOutside(true);
            final View sheet = sheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundColor(Color.TRANSPARENT);
            }
            if (sheetDialog.getWindow() != null) {
                sheetDialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            sheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            sheetDialog.getBehavior().setSkipCollapsed(true);
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
        if (!ThemeHelper.isLightThemeSelected(requireActivity())) {
            return 0xFF0F0F0F;
        }
        final Resources.Theme theme = requireActivity().getTheme();
        final TypedValue value = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorBackground, value, true)) {
            return value.data;
        }
        return Color.WHITE;
    }
}
