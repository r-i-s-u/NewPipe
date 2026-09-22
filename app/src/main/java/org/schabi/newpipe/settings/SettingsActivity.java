package org.schabi.newpipe.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.SoftwareKeyboardControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.evernote.android.state.State;
import com.jakewharton.rxbinding4.widget.RxTextView;
import com.livefront.bridge.Bridge;

import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.R;
import org.schabi.newpipe.databinding.SettingsLayoutBinding;
import org.schabi.newpipe.util.DeviceUtils;
import org.schabi.newpipe.util.ReleaseVersionUtil;
import org.schabi.newpipe.util.ThemeHelper;
import org.schabi.newpipe.views.FocusOverlayView;

import java.util.concurrent.TimeUnit;

/*
 * Created by Christian Schabesberger on 31.08.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * SettingsActivity.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private static final String TAG = "SettingsActivity";
    private static final boolean DEBUG = MainActivity.DEBUG;

    @IdRes
    private static final int FRAGMENT_HOLDER_ID = R.id.settings_fragment_holder;



    @Override
    protected void onCreate(final Bundle savedInstanceBundle) {
        setTheme(ThemeHelper.getSettingsThemeStyle(this));

        super.onCreate(savedInstanceBundle);
        Bridge.restoreInstanceState(this, savedInstanceBundle);
        final boolean restored = savedInstanceBundle != null;


        final SettingsLayoutBinding settingsLayoutBinding =
                SettingsLayoutBinding.inflate(getLayoutInflater());
        setContentView(settingsLayoutBinding.getRoot());

        setSupportActionBar(settingsLayoutBinding.settingsToolbarLayout.toolbar);

        if (!restored) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment_holder, new MainSettingsFragment())
                    .commit();
        }

        if (DeviceUtils.isTv(this)) {
            FocusOverlayView.setupFocusObserver(this);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        Bridge.saveInstanceState(this, outState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                finish();
            } else {
                getSupportFragmentManager().popBackStack();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull final PreferenceFragmentCompat caller,
                                             final Preference preference) {
        showSettingsFragment(instantiateFragment(preference.getFragment()));
        return true;
    }

    private Fragment instantiateFragment(@NonNull final String className) {
        return getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(this.getClassLoader(), className);
    }

    private void showSettingsFragment(final Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out,
                        R.animator.custom_fade_in, R.animator.custom_fade_out)
                .replace(FRAGMENT_HOLDER_ID, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void setMenuSearchItem(final MenuItem menuSearchItem) {
    }

    public void setSearchActive(final boolean active) {
    }
}
