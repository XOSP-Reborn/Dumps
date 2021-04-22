package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Intent;
import android.widget.Switch;
import androidx.appcompat.R$styleable;
import com.android.settingslib.TetherUtil;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0012R$plurals;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.sonymobile.settingslib.logging.LoggingManager;
import com.sonymobile.systemui.qs.tiles.ForegroundChecker;
import com.sonymobile.systemui.qs.tiles.TetheringDialogDummyActivity;

public class HotspotTile extends QSTileImpl<QSTile.BooleanState> {
    private static final Intent TETHER_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
    private final HotspotAndDataSaverCallbacks mCallbacks = new HotspotAndDataSaverCallbacks();
    private final DataSaverController mDataSaverController;
    private final QSTile.Icon mEnabledStatic = QSTileImpl.ResourceIcon.get(C0006R$drawable.ic_hotspot);
    private ForegroundChecker mForegroundChecker = null;
    private final HotspotController mHotspotController;
    private boolean mListening;

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public int getMetricsCategory() {
        return R$styleable.AppCompatTheme_windowFixedWidthMajor;
    }

    public HotspotTile(QSHost qSHost, HotspotController hotspotController, DataSaverController dataSaverController) {
        super(qSHost);
        this.mHotspotController = hotspotController;
        this.mDataSaverController = dataSaverController;
        this.mHotspotController.observe(this, this.mCallbacks);
        this.mDataSaverController.observe(this, this.mCallbacks);
    }

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public boolean isAvailable() {
        return this.mHotspotController.isHotspotSupported();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleDestroy() {
        super.handleDestroy();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            if (z) {
                refreshState();
            }
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent(TETHER_SETTINGS);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleClick() {
        boolean z = ((QSTile.BooleanState) this.mState).value;
        if (z || !this.mDataSaverController.isDataSaverEnabled()) {
            Object obj = null;
            LoggingManager.logQSEvent(this.mContext, "hotspot", "click", null);
            if (z || TetherUtil.isProvisioningNeeded(this.mContext)) {
                if (!z) {
                    obj = QSTileImpl.ARG_SHOW_TRANSIENT_ENABLING;
                }
                refreshState(obj);
                this.mHotspotController.setHotspotEnabled(!z);
                return;
            }
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() {
                /* class com.android.systemui.qs.tiles.$$Lambda$HotspotTile$p0uo47GQQYmpVGSu1ovI6RRDgA */

                public final void run() {
                    HotspotTile.this.lambda$handleClick$0$HotspotTile();
                }
            });
        }
    }

    public /* synthetic */ void lambda$handleClick$0$HotspotTile() {
        Intent intent = new Intent(this.mContext, TetheringDialogDummyActivity.class);
        intent.setFlags(268435456);
        collapseOnNewDialog();
        this.mContext.startActivity(intent);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(C0014R$string.quick_settings_hotspot_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        boolean z;
        int i;
        int i2 = 1;
        boolean z2 = obj == QSTileImpl.ARG_SHOW_TRANSIENT_ENABLING;
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        boolean z3 = z2 || this.mHotspotController.isHotspotTransient();
        checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_config_tethering");
        if (obj instanceof CallbackInfo) {
            CallbackInfo callbackInfo = (CallbackInfo) obj;
            booleanState.value = z2 || callbackInfo.isHotspotEnabled;
            i = callbackInfo.numConnectedDevices;
            z = callbackInfo.isDataSaverEnabled;
        } else {
            booleanState.value = z2 || this.mHotspotController.isHotspotEnabled();
            i = this.mHotspotController.getNumConnectedDevices();
            z = this.mDataSaverController.isDataSaverEnabled();
        }
        booleanState.icon = this.mEnabledStatic;
        booleanState.label = this.mContext.getString(C0014R$string.quick_settings_hotspot_label);
        booleanState.isTransient = z3;
        booleanState.slash.isSlashed = !booleanState.value && !booleanState.isTransient;
        if (booleanState.isTransient) {
            booleanState.icon = QSTileImpl.ResourceIcon.get(17302427);
        }
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.contentDescription = booleanState.label;
        boolean z4 = booleanState.value || booleanState.isTransient;
        if (z) {
            booleanState.state = 0;
        } else {
            if (z4) {
                i2 = 2;
            }
            booleanState.state = i2;
        }
        booleanState.secondaryLabel = getSecondaryLabel(z4, z3, z, i);
    }

    private String getSecondaryLabel(boolean z, boolean z2, boolean z3, int i) {
        if (z2) {
            return this.mContext.getString(C0014R$string.quick_settings_hotspot_secondary_label_transient);
        }
        if (z3) {
            return this.mContext.getString(C0014R$string.quick_settings_hotspot_secondary_label_data_saver_enabled);
        }
        if (i <= 0 || !z) {
            return null;
        }
        return this.mContext.getResources().getQuantityString(C0012R$plurals.quick_settings_hotspot_secondary_label_num_devices, i, Integer.valueOf(i));
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(C0014R$string.accessibility_quick_settings_hotspot_changed_on);
        }
        return this.mContext.getString(C0014R$string.accessibility_quick_settings_hotspot_changed_off);
    }

    private final class HotspotAndDataSaverCallbacks implements HotspotController.Callback, DataSaverController.Listener {
        CallbackInfo mCallbackInfo;

        private HotspotAndDataSaverCallbacks() {
            this.mCallbackInfo = new CallbackInfo();
        }

        @Override // com.android.systemui.statusbar.policy.DataSaverController.Listener
        public void onDataSaverChanged(boolean z) {
            CallbackInfo callbackInfo = this.mCallbackInfo;
            callbackInfo.isDataSaverEnabled = z;
            HotspotTile.this.refreshState(callbackInfo);
        }

        @Override // com.android.systemui.statusbar.policy.HotspotController.Callback
        public void onHotspotChanged(boolean z, int i) {
            CallbackInfo callbackInfo = this.mCallbackInfo;
            callbackInfo.isHotspotEnabled = z;
            callbackInfo.numConnectedDevices = i;
            HotspotTile.this.refreshState(callbackInfo);
        }
    }

    /* access modifiers changed from: protected */
    public static final class CallbackInfo {
        boolean isDataSaverEnabled;
        boolean isHotspotEnabled;
        int numConnectedDevices;

        protected CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[" + "isHotspotEnabled=" + this.isHotspotEnabled + ",numConnectedDevices=" + this.numConnectedDevices + ",isDataSaverEnabled=" + this.isDataSaverEnabled + ']';
        }
    }

    private void collapseOnNewDialog() {
        this.mForegroundChecker = new ForegroundChecker(this.mContext);
        ForegroundChecker foregroundChecker = this.mForegroundChecker;
        if (foregroundChecker != null) {
            this.mHandler.postDelayed(foregroundChecker, 400);
        }
    }
}
