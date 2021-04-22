package com.sonymobile.runtimeskinning;

import android.content.res.Resources;
import android.util.Pair;
import android.util.TypedValue;
import java.util.Map;

class ResolverUtil {
    public static int resolveReference(Resources resources, Map<Integer, Pair<Integer, Resources>> map, int i) throws Resources.NotFoundException {
        int i2;
        if (map.get(Integer.valueOf(i)) != null) {
            return i;
        }
        TypedValue typedValue = new TypedValue();
        resources.getValue(i, typedValue, false);
        while (typedValue.type == 1 && (i2 = typedValue.data) != 0 && i2 != 1) {
            if (map.get(Integer.valueOf(i2)) != null) {
                return i2;
            }
            resources.getValue(i2, typedValue, false);
            i = i2;
        }
        return i;
    }
}
