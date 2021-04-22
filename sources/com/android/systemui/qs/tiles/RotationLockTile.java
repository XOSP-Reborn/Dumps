package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.widget.Switch;
import androidx.appcompat.R$styleable;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.sonymobile.settingslib.logging.LoggingManager;

public class RotationLockTile extends QSTileImpl<QSTile.BooleanState> {
    private final RotationLockController.RotationLockControllerCallback mCallback = new RotationLockController.RotationLockControllerCallback() {
        /* class com.android.systemui.qs.tiles.RotationLockTile.AnonymousClass1 */

        @Override // com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback
        public void onRotationLockStateChanged(boolean z, boolean z2) {
            RotationLockTile.this.refreshState(Boolean.valueOf(z));
        }
    };
    private final RotationLockController mController;
    private final QSTile.Icon mIcon = QSTileImpl.ResourceIcon.get(17302781);

    @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
    public int getMetricsCategory() {
        return R$styleable.AppCompatTheme_windowMinWidthMinor;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
    }

    public RotationLockTile(QSHost qSHost, RotationLockController rotationLockController) {
        super(qSHost);
        this.mController = rotationLockController;
        this.mController.observe(this, this.mCallback);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.DISPLAY_SETTINGS");
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleClick() {
        boolean z = true;
        LoggingManager.logQSEvent(this.mContext, "rotation", "click", Boolean.valueOf(!((QSTile.BooleanState) this.mState).value));
        boolean z2 = !((QSTile.BooleanState) this.mState).value;
        RotationLockController rotationLockController = this.mController;
        if (z2) {
            z = false;
        }
        rotationLockController.setRotationLocked(z);
        refreshState(Boolean.valueOf(z2));
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return ((QSTile.BooleanState) getState()).label;
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        boolean isRotationLocked = this.mController.isRotationLocked();
        booleanState.value = !isRotationLocked;
        booleanState.label = this.mContext.getString(C0014R$string.quick_settings_rotation_unlocked_label);
        booleanState.icon = this.mIcon;
        booleanState.contentDescription = getAccessibilityString(isRotationLocked);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.state = booleanState.value ? 2 : 1;
    }

    public static boolean isCurrentOrientationLockPortrait(RotationLockController rotationLockController, Context context) {
        int rotationLockOrientation = rotationLockController.getRotationLockOrientation();
        return rotationLockOrientation == 0 ? context.getResources().getConfiguration().orientation != 2 : rotationLockOrientation != 2;
    }

    private String getAccessibilityString(boolean z) {
        return this.mContext.getString(C0014R$string.accessibility_quick_settings_rotation);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public String composeChangeAnnouncement() {
        return getAccessibilityString(((QSTile.BooleanState) this.mState).value);
    }
}
