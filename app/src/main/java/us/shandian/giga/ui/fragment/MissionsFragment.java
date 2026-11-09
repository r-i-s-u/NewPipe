package us.shandian.giga.ui.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nononsenseapps.filepicker.Utils;

import org.schabi.newpipe.R;
import org.schabi.newpipe.streams.io.NoFileManagerSafeGuard;
import org.schabi.newpipe.streams.io.StoredFileHelper;
import org.schabi.newpipe.util.FilePickerActivityHelper;

import java.io.File;
import java.io.IOException;

import us.shandian.giga.get.DownloadMission;
import us.shandian.giga.service.DownloadManager;
import us.shandian.giga.service.DownloadManagerService;
import us.shandian.giga.service.DownloadManagerService.DownloadManagerBinder;
import us.shandian.giga.ui.adapter.MissionAdapter;

public class MissionsFragment extends Fragment {

    private static final String TAG = "MissionsFragment";
    private static final int SPAN_SIZE = 3;

    private SharedPreferences mPrefs;
    private boolean mLinear;
    private MenuItem mSwitch;
    private MenuItem mClear = null;
    private MenuItem mStart = null;
    private MenuItem mPause = null;

    private RecyclerView mList;
    private View mEmpty;
    private MissionAdapter mAdapter;
    private GridLayoutManager mGridManager;
    private LinearLayoutManager mLinearManager;
    private Context mContext;

    private DownloadManagerBinder mBinder;
    private int mKind = 2; // 2 = video, 1 = audio
    private boolean mForceUpdate;

    private DownloadMission unsafeMissionTarget = null;
    private final ActivityResultLauncher<Intent> requestDownloadSaveAsLauncher =
            registerForActivityResult(new StartActivityForResult(), this::requestDownloadSaveAsResult);
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mBinder = (DownloadManagerBinder) binder;
            mBinder.clearDownloadNotifications();

            mAdapter = new MissionAdapter(mContext, mBinder.getDownloadManager(), mEmpty, getView());

            mAdapter.setRecover(MissionsFragment.this::recoverMission);

            setAdapterButtons();

            mBinder.addMissionEventListener(mAdapter);
            mBinder.enableNotifications(false);

