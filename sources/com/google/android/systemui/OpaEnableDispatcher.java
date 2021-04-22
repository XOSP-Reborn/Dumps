package com.google.android.systemui;

import android.content.ComponentName;
import android.content.Context;
import android.view.View;
import com.android.internal.app.AssistUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.StatusBar;
import java.util.ArrayList;

public class OpaEnableDispatcher {
    private final AssistUtils mAssistUtils;
    private final Context mContext;

    public OpaEnableDispatcher(Context context, AssistUtils assistUtils) {
        this.mContext = context;
        this.mAssistUtils = assistUtils;
    }

    public void dispatchOpaEnabled(boolean z) {
        dispatchUnchecked(z && isGsaCurrentAssistant());
    }

    private void dispatchUnchecked(boolean z) {
        NavigationBarView navigationBarView;
        StatusBar statusBar = (StatusBar) ((SystemUIApplication) this.mContext.getApplicationContext()).getComponent(StatusBar.class);
        if (!(statusBar == null || (navigationBarView = statusBar.getNavigationBarView()) == null)) {
            ArrayList<View> views = navigationBarView.getHomeButton().getViews();
            for (int i = 0; i < views.size(); i++) {
                ((OpaLayout) views.get(i)).setOpaEnabled(z);
            }
        }
    }

    private boolean isGsaCurrentAssistant() {
        ComponentName assistComponentForUser = this.mAssistUtils.getAssistComponentForUser(KeyguardUpdateMonitor.getCurrentUser());
        return assistComponentForUser != null && "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService".equals(assistComponentForUser.flattenToString());
    }
}
