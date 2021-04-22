package com.android.systemui.qs.tiles;

import com.android.systemui.qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NfcTile_Factory implements Factory<NfcTile> {
    private final Provider<QSHost> hostProvider;

    public NfcTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    @Override // javax.inject.Provider
    public NfcTile get() {
        return provideInstance(this.hostProvider);
    }

    public static NfcTile provideInstance(Provider<QSHost> provider) {
        return new NfcTile(provider.get());
    }

    public static NfcTile_Factory create(Provider<QSHost> provider) {
        return new NfcTile_Factory(provider);
    }
}
