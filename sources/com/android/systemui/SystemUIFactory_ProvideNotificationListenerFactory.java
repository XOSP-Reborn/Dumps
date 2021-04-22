package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.NotificationListener;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideNotificationListenerFactory implements Factory<NotificationListener> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideNotificationListenerFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public NotificationListener get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static NotificationListener provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideNotificationListener(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideNotificationListenerFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideNotificationListenerFactory(systemUIFactory, provider);
    }

    public static NotificationListener proxyProvideNotificationListener(SystemUIFactory systemUIFactory, Context context) {
        NotificationListener provideNotificationListener = systemUIFactory.provideNotificationListener(context);
        Preconditions.checkNotNull(provideNotificationListener, "Cannot return null from a non-@Nullable @Provides method");
        return provideNotificationListener;
    }
}
