package com.android.systemui.statusbar;

import android.content.Context;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class PulseExpansionHandler_Factory implements Factory<PulseExpansionHandler> {
    private final Provider<Context> contextProvider;
    private final Provider<NotificationWakeUpCoordinator> mWakeUpCoordinatorProvider;

    public PulseExpansionHandler_Factory(Provider<Context> provider, Provider<NotificationWakeUpCoordinator> provider2) {
        this.contextProvider = provider;
        this.mWakeUpCoordinatorProvider = provider2;
    }

    @Override // javax.inject.Provider
    public PulseExpansionHandler get() {
        return provideInstance(this.contextProvider, this.mWakeUpCoordinatorProvider);
    }

    public static PulseExpansionHandler provideInstance(Provider<Context> provider, Provider<NotificationWakeUpCoordinator> provider2) {
        return new PulseExpansionHandler(provider.get(), provider2.get());
    }

    public static PulseExpansionHandler_Factory create(Provider<Context> provider, Provider<NotificationWakeUpCoordinator> provider2) {
        return new PulseExpansionHandler_Factory(provider, provider2);
    }
}
