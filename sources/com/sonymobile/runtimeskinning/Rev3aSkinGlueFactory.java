package com.sonymobile.runtimeskinning;

import android.content.Context;
import com.sonymobile.runtimeskinning.Rev3SkinGlueFactory;
import java.lang.reflect.Method;

class Rev3aSkinGlueFactory extends Rev3SkinGlueFactory {
    private static Method sMethodIsUserSkinningAvailable;

    Rev3aSkinGlueFactory() {
    }

    @Override // com.sonymobile.runtimeskinning.Rev3SkinGlueFactory, com.sonymobile.runtimeskinning.Rev3SkinGlueFactory, com.sonymobile.runtimeskinning.SkinGlueFactory
    public SkinGlueRev3a produceInstance(Context context) {
        return new SkinGlueRev3a();
    }

    /* access modifiers changed from: protected */
    @Override // com.sonymobile.runtimeskinning.Rev3SkinGlueFactory
    public boolean init() {
        boolean z;
        synchronized (Rev3SkinGlueFactory.sLock) {
            if (sMethodIsUserSkinningAvailable == null) {
                sMethodIsUserSkinningAvailable = ReflectionUtils.getMethod("android.content.pm.IPackageManager", "isUserSkinningAvailable", Boolean.TYPE, (Class<?>[]) null);
            }
            z = super.init() && sMethodIsUserSkinningAvailable != null;
        }
        return z;
    }

    /* access modifiers changed from: private */
    public class SkinGlueRev3a extends Rev3SkinGlueFactory.SkinGlueRev3 {
        private SkinGlueRev3a() {
            super();
        }
    }
}
