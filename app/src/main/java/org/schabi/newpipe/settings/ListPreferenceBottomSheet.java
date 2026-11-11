package org.schabi.newpipe.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.schabi.newpipe.R;

/**
 * Shows the options of a {@link ListPreference} in a bottom sheet
 * (drag handle, title, close button, radio list) instead of the default dialog.
 */
public class ListPreferenceBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "ListPreferenceBottomSheet";

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
        return inflater.inflate(R.layout.bottom_sheet_list_preference, container, false);
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
        title.setText(preference.getDialogTitle() != null
                ? preference.getDialogTitle() : preference.getTitle());
        view.findViewById(R.id.lpbs_close).setOnClickListener(v -> dismiss());

        final LinearLayout options = view.findViewById(R.id.lpbs_options);
        final LayoutInflater inflater = LayoutInflater.from(requireContext());
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        final String current = preference.getValue();

        for (int i = 0; i < entries.length; i++) {
            final String value = values[i].toString();
            final View row = inflater.inflate(R.layout.item_bottom_sheet_option, options, false);
            ((TextView) row.findViewById(R.id.lpbs_option_title)).setText(entries[i]);
            ((RadioButton) row.findViewById(R.id.lpbs_radio))
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
            ((BottomSheetDialog) dialog).getBehavior()
                    .setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}
