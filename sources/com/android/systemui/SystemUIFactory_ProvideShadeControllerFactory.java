package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.phone.ShadeController;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideShadeControllerFactory implements Factory<ShadeController> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideShadeControllerFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public ShadeController get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static ShadeController provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideShadeController(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideShadeControllerFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideShadeControllerFactory(systemUIFactory, provider);
    }

    public static ShadeController proxyProvideShadeController(SystemUIFactory systemUIFactory, Context context) {
        ShadeController provideShadeController = systemUIFactory.provideShadeController(context);
        Preconditions.checkNotNull(provideShadeController, "Cannot return null from a non-@Nullable @Provides method");
        return provideShadeController;
    }
}
