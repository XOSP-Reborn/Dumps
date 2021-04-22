package com.android.keyguard.clock;

import android.content.ContentResolver;
import android.provider.Settings;

public class SettingsWrapper {
    private ContentResolver mContentResolver;

    public SettingsWrapper(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    public String getLockScreenCustomClockFace(int i) {
        return Settings.Secure.getStringForUser(this.mContentResolver, "lock_screen_custom_clock_face", i);
    }

    public String getDockedClockFace(int i) {
        return Settings.Secure.getStringForUser(this.mContentResolver, "docked_clock_face", i);
    }
}
