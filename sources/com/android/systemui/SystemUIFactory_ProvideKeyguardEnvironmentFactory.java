package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideKeyguardEnvironmentFactory implements Factory<NotificationData.KeyguardEnvironment> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideKeyguardEnvironmentFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public NotificationData.KeyguardEnvironment get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static NotificationData.KeyguardEnvironment provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideKeyguardEnvironment(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideKeyguardEnvironmentFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideKeyguardEnvironmentFactory(systemUIFactory, provider);
    }

    public static NotificationData.KeyguardEnvironment proxyProvideKeyguardEnvironment(SystemUIFactory systemUIFactory, Context context) {
        NotificationData.KeyguardEnvironment provideKeyguardEnvironment = systemUIFactory.provideKeyguardEnvironment(context);
        Preconditions.checkNotNull(provideKeyguardEnvironment, "Cannot return null from a non-@Nullable @Provides method");
        return provideKeyguardEnvironment;
    }
}
