package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.UserManager;
import android.util.Log;
import com.android.systemui.statusbar.policy.HotspotController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class HotspotControllerImpl implements HotspotController, WifiManager.SoftApCallback {
    private static final boolean DEBUG = Log.isLoggable("HotspotController", 3);
    private final ArrayList<HotspotController.Callback> mCallbacks = new ArrayList<>();
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private int mHotspotState;
    private final Handler mMainHandler;
    private int mNumConnectedDevices;
    private boolean mWaitingForTerminalState;
    private final WifiManager mWifiManager;

    private static String stateToString(int i) {
        switch (i) {
            case 10:
                return "DISABLING";
            case 11:
                return "DISABLED";
            case 12:
                return "ENABLING";
            case 13:
                return "ENABLED";
            case 14:
                return "FAILED";
            default:
                return null;
        }
    }

    public HotspotControllerImpl(Context context, Handler handler) {
        this.mContext = context;
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mMainHandler = handler;
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public boolean isHotspotSupported() {
        return this.mConnectivityManager.isTetheringSupported() && this.mConnectivityManager.getTetherableWifiRegexs().length != 0 && UserManager.get(this.mContext).isUserAdmin(ActivityManager.getCurrentUser());
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("HotspotController state:");
        printWriter.print("  mHotspotState=");
        printWriter.println(stateToString(this.mHotspotState));
        printWriter.print("  mNumConnectedDevices=");
        printWriter.println(this.mNumConnectedDevices);
        printWriter.print("  mWaitingForTerminalState=");
        printWriter.println(this.mWaitingForTerminalState);
    }

    public void addCallback(HotspotController.Callback callback) {
        synchronized (this.mCallbacks) {
            if (callback != null) {
                if (!this.mCallbacks.contains(callback)) {
                    if (DEBUG) {
                        Log.d("HotspotController", "addCallback " + callback);
                    }
                    this.mCallbacks.add(callback);
                    if (this.mWifiManager != null) {
                        if (this.mCallbacks.size() == 1) {
                            this.mWifiManager.registerSoftApCallback(this, this.mMainHandler);
                        } else {
                            this.mMainHandler.post(new Runnable(callback) {
                                /* class com.android.systemui.statusbar.policy.$$Lambda$HotspotControllerImpl$C17PPPxxCRpTmr2izVaDhyC9AQ */
                                private final /* synthetic */ HotspotController.Callback f$1;

                                {
                                    this.f$1 = r2;
                                }

                                public final void run() {
                                    HotspotControllerImpl.this.lambda$addCallback$0$HotspotControllerImpl(this.f$1);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    public /* synthetic */ void lambda$addCallback$0$HotspotControllerImpl(HotspotController.Callback callback) {
        callback.onHotspotChanged(isHotspotEnabled(), this.mNumConnectedDevices);
    }

    public void removeCallback(HotspotController.Callback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("HotspotController", "removeCallback " + callback);
            }
            synchronized (this.mCallbacks) {
                this.mCallbacks.remove(callback);
                if (this.mCallbacks.isEmpty() && this.mWifiManager != null) {
                    this.mWifiManager.unregisterSoftApCallback(this);
                }
            }
        }
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public boolean isHotspotEnabled() {
        return this.mHotspotState == 13;
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public boolean isHotspotTransient() {
        return this.mWaitingForTerminalState || this.mHotspotState == 12;
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public void setHotspotEnabled(boolean z) {
        if (this.mWaitingForTerminalState) {
            if (DEBUG) {
                Log.d("HotspotController", "Ignoring setHotspotEnabled; waiting for terminal state.");
            }
        } else if (z) {
            this.mWaitingForTerminalState = true;
            if (DEBUG) {
                Log.d("HotspotController", "Starting tethering");
            }
            this.mConnectivityManager.startTethering(0, false, new ConnectivityManager.OnStartTetheringCallback() {
                /* class com.android.systemui.statusbar.policy.HotspotControllerImpl.AnonymousClass1 */

                public void onTetheringFailed() {
                    if (HotspotControllerImpl.DEBUG) {
                        Log.d("HotspotController", "onTetheringFailed");
                    }
                    HotspotControllerImpl.this.maybeResetSoftApState();
                    HotspotControllerImpl.this.fireHotspotChangedCallback();
                }
            });
        } else {
            this.mConnectivityManager.stopTethering(0);
        }
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public int getNumConnectedDevices() {
        return this.mNumConnectedDevices;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void fireHotspotChangedCallback() {
        synchronized (this.mCallbacks) {
            Iterator<HotspotController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onHotspotChanged(isHotspotEnabled(), this.mNumConnectedDevices);
            }
        }
    }

    public void onStateChanged(int i, int i2) {
        this.mHotspotState = i;
        maybeResetSoftApState();
        if (!isHotspotEnabled()) {
            this.mNumConnectedDevices = 0;
        }
        fireHotspotChangedCallback();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void maybeResetSoftApState() {
        if (this.mWaitingForTerminalState) {
            int i = this.mHotspotState;
            if (!(i == 11 || i == 13)) {
                if (i == 14) {
                    this.mConnectivityManager.stopTethering(0);
                } else {
                    return;
                }
            }
            this.mWaitingForTerminalState = false;
        }
    }

    public void onNumClientsChanged(int i) {
        this.mNumConnectedDevices = i;
        fireHotspotChangedCallback();
    }
}
