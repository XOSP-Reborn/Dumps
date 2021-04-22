package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.SystemProperties;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.sonymobile.settingslib.logging.LoggingManager;

public class AirplaneModeTile extends QSTileImpl<QSTile.BooleanState> {
    private final ActivityStarter mActivityStarter;
    private final QSTile.Icon mIcon = QSTileImpl.ResourceIcon.get(17302780);
    private boolean mListening;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.qs.tiles.AirplaneModeTile.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                AirplaneModeTile.this.refreshState();
            }
        }
    };
    private final GlobalSetting mSetting;

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public int getMetricsCategory() {
        return 112;
    }

    public AirplaneModeTile(QSHost qSHost, ActivityStarter activityStarter) {
        super(qSHost);
        this.mActivityStarter = activityStarter;
        this.mSetting = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
            /* class com.android.systemui.qs.tiles.AirplaneModeTile.AnonymousClass1 */

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.qs.GlobalSetting
            public void handleValueChanged(int i) {
                AirplaneModeTile.this.handleRefreshState(Integer.valueOf(i));
            }
        };
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleClick() {
        boolean z = ((QSTile.BooleanState) this.mState).value;
        MetricsLogger.action(this.mContext, getMetricsCategory(), !z);
        LoggingManager.logQSEvent(this.mContext, "airplane", "click", Boolean.valueOf(!((QSTile.BooleanState) this.mState).value));
        if (z || !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
            setEnabled(!z);
        } else {
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS"), 0);
        }
    }

    private void setEnabled(boolean z) {
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).setAirplaneMode(z);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.AIRPLANE_MODE_SETTINGS");
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(C0014R$string.airplane_mode);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_airplane_mode");
        int i = 1;
        boolean z = (obj instanceof Integer ? ((Integer) obj).intValue() : this.mSetting.getValue()) != 0;
        booleanState.value = z;
        booleanState.label = this.mContext.getString(C0014R$string.airplane_mode);
        booleanState.icon = this.mIcon;
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        booleanState.slash.isSlashed = !z;
        if (z) {
            i = 2;
        }
        booleanState.state = i;
        booleanState.contentDescription = booleanState.label;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(C0014R$string.accessibility_quick_settings_airplane_changed_on);
        }
        return this.mContext.getString(C0014R$string.accessibility_quick_settings_airplane_changed_off);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            if (z) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
                this.mContext.registerReceiver(this.mReceiver, intentFilter);
            } else {
                this.mContext.unregisterReceiver(this.mReceiver);
            }
            this.mSetting.setListening(z);
        }
    }
}
