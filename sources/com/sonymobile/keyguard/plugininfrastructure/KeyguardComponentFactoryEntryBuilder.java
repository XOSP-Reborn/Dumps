package com.sonymobile.keyguard.plugininfrastructure;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

public class KeyguardComponentFactoryEntryBuilder {
    private static final String TAG = KeyguardComponentFactoryEntry.class.getSimpleName();
    private final Context mContext;
    private String[] mParameters;
    private final Resources mResources;

    public KeyguardComponentFactoryEntryBuilder(Resources resources, Context context) {
        this.mResources = resources;
        this.mContext = context;
    }

    public final KeyguardComponentFactoryEntryBuilder setFromResourceId(int i) {
        this.mParameters = this.mResources.getStringArray(i);
        return this;
    }

    private int[] getAvailableKeyguardClockImages() throws Resources.NotFoundException {
        int identifier = this.mResources.getIdentifier(this.mParameters[3], "array", "com.android.systemui");
        if (identifier == 0) {
            return null;
        }
        TypedArray obtainTypedArray = this.mResources.obtainTypedArray(identifier);
        try {
            int length = obtainTypedArray.length();
            int[] iArr = new int[length];
            for (int i = 0; i < length; i++) {
                iArr[i] = obtainTypedArray.getResourceId(i, 0);
            }
            return iArr;
        } finally {
            if (obtainTypedArray != null) {
                obtainTypedArray.recycle();
            }
        }
    }

    private Drawable getPreviewImageDrawable() throws Resources.NotFoundException {
        int[] availableKeyguardClockImages = getAvailableKeyguardClockImages();
        if (this.mParameters.length >= 8) {
            return getPreviewImageDrawableV2();
        }
        if (availableKeyguardClockImages == null || availableKeyguardClockImages.length < 1) {
            return null;
        }
        return this.mResources.getDrawable(availableKeyguardClockImages[0], this.mContext.getTheme());
    }

    private Drawable getPreviewImageDrawableV2() {
        int identifier = this.mResources.getIdentifier(this.mParameters[9], "drawable", "com.android.systemui");
        if (identifier != 0) {
            return this.mResources.getDrawable(identifier, this.mContext.getTheme());
        }
        return null;
    }

    private boolean getBooleanFromIndex(int i, boolean z) {
        String[] strArr = this.mParameters;
        return strArr.length > i ? Boolean.parseBoolean(strArr[i]) : z;
    }

    private boolean isEnabled() {
        boolean booleanFromIndex = getBooleanFromIndex(5, true);
        if (this.mParameters.length >= 8) {
            booleanFromIndex = getEnabledV2();
        }
        return isSelectableByThemes() ? isSkinnableClockAvailable() : booleanFromIndex;
    }

    private boolean isSkinnableClockAvailable() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "com.sonymobile.runtimeskinning.lockscreen.clock.available", 0, -2) == 1;
    }

    private boolean isSelectableByThemes() {
        return getBooleanFromIndex(6, false);
    }

    private boolean getEnabledV2() {
        int identifier = this.mResources.getIdentifier(this.mParameters[7], "string", "com.android.systemui");
        if (identifier != 0) {
            return Boolean.parseBoolean(this.mResources.getString(identifier));
        }
        return false;
    }

    private int getPriority() {
        return Integer.parseInt(this.mParameters[2]);
    }

    private String getName() {
        int identifier;
        String[] strArr = this.mParameters;
        String str = strArr[0];
        return (strArr.length < 8 || (identifier = this.mResources.getIdentifier(strArr[8], "string", "com.android.systemui")) == 0) ? str : this.mResources.getString(identifier);
    }

    public final KeyguardComponentFactoryEntry build() {
        if (this.mParameters.length < 5) {
            return null;
        }
        return new KeyguardComponentFactoryEntry(getName(), this.mParameters[1], getPriority(), getPreviewImageDrawable(), this.mParameters[4], isEnabled(), isSelectableByThemes());
    }
}
