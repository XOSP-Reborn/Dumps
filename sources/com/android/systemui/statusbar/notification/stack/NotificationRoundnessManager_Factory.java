package com.android.systemui.statusbar.notification.stack;

import com.android.systemui.statusbar.AmbientPulseManager;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NotificationRoundnessManager_Factory implements Factory<NotificationRoundnessManager> {
    private final Provider<AmbientPulseManager> ambientPulseManagerProvider;

    public NotificationRoundnessManager_Factory(Provider<AmbientPulseManager> provider) {
        this.ambientPulseManagerProvider = provider;
    }

    @Override // javax.inject.Provider
    public NotificationRoundnessManager get() {
        return provideInstance(this.ambientPulseManagerProvider);
    }

    public static NotificationRoundnessManager provideInstance(Provider<AmbientPulseManager> provider) {
        return new NotificationRoundnessManager(provider.get());
    }

    public static NotificationRoundnessManager_Factory create(Provider<AmbientPulseManager> provider) {
        return new NotificationRoundnessManager_Factory(provider);
    }
}
