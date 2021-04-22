package com.sonymobile.systemui.lockscreen;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenClockController_Factory implements Factory<LockscreenClockController> {
    private final Provider<Context> contextProvider;

    public LockscreenClockController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public LockscreenClockController get() {
        return provideInstance(this.contextProvider);
    }

    public static LockscreenClockController provideInstance(Provider<Context> provider) {
        return new LockscreenClockController(provider.get());
    }

    public static LockscreenClockController_Factory create(Provider<Context> provider) {
        return new LockscreenClockController_Factory(provider);
    }
}
