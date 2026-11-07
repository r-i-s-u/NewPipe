package org.schabi.newpipe;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import org.schabi.newpipe.databinding.ActivityDownloaderBinding;
import org.schabi.newpipe.util.DeviceUtils;
import org.schabi.newpipe.util.ThemeHelper;

import us.shandian.giga.service.DownloadManagerService;
import us.shandian.giga.ui.fragment.MissionsFragment;

public class MainActivity extends AppCompatActivity {

    private static final String MISSIONS_FRAGMENT_TAG = "fragment_tag";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // Service
        final Intent i = new Intent();
        i.setClass(this, DownloadManagerService.class);
        startService(i);

        ThemeHelper.setTheme(this);
        getTheme().applyStyle(R.style.DownloadsActionMode, true);

        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }

        final ActivityDownloaderBinding downloaderBinding =
                ActivityDownloaderBinding.inflate(getLayoutInflater());
        setContentView(downloaderBinding.getRoot());

        setSupportActionBar(downloaderBinding.toolbarLayout.toolbar);

        downloaderBinding.toolbarLayout.toolbar.setTitleMarginStart(0);
        downloaderBinding.toolbarLayout.toolbar.setTitleTextAppearance(
                this, R.style.DownloadsTitleExpanded);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.videos_title);
        }

        getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateFragments();
                getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    private void updateFragments() {
        final MissionsFragment fragment = new MissionsFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, fragment, MISSIONS_FRAGMENT_TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public void setKindTitle(final boolean audio) {
        final ActionBar bar = getSupportActionBar();
        final com.google.android.material.appbar.AppBarLayout appBar =
                findViewById(R.id.app_bar);
        if (appBar != null) {
            appBar.setExpanded(true, false);
        }
        if (bar == null) {
            return;
        }
        final androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        android.widget.TextView tv = null;
        if (tb != null) {
            for (int i = 0; i < tb.getChildCount(); i++) {
                if (tb.getChildAt(i) instanceof android.widget.TextView) {
                    tv = (android.widget.TextView) tb.getChildAt(i);
                    break;
                }
            }
        }
        final int res = audio ? R.string.audio : R.string.videos_title;
        if (tv == null) {
            bar.setTitle(res);
            return;
        }
        final android.widget.TextView t = tv;
        final float d = (audio ? 1f : -1f) * 32 * getResources().getDisplayMetrics().density;
        t.animate().cancel();
        t.animate().alpha(0f).translationX(-d).setDuration(90)
                .setInterpolator(new android.view.animation.PathInterpolator(0.4f, 0f, 1f, 1f))
                .withEndAction(() -> {
                    bar.setTitle(res);
                    t.setTranslationX(d);
                    t.animate().alpha(1f).translationX(0f).setDuration(210)
                            .setInterpolator(new android.view.animation.PathInterpolator(
                                    0f, 0f, 0.2f, 1f))
                            .start();
                }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        final MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.download_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.download_settings) {
            startActivity(new android.content.Intent(this, org.schabi.newpipe.settings.SettingsActivity.class));
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
        }
    }
