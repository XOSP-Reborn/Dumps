package com.sonymobile.runtimeskinning;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import com.sonymobile.runtimeskinning.Rev5SkinGlueFactory;
import java.util.ArrayList;

class Rev5aSkinGlueFactory extends Rev5SkinGlueFactory {
    private static boolean DEBUG = false;

    Rev5aSkinGlueFactory() {
    }

    @Override // com.sonymobile.runtimeskinning.Rev5SkinGlueFactory, com.sonymobile.runtimeskinning.Rev5SkinGlueFactory, com.sonymobile.runtimeskinning.SkinGlueFactory
    public SkinGlueRev5a produceInstance(Context context) {
        return new SkinGlueRev5a(context);
    }

    @Override // com.sonymobile.runtimeskinning.Rev5SkinGlueFactory, com.sonymobile.runtimeskinning.SkinGlueFactory
    public boolean isApplicable(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.sonymobile.runtimeskinning.core", "com.sonymobile.runtimeskinning.manager.SkinManagerService"));
        if (Build.VERSION.SDK_INT != 26 || context.getPackageManager().resolveService(intent, 0) == null || !init()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public static class SkinGlueRev5a extends Rev5SkinGlueFactory.SkinGlueRev5 {
        private SkinGlueRev5a(Context context) {
            super(context);
        }

        @Override // com.sonymobile.runtimeskinning.Rev5SkinGlueFactory.SkinGlueRev5, com.sonymobile.runtimeskinning.SkinGlue
        public void reset() {
            super.reset();
        }

        @Override // com.sonymobile.runtimeskinning.Rev5SkinGlueFactory.SkinGlueRev5, com.sonymobile.runtimeskinning.SkinGlue
        public SkinnedResources getSkinnedResourcesForCurrentSkin(String str, String str2, Context context) throws RemoteException, RuntimeSkinningException {
            ArrayList arrayList;
            int[] iArr;
            String str3;
            if (Rev5aSkinGlueFactory.DEBUG) {
                Log.d("SkinGlueRev5a", "getSkinnedResourcesForCurrentSkin");
            }
            ISkinManager service = getService();
            if (service != null) {
                Bundle skinState = service.getSkinState();
                ArrayList arrayList2 = null;
                if (skinState != null) {
                    String string = skinState.getString("skinPackage");
                    int[] intArray = skinState.getIntArray("enabledGroups");
                    ArrayList<String> stringArrayList = skinState.getStringArrayList("overlayTargets");
                    ArrayList<String> stringArrayList2 = skinState.getStringArrayList("overlayPaths");
                    if (stringArrayList != null) {
                        arrayList2 = new ArrayList();
                        for (int i = 0; i < stringArrayList.size(); i++) {
                            if (str.equals(stringArrayList.get(i))) {
                                arrayList2.add(stringArrayList2.get(i));
                            }
                        }
                    }
                    arrayList = arrayList2;
                    iArr = intArray;
                    str3 = string;
                } else {
                    str3 = service.getSkin();
                    iArr = null;
                    arrayList = null;
                }
                return SkinnedResourcesUtil.getSkinnedResources(str3, str, str2, iArr, arrayList, context);
            }
            throw new RuntimeSkinningException("Failed to connect to the skin manager");
        }
    }
}
