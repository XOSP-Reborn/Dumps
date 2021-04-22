package com.android.keyguard;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.systemui.C0007R$id;
import com.sonymobile.keyguard.plugin.docomoclock.DocomoClockLog;
import java.util.List;

public class MachiCharaWidget extends LinearLayout {
    private ActivityManager mActivityManager = null;
    private BroadcastReceiver mBroadCastReceiver = null;
    private Context mContext;
    private DevicePolicyManager mDevicePolicyManager;
    private Handler mHandler;
    private boolean mIsDoze = false;
    private RemoteViews mRemoteViews;
    private KeyguardSecurityModel mSecurityModel;
    public boolean mUpdateViewFlg = false;
    int mUserHandle;

    public MachiCharaWidget(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        this.mHandler = new Handler();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerReceiver();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterReceiver();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateView() {
        LinearLayout linearLayout = (LinearLayout) findViewById(C0007R$id.chara);
        if (linearLayout != null) {
            if (linearLayout.getChildCount() > 0) {
                linearLayout.removeAllViews();
            }
            ImageView imageView = null;
            FrameLayout frameLayout = (FrameLayout) linearLayout.getParent();
            if (frameLayout != null) {
                imageView = (ImageView) frameLayout.findViewById(C0007R$id.mic_button);
            }
            if (imageView != null) {
                RemoteViews remoteViews = this.mRemoteViews;
                if (remoteViews != null) {
                    try {
                        linearLayout.addView(remoteViews.apply(getContext(), this));
                        if (!isShowMachiChara()) {
                            this.mUpdateViewFlg = true;
                            updateCharaLayoutVisibility(8);
                            imageView.setVisibility(0);
                        } else {
                            this.mUpdateViewFlg = true;
                            imageView.setVisibility(8);
                            updateCharaLayoutVisibility(0);
                        }
                        if (DocomoClockLog.DEBUG) {
                            Log.d("DocomoLockScreen", "MachiCharaWidget.updateView(): set remoteviews");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    this.mUpdateViewFlg = true;
                    updateCharaLayoutVisibility(8);
                    imageView.setVisibility(0);
                    if (DocomoClockLog.DEBUG) {
                        Log.d("DocomoLockScreen", "MachiCharaWidget.updateView(): remoteviews is null");
                    }
                }
            }
        }
    }

    private void registerReceiver() {
        if (this.mBroadCastReceiver == null) {
            this.mBroadCastReceiver = new BroadcastReceiver() {
                /* class com.android.keyguard.MachiCharaWidget.AnonymousClass1 */

                public void onReceive(Context context, Intent intent) {
                    if (DocomoClockLog.DEBUG) {
                        Log.d("DocomoLockScreen", "MachiCharaWidget: Intent.getAction() - " + intent.getAction());
                    }
                    if (intent.getAction().equals("com.nttdocomo.android.mascot.KEYGUARD_UPDATE")) {
                        MachiCharaWidget.this.mRemoteViews = (RemoteViews) intent.getParcelableExtra("RemoteViews");
                        if (MachiCharaWidget.this.mRemoteViews != null && !MachiCharaWidget.this.isMascotAppRunning()) {
                            MachiCharaWidget.this.mRemoteViews = null;
                        }
                        MachiCharaWidget.this.mHandler.post(new Runnable() {
                            /* class com.android.keyguard.MachiCharaWidget.AnonymousClass1.AnonymousClass1 */

                            public void run() {
                                MachiCharaWidget.this.updateView();
                            }
                        });
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.nttdocomo.android.mascot.KEYGUARD_UPDATE");
            this.mContext.registerReceiver(this.mBroadCastReceiver, intentFilter);
        }
    }

    private void unregisterReceiver() {
        try {
            if (this.mBroadCastReceiver != null) {
                this.mContext.unregisterReceiver(this.mBroadCastReceiver);
                this.mBroadCastReceiver = null;
            }
        } catch (Exception e) {
            Log.e("DocomoLockScreen", "unregisterReceiver: exception:" + e.toString());
        }
    }

    public boolean isShowMachiChara() {
        getActivityManager();
        this.mUserHandle = ActivityManager.getCurrentUser();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (!isLockscreenPublicMode(this.mUserHandle)) {
            return isLockscreenNotificationSetting();
        }
        return isUsersAllowingPrivateNotifications(this.mUserHandle);
    }

    public boolean isLockscreenPublicMode(int i) {
        this.mSecurityModel = new KeyguardSecurityModel(getContext());
        return this.mSecurityModel.getSecurityMode(i) != KeyguardSecurityModel.SecurityMode.None;
    }

    public boolean isLockscreenNotificationSetting() {
        return (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, this.mUserHandle) != 0) && ((this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mUserHandle) & 4) == 0);
    }

    public boolean isUsersAllowingPrivateNotifications(int i) {
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        boolean z = true;
        if (i == -1) {
            return true;
        }
        if (sparseBooleanArray.indexOfKey(i) >= 0) {
            return sparseBooleanArray.get(i);
        }
        boolean z2 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0, i) != 0;
        boolean z3 = (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, i) & 8) == 0;
        if (!z2 || !z3) {
            z = false;
        }
        sparseBooleanArray.append(i, z);
        return z2;
    }

    public boolean isMascotAppRunning() {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = getActivityManager().getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if ("com.nttdocomo.android.mascot".equals(runningAppProcessInfo.processName)) {
                return true;
            }
        }
        return false;
    }

    public synchronized ActivityManager getActivityManager() {
        if (this.mActivityManager == null) {
            this.mActivityManager = (ActivityManager) getContext().getSystemService("activity");
        }
        return this.mActivityManager;
    }

    public void setDoze() {
        this.mIsDoze = true;
        updateCharaLayoutVisibility(8);
    }

    private void updateCharaLayoutVisibility(int i) {
        if (this.mIsDoze) {
            setVisibility(8);
        } else {
            setVisibility(i);
        }
    }
}
