package com.android.systemui.statusbar.phone;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NavigationBarLockControllerImpl_Factory implements Factory<NavigationBarLockControllerImpl> {
    private final Provider<Context> contextProvider;

    public NavigationBarLockControllerImpl_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public NavigationBarLockControllerImpl get() {
        return provideInstance(this.contextProvider);
    }

    public static NavigationBarLockControllerImpl provideInstance(Provider<Context> provider) {
        return new NavigationBarLockControllerImpl(provider.get());
    }

    public static NavigationBarLockControllerImpl_Factory create(Provider<Context> provider) {
        return new NavigationBarLockControllerImpl_Factory(provider);
    }
}
