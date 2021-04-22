package com.android.systemui.statusbar.notification;

import android.content.Context;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class DynamicPrivacyController_Factory implements Factory<DynamicPrivacyController> {
    private final Provider<Context> contextProvider;
    private final Provider<NotificationLockscreenUserManager> notificationLockscreenUserManagerProvider;

    public DynamicPrivacyController_Factory(Provider<Context> provider, Provider<NotificationLockscreenUserManager> provider2) {
        this.contextProvider = provider;
        this.notificationLockscreenUserManagerProvider = provider2;
    }

    @Override // javax.inject.Provider
    public DynamicPrivacyController get() {
        return provideInstance(this.contextProvider, this.notificationLockscreenUserManagerProvider);
    }

    public static DynamicPrivacyController provideInstance(Provider<Context> provider, Provider<NotificationLockscreenUserManager> provider2) {
        return new DynamicPrivacyController(provider.get(), provider2.get());
    }

    public static DynamicPrivacyController_Factory create(Provider<Context> provider, Provider<NotificationLockscreenUserManager> provider2) {
        return new DynamicPrivacyController_Factory(provider, provider2);
    }
}
