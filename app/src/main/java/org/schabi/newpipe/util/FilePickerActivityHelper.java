package org.schabi.newpipe.util;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.filepicker.FilePickerFragment;

import org.schabi.newpipe.R;

import java.io.File;

public class FilePickerActivityHelper extends com.nononsenseapps.filepicker.FilePickerActivity {
    private CustomFilePickerFragment currentFragment;

    public static boolean isOwnFileUri(@NonNull final Context context, @NonNull final Uri uri) {
        if (uri.getAuthority() == null) {
            return false;
        }
        return uri.getAuthority().startsWith(context.getPackageName());
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (ThemeHelper.isLightThemeSelected(this)) {
            this.setTheme(R.style.FilePickerThemeLight);
        } else {
            this.setTheme(R.style.FilePickerThemeDark);
        }
        super.onCreate(savedInstanceState);

        final View decor = getWindow().getDecorView();
        final View content = findViewById(android.R.id.content);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(decor, (v, insets) -> {
            final androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                            | androidx.core.view.WindowInsetsCompat.Type.ime());
            content.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(decor);

    }

    @Override
    public void onBackPressed() {
        // If at top most level, normal behaviour
        if (currentFragment.isBackTop()) {
            super.onBackPressed();
        } else {
            // Else go up
            currentFragment.goUp();
        }
    }

    @Override
    protected AbstractFilePickerFragment<File> getFragment(@Nullable final String startPath,
                                                           final int mode,
                                                           final boolean allowMultiple,
                                                           final boolean allowCreateDir,
                                                           final boolean allowExistingFile,
                                                           final boolean singleClick) {
        final CustomFilePickerFragment fragment = new CustomFilePickerFragment();
        fragment.setArgs(startPath != null ? startPath
                        : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
        currentFragment = fragment;
        return currentFragment;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Internal
    //////////////////////////////////////////////////////////////////////////*/

    public static class CustomFilePickerFragment extends FilePickerFragment {

        @Override
        public void onViewCreated(@NonNull final View view,
                                  @Nullable final Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            androidx.appcompat.widget.Toolbar toolbar = findToolbar(view);
            if (toolbar == null) {
                toolbar = findToolbar(requireActivity().findViewById(android.R.id.content));
            }
            if (toolbar != null) {
                toolbar.setNavigationIcon(R.drawable.ic_picker_close);
                toolbar.setNavigationOnClickListener(v -> {
                    requireActivity().setResult(android.app.Activity.RESULT_CANCELED);
                    requireActivity().finish();
                });
            }
        }

        private static androidx.appcompat.widget.Toolbar findToolbar(final View v) {
            if (v instanceof androidx.appcompat.widget.Toolbar) {
                return (androidx.appcompat.widget.Toolbar) v;
            }
            if (v instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) v;
                for (int i = 0; i < group.getChildCount(); i++) {
                    final androidx.appcompat.widget.Toolbar found = findToolbar(group.getChildAt(i));
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }
        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                                 final Bundle savedInstanceState) {
            final View root = super.onCreateView(inflater, container, savedInstanceState);
            final View nameView = root.findViewById(
                    com.nononsenseapps.filepicker.R.id.nnf_text_filename);
            if (nameView instanceof android.widget.EditText) {
                final android.widget.EditText nameEdit = (android.widget.EditText) nameView;
                nameEdit.setSingleLine(false);
                nameEdit.setHorizontallyScrolling(false);
                nameEdit.setMaxLines(6);
                // filenames must not contain line breaks
                nameEdit.setFilters(new android.text.InputFilter[]{
                        (src, start, end, dest, ds, de) -> src.toString().contains("\n")
                                ? src.toString().replace("\n", "") : null});
            }
            return root;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                          final int viewType) {
            final RecyclerView.ViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);

            final View view = viewHolder.itemView.findViewById(android.R.id.text1);
            if (view instanceof TextView) {
                ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.file_picker_items_text_size));
            }

            return viewHolder;
        }

        @Override
        public void onClickOk(@NonNull final View view) {
            if (mode == MODE_NEW_FILE && getNewFileName().isEmpty()) {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(getActivity(), R.string.file_name_empty_error,
                        Toast.LENGTH_SHORT);
                mToast.show();
                return;
            }

            super.onClickOk(view);
        }

        @Override
        protected boolean isItemVisible(@NonNull final File file) {
            if (file.isDirectory() && file.isHidden()) {
                return true;
            }
            return super.isItemVisible(file);
        }

        public File getBackTop() {
            if (getArguments() == null) {
                return Environment.getExternalStorageDirectory();
            }

            final String path = getArguments().getString(KEY_START_PATH, "/");
            if (path.contains(Environment.getExternalStorageDirectory().getPath())) {
                return Environment.getExternalStorageDirectory();
            }

            return getPath(path);
        }

        public boolean isBackTop() {
            return compareFiles(mCurrentPath,
                    getBackTop()) == 0 || compareFiles(mCurrentPath, new File("/")) == 0;
        }

        @Override
        protected boolean hasPermission(@NonNull final File path) {
            return android.os.Build.VERSION.SDK_INT >= 30
                    ? Environment.isExternalStorageManager()
                    : super.hasPermission(path);
        }

        @Override
        protected void handlePermission(@NonNull final File path) {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + requireContext().getPackageName())));
            } else {
                super.handlePermission(path);
            }
        }

        @Override
        public void onLoadFinished(@NonNull final Loader<SortedList<File>> loader,
                                   final SortedList<File> data) {
            super.onLoadFinished(loader, data);
            layoutManager.scrollToPosition(0);
        }
    }
}
