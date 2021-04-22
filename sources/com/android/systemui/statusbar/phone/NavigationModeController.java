package com.android.systemui.statusbar.phone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ApkAssets;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseBooleanArray;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dumpable;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.util.NotificationChannels;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class NavigationModeController implements Dumpable {
    private static final String TAG = "NavigationModeController";
    private final Context mContext;
    private Context mCurrentUserContext;
    private final DeviceProvisionedController.DeviceProvisionedListener mDeviceProvisionedCallback = new DeviceProvisionedController.DeviceProvisionedListener() {
        /* class com.android.systemui.statusbar.phone.NavigationModeController.AnonymousClass2 */

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onDeviceProvisionedChanged() {
            NavigationModeController.this.restoreGesturalNavOverlayIfNecessary();
        }

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onUserSetupChanged() {
            NavigationModeController.this.restoreGesturalNavOverlayIfNecessary();
        }

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onUserSwitched() {
            NavigationModeController.this.updateCurrentInteractionMode(true);
            NavigationModeController.this.switchFromGestureNavModeIfNotSupportedByDefaultLauncher();
            NavigationModeController.this.deferGesturalNavOverlayIfNecessary();
        }
    };
    private final DeviceProvisionedController mDeviceProvisionedController;
    private String mLastDefaultLauncher;
    private ArrayList<ModeChangedListener> mListeners = new ArrayList<>();
    private int mMode = 0;
    private final IOverlayManager mOverlayManager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.phone.NavigationModeController.AnonymousClass1 */

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
        /* JADX WARNING: Removed duplicated region for block: B:16:0x0054  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(android.content.Context r3, android.content.Intent r4) {
            /*
                r2 = this;
                java.lang.String r3 = r4.getAction()
                int r4 = r3.hashCode()
                r0 = -1946981856(0xffffffff8bf36a20, float:-9.3759874E-32)
                r1 = 1
                if (r4 == r0) goto L_0x001e
                r0 = 1358685446(0x50fbe506, float:3.3808724E10)
                if (r4 == r0) goto L_0x0014
                goto L_0x0028
            L_0x0014:
                java.lang.String r4 = "android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED"
                boolean r3 = r3.equals(r4)
                if (r3 == 0) goto L_0x0028
                r3 = r1
                goto L_0x0029
            L_0x001e:
                java.lang.String r4 = "android.intent.action.OVERLAY_CHANGED"
                boolean r3 = r3.equals(r4)
                if (r3 == 0) goto L_0x0028
                r3 = 0
                goto L_0x0029
            L_0x0028:
                r3 = -1
            L_0x0029:
                if (r3 == 0) goto L_0x0054
                if (r3 == r1) goto L_0x002e
                goto L_0x0059
            L_0x002e:
                com.android.systemui.statusbar.phone.NavigationModeController r3 = com.android.systemui.statusbar.phone.NavigationModeController.this
                android.content.Context r4 = com.android.systemui.statusbar.phone.NavigationModeController.access$000(r3)
                java.lang.String r3 = com.android.systemui.statusbar.phone.NavigationModeController.access$100(r3, r4)
                com.android.systemui.statusbar.phone.NavigationModeController r4 = com.android.systemui.statusbar.phone.NavigationModeController.this
                java.lang.String r4 = com.android.systemui.statusbar.phone.NavigationModeController.access$200(r4)
                boolean r4 = android.text.TextUtils.equals(r4, r3)
                if (r4 != 0) goto L_0x0059
                com.android.systemui.statusbar.phone.NavigationModeController r4 = com.android.systemui.statusbar.phone.NavigationModeController.this
                com.android.systemui.statusbar.phone.NavigationModeController.access$300(r4)
                com.android.systemui.statusbar.phone.NavigationModeController r4 = com.android.systemui.statusbar.phone.NavigationModeController.this
                com.android.systemui.statusbar.phone.NavigationModeController.access$400(r4)
                com.android.systemui.statusbar.phone.NavigationModeController r2 = com.android.systemui.statusbar.phone.NavigationModeController.this
                com.android.systemui.statusbar.phone.NavigationModeController.access$202(r2, r3)
                goto L_0x0059
            L_0x0054:
                com.android.systemui.statusbar.phone.NavigationModeController r2 = com.android.systemui.statusbar.phone.NavigationModeController.this
                r2.updateCurrentInteractionMode(r1)
            L_0x0059:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NavigationModeController.AnonymousClass1.onReceive(android.content.Context, android.content.Intent):void");
        }
    };
    private SparseBooleanArray mRestoreGesturalNavBarMode = new SparseBooleanArray();
    private final UiOffloadThread mUiOffloadThread;

    public interface ModeChangedListener {
        void onNavigationModeChanged(int i);
    }

    public NavigationModeController(Context context, DeviceProvisionedController deviceProvisionedController, UiOffloadThread uiOffloadThread) {
        this.mContext = context;
        this.mCurrentUserContext = context;
        this.mOverlayManager = IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        this.mUiOffloadThread = uiOffloadThread;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedCallback);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.OVERLAY_CHANGED");
        intentFilter.addDataScheme("package");
        intentFilter.addDataSchemeSpecificPart("android", 0);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED"), null, null);
        this.mLastDefaultLauncher = getDefaultLauncherPackageName(this.mContext);
        updateCurrentInteractionMode(false);
        switchFromGestureNavModeIfNotSupportedByDefaultLauncher();
        deferGesturalNavOverlayIfNecessary();
    }

    public void updateCurrentInteractionMode(boolean z) {
        this.mCurrentUserContext = getCurrentUserContext();
        int currentInteractionMode = getCurrentInteractionMode(this.mCurrentUserContext);
        this.mMode = currentInteractionMode;
        this.mUiOffloadThread.submit(new Runnable(currentInteractionMode) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationModeController$Az4iHIVUWwUXS_IGosEIyzFux8w */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                NavigationModeController.this.lambda$updateCurrentInteractionMode$0$NavigationModeController(this.f$1);
            }
        });
        if (z) {
            for (int i = 0; i < this.mListeners.size(); i++) {
                this.mListeners.get(i).onNavigationModeChanged(currentInteractionMode);
            }
        }
    }

    public /* synthetic */ void lambda$updateCurrentInteractionMode$0$NavigationModeController(int i) {
        Settings.Secure.putString(this.mCurrentUserContext.getContentResolver(), "navigation_mode", String.valueOf(i));
    }

    public int addListener(ModeChangedListener modeChangedListener) {
        this.mListeners.add(modeChangedListener);
        return getCurrentInteractionMode(this.mCurrentUserContext);
    }

    public void removeListener(ModeChangedListener modeChangedListener) {
        this.mListeners.remove(modeChangedListener);
    }

    private int getCurrentInteractionMode(Context context) {
        return context.getResources().getInteger(17694851);
    }

    public Context getCurrentUserContext() {
        int currentUserId = ActivityManagerWrapper.getInstance().getCurrentUserId();
        if (this.mContext.getUserId() == currentUserId) {
            return this.mContext;
        }
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, UserHandle.of(currentUserId));
        } catch (PackageManager.NameNotFoundException unused) {
            return null;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void deferGesturalNavOverlayIfNecessary() {
        int currentUser = this.mDeviceProvisionedController.getCurrentUser();
        this.mRestoreGesturalNavBarMode.put(currentUser, false);
        if (!this.mDeviceProvisionedController.isDeviceProvisioned() || !this.mDeviceProvisionedController.isCurrentUserSetup()) {
            ArrayList arrayList = new ArrayList();
            try {
                arrayList.addAll(Arrays.asList(this.mOverlayManager.getDefaultOverlayPackages()));
            } catch (RemoteException unused) {
                Log.e(TAG, "deferGesturalNavOverlayIfNecessary: failed to fetch default overlays");
            }
            if (arrayList.contains("com.android.internal.systemui.navbar.gestural")) {
                setModeOverlay("com.android.internal.systemui.navbar.threebutton", -2);
                this.mRestoreGesturalNavBarMode.put(currentUser, true);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void restoreGesturalNavOverlayIfNecessary() {
        int currentUser = this.mDeviceProvisionedController.getCurrentUser();
        if (this.mRestoreGesturalNavBarMode.get(currentUser)) {
            setModeOverlay("com.android.internal.systemui.navbar.gestural", -2);
            this.mRestoreGesturalNavBarMode.put(currentUser, false);
        }
    }

    public void setModeOverlay(String str, int i) {
        this.mUiOffloadThread.submit(new Runnable(str, i) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationModeController$XNbfE14hTqTsqzjGfhml_ek2wAw */
            private final /* synthetic */ String f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                NavigationModeController.this.lambda$setModeOverlay$1$NavigationModeController(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$setModeOverlay$1$NavigationModeController(String str, int i) {
        try {
            this.mOverlayManager.setEnabledExclusiveInCategory(str, i);
        } catch (RemoteException unused) {
            String str2 = TAG;
            Log.e(str2, "Failed to enable overlay " + str + " for user " + i);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void switchFromGestureNavModeIfNotSupportedByDefaultLauncher() {
        Boolean isGestureNavSupportedByDefaultLauncher;
        if (getCurrentInteractionMode(this.mCurrentUserContext) == 2 && (isGestureNavSupportedByDefaultLauncher = isGestureNavSupportedByDefaultLauncher(this.mCurrentUserContext)) != null && !isGestureNavSupportedByDefaultLauncher.booleanValue()) {
            String str = TAG;
            Log.d(str, "Switching system navigation to 3-button mode: defaultLauncher=" + getDefaultLauncherPackageName(this.mCurrentUserContext) + " contextUser=" + this.mCurrentUserContext.getUserId());
            setModeOverlay("com.android.internal.systemui.navbar.threebutton", -2);
            showNotification(this.mCurrentUserContext, C0014R$string.notification_content_system_nav_changed);
            this.mCurrentUserContext.getSharedPreferences("navigation_mode_controller_preferences", 0).edit().putBoolean("switched_from_gesture_nav", true).apply();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showNotificationIfDefaultLauncherSupportsGestureNav() {
        Boolean isGestureNavSupportedByDefaultLauncher;
        if (this.mCurrentUserContext.getSharedPreferences("navigation_mode_controller_preferences", 0).getBoolean("switched_from_gesture_nav", false) && getCurrentInteractionMode(this.mCurrentUserContext) != 2 && (isGestureNavSupportedByDefaultLauncher = isGestureNavSupportedByDefaultLauncher(this.mCurrentUserContext)) != null && isGestureNavSupportedByDefaultLauncher.booleanValue()) {
            showNotification(this.mCurrentUserContext, C0014R$string.notification_content_gesture_nav_available);
            this.mCurrentUserContext.getSharedPreferences("navigation_mode_controller_preferences", 0).edit().putBoolean("switched_from_gesture_nav", false).apply();
        }
    }

    private Boolean isGestureNavSupportedByDefaultLauncher(Context context) {
        String defaultLauncherPackageName = getDefaultLauncherPackageName(context);
        if (defaultLauncherPackageName != null && isSystemApp(context, defaultLauncherPackageName)) {
            return Boolean.valueOf(ComponentName.unflattenFromString(context.getString(17039804)).getPackageName().equals(defaultLauncherPackageName));
        }
        return false;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private String getDefaultLauncherPackageName(Context context) {
        ComponentName homeActivities = context.getPackageManager().getHomeActivities(new ArrayList());
        if (homeActivities == null) {
            return null;
        }
        return homeActivities.getPackageName();
    }

    private boolean isSystemApp(Context context, String str) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(str, 128);
            if (applicationInfo == null || (applicationInfo.flags & 129) == 0) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    private void showNotification(Context context, int i) {
        ((NotificationManager) context.getSystemService(NotificationManager.class)).notify(TAG, 0, new Notification.Builder(this.mContext, NotificationChannels.ALERTS).setContentText(context.getResources().getString(i)).setStyle(new Notification.BigTextStyle()).setSmallIcon(C0006R$drawable.ic_info).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).build());
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        printWriter.println("NavigationModeController:");
        printWriter.println("  mode=" + this.mMode);
        try {
            str = String.join(", ", this.mOverlayManager.getDefaultOverlayPackages());
        } catch (RemoteException unused) {
            str = "failed_to_fetch";
        }
        printWriter.println("  defaultOverlays=" + str);
        dumpAssetPaths(this.mCurrentUserContext);
        printWriter.println("  defaultLauncher=" + this.mLastDefaultLauncher);
        boolean z = this.mCurrentUserContext.getSharedPreferences("navigation_mode_controller_preferences", 0).getBoolean("switched_from_gesture_nav", false);
        printWriter.println("  previouslySwitchedFromGestureNav=" + z);
    }

    private void dumpAssetPaths(Context context) {
        Log.d(TAG, "assetPaths=");
        ApkAssets[] apkAssets = context.getResources().getAssets().getApkAssets();
        for (ApkAssets apkAssets2 : apkAssets) {
            Log.d(TAG, "    " + apkAssets2.getAssetPath());
        }
    }
}
