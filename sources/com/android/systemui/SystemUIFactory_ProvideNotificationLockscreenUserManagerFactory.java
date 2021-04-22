package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideNotificationLockscreenUserManagerFactory implements Factory<NotificationLockscreenUserManager> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideNotificationLockscreenUserManagerFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public NotificationLockscreenUserManager get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static NotificationLockscreenUserManager provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideNotificationLockscreenUserManager(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideNotificationLockscreenUserManagerFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideNotificationLockscreenUserManagerFactory(systemUIFactory, provider);
    }

    public static NotificationLockscreenUserManager proxyProvideNotificationLockscreenUserManager(SystemUIFactory systemUIFactory, Context context) {
        NotificationLockscreenUserManager provideNotificationLockscreenUserManager = systemUIFactory.provideNotificationLockscreenUserManager(context);
        Preconditions.checkNotNull(provideNotificationLockscreenUserManager, "Cannot return null from a non-@Nullable @Provides method");
        return provideNotificationLockscreenUserManager;
    }
}
