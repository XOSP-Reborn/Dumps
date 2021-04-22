package com.android.systemui.qs;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class AutoAddTracker_Factory implements Factory<AutoAddTracker> {
    private final Provider<Context> contextProvider;

    public AutoAddTracker_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public AutoAddTracker get() {
        return provideInstance(this.contextProvider);
    }

    public static AutoAddTracker provideInstance(Provider<Context> provider) {
        return new AutoAddTracker(provider.get());
    }

    public static AutoAddTracker_Factory create(Provider<Context> provider) {
        return new AutoAddTracker_Factory(provider);
    }
}
