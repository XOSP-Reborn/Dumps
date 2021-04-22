package com.android.systemui.statusbar.phone;

import dagger.internal.Factory;

public final class NotificationGroupManager_Factory implements Factory<NotificationGroupManager> {
    private static final NotificationGroupManager_Factory INSTANCE = new NotificationGroupManager_Factory();

    @Override // javax.inject.Provider
    public NotificationGroupManager get() {
        return provideInstance();
    }

    public static NotificationGroupManager provideInstance() {
        return new NotificationGroupManager();
    }

    public static NotificationGroupManager_Factory create() {
        return INSTANCE;
    }
}
