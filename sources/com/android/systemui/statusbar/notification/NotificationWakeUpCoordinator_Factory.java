package com.android.systemui.statusbar.notification;

import android.content.Context;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.AmbientPulseManager;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NotificationWakeUpCoordinator_Factory implements Factory<NotificationWakeUpCoordinator> {
    private final Provider<AmbientPulseManager> mAmbientPulseManagerProvider;
    private final Provider<Context> mContextProvider;
    private final Provider<StatusBarStateController> mStatusBarStateControllerProvider;

    public NotificationWakeUpCoordinator_Factory(Provider<Context> provider, Provider<AmbientPulseManager> provider2, Provider<StatusBarStateController> provider3) {
        this.mContextProvider = provider;
        this.mAmbientPulseManagerProvider = provider2;
        this.mStatusBarStateControllerProvider = provider3;
    }

    @Override // javax.inject.Provider
    public NotificationWakeUpCoordinator get() {
        return provideInstance(this.mContextProvider, this.mAmbientPulseManagerProvider, this.mStatusBarStateControllerProvider);
    }

    public static NotificationWakeUpCoordinator provideInstance(Provider<Context> provider, Provider<AmbientPulseManager> provider2, Provider<StatusBarStateController> provider3) {
        return new NotificationWakeUpCoordinator(provider.get(), provider2.get(), provider3.get());
    }

    public static NotificationWakeUpCoordinator_Factory create(Provider<Context> provider, Provider<AmbientPulseManager> provider2, Provider<StatusBarStateController> provider3) {
        return new NotificationWakeUpCoordinator_Factory(provider, provider2, provider3);
    }
}
