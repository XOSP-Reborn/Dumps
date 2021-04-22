package com.android.systemui.statusbar;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class AmbientPulseManager_Factory implements Factory<AmbientPulseManager> {
    private final Provider<Context> contextProvider;

    public AmbientPulseManager_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public AmbientPulseManager get() {
        return provideInstance(this.contextProvider);
    }

    public static AmbientPulseManager provideInstance(Provider<Context> provider) {
        return new AmbientPulseManager(provider.get());
    }

    public static AmbientPulseManager_Factory create(Provider<Context> provider) {
        return new AmbientPulseManager_Factory(provider);
    }
}
