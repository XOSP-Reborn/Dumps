package com.sonymobile.keyguard.plugininfrastructure;

import android.content.Context;
import com.android.systemui.C0014R$string;

public class RealCustomizationResourceLoader implements CustomizationResourceLoader {
    private final Context mContext;

    public RealCustomizationResourceLoader(Context context) {
        this.mContext = context;
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.CustomizationResourceLoader
    public final String getSomcCustomizedDefaultFullyQualifiedClockPluginName() {
        return this.mContext.getResources().getString(C0014R$string.config_somc_default_clock_plugin_class_name);
    }
}
