package com.sonymobile.keyguard.plugininfrastructure;

import android.net.Uri;

public interface KeyguardPluginSecureSettingsAbstraction {
    String getExplicitlySelectedKeyguardPlugin();

    Uri getExplicitlySelectedKeyguardPluginValueUri();

    String getFallbackKeyguardPlugin();

    void setExplicitlySelectedKeyguardPlugin(String str, KeyguardPluginConstants$ClockSelectionSource keyguardPluginConstants$ClockSelectionSource);

    void setFallbackKeyguardPlugin(String str);
}
