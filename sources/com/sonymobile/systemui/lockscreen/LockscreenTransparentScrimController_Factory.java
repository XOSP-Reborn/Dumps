package com.sonymobile.systemui.lockscreen;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenTransparentScrimController_Factory implements Factory<LockscreenTransparentScrimController> {
    private final Provider<Context> contextProvider;

    public LockscreenTransparentScrimController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public LockscreenTransparentScrimController get() {
        return provideInstance(this.contextProvider);
    }

    public static LockscreenTransparentScrimController provideInstance(Provider<Context> provider) {
        return new LockscreenTransparentScrimController(provider.get());
    }

    public static LockscreenTransparentScrimController_Factory create(Provider<Context> provider) {
        return new LockscreenTransparentScrimController_Factory(provider);
    }
}
