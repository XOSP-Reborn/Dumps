package com.android.systemui.assist;

import android.provider.DeviceConfig;
import java.util.concurrent.Executor;

/* access modifiers changed from: package-private */
public class PhenotypeHelper {
    PhenotypeHelper() {
    }

    /* access modifiers changed from: package-private */
    public long getLong(String str, long j) {
        return DeviceConfig.getLong("systemui", str, j);
    }

    /* access modifiers changed from: package-private */
    public int getInt(String str, int i) {
        return DeviceConfig.getInt("systemui", str, i);
    }

    /* access modifiers changed from: package-private */
    public String getString(String str, String str2) {
        return DeviceConfig.getString("systemui", str, str2);
    }

    /* access modifiers changed from: package-private */
    public boolean getBoolean(String str, boolean z) {
        return DeviceConfig.getBoolean("systemui", str, z);
    }

    /* access modifiers changed from: package-private */
    public void addOnPropertiesChangedListener(Executor executor, DeviceConfig.OnPropertiesChangedListener onPropertiesChangedListener) {
        DeviceConfig.addOnPropertiesChangedListener("systemui", executor, onPropertiesChangedListener);
    }
}
