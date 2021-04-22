package com.android.systemui.qs.tiles;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;

public class UiModeNightTile extends QSTileImpl<QSTile.BooleanState> implements ConfigurationController.ConfigurationListener, BatteryController.BatteryStateChangeCallback {
    private final BatteryController mBatteryController;
    private final QSTile.Icon mIcon = QSTileImpl.ResourceIcon.get(17302787);
    private final UiModeManager mUiModeManager;

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public int getMetricsCategory() {
        return 1706;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
    }

    public UiModeNightTile(QSHost qSHost, ConfigurationController configurationController, BatteryController batteryController) {
        super(qSHost);
        this.mBatteryController = batteryController;
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        configurationController.observe(getLifecycle(), this);
        batteryController.observe(getLifecycle(), this);
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onUiModeChanged() {
        refreshState();
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
    public void onPowerSaveChanged(boolean z) {
        refreshState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleClick() {
        if (((QSTile.BooleanState) getState()).state != 0) {
            int i = 1;
            boolean z = !((QSTile.BooleanState) this.mState).value;
            UiModeManager uiModeManager = this.mUiModeManager;
            if (z) {
                i = 2;
            }
            uiModeManager.setNightMode(i);
            refreshState(Boolean.valueOf(z));
        }
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        int i;
        boolean isPowerSave = this.mBatteryController.isPowerSave();
        int i2 = 1;
        booleanState.value = (this.mContext.getResources().getConfiguration().uiMode & 48) == 32;
        Context context = this.mContext;
        if (isPowerSave) {
            i = C0014R$string.quick_settings_ui_mode_night_label_battery_saver;
        } else {
            i = C0014R$string.quick_settings_ui_mode_night_label;
        }
        booleanState.label = context.getString(i);
        booleanState.contentDescription = booleanState.label;
        booleanState.icon = this.mIcon;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        if (isPowerSave) {
            booleanState.state = 0;
        } else {
            if (booleanState.value) {
                i2 = 2;
            }
            booleanState.state = i2;
        }
        booleanState.showRippleEffect = false;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.DISPLAY_SETTINGS");
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return ((QSTile.BooleanState) getState()).label;
    }
}
