package com.sonymobile.keyguard.plugininfrastructure;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import java.util.Iterator;
import java.util.LinkedList;

public class RealDefaultKeyguardFactoryProvider implements DefaultKeyguardFactoryProvider {
    private final CustomizationResourceLoader mCustomizationResourceLoader;
    private final KeyguardPluginMetaDataLoader mMetaDataLoader;

    public RealDefaultKeyguardFactoryProvider(KeyguardPluginMetaDataLoader keyguardPluginMetaDataLoader, CustomizationResourceLoader customizationResourceLoader) {
        if (keyguardPluginMetaDataLoader == null || customizationResourceLoader == null) {
            throw new IllegalArgumentException("KeyguardPluginMetaDataLoader or CustomizationResourceLoader cannot be null.");
        }
        this.mMetaDataLoader = keyguardPluginMetaDataLoader;
        this.mCustomizationResourceLoader = customizationResourceLoader;
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.DefaultKeyguardFactoryProvider
    public final String getDefaultKeyguardFactoryClassName() {
        String somcCustomizedDefaultFullyQualifiedClockPluginName = this.mCustomizationResourceLoader.getSomcCustomizedDefaultFullyQualifiedClockPluginName();
        return TextUtils.isEmpty(somcCustomizedDefaultFullyQualifiedClockPluginName) ? getClockWithHighestPriority() : somcCustomizedDefaultFullyQualifiedClockPluginName;
    }

    private String getClockWithHighestPriority() {
        KeyguardComponentFactoryEntry keyguardComponentFactoryEntry;
        try {
            LinkedList<KeyguardComponentFactoryEntry> availableKeyguardFactories = this.mMetaDataLoader.getAvailableKeyguardFactories();
            if (availableKeyguardFactories != null) {
                Iterator<KeyguardComponentFactoryEntry> it = availableKeyguardFactories.iterator();
                int i = Integer.MIN_VALUE;
                keyguardComponentFactoryEntry = null;
                while (it.hasNext()) {
                    KeyguardComponentFactoryEntry next = it.next();
                    if (next.getPriority() > i) {
                        i = next.getPriority();
                        keyguardComponentFactoryEntry = next;
                    }
                }
            } else {
                keyguardComponentFactoryEntry = null;
            }
            if (keyguardComponentFactoryEntry != null) {
                return keyguardComponentFactoryEntry.getFullyQualifiedClassName();
            }
            return null;
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException | NumberFormatException e) {
            Log.w("PriorityBasedDefaultKeyguardFactoryProvider", e);
            return null;
        }
    }
}
