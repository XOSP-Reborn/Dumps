package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.PorterDuffColorFilter;
import com.android.systemui.C0008R$integer;

public class NotificationIconDozeHelper extends NotificationDozeHelper {
    private int mColor = -16777216;
    private PorterDuffColorFilter mImageColorFilter = null;
    private final int mImageDarkAlpha;
    private final int mImageDarkColor = -1;

    public NotificationIconDozeHelper(Context context) {
        this.mImageDarkAlpha = context.getResources().getInteger(C0008R$integer.doze_small_icon_alpha);
    }

    public void setColor(int i) {
        this.mColor = i;
    }
}
