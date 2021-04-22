package com.sonymobile.systemui.lockscreen;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class LockscreenAlbumArtController_Factory implements Factory<LockscreenAlbumArtController> {
    private final Provider<Context> contextProvider;

    public LockscreenAlbumArtController_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    @Override // javax.inject.Provider
    public LockscreenAlbumArtController get() {
        return provideInstance(this.contextProvider);
    }

    public static LockscreenAlbumArtController provideInstance(Provider<Context> provider) {
        return new LockscreenAlbumArtController(provider.get());
    }

    public static LockscreenAlbumArtController_Factory create(Provider<Context> provider) {
        return new LockscreenAlbumArtController_Factory(provider);
    }
}
