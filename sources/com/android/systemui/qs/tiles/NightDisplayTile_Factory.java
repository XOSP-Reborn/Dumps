package com.android.systemui.qs.tiles;

import com.android.systemui.qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NightDisplayTile_Factory implements Factory<NightDisplayTile> {
    private final Provider<QSHost> hostProvider;

    public NightDisplayTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    @Override // javax.inject.Provider
    public NightDisplayTile get() {
        return provideInstance(this.hostProvider);
    }

    public static NightDisplayTile provideInstance(Provider<QSHost> provider) {
        return new NightDisplayTile(provider.get());
    }

    public static NightDisplayTile_Factory create(Provider<QSHost> provider) {
        return new NightDisplayTile_Factory(provider);
    }
}
