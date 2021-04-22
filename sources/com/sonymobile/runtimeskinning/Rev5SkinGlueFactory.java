package com.sonymobile.runtimeskinning;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.sonymobile.runtimeskinning.ISkinManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* access modifiers changed from: package-private */
public class Rev5SkinGlueFactory implements SkinGlueFactory {
    private static boolean DEBUG = false;
    private static Method sBindServiceAsUser;
    private static Field sCurrentUserHandle;
    private static Method sGetCookieName;
    private static final Object sLock = new Object();
    private static Method sMyUserId;
    private static volatile SkinManagerServiceConnection sServiceConnection;

    Rev5SkinGlueFactory() {
    }

    @Override // com.sonymobile.runtimeskinning.SkinGlueFactory
    public SkinGlueRev5 produceInstance(Context context) {
        return new SkinGlueRev5(context);
    }

    @Override // com.sonymobile.runtimeskinning.SkinGlueFactory
    public boolean isApplicable(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.sonymobile.runtimeskinning.core", "com.sonymobile.runtimeskinning.manager.SkinManagerService"));
        int i = Build.VERSION.SDK_INT;
        if ((i == 24 || i == 25) && context.getPackageManager().resolveService(intent, 0) != null && init()) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean init() {
        boolean z;
        synchronized (sLock) {
            z = false;
            if (sMyUserId == null) {
                sMyUserId = ReflectionUtils.getMethod("android.os.UserHandle", "myUserId", Integer.TYPE, new Class[0]);
            }
            if (sBindServiceAsUser == null) {
                sBindServiceAsUser = ReflectionUtils.getMethod(Context.class, "bindServiceAsUser", Boolean.TYPE, Intent.class, ServiceConnection.class, Integer.TYPE, UserHandle.class);
            }
            if (sCurrentUserHandle == null) {
                sCurrentUserHandle = ReflectionUtils.getField(UserHandle.class, "CURRENT", UserHandle.class);
            }
            if (sGetCookieName == null) {
                sGetCookieName = ReflectionUtils.getMethod(AssetManager.class, "getCookieName", String.class, Integer.TYPE);
            }
            if (!(sMyUserId == null || sBindServiceAsUser == null || sCurrentUserHandle == null || sGetCookieName == null)) {
                z = true;
            }
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public static class SkinGlueRev5 implements SkinGlue {
        protected SkinGlueRev5(Context context) {
            synchronized (Rev5SkinGlueFactory.sLock) {
                if (Rev5SkinGlueFactory.sServiceConnection == null) {
                    SkinManagerServiceConnection unused = Rev5SkinGlueFactory.sServiceConnection = new SkinManagerServiceConnection(context);
                }
            }
            Rev5SkinGlueFactory.sServiceConnection.bind(this);
        }

        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public SkinnedResources getSkinnedResourcesForCurrentSkin(String str, String str2, Context context) throws RemoteException, RuntimeSkinningException {
            if (Rev5SkinGlueFactory.DEBUG) {
                Log.d("SkinGlueRev5", "getSkinnedResourcesForCurrentSkin");
            }
            try {
                PackageManager packageManager = context.getPackageManager();
                return new LegacySkinnedResources(packageManager.getResourcesForApplication(str), packageManager.getApplicationInfo(str, 0).sourceDir, packageManager.getApplicationInfo("android", 0).sourceDir, Rev5SkinGlueFactory.sGetCookieName);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeSkinningException(e);
            }
        }

        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public void reset() {
            Rev5SkinGlueFactory.sServiceConnection.unbind(this);
        }

        /* access modifiers changed from: package-private */
        public ISkinManager getService() throws IllegalStateException {
            return Rev5SkinGlueFactory.sServiceConnection.getService();
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
                if (Rev5SkinGlueFactory.DEBUG) {
                    Log.d("SkinGlueRev5-connection", "onServiceConnected mService=" + this.mService);
                }
                this.mLock.notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (this.mLock) {
                this.mService = null;
                if (Rev5SkinGlueFactory.DEBUG) {
                    Log.d("SkinGlueRev5-connection", "onServiceDisconnected mService=null");
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
            throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.runtimeskinning.Rev5SkinGlueFactory.SkinManagerServiceConnection.bindLocked():void");
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
                            Log.i("SkinGlueRev5-connection", "Interrupted while trying to retrieve ISkinManager");
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
