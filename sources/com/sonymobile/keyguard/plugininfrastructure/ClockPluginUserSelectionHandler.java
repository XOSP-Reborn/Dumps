package com.sonymobile.keyguard.plugininfrastructure;

public interface ClockPluginUserSelectionHandler {
    String getPresentableUserSelection();

    void updateUserSelection(String str, KeyguardPluginConstants$ClockSelectionSource keyguardPluginConstants$ClockSelectionSource);
}
