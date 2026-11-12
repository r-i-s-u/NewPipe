package org.schabi.newpipe.util;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.schabi.newpipe.R;

/**
 * "New folder" bottom sheet for the file picker: title, plain text field,
 * close button and a Done pill above the keyboard.
 */
public class NewFolderBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "NewFolderBottomSheet";
    private static final float CORNER_RADIUS_DP = 10f;

    public interface OnName {
        void onName(@NonNull String name);
    }

    private OnName callback;

    public static void show(@NonNull final FragmentManager fragmentManager,
                            @NonNull final OnName callback) {
        final NewFolderBottomSheet sheet = new NewFolderBottomSheet();
        sheet.callback = callback;
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
        if (callback == null) {
            dismissAllowingStateLoss();
            return;
        }
        final float dens = getResources().getDisplayMetrics().density;

        final TextView title = view.findViewById(R.id.sheet_title);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            title.setTypeface(Typeface.create(Typeface.DEFAULT, 600, false));
        } else {
            title.setTypeface(Typeface.DEFAULT_BOLD);
        }
        title.setText(getString(com.nononsenseapps.filepicker.R.string.nnf_new_folder));

        final ColorStateList primary = title.getTextColors();
        final ColorStateList secondary = ColorStateList.valueOf(
                (primary.getDefaultColor() & 0x00FFFFFF) | 0x99000000);

        final EditText input = new EditText(requireActivity());
        input.setBackground(null);
        input.setPadding(0, (int) (2 * dens), 0, 0);
        input.setTextSize(18);
        input.setTextColor(primary);
        input.setHintTextColor(secondary);
        input.setHint(getString(com.nononsenseapps.filepicker.R.string.nnf_name));
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            final GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(primary.getDefaultColor());
            cursor.setSize((int) (2 * dens), (int) (24 * dens));
            input.setTextCursorDrawable(cursor);
        }
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit(input);
                return true;
            }
            return false;
        });

        final FrameLayout box = new FrameLayout(requireActivity());
        box.setPadding((int) (16 * dens), (int) (8 * dens), (int) (16 * dens),
                (int) (8 * dens));
        box.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((ViewGroup) view.findViewById(R.id.sheet_content)).addView(box);

        final ImageButton close = view.findViewById(R.id.sheet_tick);
        close.setVisibility(View.VISIBLE);
        close.setImageResource(R.drawable.ic_close);
        close.setOnClickListener(v -> dismiss());

        if (view instanceof LinearLayout) {
            final LinearLayout rootLayout = (LinearLayout) view;
            final View sc = view.findViewById(R.id.sheet_content);
            if (sc != null && sc.getParent() instanceof View) {
                final View scroller = (View) sc.getParent();
                final ViewGroup.LayoutParams slp = scroller.getLayoutParams();
                if (slp instanceof LinearLayout.LayoutParams) {
                    slp.height = 0;
                    ((LinearLayout.LayoutParams) slp).weight = 1f;
                    scroller.setLayoutParams(slp);
                }
            }
            rootLayout.setPadding(rootLayout.getPaddingLeft(), rootLayout.getPaddingTop(),
                    rootLayout.getPaddingRight(), 0);

            final View divider = new View(requireActivity());
            divider.setBackgroundColor(ContextCompat.getColor(
                    requireActivity(), R.color.stroke_color));
            rootLayout.addView(divider, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) dens)));

            final TextView done = new TextView(requireActivity());
            done.setText("Done");
            done.setTextSize(16);
            done.setGravity(Gravity.CENTER);
            done.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            done.setTextColor(ContextCompat.getColor(
                    requireActivity(), R.color.dialog_btn_primary_text));
            final GradientDrawable pill = new GradientDrawable();
            pill.setColor(ContextCompat.getColor(
                    requireActivity(), R.color.dialog_btn_primary_bg));
            pill.setCornerRadius(100 * dens);
            done.setBackground(pill);
            done.setClickable(true);
            done.setOnClickListener(v -> submit(input));
            done.setOnTouchListener((v, ev) -> {
                final int a = ev.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                }
                return false;
            });
            final LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (40 * dens));
            dlp.setMargins((int) (12 * dens), (int) (12 * dens), (int) (12 * dens),
                    (int) (12 * dens));
            rootLayout.addView(done, dlp);
        }

        input.postDelayed(() -> {
            input.requestFocus();
            final InputMethodManager imm = (InputMethodManager) input.getContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, 0);
            }
        }, 250);
    }

    private void submit(@NonNull final EditText input) {
        final String name = input.getText().toString().trim();
        if (name.isEmpty() || name.contains("/") || ".".equals(name) || "..".equals(name)) {
            return;
        }
        final OnName cb = callback;
        dismissAllowingStateLoss();
        if (cb != null) {
            cb.onName(name);
        }
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
            final DisplayMetrics dm = getResources().getDisplayMetrics();
            final int sbId = getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            final int topGap = (sbId > 0 ? getResources().getDimensionPixelSize(sbId) : 0)
                    + (int) (48 * dm.density);
            final View rootView = getView();
            if (rootView != null && rootView.getLayoutParams() != null) {
                rootView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            final View sheetParent = sheet != null && sheet.getParent() instanceof View
                    ? (View) sheet.getParent() : null;
            if (sheet != null && sheetParent != null) {
                final Runnable applyHeight = () -> {
                    final int[] pl = new int[2];
                    sheetParent.getLocationOnScreen(pl);
                    final int h = pl[1] + sheetParent.getHeight() - topGap;
                    if (h > 0 && sheet.getLayoutParams() != null
                            && sheet.getLayoutParams().height != h) {
                        sheet.getLayoutParams().height = h;
                        sheet.requestLayout();
                    }
                };
                applyHeight.run();
                sheetParent.addOnLayoutChangeListener(
                        (v, l, t, r, b, oL, oT, oR, oB) -> applyHeight.run());
                sheetParent.getViewTreeObserver().addOnGlobalLayoutListener(applyHeight::run);
                sheetParent.post(applyHeight);
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
