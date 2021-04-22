package com.sonymobile.runtimeskinning;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.RemoteException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class Rev3SkinGlueFactory implements SkinGlueFactory {
    private static Field sFieldConfigSkinPackage;
    private static Field sFieldIsSkin;
    private static Field sFieldIsVerifiedSkin;
    private static Method sGetCookieName;
    private static Method sGetPackageManager;
    protected static final Object sLock = new Object();
    private static Method sMethodGetRuntimeSkin;
    private static Method sMethodSetRuntimeSkin;
    private static Method sMyUserId;

    Rev3SkinGlueFactory() {
    }

    @Override // com.sonymobile.runtimeskinning.SkinGlueFactory
    public SkinGlueRev3 produceInstance(Context context) {
        return new SkinGlueRev3();
    }

    @Override // com.sonymobile.runtimeskinning.SkinGlueFactory
    public boolean isApplicable(Context context) {
        return Build.VERSION.SDK_INT >= 19 && init();
    }

    /* access modifiers changed from: protected */
    public boolean init() {
        boolean z;
        synchronized (sLock) {
            if (sFieldIsSkin == null) {
                sFieldIsSkin = ReflectionUtils.getField(PackageInfo.class, "isSkin", Boolean.TYPE);
            }
            if (sFieldIsVerifiedSkin == null) {
                sFieldIsVerifiedSkin = ReflectionUtils.getField(PackageInfo.class, "isVerifiedSkin", Boolean.TYPE);
            }
            if (sFieldConfigSkinPackage == null) {
                sFieldConfigSkinPackage = ReflectionUtils.getField(ActivityInfo.class, "CONFIG_SKIN_PACKAGE", Integer.TYPE);
            }
            z = true;
            if (sMethodGetRuntimeSkin == null) {
                sMethodGetRuntimeSkin = ReflectionUtils.getMethod("android.content.pm.IPackageManager", "getRuntimeSkin", String.class, Integer.TYPE);
            }
            if (sMethodSetRuntimeSkin == null) {
                sMethodSetRuntimeSkin = ReflectionUtils.getMethod("android.content.pm.IPackageManager", "setRuntimeSkin", Boolean.TYPE, Integer.TYPE, String.class);
            }
            if (sGetPackageManager == null) {
                sGetPackageManager = ReflectionUtils.getMethod("android.app.AppGlobals", "getPackageManager", Object.class, new Class[0]);
            }
            if (sMyUserId == null) {
                sMyUserId = ReflectionUtils.getMethod("android.os.UserHandle", "myUserId", Integer.TYPE, new Class[0]);
            }
            if (sGetCookieName == null) {
                sGetCookieName = ReflectionUtils.getMethod(AssetManager.class, "getCookieName", String.class, Integer.TYPE);
            }
            if (sFieldIsSkin == null || sFieldIsVerifiedSkin == null || sFieldConfigSkinPackage == null || sMethodGetRuntimeSkin == null || sMethodSetRuntimeSkin == null || sGetPackageManager == null || sMyUserId == null || sGetCookieName == null) {
                z = false;
            }
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public class SkinGlueRev3 implements SkinGlue {
        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public void reset() {
        }

        protected SkinGlueRev3() {
        }

        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public SkinnedResources getSkinnedResourcesForCurrentSkin(String str, String str2, Context context) throws RemoteException, RuntimeSkinningException {
            try {
                PackageManager packageManager = context.getPackageManager();
                return new LegacySkinnedResources(packageManager.getResourcesForApplication(str), packageManager.getApplicationInfo(str, 0).sourceDir, packageManager.getApplicationInfo("android", 0).sourceDir, Rev3SkinGlueFactory.sGetCookieName);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeSkinningException(e);
            }
        }
    }
}
