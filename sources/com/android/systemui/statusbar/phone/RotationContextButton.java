package com.android.systemui.statusbar.phone;

import android.view.ContextThemeWrapper;
import android.view.View;
import com.android.systemui.C0007R$id;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.sonymobile.runtimeskinning.SkinningBridge;

public class RotationContextButton extends ContextualButton implements NavigationModeController.ModeChangedListener, RotationButton {
    private int mNavBarMode = 0;
    private RotationButtonController mRotationButtonController;

    public RotationContextButton(int i, int i2) {
        super(i, i2);
    }

    @Override // com.android.systemui.statusbar.phone.RotationButton
    public void setRotationButtonController(RotationButtonController rotationButtonController) {
        this.mRotationButtonController = rotationButtonController;
    }

    @Override // com.android.systemui.statusbar.phone.ButtonDispatcher, com.android.systemui.statusbar.phone.ContextualButton
    public void setVisibility(int i) {
        super.setVisibility(i);
        KeyButtonDrawable imageDrawable = getImageDrawable();
        if (i == 0 && imageDrawable != null) {
            imageDrawable.resetAnimation();
            imageDrawable.startAnimation();
        }
        SkinningBridge.onButtonVisibilityChanged(C0007R$id.rotate_suggestion, i == 0 ? "true" : "false");
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.phone.ContextualButton
    public KeyButtonDrawable getNewDrawable() {
        return KeyButtonDrawable.create(new ContextThemeWrapper(getContext().getApplicationContext(), this.mRotationButtonController.getStyleRes()), this.mIconResId, false, null);
    }

    @Override // com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener
    public void onNavigationModeChanged(int i) {
        this.mNavBarMode = i;
    }

    @Override // com.android.systemui.statusbar.phone.RotationButton
    public boolean acceptRotationProposal() {
        View currentView = getCurrentView();
        return currentView != null && currentView.isAttachedToWindow();
    }
}
