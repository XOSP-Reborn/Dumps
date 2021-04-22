package com.android.systemui.shared.system;

import android.app.ActivityOptions;

public abstract class ActivityOptionsCompat {
    public static ActivityOptions makeSplitScreenOptions(boolean z) {
        return makeSplitScreenOptions(z, true);
    }

    public static ActivityOptions makeSplitScreenOptions(boolean z, boolean z2) {
        ActivityOptions makeBasic = ActivityOptions.makeBasic();
        makeBasic.setLaunchWindowingMode(z2 ? 3 : 4);
        makeBasic.setSplitScreenCreateMode(!z ? 1 : 0);
        return makeBasic;
    }
}
