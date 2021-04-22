package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideNotificationInterruptionStateProviderFactory implements Factory<NotificationInterruptionStateProvider> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideNotificationInterruptionStateProviderFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public NotificationInterruptionStateProvider get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static NotificationInterruptionStateProvider provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideNotificationInterruptionStateProvider(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideNotificationInterruptionStateProviderFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideNotificationInterruptionStateProviderFactory(systemUIFactory, provider);
    }

    public static NotificationInterruptionStateProvider proxyProvideNotificationInterruptionStateProvider(SystemUIFactory systemUIFactory, Context context) {
        NotificationInterruptionStateProvider provideNotificationInterruptionStateProvider = systemUIFactory.provideNotificationInterruptionStateProvider(context);
        Preconditions.checkNotNull(provideNotificationInterruptionStateProvider, "Cannot return null from a non-@Nullable @Provides method");
        return provideNotificationInterruptionStateProvider;
    }
}
