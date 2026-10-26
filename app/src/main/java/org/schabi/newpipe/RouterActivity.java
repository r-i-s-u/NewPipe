package org.schabi.newpipe;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.math.MathUtils;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;

import com.evernote.android.state.State;
import com.livefront.bridge.Bridge;

import org.schabi.newpipe.databinding.ListRadioIconItemBinding;
import org.schabi.newpipe.databinding.SingleChoiceDialogViewBinding;
import org.schabi.newpipe.download.DownloadDialog;
import org.schabi.newpipe.download.LoadingDialog;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.StreamingService.LinkType;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.ExtractorHelper;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.PermissionHelper;
import org.schabi.newpipe.util.ThemeHelper;
import org.schabi.newpipe.util.external_communication.ShareUtils;
import org.schabi.newpipe.util.urlfinder.UrlFinder;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Get the url from the intent and open it in the chosen preferred player.
 */
public class RouterActivity extends AppCompatActivity {
    protected final CompositeDisposable disposables = new CompositeDisposable();
    @State
    protected int currentServiceId = -1;
    @State
    protected LinkType currentLinkType;
    @State
    protected int selectedRadioPosition = -1;
    protected int selectedPreviously = -1;
    protected String currentUrl;
    private StreamingService currentService;
    private boolean selectionIsDownload = false;
    private AlertDialog alertDialogChoice = null;
    private FragmentManager.FragmentLifecycleCallbacks dismissListener = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeHelper.setDayNightMode(this);
        setTheme(ThemeHelper.isLightThemeSelected(this)
                ? R.style.RouterActivityThemeLight : R.style.RouterActivityThemeDark);

        // Pass-through touch events to background activities
        // so that our transparent window won't lock UI in the mean time
        // network request is underway before showing PlaylistDialog or DownloadDialog
        // (ref: https://stackoverflow.com/a/10606141)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        // Android never fails to impress us with a list of new restrictions per API.
        // Starting with S (Android 12) one of the prerequisite conditions has to be met
        // before the FLAG_NOT_TOUCHABLE flag is allowed to kick in:
        // @see WindowManager.LayoutParams#FLAG_NOT_TOUCHABLE
        // For our present purpose it seems we can just set LayoutParams.alpha to 0
        // on the strength of "4. Fully transparent windows" without affecting the scrim of dialogs
        final WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0f;
        getWindow().setAttributes(params);

        super.onCreate(savedInstanceState);
        Bridge.restoreInstanceState(this, savedInstanceState);

