package com.sonymobile.runtimeskinning;

import android.content.Context;
import android.os.Build;
import com.sonymobile.runtimeskinning.Rev3SkinGlueFactory;
import java.lang.reflect.Method;

class Rev4SkinGlueFactory extends Rev3SkinGlueFactory {
    private static Method sMethodSetRuntimeSkin2;

    Rev4SkinGlueFactory() {
    }

    @Override // com.sonymobile.runtimeskinning.Rev3SkinGlueFactory, com.sonymobile.runtimeskinning.Rev3SkinGlueFactory, com.sonymobile.runtimeskinning.SkinGlueFactory
    public SkinGlueRev4 produceInstance(Context context) {
        return new SkinGlueRev4();
    }

    @Override // com.sonymobile.runtimeskinning.Rev3SkinGlueFactory, com.sonymobile.runtimeskinning.SkinGlueFactory
    public boolean isApplicable(Context context) {
        return Build.VERSION.SDK_INT >= 23 && init();
    }

    /* access modifiers changed from: protected */
    @Override // com.sonymobile.runtimeskinning.Rev3SkinGlueFactory
    public boolean init() {
        boolean z;
        synchronized (Rev3SkinGlueFactory.sLock) {
            z = true;
            if (sMethodSetRuntimeSkin2 == null) {
                sMethodSetRuntimeSkin2 = ReflectionUtils.getMethod("android.content.pm.IPackageManager", "setRuntimeSkin2", Boolean.TYPE, Integer.TYPE, String.class, int[].class);
            }
            if (!super.init() || sMethodSetRuntimeSkin2 == null) {
                z = false;
            }
        }
        return z;
    }

    /* access modifiers changed from: private */
    public class SkinGlueRev4 extends Rev3SkinGlueFactory.SkinGlueRev3 {
        private SkinGlueRev4() {
            super();
        }
    }
}
