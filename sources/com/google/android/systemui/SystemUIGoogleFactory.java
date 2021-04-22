package com.google.android.systemui;

import android.content.Context;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

public class SystemUIGoogleFactory extends SystemUIFactory {
    @Override // com.android.systemui.SystemUIFactory
    public AssistManager provideAssistManager(DeviceProvisionedController deviceProvisionedController, Context context) {
        return new AssistManagerGoogle(deviceProvisionedController, context);
    }
}
