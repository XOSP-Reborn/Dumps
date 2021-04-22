package com.android.systemui;

import android.content.Context;
import com.android.systemui.dock.DockManager;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideDockManagerFactory implements Factory<DockManager> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideDockManagerFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public DockManager get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static DockManager provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideDockManager(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideDockManagerFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideDockManagerFactory(systemUIFactory, provider);
    }

    public static DockManager proxyProvideDockManager(SystemUIFactory systemUIFactory, Context context) {
        return systemUIFactory.provideDockManager(context);
    }
}
