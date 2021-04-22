package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.systemui.C0014R$string;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController;
import java.util.Objects;

public class WifiSignalController extends SignalController<WifiState, SignalController.IconGroup> {
    private final SignalController.IconGroup mDefaultWifiIconGroup;
    private final boolean mHasMobileData;
    private final SignalController.IconGroup mWifi4IconGroup;
    private final SignalController.IconGroup mWifi5IconGroup;
    private final SignalController.IconGroup mWifi6IconGroup;
    private final WifiStatusTracker mWifiTracker;

    public WifiSignalController(Context context, boolean z, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl, WifiManager wifiManager) {
        super("WifiSignalController", context, 1, callbackHandler, networkControllerImpl);
        this.mWifiTracker = new WifiStatusTracker(this.mContext, wifiManager, (NetworkScoreManager) context.getSystemService(NetworkScoreManager.class), (ConnectivityManager) context.getSystemService(ConnectivityManager.class), new Runnable() {
            /* class com.android.systemui.statusbar.policy.$$Lambda$WifiSignalController$AffzGdHvQakHA4bIzi_tW1MVLCY */

            public final void run() {
                WifiSignalController.this.handleStatusUpdated();
            }
        });
        this.mWifiTracker.setListening(true);
        this.mHasMobileData = z;
        if (wifiManager != null) {
            wifiManager.registerTrafficStateCallback(new WifiTrafficStateCallback(), null);
        }
        this.mDefaultWifiIconGroup = new SignalController.IconGroup("Wi-Fi Icons", WifiIcons.WIFI_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, 17302865, 17302865, 17302865, 17302865, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mWifi4IconGroup = new SignalController.IconGroup("Wi-Fi 4 Icons", WifiIcons.WIFI_4_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_4_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, 17302865, 17302865, 17302865, 17302865, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mWifi5IconGroup = new SignalController.IconGroup("Wi-Fi 5 Icons", WifiIcons.WIFI_5_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_5_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, 17302865, 17302865, 17302865, 17302865, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mWifi6IconGroup = new SignalController.IconGroup("Wi-Fi 6 Icons", WifiIcons.WIFI_6_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_6_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, 17302865, 17302865, 17302865, 17302865, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        SignalController.IconGroup iconGroup = this.mDefaultWifiIconGroup;
        ((WifiState) this.mLastState).iconGroup = iconGroup;
        ((WifiState) this.mCurrentState).iconGroup = iconGroup;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.policy.SignalController
    public WifiState cleanState() {
        return new WifiState();
    }

    /* access modifiers changed from: package-private */
    public void refreshLocale() {
        this.mWifiTracker.refreshLocale();
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public void notifyListeners(NetworkController.SignalCallback signalCallback) {
        T t = this.mCurrentState;
        boolean z = ((WifiState) t).enabled && (((WifiState) t).connected || !this.mHasMobileData);
        String str = z ? ((WifiState) this.mCurrentState).ssid : null;
        boolean z2 = z && ((WifiState) this.mCurrentState).ssid != null;
        String stringIfExists = getStringIfExists(getContentDescription());
        if (((WifiState) this.mCurrentState).inetCondition == 0) {
            stringIfExists = stringIfExists + "," + this.mContext.getString(C0014R$string.data_connection_no_internet);
        }
        NetworkController.IconState iconState = new NetworkController.IconState(z, getCurrentIconId(), stringIfExists);
        NetworkController.IconState iconState2 = new NetworkController.IconState(((WifiState) this.mCurrentState).connected, getQsCurrentIconId(), stringIfExists);
        T t2 = this.mCurrentState;
        boolean z3 = ((WifiState) t2).enabled;
        boolean z4 = z2 && ((WifiState) t2).activityIn;
        boolean z5 = z2 && ((WifiState) this.mCurrentState).activityOut;
        T t3 = this.mCurrentState;
        signalCallback.setWifiIndicators(z3, iconState, iconState2, z4, z5, str, ((WifiState) t3).isTransient, ((WifiState) t3).statusLabel);
    }

    private void updateIconGroup() {
        T t = this.mCurrentState;
        if (((WifiState) t).wifiGenerationVersion == 4) {
            ((WifiState) t).iconGroup = this.mWifi4IconGroup;
        } else if (((WifiState) t).wifiGenerationVersion == 5) {
            ((WifiState) t).iconGroup = ((WifiState) t).isReady ? this.mWifi6IconGroup : this.mWifi5IconGroup;
        } else if (((WifiState) t).wifiGenerationVersion == 6) {
            ((WifiState) t).iconGroup = this.mWifi6IconGroup;
        } else {
            ((WifiState) t).iconGroup = this.mDefaultWifiIconGroup;
        }
    }

    public void handleBroadcast(Intent intent) {
        this.mWifiTracker.handleBroadcast(intent);
        T t = this.mCurrentState;
        WifiStatusTracker wifiStatusTracker = this.mWifiTracker;
        ((WifiState) t).enabled = wifiStatusTracker.enabled;
        ((WifiState) t).connected = wifiStatusTracker.connected;
        ((WifiState) t).ssid = wifiStatusTracker.ssid;
        ((WifiState) t).rssi = wifiStatusTracker.rssi;
        ((WifiState) t).level = wifiStatusTracker.level;
        ((WifiState) t).statusLabel = wifiStatusTracker.statusLabel;
        ((WifiState) t).wifiGenerationVersion = wifiStatusTracker.wifiGeneration;
        ((WifiState) t).isReady = wifiStatusTracker.vhtMax8SpatialStreamsSupport && wifiStatusTracker.twtSupport;
        updateIconGroup();
        notifyListenersIfNecessary();
    }

    /* access modifiers changed from: private */
    public void handleStatusUpdated() {
        ((WifiState) this.mCurrentState).statusLabel = this.mWifiTracker.statusLabel;
        notifyListenersIfNecessary();
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setActivity(int i) {
        boolean z = false;
        ((WifiState) this.mCurrentState).activityIn = i == 3 || i == 1;
        WifiState wifiState = (WifiState) this.mCurrentState;
        if (i == 3 || i == 2) {
            z = true;
        }
        wifiState.activityOut = z;
        notifyListenersIfNecessary();
    }

    private class WifiTrafficStateCallback implements WifiManager.TrafficStateCallback {
        private WifiTrafficStateCallback() {
        }

        public void onStateChanged(int i) {
            WifiSignalController.this.setActivity(i);
        }
    }

    /* access modifiers changed from: package-private */
    public static class WifiState extends SignalController.State {
        boolean isReady;
        boolean isTransient;
        String ssid;
        String statusLabel;
        int wifiGenerationVersion;

        WifiState() {
        }

        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public void copyFrom(SignalController.State state) {
            super.copyFrom(state);
            WifiState wifiState = (WifiState) state;
            this.ssid = wifiState.ssid;
            this.wifiGenerationVersion = wifiState.wifiGenerationVersion;
            this.isReady = wifiState.isReady;
            this.isTransient = wifiState.isTransient;
            this.statusLabel = wifiState.statusLabel;
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public void toString(StringBuilder sb) {
            super.toString(sb);
            sb.append(",ssid=");
            sb.append(this.ssid);
            sb.append(",wifiGenerationVersion=");
            sb.append(this.wifiGenerationVersion);
            sb.append(",isReady=");
            sb.append(this.isReady);
            sb.append(",isTransient=");
            sb.append(this.isTransient);
            sb.append(",statusLabel=");
            sb.append(this.statusLabel);
        }

        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            WifiState wifiState = (WifiState) obj;
            if (Objects.equals(wifiState.ssid, this.ssid) && wifiState.wifiGenerationVersion == this.wifiGenerationVersion && wifiState.isReady == this.isReady && wifiState.isTransient == this.isTransient && TextUtils.equals(wifiState.statusLabel, this.statusLabel)) {
                return true;
            }
            return false;
        }
    }
}
