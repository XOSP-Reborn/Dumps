package com.android.systemui.statusbar.phone;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class StatusBarWindowController_Factory implements Factory<StatusBarWindowController> {
    private final Provider<Context> contextProvider;

    public StatusBarWindowController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public StatusBarWindowController get() {
        return provideInstance(this.contextProvider);
    }

    public static StatusBarWindowController provideInstance(Provider<Context> provider) {
        return new StatusBarWindowController(provider.get());
    }

    public static StatusBarWindowController_Factory create(Provider<Context> provider) {
        return new StatusBarWindowController_Factory(provider);
    }
}
