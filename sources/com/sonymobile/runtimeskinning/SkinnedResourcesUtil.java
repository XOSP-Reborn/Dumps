package com.sonymobile.runtimeskinning;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;

class SkinnedResourcesUtil {
    public static SkinnedResources getSkinnedResources(String str, String str2, String str3, int[] iArr, List<String> list, Context context) throws RuntimeSkinningException {
        List<String> list2;
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getPackageInfo(str2, 128).applicationInfo;
            if (str != null) {
                ApplicationInfo applicationInfo2 = packageManager.getPackageInfo(str, 128).applicationInfo;
                if (!packageManager.isSafeMode() || (applicationInfo2.flags & 1) != 0) {
                    list2 = OverlayExtractor.getSkinOverlayPaths(context, str, str2, str3, iArr);
                    return createSkinnedResources(context, list2, applicationInfo, list);
                }
            }
            list2 = null;
            return createSkinnedResources(context, list2, applicationInfo, list);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeSkinningException(e);
        }
    }

    private static SkinnedResources createSkinnedResources(Context context, List<String> list, ApplicationInfo applicationInfo, List<String> list2) throws RuntimeSkinningException {
        Pair<String, Resources> createOverlayResources;
        try {
            Resources resourcesForApplication = context.getPackageManager().getResourcesForApplication(applicationInfo);
            Pair<String, Resources> createOverlayResources2 = createOverlayResources(applicationInfo.publicSourceDir, resourcesForApplication.getDisplayMetrics(), resourcesForApplication.getConfiguration());
            if (createOverlayResources2 != null) {
                Resources resources = (Resources) createOverlayResources2.second;
                Map arrayMap = new ArrayMap();
                ArrayList arrayList = new ArrayList();
                if (list != null) {
                    for (String str : list) {
                        if ((list2 == null || list2.contains(str)) && (createOverlayResources = createOverlayResources(str, resources.getDisplayMetrics(), resources.getConfiguration())) != null) {
                            arrayList.add(createOverlayResources);
                        }
                    }
                    try {
                        arrayMap = OverlayExtractor.getResourcesAllowedToOverlay(resources, applicationInfo);
                    } catch (IOException | XmlPullParserException e) {
                        Log.e("runtime-skinning-lib", "Failed to parse which resources are allowed to overlay", e);
                    }
                }
                return new LocallySkinnedResources(applicationInfo.packageName, resources, arrayList, arrayMap);
            }
            throw new RuntimeSkinningException("Failed to create resources for target " + applicationInfo.packageName);
        } catch (PackageManager.NameNotFoundException e2) {
            throw new RuntimeSkinningException(e2);
        }
    }

    private static Pair<String, Resources> createOverlayResources(String str, DisplayMetrics displayMetrics, Configuration configuration) {
        try {
            ExceptionHandler exceptionHandler = new ExceptionHandler();
            AssetManager assetManager = AssetManagerUtils.getAssetManager();
            int addAssetPath = AssetManagerUtils.addAssetPath(assetManager, str, exceptionHandler);
            exceptionHandler.reThrow();
            if (addAssetPath <= 0) {
                return null;
            }
            Resources resources = new Resources(assetManager, displayMetrics, configuration);
            return new Pair<>(OverlayExtractor.getPackageName(resources, addAssetPath), resources);
        } catch (RemoteException | IOException | XmlPullParserException e) {
            Log.e("runtime-skinning-lib", "Failed to create overlay resources of " + str, e);
            return null;
        }
    }
}
