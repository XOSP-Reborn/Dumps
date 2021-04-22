package com.sonymobile.keyguard.plugininfrastructure;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;
import com.android.systemui.Dependency;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Iterator;
import java.util.LinkedList;

public class KeyguardPluginMetaDataLoader {
    private static final String TAG = "KeyguardPluginMetaDataLoader";
    private final Context mContext;

    public KeyguardPluginMetaDataLoader(Context context) {
        this.mContext = context;
    }

    public final LinkedList<KeyguardComponentFactoryEntry> getAvailableKeyguardFactories() throws PackageManager.NameNotFoundException {
        LinkedList<KeyguardComponentFactoryEntry> allKeyguardFactories = getAllKeyguardFactories();
        LinkedList<KeyguardComponentFactoryEntry> linkedList = new LinkedList<>();
        if (allKeyguardFactories != null) {
            Iterator<KeyguardComponentFactoryEntry> it = allKeyguardFactories.iterator();
            while (it.hasNext()) {
                KeyguardComponentFactoryEntry next = it.next();
                if (next != null && next.getEnabled()) {
                    linkedList.add(next);
                }
            }
        }
        return linkedList;
    }

    public final LinkedList<KeyguardComponentFactoryEntry> getAllKeyguardFactories() throws PackageManager.NameNotFoundException {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null) {
            return null;
        }
        int i = packageManager.getApplicationInfo("com.android.systemui", 128).metaData.getInt("com.sonymobile.keyguard.KEYGUARD_PLUGINS", -1);
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources == null) {
            resources = packageManager.getResourcesForApplication("com.android.systemui");
        }
        TypedArray obtainTypedArray = resources.obtainTypedArray(i);
        try {
            return getSuppliedFactories(obtainTypedArray, resources);
        } finally {
            if (obtainTypedArray != null) {
                obtainTypedArray.recycle();
            }
        }
    }

    private LinkedList<KeyguardComponentFactoryEntry> getSuppliedFactories(TypedArray typedArray, Resources resources) {
        KeyguardComponentFactoryEntry factoryEntryFromResourceId;
        LinkedList<KeyguardComponentFactoryEntry> linkedList = new LinkedList<>();
        for (int i = 0; i < typedArray.length(); i++) {
            int resourceId = typedArray.getResourceId(i, 0);
            if (!(resourceId == 0 || (factoryEntryFromResourceId = getFactoryEntryFromResourceId(resourceId, resources)) == null)) {
                linkedList.add(factoryEntryFromResourceId);
            }
        }
        return linkedList;
    }

    public final KeyguardComponentFactoryEntry getFactoryEntryFromClassName(String str) {
        try {
            LinkedList<KeyguardComponentFactoryEntry> allKeyguardFactories = getAllKeyguardFactories();
            if (allKeyguardFactories == null) {
                return null;
            }
            Iterator<KeyguardComponentFactoryEntry> it = allKeyguardFactories.iterator();
            while (it.hasNext()) {
                KeyguardComponentFactoryEntry next = it.next();
                if (next.getFullyQualifiedClassName().equals(str)) {
                    return next;
                }
            }
            return null;
        } catch (PackageManager.NameNotFoundException unused) {
            String str2 = TAG;
            Log.w(str2, "getFactoryEntryFromClassName - NameNotFoundException while searching for " + str);
            return null;
        }
    }

    private KeyguardComponentFactoryEntry getFactoryEntryFromResourceId(int i, Resources resources) {
        KeyguardComponentFactoryEntryBuilder keyguardComponentFactoryEntryBuilder = new KeyguardComponentFactoryEntryBuilder(resources, this.mContext);
        keyguardComponentFactoryEntryBuilder.setFromResourceId(i);
        return keyguardComponentFactoryEntryBuilder.build();
    }
}
