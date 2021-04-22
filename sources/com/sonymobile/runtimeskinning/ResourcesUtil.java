package com.sonymobile.runtimeskinning;

import android.content.res.Resources;
import android.util.TypedValue;

class ResourcesUtil {
    public static int getFirstCookieValue(Resources resources) {
        int identifier = Resources.getSystem().getIdentifier("config_annoy_dianne", "bool", "android");
        TypedValue typedValue = new TypedValue();
        Resources.getSystem().getValue(identifier, typedValue, true);
        return typedValue.assetCookie;
    }
}
