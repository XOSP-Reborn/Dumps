package com.sonymobile.systemui.lockscreen.theme;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenSkinningController_Factory implements Factory<LockscreenSkinningController> {
    private final Provider<Context> contextProvider;

    public LockscreenSkinningController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public LockscreenSkinningController get() {
        return provideInstance(this.contextProvider);
    }

    public static LockscreenSkinningController provideInstance(Provider<Context> provider) {
        return new LockscreenSkinningController(provider.get());
    }

    public static LockscreenSkinningController_Factory create(Provider<Context> provider) {
        return new LockscreenSkinningController_Factory(provider);
    }
}
