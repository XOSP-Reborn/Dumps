package com.android.systemui.statusbar.notification.stack;

import android.content.Context;
import android.util.AttributeSet;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import dagger.internal.Factory;

public final class NotificationStackScrollLayout_Factory implements Factory<NotificationStackScrollLayout> {
    public static NotificationStackScrollLayout newNotificationStackScrollLayout(Context context, AttributeSet attributeSet, boolean z, Object obj, AmbientPulseManager ambientPulseManager, DynamicPrivacyController dynamicPrivacyController, ConfigurationController configurationController, ActivityStarter activityStarter, StatusBarStateController statusBarStateController) {
        return new NotificationStackScrollLayout(context, attributeSet, z, (NotificationRoundnessManager) obj, ambientPulseManager, dynamicPrivacyController, configurationController, activityStarter, statusBarStateController);
    }
}
