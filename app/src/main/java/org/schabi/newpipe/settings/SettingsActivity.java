package org.schabi.newpipe.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.ThemeHelper;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(ThemeHelper.getSettingsThemeStyle(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        findViewById(R.id.settings_back).setOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            final FragmentTransaction transaction =
                    getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.settings_fragment_holder, new SettingsFragment());
            transaction.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
