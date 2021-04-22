package com.android.systemui;

import dagger.internal.Factory;

public final class SystemUIFactory_ProvideAllowNotificationLongPressFactory implements Factory<Boolean> {
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideAllowNotificationLongPressFactory(SystemUIFactory systemUIFactory) {
        this.module = systemUIFactory;
    }

    @Override // javax.inject.Provider
    public Boolean get() {
        return provideInstance(this.module);
    }

    public static Boolean provideInstance(SystemUIFactory systemUIFactory) {
        return Boolean.valueOf(proxyProvideAllowNotificationLongPress(systemUIFactory));
    }

    public static SystemUIFactory_ProvideAllowNotificationLongPressFactory create(SystemUIFactory systemUIFactory) {
        return new SystemUIFactory_ProvideAllowNotificationLongPressFactory(systemUIFactory);
    }

    public static boolean proxyProvideAllowNotificationLongPress(SystemUIFactory systemUIFactory) {
        return systemUIFactory.provideAllowNotificationLongPress();
    }
}
