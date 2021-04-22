package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import androidx.appcompat.R$styleable;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import com.sonymobile.settingslib.logging.LoggingManager;
import com.sonymobile.systemui.qs.tiles.ForegroundChecker;

public class CellularTile extends QSTileImpl<QSTile.SignalState> {
    private static final Intent DATA_TRAFFIC_SWITCH = new Intent().setAction("com.android.phone.intent.ACTION_DATA_TRAFFIC_SWITCH");
    private final ActivityStarter mActivityStarter;
    private final NetworkController mController;
    private final DataUsageController mDataController;
    private final CellularDetailAdapter mDetailAdapter;
    private final CellSignalCallback mSignalCallback = new CellSignalCallback();

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public int getMetricsCategory() {
        return R$styleable.AppCompatTheme_windowActionBar;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
    }

    public CellularTile(QSHost qSHost, NetworkController networkController, ActivityStarter activityStarter) {
        super(qSHost);
        this.mController = networkController;
        this.mActivityStarter = activityStarter;
        this.mDataController = this.mController.getMobileDataController();
        this.mDetailAdapter = new CellularDetailAdapter();
        this.mController.observe(getLifecycle(), this.mSignalCallback);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return getCellularSettingIntent();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleClick() {
        LoggingManager.logQSEvent(this.mContext, "cell", "click", null);
        if (((QSTile.SignalState) getState()).state != 0) {
            this.mContext.sendBroadcast(DATA_TRAFFIC_SWITCH);
            this.mHandler.postDelayed(new ForegroundChecker(this.mContext), 400);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSecondaryClick() {
        if (this.mDataController.isMobileDataSupported()) {
            showDetail(true);
        } else {
            this.mActivityStarter.postStartActivityDismissingKeyguard(getCellularSettingIntent(), 0);
        }
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(C0014R$string.quick_settings_cellular_detail_title);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.SignalState signalState, Object obj) {
        Object obj2;
        CallbackInfo callbackInfo = (CallbackInfo) obj;
        if (callbackInfo == null) {
            callbackInfo = this.mSignalCallback.mInfo;
        }
        Resources resources = this.mContext.getResources();
        signalState.label = resources.getString(C0014R$string.mobile_data);
        boolean z = this.mDataController.isMobileDataSupported() && this.mDataController.isMobileDataEnabled();
        signalState.value = z;
        signalState.activityIn = z && callbackInfo.activityIn;
        signalState.activityOut = z && callbackInfo.activityOut;
        signalState.expandedAccessibilityClassName = Switch.class.getName();
        if (callbackInfo.noSim || (ActivityManager.getCurrentUser() != 0)) {
            signalState.icon = QSTileImpl.ResourceIcon.get(C0006R$drawable.ic_qs_no_sim);
        } else {
            signalState.icon = QSTileImpl.ResourceIcon.get(C0006R$drawable.ic_swap_vert);
        }
        if (callbackInfo.noSim || (ActivityManager.getCurrentUser() != 0)) {
            signalState.state = 0;
            if (callbackInfo.noSim) {
                signalState.secondaryLabel = resources.getString(C0014R$string.keyguard_missing_sim_message_short);
            }
        } else if (callbackInfo.airplaneModeEnabled) {
            signalState.state = 0;
            signalState.secondaryLabel = resources.getString(C0014R$string.status_bar_airplane);
        } else if (z) {
            signalState.state = 2;
            signalState.secondaryLabel = appendMobileDataType(callbackInfo.multipleSubs ? callbackInfo.dataSubscriptionName : "", getMobileDataContentName(callbackInfo));
        } else {
            signalState.state = 1;
            signalState.secondaryLabel = resources.getString(C0014R$string.cell_data_off);
        }
        if (signalState.state == 1) {
            obj2 = resources.getString(C0014R$string.cell_data_off_content_description);
        } else {
            obj2 = signalState.secondaryLabel;
        }
        signalState.contentDescription = ((Object) signalState.label) + ", " + obj2;
    }

    private CharSequence appendMobileDataType(CharSequence charSequence, CharSequence charSequence2) {
        if (TextUtils.isEmpty(charSequence2)) {
            return charSequence;
        }
        if (TextUtils.isEmpty(charSequence)) {
            return charSequence2;
        }
        return this.mContext.getString(C0014R$string.mobile_carrier_text_format, charSequence, charSequence2);
    }

    private CharSequence getMobileDataContentName(CallbackInfo callbackInfo) {
        if (callbackInfo.roaming && !TextUtils.isEmpty(callbackInfo.dataContentDescription)) {
            String string = this.mContext.getString(C0014R$string.data_connection_roaming);
            String charSequence = callbackInfo.dataContentDescription.toString();
            return this.mContext.getString(C0014R$string.mobile_data_text_format, string, charSequence);
        } else if (callbackInfo.roaming) {
            return this.mContext.getString(C0014R$string.data_connection_roaming);
        } else {
            return callbackInfo.dataContentDescription;
        }
    }

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public boolean isAvailable() {
        return this.mController.hasMobileDataFeature();
    }

    /* access modifiers changed from: private */
    public static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean airplaneModeEnabled;
        CharSequence dataContentDescription;
        CharSequence dataSubscriptionName;
        boolean multipleSubs;
        boolean noSim;
        boolean roaming;

        private CallbackInfo() {
        }
    }

    /* access modifiers changed from: private */
    public final class CellSignalCallback implements NetworkController.SignalCallback {
        private final CallbackInfo mInfo;

        private CellSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setMobileDataIndicators(NetworkController.IconState iconState, NetworkController.IconState iconState2, int i, int i2, boolean z, boolean z2, int i3, String str, String str2, boolean z3, int i4, boolean z4) {
            if (iconState2 != null) {
                this.mInfo.dataSubscriptionName = CellularTile.this.mController.getMobileDataNetworkName();
                CallbackInfo callbackInfo = this.mInfo;
                if (str2 == null) {
                    str = null;
                }
                callbackInfo.dataContentDescription = str;
                CallbackInfo callbackInfo2 = this.mInfo;
                callbackInfo2.activityIn = z;
                callbackInfo2.activityOut = z2;
                callbackInfo2.roaming = z4;
                boolean z5 = true;
                if (CellularTile.this.mController.getNumberSubscriptions() <= 1) {
                    z5 = false;
                }
                callbackInfo2.multipleSubs = z5;
                CellularTile.this.refreshState(this.mInfo);
            }
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setNoSims(boolean z, boolean z2) {
            CallbackInfo callbackInfo = this.mInfo;
            callbackInfo.noSim = z;
            CellularTile.this.refreshState(callbackInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setIsAirplaneMode(NetworkController.IconState iconState) {
            CallbackInfo callbackInfo = this.mInfo;
            callbackInfo.airplaneModeEnabled = iconState.visible;
            CellularTile.this.refreshState(callbackInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setMobileDataEnabled(boolean z) {
            CellularTile.this.mDetailAdapter.setMobileDataEnabled(z);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Intent getCellularSettingIntent() {
        Intent intent = new Intent("codeaurora.intent.action.MOBILE_NETWORK_SETTINGS");
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        Context context = this.mContext;
        if (context == null || intent.resolveActivity(context.getPackageManager()) == null) {
            intent = new Intent("android.settings.NETWORK_OPERATOR_SETTINGS");
            if (defaultDataSubscriptionId != -1) {
                intent.putExtra("android.provider.extra.SUB_ID", SubscriptionManager.getDefaultDataSubscriptionId());
            }
            Log.d("CellularTile", "Using default network settings for sub: " + defaultDataSubscriptionId);
        } else {
            intent.putExtra("slot_id", SubscriptionManager.getSlotIndex(defaultDataSubscriptionId));
            Log.d("CellularTile", "Using vendor network settings for sub: " + defaultDataSubscriptionId);
        }
        return intent;
    }

    /* access modifiers changed from: private */
    public final class CellularDetailAdapter implements DetailAdapter {
        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public int getMetricsCategory() {
            return R$styleable.AppCompatTheme_windowActionModeOverlay;
        }

        private CellularDetailAdapter() {
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public CharSequence getTitle() {
            return ((QSTileImpl) CellularTile.this).mContext.getString(C0014R$string.quick_settings_cellular_detail_title);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Boolean getToggleState() {
            if (CellularTile.this.mDataController.isMobileDataSupported()) {
                return Boolean.valueOf(CellularTile.this.mDataController.isMobileDataEnabled());
            }
            return null;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Intent getSettingsIntent() {
            return CellularTile.this.getCellularSettingIntent();
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public void setToggleState(boolean z) {
            MetricsLogger.action(((QSTileImpl) CellularTile.this).mContext, 155, z);
            CellularTile.this.mDataController.setMobileDataEnabled(z);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            int i = 0;
            if (view == null) {
                view = LayoutInflater.from(((QSTileImpl) CellularTile.this).mContext).inflate(C0010R$layout.data_usage, viewGroup, false);
            }
            DataUsageDetailView dataUsageDetailView = (DataUsageDetailView) view;
            DataUsageController.DataUsageInfo dataUsageInfo = CellularTile.this.mDataController.getDataUsageInfo();
            if (dataUsageInfo == null) {
                return dataUsageDetailView;
            }
            dataUsageDetailView.bind(dataUsageInfo);
            View findViewById = dataUsageDetailView.findViewById(C0007R$id.roaming_text);
            if (!CellularTile.this.mSignalCallback.mInfo.roaming) {
                i = 4;
            }
            findViewById.setVisibility(i);
            return dataUsageDetailView;
        }

        public void setMobileDataEnabled(boolean z) {
            CellularTile.this.fireToggleStateChanged(z);
        }
    }
}
