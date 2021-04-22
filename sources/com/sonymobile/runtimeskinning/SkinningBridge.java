package com.sonymobile.runtimeskinning;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.View;
import com.android.internal.annotations.GuardedBy;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.C0001R$array;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0014R$string;
import com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint;
import java.util.HashMap;
import java.util.Map;

public class SkinningBridge {
    private static final SparseArray<String> BAR_TRANSITIONS_TO_NAME = new SparseArray<String>(7) {
        /* class com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass1 */

        {
            put(3, "lightsOut");
            put(6, "lightsOutTransparent");
            put(0, "opaque");
            put(1, "semiTransparent");
            put(2, "translucent");
            put(4, "transparent");
            put(5, "warning");
        }
    };
    private static final Object LOCK = new Object();
    private static final SparseArray<String> ORIENTATION_TO_NAME = new SparseArray<String>(4) {
        /* class com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass2 */

        {
            put(0, "portrait");
            put(1, "landscape");
            put(2, "inversePortrait");
            put(3, "seascape");
        }
    };
    private static final String TAG = "SkinningBridge";
    private static final SparseArray<String> WINDOW_STATE_TO_NAME = new SparseArray<String>(3) {
        /* class com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass3 */

        {
            put(0, "showing");
            put(1, "hiding");
            put(2, "hidden");
        }
    };
    private static String sActiveProtocolVersion;
    private static final SparseArray<String> sButtonMappings = new SparseArray<>();
    @GuardedBy({"LOCK"})
    private static Size sClockPickerDimens = new Size(0, 0);
    private static ServiceConnection sConnection;
    private static ComponentName sCurrentServiceComponent;
    private static Size sDefaultClockPickerDimens = new Size(0, 0);
    private static boolean sIsStatusBarCloaked;
    private static final KeyguardUpdateMonitorCallback sKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass5 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardVisibilityChanged(boolean z) {
            SkinningBridge.transferKeyguardVisibilityChanged(z);
        }
    };
    private static String sLastNavBarWindowState = "shown";
    private static String sLastStatusBarWindowState = "shown";
    private static View.OnLayoutChangeListener sLayoutListener = $$Lambda$SkinningBridge$I7g5usEQzcMyXgEqi9heM4YWw.INSTANCE;
    private static Runnable sLightBarControllerRefresher;
    private static final int[] sLocationBuffer = new int[2];
    private static View sNavbarView;
    private static ISkinningBridgeEndpoint sRemoteEndpoint;
    private static final BroadcastReceiver sSkinnableReceiver = new BroadcastReceiver() {
        /* class com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass4 */

        public void onReceive(Context context, Intent intent) {
            UserHandle userHandle = (UserHandle) intent.getParcelableExtra("com.sonymobile.runtimeskinning.intent.extra.CALLING_USER");
            if (userHandle != null && userHandle.getIdentifier() == SkinningBridge.sUserId) {
                String action = intent.getAction();
                char c = 65535;
                int hashCode = action.hashCode();
                if (hashCode != -266099614) {
                    if (hashCode == 1491547619 && action.equals("com.sonymobile.runtimeskinning.intent.action.DISABLE_BRIDGE")) {
                        c = 1;
                    }
                } else if (action.equals("com.sonymobile.runtimeskinning.intent.action.ENABLE_BRIDGE")) {
                    c = 0;
                }
                if (c == 0) {
                    SkinningBridge.setLockscreenClockAvailable(false, false);
                    ComponentName unflattenFromString = ComponentName.unflattenFromString(context.getResources().getString(C0014R$string.skinning_bridge_endpoint));
                    if (SkinningBridge.sCurrentServiceComponent != null && !unflattenFromString.equals(SkinningBridge.sCurrentServiceComponent)) {
                        SkinningBridge.disconnect();
                    }
                    if (SkinningBridge.sCurrentServiceComponent == null) {
                        ComponentName unused = SkinningBridge.sCurrentServiceComponent = unflattenFromString;
                        SkinningBridge.connect(SkinningBridge.sCurrentServiceComponent, intent.getExtras());
                    }
                } else if (c == 1 && SkinningBridge.sConnection != null) {
                    SkinningBridge.disconnect();
                }
            }
        }
    };
    private static Drawable sStatusBarBackground;
    private static String sStatusBarBackgroundColor = "auto";
    private static View sStatusBarView;
    private static String sThemeableClockFactoryClass;
    private static final Map<String, Map<String, Object>> sTransferHistory = new HashMap();
    private static int sUserId = -10000;

    static /* synthetic */ void lambda$static$0(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        view.getLocationOnScreen(sLocationBuffer);
        int[] iArr = sLocationBuffer;
        handleNavigationBarPositionChanged(iArr[0], iArr[1]);
    }

    /* access modifiers changed from: private */
    public static boolean connect(ComponentName componentName, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.setComponent(componentName);
        intent.setFlags(268435456);
        sConnection = new ServiceConnection() {
            /* class com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass6 */

            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                ISkinningBridgeEndpoint unused = SkinningBridge.sRemoteEndpoint = ISkinningBridgeEndpoint.Stub.asInterface(iBinder);
                try {
                    if (SkinningBridge.sRemoteEndpoint.useVersion("3")) {
                        String unused2 = SkinningBridge.sActiveProtocolVersion = "3";
                        SkinningBridge.sRemoteEndpoint.registerEndpoint(new ISkinningBridgeEndpoint.Stub() {
                            /* class com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass6.AnonymousClass1 */

                            @Override // com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint
                            public void registerEndpoint(ISkinningBridgeEndpoint iSkinningBridgeEndpoint) throws RemoteException {
                            }

                            @Override // com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint
                            public boolean useVersion(String str) throws RemoteException {
                                return SkinningBridge.sActiveProtocolVersion.equals(str);
                            }

                            /* JADX WARNING: Code restructure failed: missing block: B:45:0x00e3, code lost:
                                if (com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginConstants$ClockSelectionSource.LockscreenSettings.name().equals(r8) == false) goto L_0x00e5;
                             */
                            /* JADX WARNING: Removed duplicated region for block: B:21:0x004c  */
                            /* JADX WARNING: Removed duplicated region for block: B:52:0x010a  */
                            @Override // com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint
                            /* Code decompiled incorrectly, please refer to instructions dump. */
                            public void transfer(java.util.Map r9) throws android.os.RemoteException {
                                /*
                                // Method dump skipped, instructions count: 287
                                */
                                throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.runtimeskinning.SkinningBridge.AnonymousClass6.AnonymousClass1.transfer(java.util.Map):void");
                            }

                            static /* synthetic */ void lambda$transfer$0(boolean z) {
                                SkinningBridge.sNavbarView.setAlpha(z ? 0.0f : 1.0f);
                            }
                        });
                        for (Map map : SkinningBridge.sTransferHistory.values()) {
                            SkinningBridge.sRemoteEndpoint.transfer(map);
                        }
                        return;
                    }
                    Log.w(SkinningBridge.TAG, "Unsupported protocol version");
                    SkinningBridge.disconnect();
                } catch (Throwable th) {
                    Log.e(SkinningBridge.TAG, "Error while communicating with skinning bridge endpoint application", th);
                }
            }

            public void onServiceDisconnected(ComponentName componentName) {
                SkinningBridge.disconnect();
                SkinningBridge.reestablishBridge();
            }
        };
        boolean bindServiceAsUser = sNavbarView.getContext().bindServiceAsUser(intent, sConnection, 65, UserHandle.CURRENT);
        sCurrentServiceComponent = componentName;
        if (!bindServiceAsUser) {
            sConnection = null;
        }
        return bindServiceAsUser;
    }

    /* access modifiers changed from: private */
    public static void reestablishBridge() {
        Intent intent = new Intent("com.sonymobile.runtimeskinning.intent.action.REINIT_BRIDGE");
        intent.setFlags(268435456);
        intent.setPackage(ComponentName.unflattenFromString(sNavbarView.getResources().getString(C0014R$string.skinning_bridge_endpoint)).getPackageName());
        sNavbarView.getContext().sendBroadcast(intent, "com.sonymobile.runtimeskinning.permission.RECEIVE_REINIT_BRIDGE");
    }

    /* access modifiers changed from: private */
    public static void disconnect() {
        setLockscreenClockAvailable(false, false);
        sNavbarView.getContext().unbindService(sConnection);
        setStatusbarCloaked(false);
        sNavbarView.setAlpha(1.0f);
        sRemoteEndpoint = null;
        sConnection = null;
        sCurrentServiceComponent = null;
    }

    public static void init(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        if (sNavbarView == null) {
            sButtonMappings.put(i, "back");
            sButtonMappings.put(i2, "home");
            sButtonMappings.put(i3, "home");
            sButtonMappings.put(i4, "recent");
            sButtonMappings.put(i5, "menu");
            sButtonMappings.put(i6, "imeSwitch");
            sButtonMappings.put(i7, "accessibility");
            sButtonMappings.put(i8, "rotateSuggestion");
            sNavbarView = view;
            sNavbarView.addOnLayoutChangeListener(sLayoutListener);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.sonymobile.runtimeskinning.intent.action.ENABLE_BRIDGE");
            intentFilter.addAction("com.sonymobile.runtimeskinning.intent.action.DISABLE_BRIDGE");
            sNavbarView.getContext().registerReceiverAsUser(sSkinnableReceiver, UserHandle.ALL, intentFilter, "com.sonymobile.runtimeskinning.permission.MANAGE_BRIDGE_STATE", null);
            sThemeableClockFactoryClass = view.getResources().getStringArray(C0001R$array.themeable_analog_clock_plugin_factory)[1];
            KeyguardUpdateMonitor.getInstance(sNavbarView.getContext()).registerCallback(sKeyguardUpdateMonitorCallback);
            int dimension = (int) view.getResources().getDimension(C0005R$dimen.somc_themeable_analog_clock_max_width);
            int dimension2 = (int) view.getResources().getDimension(C0005R$dimen.somc_themeable_analog_clock_max_height);
            sDefaultClockPickerDimens = new Size(dimension, dimension2);
            setPreferredLockScreenClockDimens(dimension, dimension2);
            return;
        }
        throw new IllegalStateException("init() may only be called once");
    }

    public static void onStatusBarCreated(View view) {
        synchronized (LOCK) {
            sStatusBarView = view;
            sStatusBarBackground = sStatusBarView.getBackground();
            setStatusbarCloaked(sIsStatusBarCloaked);
        }
    }

    public static void onLightBarControllerCreated(Runnable runnable) {
        sLightBarControllerRefresher = runnable;
    }

    private static void sendBroadcast(Context context, Intent intent) {
        intent.setFlags(268435456);
        String[] stringArray = context.getResources().getStringArray(C0001R$array.skinning_bridge_intent_receivers);
        int length = stringArray.length;
        for (int i = 0; i < length; i++) {
            String str = stringArray[i];
            ComponentName unflattenFromString = ComponentName.unflattenFromString(str);
            if (unflattenFromString != null) {
                str = unflattenFromString.getPackageName();
            }
            try {
                context.getPackageManager().getPackageInfo(str, 1048576);
                intent.setPackage(str);
                context.sendBroadcastAsUser(intent, new UserHandle(sUserId), "com.sonymobile.runtimeskinning.permission.RECEIVE_FORWARDED_INTENTS");
            } catch (PackageManager.NameNotFoundException unused) {
            }
        }
    }

    public static void onUserSwitched(Context context, int i) {
        View view;
        if (sUserId != -10000 || (view = sNavbarView) == null) {
            setLockscreenClockAvailable(false, false);
        } else {
            view.post($$Lambda$SkinningBridge$0FP5F4MjcqamRew3m1532UGeXfs.INSTANCE);
        }
        sUserId = i;
        sendBroadcast(context, new Intent("com.sonymobile.runtimeskinning.intent.action.USER_SWITCHED"));
        if (sConnection != null) {
            disconnect();
        }
    }

    public static void onStatusBarModeChanged(int i, boolean z) {
        String str = BAR_TRANSITIONS_TO_NAME.get(i);
        if (str != null) {
            HashMap hashMap = new HashMap(3);
            hashMap.put("event", "statusBarModeChanged");
            hashMap.put("mode", str);
            hashMap.put("color", z ? "light" : "dark");
            transferToRemote(hashMap, "statusBarModeChanged");
        }
    }

    public static void onNavigationBarModeChanged(int i, boolean z) {
        String str = BAR_TRANSITIONS_TO_NAME.get(i);
        if (str != null) {
            HashMap hashMap = new HashMap(3);
            hashMap.put("event", "navigationBarModeChanged");
            hashMap.put("mode", str);
            hashMap.put("color", z ? "light" : "dark");
            transferToRemote(hashMap, "navigationBarModeChanged");
        }
    }

    public static void onButtonVariantChanged(int i, String str) {
        String str2;
        String str3 = sButtonMappings.get(i);
        if (str3 != null) {
            HashMap hashMap = new HashMap(3);
            hashMap.put("event", "buttonVariantChanged");
            hashMap.put("button", str3);
            hashMap.put("variant", str);
            if (str3.equals("rotateSuggestion")) {
                str2 = null;
            } else {
                str2 = "buttonVariantChanged:" + str3;
            }
            transferToRemote(hashMap, str2);
        }
    }

    public static void onButtonVisibilityChanged(int i, String str) {
        String str2 = sButtonMappings.get(i);
        if (str2 != null) {
            HashMap hashMap = new HashMap(3);
            hashMap.put("event", "buttonVisibilityChanged");
            hashMap.put("button", str2);
            hashMap.put("visible", str);
            transferToRemote(hashMap, "buttonVisibilityChanged:" + str2);
        }
    }

    public static void onButtonStateChanged(int i, String str) {
        String str2 = sButtonMappings.get(i);
        if (str2 != null) {
            HashMap hashMap = new HashMap(3);
            hashMap.put("event", "buttonStateChanged");
            hashMap.put("button", str2);
            hashMap.put("state", str);
            transferToRemote(hashMap, "buttonStateChanged:" + str2);
        }
    }

    public static void onButtonClicked(int i) {
        String str = sButtonMappings.get(i);
        if (str != null) {
            HashMap hashMap = new HashMap(2);
            hashMap.put("event", "buttonClicked");
            hashMap.put("button", str);
            transferToRemote(hashMap, null);
        }
    }

    public static void onNavigationBarVisibilityChanged(boolean z) {
        HashMap hashMap = new HashMap(2);
        hashMap.put("event", "navigationBarVisibilityChanged");
        hashMap.put("visible", String.valueOf(z));
        transferToRemote(hashMap, "navigationBarVisibilityChanged");
    }

    public static void onSystemUiVisibilityChanged(int i) {
        if ((i & 2) == 0 && !sLastNavBarWindowState.equals("hiding") && !sLastNavBarWindowState.equals("hidden")) {
            transferNavigationBarWindowStateChanged("shown");
        }
        if ((i & 4) == 0 && !sLastStatusBarWindowState.equals("hiding") && !sLastStatusBarWindowState.equals("hidden")) {
            transferStatusBarWindowStateChanged("shown");
        }
    }

    public static void onDockedStackVisibilityChanged(boolean z) {
        HashMap hashMap = new HashMap(2);
        hashMap.put("event", "splitScreenChanged");
        hashMap.put("split", String.valueOf(z));
        transferToRemote(hashMap, "splitScreenChanged");
    }

    public static void onWindowStateChanged(int i, int i2) {
        if (i == 1) {
            String str = WINDOW_STATE_TO_NAME.get(i2);
            if (sStatusBarView != null && str != null) {
                transferStatusBarWindowStateChanged(str);
            }
        } else if (i == 2) {
            String str2 = WINDOW_STATE_TO_NAME.get(i2);
            if (sNavbarView != null && str2 != null) {
                transferNavigationBarWindowStateChanged(str2);
            }
        }
    }

    public static boolean massageLightBarMode(boolean z, int i) {
        if (i != 8192) {
            return z;
        }
        String str = sStatusBarBackgroundColor;
        char c = 65535;
        int hashCode = str.hashCode();
        if (hashCode != 3005871) {
            if (hashCode != 3075958) {
                if (hashCode == 102970646 && str.equals("light")) {
                    c = 1;
                }
            } else if (str.equals("dark")) {
                c = 0;
            }
        } else if (str.equals("auto")) {
            c = 2;
        }
        if (c == 0) {
            return false;
        }
        if (c != 1) {
            return z;
        }
        return true;
    }

    public static int massageSystemUiVisibility(int i) {
        transferFocusedWindowTypeChanged((i & 16384) == 16384 ? "baseApp" : "unknown");
        return i & -16385;
    }

    public static void onSurfaceAvailable(SurfaceTexture surfaceTexture) {
        HashMap hashMap = new HashMap(3);
        hashMap.put("event", "surfaceAvailable");
        hashMap.put("id", Integer.valueOf(System.identityHashCode(surfaceTexture)));
        hashMap.put("surface", new Surface(surfaceTexture));
        transferToRemote(hashMap, "surfaceAvailable");
    }

    public static void onSurfaceChanged(SurfaceTexture surfaceTexture, String str, int i, int i2, int i3, int i4) {
        HashMap hashMap = new HashMap(4);
        hashMap.put("event", "surfaceChanged");
        hashMap.put("id", Integer.valueOf(System.identityHashCode(surfaceTexture)));
        hashMap.put("place", str);
        hashMap.put("region", new Rect(i, i2, i3, i4));
        transferToRemote(hashMap, "surfaceChanged");
    }

    public static void onSurfaceDestroyed(SurfaceTexture surfaceTexture) {
        HashMap hashMap = new HashMap(2);
        hashMap.put("event", "surfaceDestroyed");
        hashMap.put("id", Integer.valueOf(System.identityHashCode(surfaceTexture)));
        transferToRemote(hashMap, "surfaceDestroyed");
    }

    public static Size getLockScreenClockDimens() {
        Size size;
        synchronized (LOCK) {
            size = sClockPickerDimens;
        }
        return size;
    }

    /* access modifiers changed from: private */
    public static void transferKeyguardVisibilityChanged(boolean z) {
        HashMap hashMap = new HashMap(2);
        hashMap.put("event", "keyguardVisibilityChanged");
        hashMap.put("visible", String.valueOf(z));
        transferToRemote(hashMap, "keyguardVisibilityChanged");
    }

    private static void transferStatusBarWindowStateChanged(String str) {
        sLastStatusBarWindowState = str;
        HashMap hashMap = new HashMap(2);
        hashMap.put("event", "statusBarWindowStateChanged");
        hashMap.put("state", str);
        transferToRemote(hashMap, "statusBarWindowStateChanged");
    }

    private static void transferNavigationBarWindowStateChanged(String str) {
        sLastNavBarWindowState = str;
        HashMap hashMap = new HashMap(2);
        hashMap.put("event", "navigationBarWindowStateChanged");
        hashMap.put("state", str);
        transferToRemote(hashMap, "navigationBarWindowStateChanged");
    }

    private static void transferFocusedWindowTypeChanged(String str) {
        if (str != null) {
            HashMap hashMap = new HashMap(2);
            hashMap.put("event", "focusedWindowTypeChanged");
            hashMap.put("type", str);
            transferToRemote(hashMap, "focusedWindowTypeChanged");
        }
    }

    private static void handleNavigationBarPositionChanged(int i, int i2) {
        String str = i != 0 ? "right" : i2 == 0 ? "left" : "bottom";
        HashMap hashMap = new HashMap(2);
        hashMap.put("event", "positionChanged");
        hashMap.put("gravity", str);
        transferToRemote(hashMap, "positionChanged");
    }

    private static void transferToRemote(Map<String, Object> map, String str) {
        Map<String, Object> map2;
        if (str != null) {
            map2 = sTransferHistory.get(str);
            sTransferHistory.put(str, map);
        } else {
            map2 = null;
        }
        if (sRemoteEndpoint != null && !map.equals(map2)) {
            try {
                sRemoteEndpoint.transfer(map);
            } catch (Throwable th) {
                Log.e(TAG, "Could not transfer data to skinning bridge endpoint application", th);
            }
        }
    }

    /* access modifiers changed from: private */
    public static void setStatusbarCloaked(boolean z) {
        synchronized (LOCK) {
            sIsStatusBarCloaked = z;
            if (sStatusBarView != null) {
                sStatusBarView.post(new Runnable(z) {
                    /* class com.sonymobile.runtimeskinning.$$Lambda$SkinningBridge$SrdZjh6bHkUZO_QvzP4Oulmk4U */
                    private final /* synthetic */ boolean f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final void run() {
                        SkinningBridge.lambda$setStatusbarCloaked$2(this.f$0);
                    }
                });
            }
        }
    }

    static /* synthetic */ void lambda$setStatusbarCloaked$2(boolean z) {
        synchronized (LOCK) {
            if (z) {
                sStatusBarView.setBackgroundResource(17170445);
            } else {
                sStatusBarView.setBackground(sStatusBarBackground);
                sStatusBarBackgroundColor = "auto";
            }
            if (sLightBarControllerRefresher != null) {
                sLightBarControllerRefresher.run();
            }
        }
    }

    /* access modifiers changed from: private */
    public static void setLockscreenClockAvailable(boolean z, boolean z2) {
        if (sUserId != -10000) {
            Settings.Secure.putIntForUser(sNavbarView.getContext().getContentResolver(), "com.sonymobile.runtimeskinning.lockscreen.clock.available", z ? 1 : 0, sUserId);
            if (z && z2) {
                Settings.Secure.putStringForUser(sNavbarView.getContext().getContentResolver(), "somc.lockscreen.active.clock_factory", sThemeableClockFactoryClass, sUserId);
            } else if (!z) {
                String settingsString = getSettingsString("somc.lockscreen.active.clock_factory");
                if (sThemeableClockFactoryClass.equals(settingsString)) {
                    Settings.Secure.putStringForUser(sNavbarView.getContext().getContentResolver(), "somc.lockscreen.active.clock_factory", null, sUserId);
                    Settings.Secure.putStringForUser(sNavbarView.getContext().getContentResolver(), "somc.lockscreen.active.clock_factory", settingsString, sUserId);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public static String getSettingsString(String str) {
        if (sUserId != -10000) {
            return Settings.Secure.getStringForUser(sNavbarView.getContext().getContentResolver(), str, sUserId);
        }
        return null;
    }

    /* access modifiers changed from: private */
    public static void setPreferredLockScreenClockDimens(int i, int i2) {
        synchronized (LOCK) {
            if (i <= 0) {
                i = sDefaultClockPickerDimens.getWidth();
            }
            if (i2 <= 0) {
                i2 = sDefaultClockPickerDimens.getHeight();
            }
            sClockPickerDimens = new Size(i, i2);
        }
    }
}
