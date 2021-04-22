package com.sonymobile.systemui.lockscreen;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenStyleCoverController_Factory implements Factory<LockscreenStyleCoverController> {
    private final Provider<Context> contextProvider;

    public LockscreenStyleCoverController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public LockscreenStyleCoverController get() {
        return provideInstance(this.contextProvider);
    }

    public static LockscreenStyleCoverController provideInstance(Provider<Context> provider) {
        return new LockscreenStyleCoverController(provider.get());
    }

    public static LockscreenStyleCoverController_Factory create(Provider<Context> provider) {
        return new LockscreenStyleCoverController_Factory(provider);
    }
}
