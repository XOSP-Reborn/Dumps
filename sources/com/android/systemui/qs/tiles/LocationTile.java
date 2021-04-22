package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.widget.Switch;
import androidx.appcompat.R$styleable;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.sonymobile.settingslib.logging.LoggingManager;

public class LocationTile extends QSTileImpl<QSTile.BooleanState> {
    private final ActivityStarter mActivityStarter;
    private final Callback mCallback = new Callback();
    private final LocationController mController;
    private final QSTile.Icon mIcon = QSTileImpl.ResourceIcon.get(C0006R$drawable.ic_location);
    private final KeyguardMonitor mKeyguard;

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public int getMetricsCategory() {
        return R$styleable.AppCompatTheme_windowMinWidthMajor;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
    }

    public LocationTile(QSHost qSHost, LocationController locationController, KeyguardMonitor keyguardMonitor, ActivityStarter activityStarter) {
        super(qSHost);
        this.mController = locationController;
        this.mKeyguard = keyguardMonitor;
        this.mActivityStarter = activityStarter;
        this.mController.observe(this, this.mCallback);
        this.mKeyguard.observe(this, this.mCallback);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleClick() {
        if (!this.mKeyguard.isSecure() || !this.mKeyguard.isShowing()) {
            boolean z = ((QSTile.BooleanState) this.mState).value;
            LoggingManager.logQSEvent(this.mContext, "location", "click", Boolean.valueOf(!z));
            this.mController.setLocationEnabled(!z);
            return;
        }
        this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable() {
            /* class com.android.systemui.qs.tiles.$$Lambda$LocationTile$cnlxD4jGztrpcRYGbQTKRSm3Ng0 */

            public final void run() {
                LocationTile.this.lambda$handleClick$0$LocationTile();
            }
        });
    }

    public /* synthetic */ void lambda$handleClick$0$LocationTile() {
        boolean z = ((QSTile.BooleanState) this.mState).value;
        this.mHost.openPanels();
        LoggingManager.logQSEvent(this.mContext, "location", "click", Boolean.valueOf(!z));
        this.mController.setLocationEnabled(!z);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(C0014R$string.quick_settings_location_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        boolean isLocationEnabled = this.mController.isLocationEnabled();
        booleanState.value = isLocationEnabled;
        checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_share_location");
        if (!booleanState.disabledByPolicy) {
            checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_config_location");
        }
        booleanState.icon = this.mIcon;
        int i = 1;
        booleanState.slash.isSlashed = !booleanState.value;
        if (isLocationEnabled) {
            booleanState.label = this.mContext.getString(C0014R$string.quick_settings_location_label);
            booleanState.contentDescription = this.mContext.getString(C0014R$string.accessibility_quick_settings_location_on);
        } else {
            booleanState.label = this.mContext.getString(C0014R$string.quick_settings_location_label);
            booleanState.contentDescription = this.mContext.getString(C0014R$string.accessibility_quick_settings_location_off);
        }
        if (booleanState.value) {
            i = 2;
        }
        booleanState.state = i;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(C0014R$string.accessibility_quick_settings_location_changed_on);
        }
        return this.mContext.getString(C0014R$string.accessibility_quick_settings_location_changed_off);
    }

    private final class Callback implements LocationController.LocationChangeCallback, KeyguardMonitor.Callback {
        private Callback() {
        }

        @Override // com.android.systemui.statusbar.policy.LocationController.LocationChangeCallback
        public void onLocationSettingsChanged(boolean z) {
            LocationTile.this.refreshState();
        }

        @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
        public void onKeyguardShowingChanged() {
            LocationTile.this.refreshState();
        }
    }
}
