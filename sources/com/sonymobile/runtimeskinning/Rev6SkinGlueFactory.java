package com.sonymobile.runtimeskinning;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.sonymobile.runtimeskinning.ISkinManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* access modifiers changed from: package-private */
public class Rev6SkinGlueFactory implements SkinGlueFactory {
    private static boolean DEBUG = false;
    private static Method sBindServiceAsUser;
    private static Field sCurrentUserHandle;
    private static final Object sLock = new Object();
    private static Method sMyUserId;
    private static volatile SkinManagerServiceConnection sServiceConnection;

    Rev6SkinGlueFactory() {
    }

    @Override // com.sonymobile.runtimeskinning.SkinGlueFactory
    public SkinGlue produceInstance(Context context) {
        return new SkinGlueRev6(context);
    }

    @Override // com.sonymobile.runtimeskinning.SkinGlueFactory
    public boolean isApplicable(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.sonymobile.runtimeskinning.core", "com.sonymobile.runtimeskinning.manager.SkinManagerService"));
        if (Build.VERSION.SDK_INT <= 26 || context.getPackageManager().resolveService(intent, 0) == null || !init()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean init() {
        boolean z;
        synchronized (sLock) {
            z = false;
            if (sMyUserId == null) {
                sMyUserId = ReflectionUtils.getMethod(UserHandle.class, "myUserId", Integer.TYPE, new Class[0]);
            }
            if (sBindServiceAsUser == null) {
                sBindServiceAsUser = ReflectionUtils.getMethod(Context.class, "bindServiceAsUser", Boolean.TYPE, Intent.class, ServiceConnection.class, Integer.TYPE, UserHandle.class);
            }
            if (sCurrentUserHandle == null) {
                sCurrentUserHandle = ReflectionUtils.getField(UserHandle.class, "CURRENT", UserHandle.class);
            }
            if (!(sMyUserId == null || sBindServiceAsUser == null || sCurrentUserHandle == null)) {
                z = true;
            }
        }
        return z;
    }

    protected static class SkinGlueRev6 implements SkinGlue {
        protected SkinGlueRev6(Context context) {
            synchronized (Rev6SkinGlueFactory.sLock) {
                if (Rev6SkinGlueFactory.sServiceConnection == null) {
                    SkinManagerServiceConnection unused = Rev6SkinGlueFactory.sServiceConnection = new SkinManagerServiceConnection(context);
                }
            }
            Rev6SkinGlueFactory.sServiceConnection.bind(this);
        }

        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public SkinnedResources getSkinnedResourcesForCurrentSkin(String str, String str2, Context context) throws RemoteException, RuntimeSkinningException {
            ArrayList arrayList;
            int[] iArr;
            String str3;
            if (Rev6SkinGlueFactory.DEBUG) {
                Log.d("SkinGlueRev6", "getSkinnedResourcesForCurrentSkin");
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

        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public void reset() {
            Rev6SkinGlueFactory.sServiceConnection.unbind(this);
        }

        /* access modifiers changed from: package-private */
        public ISkinManager getService() throws IllegalStateException {
            return Rev6SkinGlueFactory.sServiceConnection.getService();
        }
    }

    /* access modifiers changed from: private */
    public static class SkinManagerServiceConnection implements ServiceConnection {
        private static final List<String> SHARED_PROCESSES = Arrays.asList("com.android.systemui");
        private final Context mContext;
        private boolean mIsBound = false;
        private final Object mLock = new Object();
        private ISkinManager mService;
        private final Set<SkinGlue> mSkinGlues = new HashSet();

        public SkinManagerServiceConnection(Context context) {
            this.mContext = context;
        }

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (this.mLock) {
                this.mService = ISkinManager.Stub.asInterface(iBinder);
                if (Rev6SkinGlueFactory.DEBUG) {
                    Log.d("SkinGlueRev6-connection", "onServiceConnected mService=" + this.mService);
                }
                this.mLock.notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (this.mLock) {
                this.mService = null;
                if (Rev6SkinGlueFactory.DEBUG) {
                    Log.d("SkinGlueRev6-connection", "onServiceDisconnected mService=null");
                }
            }
        }

        public void bind(SkinGlue skinGlue) {
            synchronized (this.mLock) {
                this.mSkinGlues.add(skinGlue);
                bindLocked();
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x0078  */
        /* JADX WARNING: Removed duplicated region for block: B:19:? A[RETURN, SYNTHETIC] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void bindLocked() {
            /*
            // Method dump skipped, instructions count: 126
            */
            throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.runtimeskinning.Rev6SkinGlueFactory.SkinManagerServiceConnection.bindLocked():void");
        }

        public void unbind(SkinGlue skinGlue) {
            synchronized (this.mLock) {
                this.mSkinGlues.remove(skinGlue);
                if (this.mSkinGlues.isEmpty()) {
                    unbindLocked();
                }
            }
        }

        private void unbindLocked() {
            if (this.mIsBound) {
                this.mContext.unbindService(this);
                this.mIsBound = false;
            }
            this.mService = null;
            this.mLock.notifyAll();
        }

        public ISkinManager getService() throws IllegalStateException {
            ISkinManager iSkinManager;
            if (Looper.myLooper() != Looper.getMainLooper()) {
                synchronized (this.mLock) {
                    while (this.mIsBound && this.mService == null) {
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException unused) {
                            Log.i("SkinGlueRev6-connection", "Interrupted while trying to retrieve ISkinManager");
                        }
                    }
                    iSkinManager = this.mService;
                }
                return iSkinManager;
            }
            throw new IllegalStateException("getService is not allowed to run on the main thread");
        }

        private Intent getServiceIntent() {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.sonymobile.runtimeskinning.core", "com.sonymobile.runtimeskinning.manager.SkinManagerService"));
            intent.setFlags(268435456);
            return intent;
        }
    }
}
