package com.android.systemui.fragments;

import com.android.systemui.SystemUIFactory;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class FragmentService_Factory implements Factory<FragmentService> {
    private final Provider<SystemUIFactory.SystemUIRootComponent> rootComponentProvider;

    public FragmentService_Factory(Provider<SystemUIFactory.SystemUIRootComponent> provider) {
        this.rootComponentProvider = provider;
    }

    @Override // javax.inject.Provider
    public FragmentService get() {
        return provideInstance(this.rootComponentProvider);
    }

    public static FragmentService provideInstance(Provider<SystemUIFactory.SystemUIRootComponent> provider) {
        return new FragmentService(provider.get());
    }

    public static FragmentService_Factory create(Provider<SystemUIFactory.SystemUIRootComponent> provider) {
        return new FragmentService_Factory(provider);
    }
}
