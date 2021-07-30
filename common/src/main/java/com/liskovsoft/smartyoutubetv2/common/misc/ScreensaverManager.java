package com.liskovsoft.smartyoutubetv2.common.misc;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.lang.ref.WeakReference;

public class ScreensaverManager {
    private static final String TAG = ScreensaverManager.class.getSimpleName();
    private static final int DIM_DELAY_MS = 10_000;
    private final Handler mHandler;
    private final WeakReference<Activity> mActivity;
    private final Runnable mDimScreen = this::dimScreen;
    private final Runnable mUndimScreen = this::undimScreen;
    private final GeneralData mGeneralData;
    private boolean mIsEnabled;
    private boolean mIsBlocked;

    public ScreensaverManager(Activity activity) {
        mActivity = new WeakReference<>(activity);
        mHandler = new Handler(Looper.getMainLooper());
        mGeneralData = GeneralData.instance(activity);
        Helpers.disableScreensaver(activity);
        enable();
    }

    public void enable() {
        if (mIsBlocked) {
            return;
        }

        Log.d(TAG, "Enable screensaver");

        disable();
        Utils.postDelayed(mHandler, mDimScreen, mGeneralData.getScreenDimmingTimoutMin() == GeneralData.SCREEN_DIMMING_NEVER ?
                10_000 : mGeneralData.getScreenDimmingTimoutMin() * 60 * 1_000);
    }

    public void disable() {
        if (mIsBlocked) {
            return;
        }

        Log.d(TAG, "Disable screensaver");

        Utils.removeCallbacks(mHandler, mDimScreen);
        Utils.postDelayed(mHandler, mUndimScreen, 0);
    }

    private void dimScreen() {
        showHide(true);
    }

    private void undimScreen() {
        showHide(false);
    }

    private void setColor(int color) {
        Activity activity = mActivity.get();

        if (activity == null) {
            return;
        }

        View dimContainer = activity.getWindow().getDecorView().findViewById(R.id.dim_container);

        if (dimContainer != null) {
            dimContainer.setBackgroundColor(color);
        }
    }

    private void showHide(boolean show) {
        Activity activity = mActivity.get();

        if (activity == null) {
            return;
        }

        PlaybackView playbackView = PlaybackPresenter.instance(activity).getView();
        if (show && playbackView != null && playbackView.getController().isPlaying()) {
            return;
        }

        View rootView = activity.getWindow().getDecorView().getRootView();

        View dimContainer = rootView.findViewById(R.id.dim_container);

        if (dimContainer == null) {
            LayoutInflater layoutInflater = activity.getLayoutInflater();
            dimContainer = layoutInflater.inflate(R.layout.dim_container, null);
            if (rootView instanceof ViewGroup) {
                ((ViewGroup) rootView).addView(dimContainer);
            }
        }

        if (mGeneralData.getScreenDimmingTimoutMin() == GeneralData.SCREEN_DIMMING_NEVER) {
            if (show) {
                Helpers.enableScreensaver(activity);
            } else {
                Helpers.disableScreensaver(activity);
            }
        } else {
            dimContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void setBlocked(boolean blocked) {
        mIsBlocked = blocked;
    }
}