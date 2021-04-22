package com.android.systemui.classifier;

import android.content.Context;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.FalsingManager;

public class FalsingManagerFactory {
    private static FalsingManager sInstance;

    public static FalsingManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = (FalsingManager) Dependency.get(FalsingManager.class);
        }
        return sInstance;
    }
}
