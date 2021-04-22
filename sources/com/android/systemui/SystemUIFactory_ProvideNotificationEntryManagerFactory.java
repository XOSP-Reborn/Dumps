package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideNotificationEntryManagerFactory implements Factory<NotificationEntryManager> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideNotificationEntryManagerFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public NotificationEntryManager get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static NotificationEntryManager provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideNotificationEntryManager(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideNotificationEntryManagerFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideNotificationEntryManagerFactory(systemUIFactory, provider);
    }

    public static NotificationEntryManager proxyProvideNotificationEntryManager(SystemUIFactory systemUIFactory, Context context) {
        NotificationEntryManager provideNotificationEntryManager = systemUIFactory.provideNotificationEntryManager(context);
        Preconditions.checkNotNull(provideNotificationEntryManager, "Cannot return null from a non-@Nullable @Provides method");
        return provideNotificationEntryManager;
    }
}
