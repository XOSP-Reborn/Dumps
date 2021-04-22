package com.sonymobile.runtimeskinning;

import android.content.Context;

/* access modifiers changed from: package-private */
public interface SkinGlueFactory {
    boolean isApplicable(Context context);

    SkinGlue produceInstance(Context context);
}
