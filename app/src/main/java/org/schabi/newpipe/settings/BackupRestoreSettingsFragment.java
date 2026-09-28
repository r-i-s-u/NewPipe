package org.schabi.newpipe.settings;

import static org.schabi.newpipe.extractor.utils.Utils.isBlank;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.NewPipeDatabase;
import org.schabi.newpipe.R;
import org.schabi.newpipe.error.ErrorInfo;
import org.schabi.newpipe.error.ErrorUtil;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.local.subscription.SubscriptionsImportExportHelper;
import org.schabi.newpipe.settings.export.BackupFileLocator;
import org.schabi.newpipe.settings.export.ImportExportManager;
import org.schabi.newpipe.streams.io.NoFileManagerSafeGuard;
import org.schabi.newpipe.streams.io.StoredFileHelper;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.ZipHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupRestoreSettingsFragment extends BasePreferenceFragment {

    private static final String ZIP_MIME_TYPE = "application/zip";

    private final SimpleDateFormat exportDateFormat =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    private ImportExportManager manager;
    private String importExportDataPathKey;
    private final ActivityResultLauncher<Intent> requestImportPathLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::requestImportPathResult);
    private final ActivityResultLauncher<Intent> requestExportPathLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::requestExportPathResult);
    private SubscriptionsImportExportHelper importExportHelper;


    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        importExportHelper = new SubscriptionsImportExportHelper(this);
    }

    @Override
    public void onCreatePreferences(@Nullable final Bundle savedInstanceState,
                                    @Nullable final String rootKey) {
        manager = new ImportExportManager(new BackupFileLocator(requireContext()));

        importExportDataPathKey = getString(R.string.import_export_data_path);

        addPreferencesFromResourceRegistry();

        final Preference importDataPreference = requirePreference(R.string.import_data);
        importDataPreference.setOnPreferenceClickListener((Preference p) -> {
            NoFileManagerSafeGuard.launchSafe(
                    requestImportPathLauncher,
                    StoredFileHelper.getPicker(requireContext(),
                            ZIP_MIME_TYPE, getImportExportDataUri()),
                    TAG,
                    getContext()
            );

            return true;
        });

        final Preference exportDataPreference = requirePreference(R.string.export_data);
        exportDataPreference.setOnPreferenceClickListener((final Preference p) -> {
            NoFileManagerSafeGuard.launchSafe(
                    requestExportPathLauncher,
                    StoredFileHelper.getNewPicker(requireContext(),
                            "NewPipeData-" + exportDateFormat.format(new Date()) + ".zip",
                            ZIP_MIME_TYPE, getImportExportDataUri()),
                    TAG,
                    getContext()
            );

            return true;
        });

        final Preference resetSettings = requirePreference(R.string.reset_settings);
        resetSettings.setOnPreferenceClickListener(preference -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.reset_all_settings);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                final SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(requireContext());
                sharedPreferences.edit().clear().apply();
                if (getActivity() == null) {
                    return;
                }
                NavigationHelper.restartApp(getActivity());
            });
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            });
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        });

        final Preference exportSubsPreference =
                requirePreference(R.string.export_subscriptions_key);
        exportSubsPreference.setOnPreferenceClickListener(reference -> {
            importExportHelper.onExportSelected();
            return true;
        });

        final Preference importSubsPreference =
                requirePreference(R.string.import_subscriptions_key);
        importSubsPreference.setOnPreferenceClickListener(preference -> {
            importExportHelper.onImportPreviousSelected();
            return true;
        });
    }

    private void requestExportPathResult(final ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            final Uri lastExportDataUri = result.getData().getData();
            final StoredFileHelper file = new StoredFileHelper(
                    requireContext(), result.getData().getData(), ZIP_MIME_TYPE);
            exportDatabase(file, lastExportDataUri);
        }
    }

    private void requestImportPathResult(final ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            final Uri lastImportDataUri = result.getData().getData();
            final StoredFileHelper file = new StoredFileHelper(
                    requireContext(), result.getData().getData(), ZIP_MIME_TYPE);
            new androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.override_current_data)
                    .setPositiveButton(R.string.ok, (d, id) ->
                            importDatabase(file, lastImportDataUri))
                    .setNegativeButton(R.string.cancel, (d, id) -> d.cancel())
                    .show();
        }
    }

    private void exportDatabase(final StoredFileHelper file, final Uri exportDataUri) {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(NewPipeDatabase::checkpoint).get();
            final SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(requireContext());
            manager.exportDatabase(preferences, file);
            saveLastImportExportDataUri(exportDataUri);
            Toast.makeText(requireContext(), R.string.export_complete_toast, Toast.LENGTH_SHORT)
                    .show();
        } catch (final Exception e) {
            showErrorSnackbar(e, "Exporting database and settings");
        }
    }

    private void importDatabase(final StoredFileHelper file, final Uri importDataUri) {
        if (!ZipHelper.isValidZipFile(file)) {
            Toast.makeText(requireContext(), R.string.no_valid_zip_file, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        try {
            manager.ensureDbDirectoryExists();
            if (!manager.extractDb(file)) {
                Toast.makeText(requireContext(), R.string.could_not_import_all_files,
                                Toast.LENGTH_LONG).show();
            }
            final boolean hasJsonPrefs = manager.exportHasJsonPrefs(file);
            if (hasJsonPrefs || manager.exportHasSerializedPrefs(file)) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.import_settings)
                        .setMessage(hasJsonPrefs ? null : requireContext()
                                .getString(R.string.import_settings_vulnerable_format))
                        .setOnDismissListener(dialog -> finishImport(importDataUri))
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                            finishImport(importDataUri);
                        })
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            final Context context = requireContext();
                            final SharedPreferences prefs = PreferenceManager
                                    .getDefaultSharedPreferences(context);
                            try {
                                if (hasJsonPrefs) {
                                    manager.loadJsonPrefs(file, prefs);
                                } else {
                                    manager.loadSerializedPrefs(file, prefs);
                                }
                            } catch (IOException | ClassNotFoundException | JsonParserException e) {
                                createErrorNotification(e, "Importing preferences");
                                return;
                            }
                            cleanImport(context, prefs);
                            finishImport(importDataUri);
                        })
                        .show();
            } else {
                finishImport(importDataUri);
            }
        } catch (final Exception e) {
            showErrorSnackbar(e, "Importing database and settings");
        }
    }

    private void cleanImport(@NonNull final Context context,
                             @NonNull final SharedPreferences prefs) {
        final String tunnelingKey = context.getString(R.string.disable_media_tunneling_key);
        final String automaticTunnelingKey =
                context.getString(R.string.disabled_media_tunneling_automatically_key);
        final boolean wasMediaTunnelingDisabledAutomatically =
                prefs.getInt(automaticTunnelingKey, -1) == 1
                        && prefs.getBoolean(tunnelingKey, false);
        if (wasMediaTunnelingDisabledAutomatically) {
            prefs.edit()
                    .putInt(automaticTunnelingKey, -1)
                    .putBoolean(tunnelingKey, false)
                    .apply();
            NewPipeSettings.setMediaTunneling(context);
        }
    }

    private void finishImport(final Uri importDataUri) {
        saveLastImportExportDataUri(importDataUri);
        NavigationHelper.restartApp(requireActivity());
    }

    private Uri getImportExportDataUri() {
        final String path = defaultPreferences.getString(importExportDataPathKey, null);
        return isBlank(path) ? null : Uri.parse(path);
    }

    private void saveLastImportExportDataUri(final Uri importExportDataUri) {
        final SharedPreferences.Editor editor = defaultPreferences.edit()
                .putString(importExportDataPathKey, importExportDataUri.toString());
        editor.apply();
    }

    private void showErrorSnackbar(final Throwable e, final String request) {
        ErrorUtil.showSnackbar(this, new ErrorInfo(e, UserAction.DATABASE_IMPORT_EXPORT, request));
    }

    private void createErrorNotification(final Throwable e, final String request) {
        ErrorUtil.createNotification(
                requireContext(),
                new ErrorInfo(e, UserAction.DATABASE_IMPORT_EXPORT, request)
        );
    }
}
