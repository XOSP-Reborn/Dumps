package com.android.systemui;

import dagger.internal.Factory;

public final class SystemUIFactory_ProvideLeakReportEmailFactory implements Factory<String> {
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideLeakReportEmailFactory(SystemUIFactory systemUIFactory) {
        this.module = systemUIFactory;
    }

    @Override // javax.inject.Provider
    public String get() {
        return provideInstance(this.module);
    }

    public static String provideInstance(SystemUIFactory systemUIFactory) {
        return proxyProvideLeakReportEmail(systemUIFactory);
    }

    public static SystemUIFactory_ProvideLeakReportEmailFactory create(SystemUIFactory systemUIFactory) {
        return new SystemUIFactory_ProvideLeakReportEmailFactory(systemUIFactory);
    }

    public static String proxyProvideLeakReportEmail(SystemUIFactory systemUIFactory) {
        return systemUIFactory.provideLeakReportEmail();
    }
}
