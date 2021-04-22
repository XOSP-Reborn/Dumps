package com.sonymobile.systemui.lockscreen;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenLoopsController_Factory implements Factory<LockscreenLoopsController> {
    private final Provider<Context> contextProvider;

    public LockscreenLoopsController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public LockscreenLoopsController get() {
        return provideInstance(this.contextProvider);
    }

    public static LockscreenLoopsController provideInstance(Provider<Context> provider) {
        return new LockscreenLoopsController(provider.get());
    }

    public static LockscreenLoopsController_Factory create(Provider<Context> provider) {
        return new LockscreenLoopsController_Factory(provider);
    }
}
