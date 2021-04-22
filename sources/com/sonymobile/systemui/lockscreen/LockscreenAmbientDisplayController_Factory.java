package com.sonymobile.systemui.lockscreen;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenAmbientDisplayController_Factory implements Factory<LockscreenAmbientDisplayController> {
    private final Provider<Context> contextProvider;

    public LockscreenAmbientDisplayController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public LockscreenAmbientDisplayController get() {
        return provideInstance(this.contextProvider);
    }

    public static LockscreenAmbientDisplayController provideInstance(Provider<Context> provider) {
        return new LockscreenAmbientDisplayController(provider.get());
    }

    public static LockscreenAmbientDisplayController_Factory create(Provider<Context> provider) {
        return new LockscreenAmbientDisplayController_Factory(provider);
    }
}