            updateList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // What to do?
        }


    };

    private void updateEmptyText() {
        if (mEmpty == null) {
            return;
        }
        final android.widget.TextView sub = mEmpty.findViewById(R.id.empty_subtitle);
        if (sub != null) {
            sub.setText(mKind == 1 ? R.string.empty_downloads_subtitle_audio
                    : R.string.empty_downloads_subtitle);
        }
    }

    private void switchKind(final boolean toAudio) {
        final int kind = toAudio ? 1 : 2;
        if (kind == mKind || mAdapter == null || mList == null) {
            return;
        }
        mKind = kind;
        final float dir = toAudio ? 1f : -1f;
        final float shift = 32 * getResources().getDisplayMetrics().density;
        final android.view.animation.Interpolator out =
                new android.view.animation.PathInterpolator(0.4f, 0f, 1f, 1f);
        final android.view.animation.Interpolator in =
                new android.view.animation.PathInterpolator(0f, 0f, 0.2f, 1f);
        if (getActivity() instanceof org.schabi.newpipe.MainActivity) {
            ((org.schabi.newpipe.MainActivity) getActivity()).setKindTitle(toAudio);
        }
        mEmpty.animate().cancel();
        mEmpty.animate().alpha(0f).translationX(-dir * shift).setDuration(90)
                .setInterpolator(out).start();
        mList.animate().cancel();
        mList.animate().alpha(0f).translationX(-dir * shift).setDuration(90)
                .setInterpolator(out).withEndAction(() -> {
                    if (mAdapter == null || mList == null) {
                        return;
                    }
                    final androidx.recyclerview.widget.RecyclerView.ItemAnimator ia =
                            mList.getItemAnimator();
                    mList.setItemAnimator(null);
                    mAdapter.setKindFilter(kind);
                    updateEmptyText();
                    mList.scrollToPosition(0);
                    mList.postDelayed(() -> {
                        if (mList != null) {
                            mList.setItemAnimator(ia);
                        }
                    }, 400);
                    for (final android.view.View v : new android.view.View[]{mList, mEmpty}) {
                        v.setTranslationX(dir * shift);
                        v.animate().alpha(1f).translationX(0f).setDuration(210)
                                .setInterpolator(in).start();
                    }
                }).start();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.missions, container, false);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        mLinear = true;

        // Bind the service
        mContext.bindService(new Intent(mContext, DownloadManagerService.class), mConnection, Context.BIND_AUTO_CREATE);

        // Views
        mEmpty = v.findViewById(R.id.list_empty_view);
        updateEmptyText();
        mList = v.findViewById(R.id.mission_recycler);

        final android.view.GestureDetector swipe = new android.view.GestureDetector(
                requireContext(), new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(final android.view.MotionEvent e1,
                                   final android.view.MotionEvent e2,
                                   final float vx, final float vy) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                final float dx = e2.getX() - e1.getX();
                final float dy = e2.getY() - e1.getY();
                final float min = 80 * getResources().getDisplayMetrics().density;
                if (Math.abs(dx) > min && Math.abs(dx) > 2 * Math.abs(dy)
                        && Math.abs(vx) > 400) {
                    if (mAdapter != null) {
                        mAdapter.markSwipe();
                    }
                    switchKind(dx < 0);
                    return true;
                }
                return false;
            }
        });
        final int swipeSlop = android.view.ViewConfiguration.get(requireContext())
                .getScaledTouchSlop();
        final float[] swipeDown = new float[2];
        mList.addOnItemTouchListener(
                new androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(
                    @NonNull final androidx.recyclerview.widget.RecyclerView rv,
                    @NonNull final android.view.MotionEvent e) {
                swipe.onTouchEvent(e);
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    swipeDown[0] = e.getX();
                    swipeDown[1] = e.getY();
                } else if (e.getActionMasked() == android.view.MotionEvent.ACTION_MOVE) {
                    final float adx = Math.abs(e.getX() - swipeDown[0]);
                    final float ady = Math.abs(e.getY() - swipeDown[1]);
                    if (adx > swipeSlop && adx > 1.5f * ady) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(
                    @NonNull final androidx.recyclerview.widget.RecyclerView rv,
                    @NonNull final android.view.MotionEvent e) {
                swipe.onTouchEvent(e);
            }
        });
        mEmpty.setOnTouchListener((view, e) -> {
            swipe.onTouchEvent(e);
            return true;
        });

        // Init layouts managers
        mGridManager = new GridLayoutManager(getActivity(), SPAN_SIZE);
        mGridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case DownloadManager.SPECIAL_PENDING:
                    case DownloadManager.SPECIAL_FINISHED:
                        return SPAN_SIZE;
                    default:
                        return 1;
                }
            }
        });
        mLinearManager = new LinearLayoutManager(getActivity());

        setHasOptionsMenu(true);

        return v;
    }

    /**
     * Added in API level 23.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Bug: in api< 23 this is never called
        // so mActivity=null
        // so app crashes with null-pointer exception
        mContext = context;
    }

    /**
     * deprecated in API level 23,
     * but must remain to allow compatibility with api<23
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);

        mContext = activity;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinder == null || mAdapter == null) return;

        mBinder.removeMissionEventListener(mAdapter);
        mBinder.enableNotifications(true);
        mContext.unbindService(mConnection);
        mAdapter.onDestroy();

        mBinder = null;
        mAdapter = null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mSwitch = menu.findItem(R.id.switch_mode);
        mClear = menu.findItem(R.id.clear_list);
        mStart = menu.findItem(R.id.start_downloads);
        mPause = menu.findItem(R.id.pause_downloads);

        if (mAdapter != null) setAdapterButtons();

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.switch_mode) {
            mLinear = !mLinear;
            updateList();
            return true;
        } else if (itemId == R.id.clear_list) {
            showClearDownloadHistoryPrompt();
            return true;
        } else if (itemId == R.id.start_downloads) {
            mBinder.getDownloadManager().startAllMissions();
            return true;
        } else if (itemId == R.id.pause_downloads) {
            mBinder.getDownloadManager().pauseAllMissions(false);
            mAdapter.refreshMissionItems();// update items view

            return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    public void showClearDownloadHistoryPrompt() {
        // ask the user whether he wants to just clear history or instead delete files on disk
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.clear_download_history)
                .setMessage(R.string.confirm_prompt)
                // Intentionally misusing buttons' purpose in order to achieve good order
                .setNegativeButton(R.string.clear_download_history, (dialog, which) ->
                        mAdapter.clearFinishedDownloads(false))
                .setNeutralButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_downloaded_files, (dialog, which) ->
                        showDeleteDownloadedFilesConfirmationPrompt())
                .show();
    }

    public void showDeleteDownloadedFilesConfirmationPrompt() {
        // make sure the user confirms once more before deleting files on disk
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.delete_downloaded_files_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) ->
                        mAdapter.clearFinishedDownloads(true))
                .show();
    }

    private void updateList() {
        if (mLinear) {
            mList.setLayoutManager(mLinearManager);
        } else {
            mList.setLayoutManager(mGridManager);
        }

        // destroy all created views in the recycler
        mList.setAdapter(null);
        mAdapter.notifyDataSetChanged();

        // re-attach the adapter in grid/lineal mode
        mAdapter.setLinear(mLinear);
        mList.setAdapter(mAdapter);

        if (mSwitch != null) {
            mSwitch.setIcon(mLinear
                            ? 0
                            : 0);
            mSwitch.setTitle(mLinear ? R.string.grid : R.string.list);
            mPrefs.edit().putBoolean("linear", mLinear).apply();
        }
    }

    private void setAdapterButtons() {
        if (mClear == null || mStart == null || mPause == null) return;

        mAdapter.setClearButton(mClear);
        mAdapter.setMasterButtons(mStart, mPause);
    }

    private void recoverMission(@NonNull DownloadMission mission) {
        unsafeMissionTarget = mission;

        final Uri initialPath;
        if (false) {
            initialPath = null;
        } else {
            final File initialSavePath;
            if (DownloadManager.TAG_AUDIO.equals(mission.storage.getType())) {
                initialSavePath = new java.io.File(android.os.Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MUSIC);
            } else {
                initialSavePath = new java.io.File(android.os.Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES);
            }
            initialPath = Uri.parse(initialSavePath.getAbsolutePath());
        }

        NoFileManagerSafeGuard.launchSafe(
                requestDownloadSaveAsLauncher,
                StoredFileHelper.getNewPicker(mContext, mission.storage.getName(),
                        mission.storage.getType(), initialPath),
                TAG,
                mContext
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter != null) {
            mAdapter.onResume();

            if (mForceUpdate) {
                mForceUpdate = false;
                mAdapter.forceUpdate();
            }

            mBinder.addMissionEventListener(mAdapter);
            mAdapter.checkMasterButtonsVisibility();
        }
        if (mBinder != null) mBinder.enableNotifications(false);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mAdapter != null) {
            mForceUpdate = true;
            mBinder.removeMissionEventListener(mAdapter);
            mAdapter.onPaused();
        }

        if (mBinder != null) mBinder.enableNotifications(true);
    }

    private void requestDownloadSaveAsResult(final ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK) {
            return;
        }

        if (unsafeMissionTarget == null || result.getData() == null) {
            return;
        }

        try {
            Uri fileUri = result.getData().getData();
            if (fileUri.getAuthority() != null && FilePickerActivityHelper.isOwnFileUri(mContext, fileUri)) {
                fileUri = Uri.fromFile(Utils.getFileForUri(fileUri));
            }

            String tag = unsafeMissionTarget.storage.getTag();
            unsafeMissionTarget.storage = new StoredFileHelper(mContext, null, fileUri, tag);
            mAdapter.recoverMission(unsafeMissionTarget);
        } catch (IOException e) {
            Toast.makeText(mContext, R.string.general_error, Toast.LENGTH_LONG).show();
        }
    }
}
