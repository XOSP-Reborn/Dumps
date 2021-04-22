package com.android.systemui.biometrics;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.biometrics.IBiometricServiceReceiverInternal;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.os.SomeArgs;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.statusbar.CommandQueue;

public class BiometricDialogImpl extends SystemUI implements CommandQueue.Callbacks {
    private Callback mCallback = new Callback();
    private BiometricDialogView mCurrentDialog;
    private SomeArgs mCurrentDialogArgs;
    private boolean mDialogShowing;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        /* class com.android.systemui.biometrics.BiometricDialogImpl.AnonymousClass1 */

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    BiometricDialogImpl.this.handleShowDialog((SomeArgs) message.obj, false, null);
                    return;
                case 2:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    BiometricDialogImpl.this.handleBiometricAuthenticated(((Boolean) someArgs.arg1).booleanValue(), (String) someArgs.arg2);
                    someArgs.recycle();
                    return;
                case 3:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    BiometricDialogImpl.this.handleBiometricHelp((String) someArgs2.arg1);
                    someArgs2.recycle();
                    return;
                case 4:
                    BiometricDialogImpl.this.handleBiometricError((String) message.obj);
                    return;
                case 5:
                    BiometricDialogImpl.this.handleHideDialog(((Boolean) message.obj).booleanValue());
                    return;
                case 6:
                    BiometricDialogImpl.this.handleButtonNegative();
                    return;
                case 7:
                    BiometricDialogImpl.this.handleUserCanceled();
                    return;
                case 8:
                    BiometricDialogImpl.this.handleButtonPositive();
                    return;
                case 9:
                    BiometricDialogImpl.this.handleTryAgainPressed();
                    return;
                default:
                    Log.w("BiometricDialogImpl", "Unknown message: " + message.what);
                    return;
            }
        }
    };
    private IBiometricServiceReceiverInternal mReceiver;
    private WakefulnessLifecycle mWakefulnessLifecycle;
    final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
        /* class com.android.systemui.biometrics.BiometricDialogImpl.AnonymousClass2 */

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onStartedGoingToSleep() {
            if (BiometricDialogImpl.this.mDialogShowing) {
                Log.d("BiometricDialogImpl", "User canceled due to screen off");
                BiometricDialogImpl.this.mHandler.obtainMessage(7).sendToTarget();
            }
        }
    };
    private WindowManager mWindowManager;

    /* access modifiers changed from: private */
    public class Callback implements DialogViewCallback {
        private Callback() {
        }

        @Override // com.android.systemui.biometrics.DialogViewCallback
        public void onUserCanceled() {
            BiometricDialogImpl.this.mHandler.obtainMessage(7).sendToTarget();
        }

        @Override // com.android.systemui.biometrics.DialogViewCallback
        public void onErrorShown() {
            BiometricDialogImpl.this.mHandler.sendMessageDelayed(BiometricDialogImpl.this.mHandler.obtainMessage(5, false), 2000);
        }

        @Override // com.android.systemui.biometrics.DialogViewCallback
        public void onNegativePressed() {
            BiometricDialogImpl.this.mHandler.obtainMessage(6).sendToTarget();
        }

        @Override // com.android.systemui.biometrics.DialogViewCallback
        public void onPositivePressed() {
            BiometricDialogImpl.this.mHandler.obtainMessage(8).sendToTarget();
        }

        @Override // com.android.systemui.biometrics.DialogViewCallback
        public void onTryAgainPressed() {
            BiometricDialogImpl.this.mHandler.obtainMessage(9).sendToTarget();
        }
    }

    @Override // com.android.systemui.SystemUI
    public void start() {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager.hasSystemFeature("android.hardware.fingerprint") || packageManager.hasSystemFeature("android.hardware.biometrics.face") || packageManager.hasSystemFeature("android.hardware.biometrics.iris")) {
            ((CommandQueue) getComponent(CommandQueue.class)).addCallback((CommandQueue.Callbacks) this);
            this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
            this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
            this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showBiometricDialog(Bundle bundle, IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal, int i, boolean z, int i2) {
        Log.d("BiometricDialogImpl", "showBiometricDialog, type: " + i + ", requireConfirmation: " + z);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(5);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = bundle;
        obtain.arg2 = iBiometricServiceReceiverInternal;
        obtain.argi1 = i;
        obtain.arg3 = Boolean.valueOf(z);
        obtain.argi2 = i2;
        this.mHandler.obtainMessage(1, obtain).sendToTarget();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onBiometricAuthenticated(boolean z, String str) {
        Log.d("BiometricDialogImpl", "onBiometricAuthenticated: " + z + " reason: " + str);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = Boolean.valueOf(z);
        obtain.arg2 = str;
        this.mHandler.obtainMessage(2, obtain).sendToTarget();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onBiometricHelp(String str) {
        Log.d("BiometricDialogImpl", "onBiometricHelp: " + str);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = str;
        this.mHandler.obtainMessage(3, obtain).sendToTarget();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onBiometricError(String str) {
        Log.d("BiometricDialogImpl", "onBiometricError: " + str);
        this.mHandler.obtainMessage(4, str).sendToTarget();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void hideBiometricDialog() {
        Log.d("BiometricDialogImpl", "hideBiometricDialog");
        this.mHandler.obtainMessage(5, false).sendToTarget();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleShowDialog(SomeArgs someArgs, boolean z, Bundle bundle) {
        BiometricDialogView biometricDialogView;
        this.mCurrentDialogArgs = someArgs;
        int i = someArgs.argi1;
        if (i == 1) {
            biometricDialogView = new FingerprintDialogView(this.mContext, this.mCallback);
        } else if (i == 4) {
            biometricDialogView = new FaceDialogView(this.mContext, this.mCallback);
        } else {
            Log.e("BiometricDialogImpl", "Unsupported type: " + i);
            return;
        }
        Log.d("BiometricDialogImpl", "handleShowDialog,  savedState: " + bundle + " mCurrentDialog: " + this.mCurrentDialog + " newDialog: " + biometricDialogView + " type: " + i);
        if (bundle != null) {
            biometricDialogView.restoreState(bundle);
        } else {
            BiometricDialogView biometricDialogView2 = this.mCurrentDialog;
            if (biometricDialogView2 != null && this.mDialogShowing) {
                biometricDialogView2.forceRemove();
            }
        }
        this.mReceiver = (IBiometricServiceReceiverInternal) someArgs.arg2;
        biometricDialogView.setBundle((Bundle) someArgs.arg1);
        biometricDialogView.setRequireConfirmation(((Boolean) someArgs.arg3).booleanValue());
        biometricDialogView.setUserId(someArgs.argi2);
        biometricDialogView.setSkipIntro(z);
        this.mCurrentDialog = biometricDialogView;
        WindowManager windowManager = this.mWindowManager;
        BiometricDialogView biometricDialogView3 = this.mCurrentDialog;
        windowManager.addView(biometricDialogView3, biometricDialogView3.getLayoutParams());
        this.mDialogShowing = true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleBiometricAuthenticated(boolean z, String str) {
        Log.d("BiometricDialogImpl", "handleBiometricAuthenticated: " + z);
        if (z) {
            this.mCurrentDialog.announceForAccessibility(this.mContext.getResources().getText(this.mCurrentDialog.getAuthenticatedAccessibilityResourceId()));
            if (this.mCurrentDialog.requiresConfirmation()) {
                this.mCurrentDialog.updateState(3);
                return;
            }
            this.mCurrentDialog.updateState(4);
            this.mHandler.postDelayed(new Runnable() {
                /* class com.android.systemui.biometrics.$$Lambda$BiometricDialogImpl$ClyZbr2BpugYn9TuyRxsmSCP_U */

                public final void run() {
                    BiometricDialogImpl.this.lambda$handleBiometricAuthenticated$0$BiometricDialogImpl();
                }
            }, (long) this.mCurrentDialog.getDelayAfterAuthenticatedDurationMs());
            return;
        }
        this.mCurrentDialog.onAuthenticationFailed(str);
    }

    public /* synthetic */ void lambda$handleBiometricAuthenticated$0$BiometricDialogImpl() {
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleBiometricHelp(String str) {
        Log.d("BiometricDialogImpl", "handleBiometricHelp: " + str);
        this.mCurrentDialog.onHelpReceived(str);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleBiometricError(String str) {
        Log.d("BiometricDialogImpl", "handleBiometricError: " + str);
        if (!this.mDialogShowing) {
            Log.d("BiometricDialogImpl", "Dialog already dismissed");
        } else {
            this.mCurrentDialog.onErrorReceived(str);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleHideDialog(boolean z) {
        Log.d("BiometricDialogImpl", "handleHideDialog, userCanceled: " + z);
        if (!this.mDialogShowing) {
            Log.w("BiometricDialogImpl", "Dialog already dismissed, userCanceled: " + z);
            return;
        }
        if (z) {
            try {
                this.mReceiver.onDialogDismissed(3);
            } catch (RemoteException e) {
                Log.e("BiometricDialogImpl", "RemoteException when hiding dialog", e);
            }
        }
        this.mReceiver = null;
        this.mDialogShowing = false;
        this.mCurrentDialog.startDismiss();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleButtonNegative() {
        IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal = this.mReceiver;
        if (iBiometricServiceReceiverInternal == null) {
            Log.e("BiometricDialogImpl", "Receiver is null");
            return;
        }
        try {
            iBiometricServiceReceiverInternal.onDialogDismissed(2);
        } catch (RemoteException e) {
            Log.e("BiometricDialogImpl", "Remote exception when handling negative button", e);
        }
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleButtonPositive() {
        IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal = this.mReceiver;
        if (iBiometricServiceReceiverInternal == null) {
            Log.e("BiometricDialogImpl", "Receiver is null");
            return;
        }
        try {
            iBiometricServiceReceiverInternal.onDialogDismissed(1);
        } catch (RemoteException e) {
            Log.e("BiometricDialogImpl", "Remote exception when handling positive button", e);
        }
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleUserCanceled() {
        handleHideDialog(true);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleTryAgainPressed() {
        try {
            this.mReceiver.onTryAgainPressed();
        } catch (RemoteException e) {
            Log.e("BiometricDialogImpl", "RemoteException when handling try again", e);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.SystemUI
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        boolean z = this.mDialogShowing;
        Bundle bundle = new Bundle();
        BiometricDialogView biometricDialogView = this.mCurrentDialog;
        if (biometricDialogView != null) {
            biometricDialogView.onSaveState(bundle);
        }
        if (this.mDialogShowing) {
            this.mCurrentDialog.forceRemove();
            this.mDialogShowing = false;
        }
        if (z) {
            handleShowDialog(this.mCurrentDialogArgs, true, bundle);
        }
    }
}
