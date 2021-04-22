package com.android.systemui;

import android.content.Context;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideAssistManagerFactory implements Factory<AssistManager> {
    private final Provider<Context> contextProvider;
    private final Provider<DeviceProvisionedController> controllerProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideAssistManagerFactory(SystemUIFactory systemUIFactory, Provider<DeviceProvisionedController> provider, Provider<Context> provider2) {
        this.module = systemUIFactory;
        this.controllerProvider = provider;
        this.contextProvider = provider2;
    }

    @Override // javax.inject.Provider
    public AssistManager get() {
        return provideInstance(this.module, this.controllerProvider, this.contextProvider);
    }

    public static AssistManager provideInstance(SystemUIFactory systemUIFactory, Provider<DeviceProvisionedController> provider, Provider<Context> provider2) {
        return proxyProvideAssistManager(systemUIFactory, provider.get(), provider2.get());
    }

    public static SystemUIFactory_ProvideAssistManagerFactory create(SystemUIFactory systemUIFactory, Provider<DeviceProvisionedController> provider, Provider<Context> provider2) {
        return new SystemUIFactory_ProvideAssistManagerFactory(systemUIFactory, provider, provider2);
    }

    public static AssistManager proxyProvideAssistManager(SystemUIFactory systemUIFactory, DeviceProvisionedController deviceProvisionedController, Context context) {
        AssistManager provideAssistManager = systemUIFactory.provideAssistManager(deviceProvisionedController, context);
        Preconditions.checkNotNull(provideAssistManager, "Cannot return null from a non-@Nullable @Provides method");
        return provideAssistManager;
    }
}
