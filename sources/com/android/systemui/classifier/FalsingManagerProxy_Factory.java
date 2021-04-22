package com.android.systemui.classifier;

import android.content.Context;
import android.os.Handler;
import com.android.systemui.shared.plugins.PluginManager;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class FalsingManagerProxy_Factory implements Factory<FalsingManagerProxy> {
    private final Provider<Context> contextProvider;
    private final Provider<Handler> handlerProvider;
    private final Provider<PluginManager> pluginManagerProvider;

    public FalsingManagerProxy_Factory(Provider<Context> provider, Provider<PluginManager> provider2, Provider<Handler> provider3) {
        this.contextProvider = provider;
        this.pluginManagerProvider = provider2;
        this.handlerProvider = provider3;
    }

    @Override // javax.inject.Provider
    public FalsingManagerProxy get() {
        return provideInstance(this.contextProvider, this.pluginManagerProvider, this.handlerProvider);
    }

    public static FalsingManagerProxy provideInstance(Provider<Context> provider, Provider<PluginManager> provider2, Provider<Handler> provider3) {
        return new FalsingManagerProxy(provider.get(), provider2.get(), provider3.get());
    }

    public static FalsingManagerProxy_Factory create(Provider<Context> provider, Provider<PluginManager> provider2, Provider<Handler> provider3) {
        return new FalsingManagerProxy_Factory(provider, provider2, provider3);
    }
}
