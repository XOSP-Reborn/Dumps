package com.sonymobile.keyguard.plugininfrastructure;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.Settings;

public class RealKeyguardPluginSecureSettingsAbstraction implements KeyguardPluginSecureSettingsAbstraction {
    private final ContentResolver mContentResolver;
    private final int mUserId;

    public RealKeyguardPluginSecureSettingsAbstraction(ContentResolver contentResolver, int i) {
        if (contentResolver != null) {
            this.mContentResolver = contentResolver;
            this.mUserId = i;
            return;
        }
        throw new IllegalArgumentException("ContentResolver may not be null.");
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginSecureSettingsAbstraction
    public final String getExplicitlySelectedKeyguardPlugin() {
        return Settings.Secure.getStringForUser(this.mContentResolver, "somc.lockscreen.active.clock_factory", this.mUserId);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginSecureSettingsAbstraction
    public final void setExplicitlySelectedKeyguardPlugin(String str, KeyguardPluginConstants$ClockSelectionSource keyguardPluginConstants$ClockSelectionSource) {
        Settings.Secure.putStringForUser(this.mContentResolver, "somc.lockscreen.active.clock_factory.source", keyguardPluginConstants$ClockSelectionSource.name(), this.mUserId);
        Settings.Secure.putStringForUser(this.mContentResolver, "somc.lockscreen.active.clock_factory", str, this.mUserId);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginSecureSettingsAbstraction
    public final String getFallbackKeyguardPlugin() {
        return Settings.Secure.getStringForUser(this.mContentResolver, "somc.lockscreen.active.clock_factory_fallback", this.mUserId);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginSecureSettingsAbstraction
    public final void setFallbackKeyguardPlugin(String str) {
        Settings.Secure.putStringForUser(this.mContentResolver, "somc.lockscreen.active.clock_factory_fallback", str, this.mUserId);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginSecureSettingsAbstraction
    public final Uri getExplicitlySelectedKeyguardPluginValueUri() {
        return Settings.Secure.getUriFor("somc.lockscreen.active.clock_factory");
    }
}
