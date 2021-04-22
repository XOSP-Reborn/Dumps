package com.sonymobile.keyguard.pin;

import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;

public class RealPinAutoUnlockSettingsSecureWrapper implements PinAutoUnlockSettingsSecureWrapper {
    private final ContentResolver mContentResolver;

    public RealPinAutoUnlockSettingsSecureWrapper(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    @Override // com.sonymobile.keyguard.pin.PinAutoUnlockSettingsSecureWrapper
    public final boolean isAutoUnlockEnabled() {
        try {
            return Settings.Secure.getIntForUser(this.mContentResolver, "somc.lockscreen_type_is_pin_and_exactly_4_digits", -2) == 1;
        } catch (Settings.SettingNotFoundException e) {
            Log.w(RealPinAutoUnlockSettingsSecureWrapper.class.getSimpleName(), e);
            return false;
        }
    }

    @Override // com.sonymobile.keyguard.pin.PinAutoUnlockSettingsSecureWrapper
    public final void enableAutoUnlock() {
        Settings.Secure.putIntForUser(this.mContentResolver, "somc.lockscreen_type_is_pin_and_exactly_4_digits", 1, -2);
    }
}
