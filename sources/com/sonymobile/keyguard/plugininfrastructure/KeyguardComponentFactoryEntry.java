package com.sonymobile.keyguard.plugininfrastructure;

import android.graphics.drawable.Drawable;

public class KeyguardComponentFactoryEntry {
    private final String mAdditionalInformationResourceName;
    private final boolean mEnabled;
    private final String mFullyQualifiedClassName;
    private final String mName;
    private final Drawable mPreviewImage;
    private final int mPriority;
    private final boolean mSelectableByThemes;

    public KeyguardComponentFactoryEntry(String str, String str2, int i, Drawable drawable, String str3, boolean z, boolean z2) {
        this.mName = str;
        this.mPriority = i;
        this.mFullyQualifiedClassName = str2;
        this.mPreviewImage = drawable;
        this.mAdditionalInformationResourceName = str3;
        this.mEnabled = z;
        this.mSelectableByThemes = z2;
    }

    public final String getName() {
        return this.mName;
    }

    public final String getFullyQualifiedClassName() {
        return this.mFullyQualifiedClassName;
    }

    public final int getPriority() {
        return this.mPriority;
    }

    public final boolean getEnabled() {
        return this.mEnabled;
    }

    public final boolean getSelectableByThemes() {
        return this.mSelectableByThemes;
    }

    public final String toString() {
        return "{\"name\":\"" + this.mName + "\", \"fullyQualifiedClassName\":\"" + this.mFullyQualifiedClassName + "\", \"priority\":\"" + this.mPriority + "\", \"previewImage\":\"" + this.mPreviewImage + "\", \"additionalInformationResourceName\":\"" + this.mAdditionalInformationResourceName + "\", \"enabled\":\"" + this.mEnabled + "\", \"selectableByThemes\":\"" + this.mSelectableByThemes + "\"}";
    }
}
