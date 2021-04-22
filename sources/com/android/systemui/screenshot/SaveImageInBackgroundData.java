package com.android.systemui.screenshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

/* compiled from: GlobalScreenshot */
class SaveImageInBackgroundData {
    Context context;
    int errorMsgResId;
    Runnable finisher;
    int iconSize;
    Bitmap image;
    Uri imageUri;
    float previewScale;
    int previewWidth;
    int previewheight;

    SaveImageInBackgroundData() {
    }

    /* access modifiers changed from: package-private */
    public void clearImage() {
        this.image = null;
        this.imageUri = null;
        this.iconSize = 0;
    }

    /* access modifiers changed from: package-private */
    public void clearContext() {
        this.context = null;
    }
}
