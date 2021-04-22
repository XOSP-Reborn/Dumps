package com.android.systemui.statusbar.phone;

import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.util.InjectionInflationController;
import dagger.MembersInjector;

public final class StatusBar_MembersInjector implements MembersInjector<StatusBar> {
    public static void injectMInjectionInflater(StatusBar statusBar, InjectionInflationController injectionInflationController) {
        statusBar.mInjectionInflater = injectionInflationController;
    }

    public static void injectMPulseExpansionHandler(StatusBar statusBar, PulseExpansionHandler pulseExpansionHandler) {
        statusBar.mPulseExpansionHandler = pulseExpansionHandler;
    }

    public static void injectMWakeUpCoordinator(StatusBar statusBar, NotificationWakeUpCoordinator notificationWakeUpCoordinator) {
        statusBar.mWakeUpCoordinator = notificationWakeUpCoordinator;
    }
}
