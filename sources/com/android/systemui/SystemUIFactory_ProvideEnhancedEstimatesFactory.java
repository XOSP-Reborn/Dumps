package com.android.systemui;

import android.content.Context;
import com.android.systemui.power.EnhancedEstimates;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideEnhancedEstimatesFactory implements Factory<EnhancedEstimates> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideEnhancedEstimatesFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public EnhancedEstimates get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static EnhancedEstimates provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideEnhancedEstimates(systemUIFactory, provider.get());
    }

    public static SystemUIFactory_ProvideEnhancedEstimatesFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideEnhancedEstimatesFactory(systemUIFactory, provider);
    }

    public static EnhancedEstimates proxyProvideEnhancedEstimates(SystemUIFactory systemUIFactory, Context context) {
        EnhancedEstimates provideEnhancedEstimates = systemUIFactory.provideEnhancedEstimates(context);
        Preconditions.checkNotNull(provideEnhancedEstimates, "Cannot return null from a non-@Nullable @Provides method");
        return provideEnhancedEstimates;
    }
}
