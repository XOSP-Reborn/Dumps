package com.sonymobile.systemui.lockscreen;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenAssistIconController_Factory implements Factory<LockscreenAssistIconController> {
    private final Provider<Context> contextProvider;
    private final Provider<LockscreenLoopsController> lockscreenLoopsControllerProvider;

    public LockscreenAssistIconController_Factory(Provider<Context> provider, Provider<LockscreenLoopsController> provider2) {
        this.contextProvider = provider;
        this.lockscreenLoopsControllerProvider = provider2;
    }

    @Override // javax.inject.Provider
    public LockscreenAssistIconController get() {
        return provideInstance(this.contextProvider, this.lockscreenLoopsControllerProvider);
    }

    public static LockscreenAssistIconController provideInstance(Provider<Context> provider, Provider<LockscreenLoopsController> provider2) {
        return new LockscreenAssistIconController(provider.get(), provider2.get());
    }

    public static LockscreenAssistIconController_Factory create(Provider<Context> provider, Provider<LockscreenLoopsController> provider2) {
        return new LockscreenAssistIconController_Factory(provider, provider2);
    }
}
