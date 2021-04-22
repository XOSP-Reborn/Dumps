package com.sonymobile.keyguard.plugininfrastructure;

public class RealClockPluginUserSelectionHandler implements ClockPluginUserSelectionHandler {
    private final DefaultKeyguardFactoryProvider mDefaultKeyguardFactoryProvider;
    private final KeyguardPluginMetaDataLoader mKeyguardPluginMetaDataLoader;
    private final KeyguardPluginSecureSettingsAbstraction mKeyguardPluginSecureSettingsAbstraction;

    public RealClockPluginUserSelectionHandler(KeyguardPluginMetaDataLoader keyguardPluginMetaDataLoader, KeyguardPluginSecureSettingsAbstraction keyguardPluginSecureSettingsAbstraction, DefaultKeyguardFactoryProvider defaultKeyguardFactoryProvider) {
        this.mKeyguardPluginMetaDataLoader = keyguardPluginMetaDataLoader;
        this.mKeyguardPluginSecureSettingsAbstraction = keyguardPluginSecureSettingsAbstraction;
        this.mDefaultKeyguardFactoryProvider = defaultKeyguardFactoryProvider;
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPluginUserSelectionHandler
    public final void updateUserSelection(String str, KeyguardPluginConstants$ClockSelectionSource keyguardPluginConstants$ClockSelectionSource) {
        updateLastPresentableSelectionStore();
        this.mKeyguardPluginSecureSettingsAbstraction.setExplicitlySelectedKeyguardPlugin(str, keyguardPluginConstants$ClockSelectionSource);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPluginUserSelectionHandler
    public final String getPresentableUserSelection() {
        String explicitlySelectedKeyguardPlugin = this.mKeyguardPluginSecureSettingsAbstraction.getExplicitlySelectedKeyguardPlugin();
        KeyguardComponentFactoryEntry factoryEntryFromClassName = this.mKeyguardPluginMetaDataLoader.getFactoryEntryFromClassName(explicitlySelectedKeyguardPlugin);
        if (factoryEntryFromClassName == null || factoryEntryFromClassName.getEnabled()) {
            return explicitlySelectedKeyguardPlugin;
        }
        String fallbackKeyguardPlugin = this.mKeyguardPluginSecureSettingsAbstraction.getFallbackKeyguardPlugin();
        updateUserSelection(fallbackKeyguardPlugin, KeyguardPluginConstants$ClockSelectionSource.Fallback);
        return fallbackKeyguardPlugin;
    }

    private void updateLastPresentableSelectionStore() {
        String explicitlySelectedKeyguardPlugin = this.mKeyguardPluginSecureSettingsAbstraction.getExplicitlySelectedKeyguardPlugin();
        if (explicitlySelectedKeyguardPlugin == null) {
            explicitlySelectedKeyguardPlugin = this.mDefaultKeyguardFactoryProvider.getDefaultKeyguardFactoryClassName();
        }
        KeyguardComponentFactoryEntry factoryEntryFromClassName = this.mKeyguardPluginMetaDataLoader.getFactoryEntryFromClassName(explicitlySelectedKeyguardPlugin);
        if (factoryEntryFromClassName != null && !factoryEntryFromClassName.getSelectableByThemes()) {
            this.mKeyguardPluginSecureSettingsAbstraction.setFallbackKeyguardPlugin(explicitlySelectedKeyguardPlugin);
        }
    }
}