        // FragmentManager will take care to recreate (Playlist|Download)Dialog when screen rotates
        // We used to .setOnDismissListener(dialog -> finish()); when creating these DialogFragments
        // but those callbacks won't survive a config change
        // Try an alternate approach to hook into FragmentManager instead, to that effect
        // (ref: https://stackoverflow.com/a/44028453)
        final FragmentManager fm = getSupportFragmentManager();
        if (dismissListener == null) {
            dismissListener = new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentDestroyed(@NonNull final FragmentManager fm,
                                                @NonNull final Fragment f) {
                    super.onFragmentDestroyed(fm, f);
                    if (f instanceof DialogFragment && fm.getFragments().isEmpty()) {
                        // No more DialogFragments, we're done
                        finish();
                    }
                }
            };
        }
        fm.registerFragmentLifecycleCallbacks(dismissListener, false);

        if (TextUtils.isEmpty(currentUrl)) {
            currentUrl = getUrl(getIntent());

            if (TextUtils.isEmpty(currentUrl)) {
                finish();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // we need to dismiss the dialog before leaving the activity or we get leaks
        if (alertDialogChoice != null) {
            alertDialogChoice.dismiss();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        Bridge.saveInstanceState(this, outState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Don't overlap the DialogFragment after rotating the screen
        // If there's no DialogFragment, we're either starting afresh
        // or we didn't make it to PlaylistDialog or DownloadDialog before the orientation change
        if (getSupportFragmentManager().getFragments().isEmpty()) {
            // Start over from scratch
            handleUrl(currentUrl);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dismissListener != null) {
            getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(dismissListener);
        }

        disposables.clear();
    }

    @Override
    public void finish() {
        // allow the activity to recreate in case orientation changes
        if (!isChangingConfigurations()) {
            super.finish();
        }
    }

    private void handleUrl(final String url) {
        disposables.add(Observable
                .fromCallable(() -> {
                    try {
                        if (currentServiceId == -1) {
                            currentService = NewPipe.getServiceByUrl(url);
                            currentServiceId = currentService.getServiceId();
                            currentLinkType = currentService.getLinkTypeByUrl(url);
                            currentUrl = url;
                        } else {
                            currentService = NewPipe.getService(currentServiceId);
                        }

                        // return whether the url was found to be supported or not
                        return currentLinkType != LinkType.NONE;
                    } catch (final ExtractionException e) {
                        // this can be reached only when the url is completely unsupported
                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isUrlSupported -> {
                    if (isUrlSupported) {
                        onSuccess();
                    } else {
                        showUnsupportedUrlDialog(url);
                    }
                }, throwable -> handleError(this, null)));
    }

    /**
     * @param context the context. It will be {@code finish()}ed at the end of the handling if it is
     *                an instance of {@link RouterActivity}.
     * @param errorInfo the error information
     */
    private static void handleError(final Context context, final Object errorInfo) {
        if (context instanceof RouterActivity) {
            ((RouterActivity) context).finish();
        }
    }

    protected void showUnsupportedUrlDialog(final String url) {
        final Context context = getThemeWrapperContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.unsupported_url)
                .setMessage(R.string.unsupported_url_dialog_message)
                .setPositiveButton(R.string.open_in_browser,
                        (dialog, which) -> ShareUtils.openUrlInBrowser(this, url))
                .setNegativeButton(R.string.share,
                        (dialog, which) -> ShareUtils.shareText(this, "", url)) // no subject
                .setNeutralButton(R.string.cancel, null)
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    protected void onSuccess() {
        final SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        final ChoiceAvailabilityChecker choiceChecker = new ChoiceAvailabilityChecker(
                getChoicesForService(currentLinkType),
                preferences.getString(getString(R.string.download_key),
                        getString(R.string.download_key)));

        // Check for non-player related choices
        if (choiceChecker.isAvailableAndSelected(
                R.string.show_info_key,
                R.string.download_key)) {
            handleChoice(choiceChecker.getSelectedChoiceKey());
            return;
        }
        // Default / Ask always
        final List<AdapterChoiceItem> availableChoices = choiceChecker.getAvailableChoices();
        switch (availableChoices.size()) {
            case 1:
                handleChoice(availableChoices.get(0).key);
                break;
            case 0:
                handleChoice(getString(R.string.show_info_key));
                break;
            default:
                showDialog(availableChoices);
                break;
        }
    }

    /**
     * This is a helper class for checking if the choices are available and/or selected.
     */
    class ChoiceAvailabilityChecker {
        private final List<AdapterChoiceItem> availableChoices;
        private final String selectedChoiceKey;

        ChoiceAvailabilityChecker(
                @NonNull final List<AdapterChoiceItem> availableChoices,
                @NonNull final String selectedChoiceKey) {
            this.availableChoices = availableChoices;
            this.selectedChoiceKey = selectedChoiceKey;
        }

        public List<AdapterChoiceItem> getAvailableChoices() {
            return availableChoices;
        }

        public String getSelectedChoiceKey() {
            return selectedChoiceKey;
        }

        public boolean isAvailableAndSelected(@StringRes final int... wantedKeys) {
            return Arrays.stream(wantedKeys).anyMatch(this::isAvailableAndSelected);
        }

        public boolean isAvailableAndSelected(@StringRes final int wantedKey) {
            final String wanted = getString(wantedKey);
            // Check if the wanted option is selected
            if (!selectedChoiceKey.equals(wanted)) {
                return false;
            }
            // Check if it's available
            return availableChoices.stream().anyMatch(item -> wanted.equals(item.key));
        }
    }

    private void showDialog(final List<AdapterChoiceItem> choices) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final Context themeWrapperContext = getThemeWrapperContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(themeWrapperContext);

        final SingleChoiceDialogViewBinding binding =
                SingleChoiceDialogViewBinding.inflate(layoutInflater);
        final RadioGroup radioGroup = binding.list;

        final DialogInterface.OnClickListener dialogButtonsClickListener = (dialog, which) -> {
            final int indexOfChild = radioGroup.indexOfChild(
                    radioGroup.findViewById(radioGroup.getCheckedRadioButtonId()));
            final AdapterChoiceItem choice = choices.get(indexOfChild);

            handleChoice(choice.key);

            // open future streams always like this one, because "always" button was used by user
            if (which == DialogInterface.BUTTON_POSITIVE) {
                preferences.edit()
                        .putString(getString(R.string.download_key), choice.key)
                        .apply();
            }
        };

        alertDialogChoice = new AlertDialog.Builder(themeWrapperContext)
                .setTitle(R.string.preferred_open_action_share_menu_title)
                .setView(binding.getRoot())
                .setCancelable(true)
                .setNegativeButton(R.string.just_once, dialogButtonsClickListener)
                .setPositiveButton(R.string.always, dialogButtonsClickListener)
                .setOnDismissListener(dialog -> {
                    if (!selectionIsDownload) {
                        finish();
                    }
                })
                .create();

        alertDialogChoice.setOnShowListener(dialog -> setDialogButtonsState(
                alertDialogChoice, radioGroup.getCheckedRadioButtonId() != -1));

        radioGroup.setOnCheckedChangeListener((group, checkedId) ->
                setDialogButtonsState(alertDialogChoice, true));
        final View.OnClickListener radioButtonsClickListener = v -> {
            final int indexOfChild = radioGroup.indexOfChild(v);
            if (indexOfChild == -1) {
                return;
            }

            selectedPreviously = selectedRadioPosition;
            selectedRadioPosition = indexOfChild;

            if (selectedPreviously == selectedRadioPosition) {
                handleChoice(choices.get(selectedRadioPosition).key);
            }
        };

        int id = 12345;
        for (final AdapterChoiceItem item : choices) {
            final RadioButton radioButton = ListRadioIconItemBinding.inflate(layoutInflater)
                    .getRoot();
            radioButton.setText(item.description);
            radioButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    AppCompatResources.getDrawable(themeWrapperContext, item.icon),
                    null, null, null);
            radioButton.setChecked(false);
            radioButton.setId(id++);
            radioButton.setLayoutParams(new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            radioButton.setOnClickListener(radioButtonsClickListener);
            radioGroup.addView(radioButton);
        }

        if (selectedRadioPosition == -1) {
            final String lastSelectedPlayer = preferences.getString(
                    getString(R.string.preferred_open_action_last_selected_key), null);
            if (!TextUtils.isEmpty(lastSelectedPlayer)) {
                for (int i = 0; i < choices.size(); i++) {
                    final AdapterChoiceItem c = choices.get(i);
                    if (lastSelectedPlayer.equals(c.key)) {
                        selectedRadioPosition = i;
                        break;
                    }
                }
            }
        }

        selectedRadioPosition = MathUtils.clamp(selectedRadioPosition, -1, choices.size() - 1);
        if (selectedRadioPosition != -1) {
            ((RadioButton) radioGroup.getChildAt(selectedRadioPosition)).setChecked(true);
        }
        selectedPreviously = selectedRadioPosition;

        alertDialogChoice.show();

    }

    private List<AdapterChoiceItem> getChoicesForService(final LinkType linkType) {
        final List<AdapterChoiceItem> items = new ArrayList<>();
        items.add(new AdapterChoiceItem(getString(R.string.show_info_key),
                getString(R.string.show_info), 0));
        if (linkType == LinkType.STREAM) {
            items.add(new AdapterChoiceItem(getString(R.string.download_key),
                    getString(R.string.download), R.drawable.ic_file_download));
        }
        return items;
    }

    protected Context getThemeWrapperContext() {
        return new ContextThemeWrapper(this, ThemeHelper.isLightThemeSelected(this)
                ? R.style.LightTheme : R.style.DarkTheme);
    }

    private void setDialogButtonsState(final AlertDialog dialog, final boolean state) {
        final Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (negativeButton == null || positiveButton == null) {
            return;
        }

        negativeButton.setEnabled(state);
        positiveButton.setEnabled(state);
    }


    private void handleChoice(final String selectedChoiceKey) {
        final List<String> validChoicesList = Arrays.asList(getResources()
                .getStringArray(R.array.preferred_open_action_values_list));
        if (validChoicesList.contains(selectedChoiceKey)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString(getString(
                            R.string.preferred_open_action_last_selected_key), selectedChoiceKey)
                    .apply();
        }


        if (selectedChoiceKey.equals(getString(R.string.download_key))) {
            if (PermissionHelper.checkStoragePermissions(this,
                    PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE)) {
                selectionIsDownload = true;
                openDownloadDialog();
            }
            return;
        }


        // stop and bypass FetcherService if InfoScreen was selected since
        // StreamDetailFragment can fetch data itself
        if (selectedChoiceKey.equals(getString(R.string.show_info_key))) {
            disposables.add(Observable
                    .fromCallable(() -> NavigationHelper.getIntentByLink(this, currentUrl))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(intent -> {
                        startActivity(intent);
                        finish();
                    }, throwable -> handleError(this, null))
            );
            return;
        }


        finish();
    }


    public static class PersistentFragment extends Fragment {
        private WeakReference<AppCompatActivity> weakContext;
        private final CompositeDisposable disposables = new CompositeDisposable();
        private int running = 0;

        private synchronized void inFlight(final boolean started) {
            if (started) {
                running++;
            } else {
                running--;
                if (running <= 0) {
                    getActivityContext().ifPresent(context -> context.getSupportFragmentManager()
                            .beginTransaction().remove(this).commit());
                }
            }
        }

        @Override
        public void onAttach(@NonNull final Context activityContext) {
            super.onAttach(activityContext);
            weakContext = new WeakReference<>((AppCompatActivity) activityContext);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            weakContext = null;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            disposables.clear();
        }

        /**
         * @return the activity context, if there is one and the activity is not finishing
         */
        private Optional<AppCompatActivity> getActivityContext() {
            return Optional.ofNullable(weakContext)
                    .map(Reference::get)
                    .filter(context -> !context.isFinishing());
        }

        // guard against IllegalStateException in calling DialogFragment.show() whilst in background
        // (which could happen, say, when the user pressed the home button while waiting for
        // the network request to return) when it internally calls FragmentTransaction.commit()
        // after the FragmentManager has saved its states (isStateSaved() == true)
        // (ref: https://stackoverflow.com/a/39813506)
        private void runOnVisible(final Consumer<AppCompatActivity> runnable) {
            getActivityContext().ifPresentOrElse(context -> {
                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                    context.runOnUiThread(() -> {
                        runnable.accept(context);
                        inFlight(false);
                    });
                } else {
                    getLifecycle().addObserver(new DefaultLifecycleObserver() {
                        @Override
                        public void onResume(@NonNull final LifecycleOwner owner) {
                            getLifecycle().removeObserver(this);
                            getActivityContext().ifPresentOrElse(context ->
                                    context.runOnUiThread(() -> {
                                        runnable.accept(context);
                                        inFlight(false);
                                    }),
                                    () -> inFlight(false)
                            );
                        }
                    });
                    // this trick doesn't seem to work on Android 10+ (API 29)
                    // which places restrictions on starting activities from the background
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                            && !context.isChangingConfigurations()) {
                        // try to bring the activity back to front if minimised
                        final Intent i = new Intent(context, RouterActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(i);
                    }
                }

            }, () ->
                // this branch is executed if there is no activity context
                inFlight(false)
            );
        }

        <T> Single<T> pleaseWait(final Single<T> single) {
            // 'abuse' ambWith() here to cancel the toast for us when the wait is over
            return single.ambWith(Single.create(emitter -> getActivityContext().ifPresent(context ->
                    context.runOnUiThread(() -> {
                        // Getting the stream info usually takes a moment
                        // Notifying the user here to ensure that no confusion arises
            }))));
        }

        @SuppressLint("CheckResult")
        private void openDownloadDialog(final int currentServiceId, final String currentUrl) {
            inFlight(true);
            final LoadingDialog loadingDialog = new LoadingDialog(R.string.loading_metadata_title);
            loadingDialog.show(getParentFragmentManager(), "loadingDialog");
            disposables.add(ExtractorHelper.getStreamInfo(currentServiceId, currentUrl, true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(this::pleaseWait)
                    .subscribe(result ->
                        runOnVisible(ctx -> {
                            loadingDialog.dismiss();
                            final FragmentManager fm = ctx.getSupportFragmentManager();
                            final DownloadDialog downloadDialog = new DownloadDialog(ctx, result);
                            // dismiss listener to be handled by FragmentManager
                            downloadDialog.show(fm, "downloadDialog");
                        }
                        ), throwable -> runOnVisible(ctx -> {
                        loadingDialog.dismiss();
                        ((RouterActivity) ctx).showUnsupportedUrlDialog(currentUrl);
                    })));
        }

    }


    private void openDownloadDialog() {
        getPersistFragment().openDownloadDialog(currentServiceId, currentUrl);
    }

    private PersistentFragment getPersistFragment() {
        final FragmentManager fm = getSupportFragmentManager();
        PersistentFragment persistFragment =
                (PersistentFragment) fm.findFragmentByTag("PERSIST_FRAGMENT");
        if (persistFragment == null) {
            persistFragment = new PersistentFragment();
            fm.beginTransaction()
                    .add(persistFragment, "PERSIST_FRAGMENT")
                    .commitNow();
        }
        return persistFragment;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (final int i : grantResults) {
            if (i == PackageManager.PERMISSION_DENIED) {
                finish();
                return;
            }
        }
        if (requestCode == PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE) {
            openDownloadDialog();
        }
    }

    private static class AdapterChoiceItem {
        final String description;
        final String key;
        @DrawableRes
        final int icon;

        AdapterChoiceItem(final String key, final String description, final int icon) {
            this.key = key;
            this.description = description;
            this.icon = icon;
        }
    }



    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    @Nullable
    private String getUrl(final Intent intent) {
        String foundUrl = null;
        if (intent.getData() != null) {
            // Called from another app
            foundUrl = intent.getData().toString();
        } else if (intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
            // Called from the share menu
            final String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
            foundUrl = UrlFinder.firstUrlFromInput(extraText);
        }

        return foundUrl;
    }
}
