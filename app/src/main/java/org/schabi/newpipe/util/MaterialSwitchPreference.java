import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.google.android.material.materialswitch.MaterialSwitch;

public class MaterialSwitchPreference extends SwitchPreference {
    public MaterialSwitchPreference(Context c, AttributeSet a, int d, int r) { super(c, a, d, r); }
    public MaterialSwitchPreference(Context c, AttributeSet a, int d) { super(c, a, d); }
    public MaterialSwitchPreference(Context c, AttributeSet a) { super(c, a); }
    public MaterialSwitchPreference(Context c) { super(c); }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View v = holder.findViewById(android.R.id.switch_widget);
        if (v instanceof MaterialSwitch) {
            ((MaterialSwitch) v).setChecked(isChecked());
        }
    }
}
