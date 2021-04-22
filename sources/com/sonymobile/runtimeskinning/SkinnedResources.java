package com.sonymobile.runtimeskinning;

import android.content.res.Resources;

public abstract class SkinnedResources extends Resources {
    public SkinnedResources(Resources resources) {
        super(resources.getAssets(), resources.getDisplayMetrics(), resources.getConfiguration());
    }
}
