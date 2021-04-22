package com.sonymobile.systemui.lockscreen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0008R$integer;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.sonymobile.keyguard.aod.AodView;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import com.sonymobile.xperiaxloops.IXperiaXLoopsService;
import com.sonymobile.xperiaxloops.IXperiaXLoopsServiceCallback;

public class LockscreenLoopsController {
    private static final String TAG = "LockscreenLoopsController";
    private AodView mAodView;
    private boolean mBouncer = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenLoopsController.AnonymousClass1 */

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LockscreenLoopsController.this.mService = IXperiaXLoopsService.Stub.asInterface(iBinder);
            LockscreenLoopsController.this.onThemeChanged();
            LockscreenLoopsController.this.setLoopsColorOnAmbient();
        }

        public void onServiceDisconnected(ComponentName componentName) {
            LockscreenLoopsController.this.mService = null;
        }
    };
    private final Context mContext;
    private boolean mDozing = false;
    private int mKeyguardStatus = -1;
    private LockscreenLoopsControllerCallback mLlcCurrentCallback;
    private LockscreenLoopsControllerCallback mLlcDozingCallback;
    private LockscreenLoopsControllerCallback mLlcNormalCallback;
    private IXperiaXLoopsServiceCallback mLoopsServiceCallback = new IXperiaXLoopsServiceCallback.Stub() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenLoopsController.AnonymousClass2 */

        @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsServiceCallback
        public void show() throws RemoteException {
            if (LockscreenLoopsController.this.mLlcCurrentCallback != null) {
                LockscreenLoopsController.this.mLlcCurrentCallback.show();
            }
        }

        @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsServiceCallback
        public void hide(boolean z) throws RemoteException {
            if (LockscreenLoopsController.this.mLlcCurrentCallback != null) {
                LockscreenLoopsController.this.mLlcCurrentCallback.hide(z);
            }
        }
    };
    private NotificationPanelView mNotificationPanel;
    private IXperiaXLoopsService mService;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenLoopsController.AnonymousClass3 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardVisibilityChanged(boolean z) {
            LockscreenLoopsController.this.updateKeyguardStatus();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardBouncerChanged(boolean z) {
            LockscreenLoopsController.this.mBouncer = z;
            LockscreenLoopsController.this.updateKeyguardStatus();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricAuthenticated(int i, BiometricSourceType biometricSourceType) {
            LockscreenLoopsController.this.handleFPAEvent(0);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricAuthFailed(BiometricSourceType biometricSourceType) {
            if (LockscreenLoopsController.this.mUpdateMonitor.isDeviceInteractive() || (LockscreenLoopsController.this.mDozing && LockscreenLoopsController.this.mUpdateMonitor.isScreenOn())) {
                LockscreenLoopsController.this.handleFPAEvent(1);
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricHelp(int i, String str, BiometricSourceType biometricSourceType) {
            if ((LockscreenLoopsController.this.mUpdateMonitor.isDeviceInteractive() || (LockscreenLoopsController.this.mDozing && LockscreenLoopsController.this.mUpdateMonitor.isScreenOn())) && i == 5) {
                LockscreenLoopsController.this.handleFPAEvent(2);
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onScreenTurnedOn() {
            LockscreenLoopsController.this.sendScreenStatus(1);
            LockscreenLoopsController.this.updateKeyguardStatus();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onScreenTurnedOff() {
            LockscreenLoopsController.this.sendScreenStatus(0);
            LockscreenLoopsController.this.updateKeyguardStatus();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitchComplete(int i) {
            LockscreenLoopsController.this.sendScreenStatus(1);
            LockscreenLoopsController.this.mKeyguardStatus = -1;
            LockscreenLoopsController.this.updateKeyguardStatus();
        }
    };

    public LockscreenLoopsController(Context context) {
        this.mContext = context;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
    }

    public void init(NotificationPanelView notificationPanelView, AodView aodView) {
        this.mNotificationPanel = notificationPanelView;
        this.mAodView = aodView;
        connect();
        updateKeyguardStatus();
    }

    private void connect() {
        Intent intent = new Intent();
        intent.setClassName("com.sonymobile.xperiaxloops", "com.sonymobile.xperiaxloops.XperiaXLoopsService");
        this.mContext.bindService(intent, this.mConnection, 1);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateKeyguardStatus() {
        int i = this.mUpdateMonitor.isKeyguardVisible() ? (!this.mDozing || !this.mUpdateMonitor.isScreenOn() || this.mUpdateMonitor.isDeviceInteractive()) ? this.mBouncer ? 1 : 0 : 2 : 3;
        if (i != this.mKeyguardStatus) {
            this.mKeyguardStatus = i;
            sendKeyguardStatus(i);
        }
    }

    public boolean isConnected() {
        return this.mService != null;
    }

    private void registerCallback(int i) {
        IXperiaXLoopsServiceCallback iXperiaXLoopsServiceCallback;
        IXperiaXLoopsService iXperiaXLoopsService = this.mService;
        if (iXperiaXLoopsService != null && (iXperiaXLoopsServiceCallback = this.mLoopsServiceCallback) != null) {
            try {
                iXperiaXLoopsService.registerCallback(i, iXperiaXLoopsServiceCallback);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void registerCallback(int i, LockscreenLoopsControllerCallback lockscreenLoopsControllerCallback, boolean z) {
        if (z) {
            this.mLlcDozingCallback = lockscreenLoopsControllerCallback;
        } else {
            this.mLlcNormalCallback = lockscreenLoopsControllerCallback;
        }
        if (z == this.mDozing) {
            this.mLlcCurrentCallback = lockscreenLoopsControllerCallback;
            registerCallback(i);
        }
    }

    private void unregisterCallback(int i) {
        IXperiaXLoopsServiceCallback iXperiaXLoopsServiceCallback;
        IXperiaXLoopsService iXperiaXLoopsService = this.mService;
        if (iXperiaXLoopsService != null && (iXperiaXLoopsServiceCallback = this.mLoopsServiceCallback) != null) {
            try {
                iXperiaXLoopsService.unregisterCallback(i, iXperiaXLoopsServiceCallback);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void unregisterCallback(int i, boolean z) {
        if (z) {
            this.mLlcDozingCallback = null;
        } else {
            this.mLlcNormalCallback = null;
        }
        if (z == this.mDozing) {
            this.mLlcCurrentCallback = null;
            unregisterCallback(i);
        }
    }

    public final void onThemeChanged() {
        int i;
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            i = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
        } else {
            i = Utils.getColorAttrDefaultColor(this.mContext, C0002R$attr.wallpaperTextColor);
        }
        setLoopsColorOnLockscreen(i);
    }

    public void setLoopsColorOnLockscreen(int i) {
        IXperiaXLoopsService iXperiaXLoopsService = this.mService;
        if (iXperiaXLoopsService != null) {
            try {
                iXperiaXLoopsService.setLoopsColorOnLockscreen(i);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void setLoopsColorOnAmbient() {
        if (this.mService != null) {
            try {
                Resources resources = this.mContext.getResources();
                if (resources != null) {
                    int integer = resources.getInteger(C0008R$integer.config_aodScrimOpacity);
                    int i = 16777215;
                    if (integer > 0) {
                        int i2 = 255 - integer;
                        i = (i2 << 16) | (i2 << 8) | i2;
                    }
                    this.mService.setLoopsColorOnAmbient(i);
                }
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void setDozing(boolean z) {
        if (z) {
            LockscreenLoopsControllerCallback lockscreenLoopsControllerCallback = this.mLlcNormalCallback;
            if (lockscreenLoopsControllerCallback != null) {
                lockscreenLoopsControllerCallback.stopClockForDozing();
            }
            this.mDozing = z;
            LockscreenLoopsControllerCallback lockscreenLoopsControllerCallback2 = this.mLlcDozingCallback;
            if (lockscreenLoopsControllerCallback2 != null) {
                lockscreenLoopsControllerCallback2.restartClockForDozing();
            }
        } else {
            LockscreenLoopsControllerCallback lockscreenLoopsControllerCallback3 = this.mLlcDozingCallback;
            if (lockscreenLoopsControllerCallback3 != null) {
                lockscreenLoopsControllerCallback3.stopClockForDozing();
            }
            this.mDozing = z;
            LockscreenLoopsControllerCallback lockscreenLoopsControllerCallback4 = this.mLlcNormalCallback;
            if (lockscreenLoopsControllerCallback4 != null) {
                lockscreenLoopsControllerCallback4.restartClockForDozing();
            }
        }
        updateKeyguardStatus();
    }

    public void requestAssistEmphasis(boolean z, float f, float f2, float f3, float f4) {
        if (this.mService != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("ICON_EMPHASIS", z);
            bundle.putFloat("ICON_X", f);
            bundle.putFloat("ICON_Y", f2);
            bundle.putFloat("ICON_WIDTH", f3);
            bundle.putFloat("ICON_HEIGHT", f4);
            try {
                this.mService.requestAssistEmphasis(bundle);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void sendKeyguardStatus(int i) {
        IXperiaXLoopsService iXperiaXLoopsService = this.mService;
        if (iXperiaXLoopsService != null) {
            try {
                iXperiaXLoopsService.sendKeyguardStatus(i);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleFPAEvent(int i) {
        Rect keyguardClockBounds;
        if (this.mDozing) {
            AodView aodView = this.mAodView;
            if (aodView != null) {
                Rect clockViewPosition = aodView.getClockViewPosition();
                sendFPAResult(i, clockViewPosition.exactCenterX(), clockViewPosition.exactCenterY(), ((float) Math.max(clockViewPosition.width(), clockViewPosition.height())) / 2.0f);
            }
        } else if (this.mBouncer) {
            NotificationPanelView notificationPanelView = this.mNotificationPanel;
            if (notificationPanelView != null) {
                int height = notificationPanelView.getHeight();
                float width = (float) this.mNotificationPanel.getWidth();
                sendFPAResult(i, width / 2.0f, ((float) height) / 4.0f, width / 3.0f);
            }
        } else {
            NotificationPanelView notificationPanelView2 = this.mNotificationPanel;
            if (notificationPanelView2 != null && (keyguardClockBounds = notificationPanelView2.getKeyguardClockBounds()) != null) {
                sendFPAResult(i, keyguardClockBounds.exactCenterX(), keyguardClockBounds.exactCenterY(), ((float) Math.max(keyguardClockBounds.width(), keyguardClockBounds.height())) / 2.0f);
            }
        }
    }

    private void sendFPAResult(int i, float f, float f2, float f3) {
        if (this.mService != null) {
            try {
                Bundle bundle = new Bundle();
                bundle.putFloat("POS_X", f);
                bundle.putFloat("POS_Y", f2);
                bundle.putFloat("RADIUS", f3);
                this.mService.sendFPAResult(i, bundle);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sendScreenStatus(int i) {
        IXperiaXLoopsService iXperiaXLoopsService = this.mService;
        if (iXperiaXLoopsService != null) {
            try {
                iXperiaXLoopsService.sendScreenStatus(i);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
