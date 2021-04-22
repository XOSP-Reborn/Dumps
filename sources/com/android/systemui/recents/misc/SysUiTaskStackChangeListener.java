package com.android.systemui.recents.misc;

import android.content.Context;
import com.android.systemui.shared.system.TaskStackChangeListener;

public abstract class SysUiTaskStackChangeListener extends TaskStackChangeListener {
    /* access modifiers changed from: protected */
    public final boolean checkCurrentUserId(Context context, boolean z) {
        return checkCurrentUserId(SystemServicesProxy.getInstance(context).getCurrentUser(), z);
    }
}
