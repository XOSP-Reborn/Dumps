package com.android.systemui;

import android.app.ActivityThread;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.TimingsTraceLog;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.plugins.OverlayPlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.util.NotificationChannels;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SystemUIApplication extends Application implements SysUiServiceProvider {
    private boolean mBootCompleted;
    private final Map<Class<?>, Object> mComponents = new HashMap();
    private SystemUI[] mServices;
    private boolean mServicesStarted;

    public void onCreate() {
        super.onCreate();
        setTheme(C0015R$style.Theme_SystemUI);
        SystemUIFactory.createFromConfig(this);
        if (Process.myUserHandle().equals(UserHandle.SYSTEM)) {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            intentFilter.setPriority(1000);
            registerReceiver(new BroadcastReceiver() {
                /* class com.android.systemui.SystemUIApplication.AnonymousClass1 */

                public void onReceive(Context context, Intent intent) {
                    if (!SystemUIApplication.this.mBootCompleted) {
                        SystemUIApplication.this.unregisterReceiver(this);
                        SystemUIApplication.this.mBootCompleted = true;
                        if (SystemUIApplication.this.mServicesStarted) {
                            int length = SystemUIApplication.this.mServices.length;
                            for (int i = 0; i < length; i++) {
                                SystemUIApplication.this.mServices[i].onBootCompleted();
                            }
                        }
                    }
                }
            }, intentFilter);
            registerReceiver(new BroadcastReceiver() {
                /* class com.android.systemui.SystemUIApplication.AnonymousClass2 */

                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction()) && SystemUIApplication.this.mBootCompleted) {
                        NotificationChannels.createAll(context);
                    }
                }
            }, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
            return;
        }
        String currentProcessName = ActivityThread.currentProcessName();
        ApplicationInfo applicationInfo = getApplicationInfo();
        if (currentProcessName != null) {
            if (currentProcessName.startsWith(applicationInfo.processName + ":")) {
                return;
            }
        }
        startSecondaryUserServicesIfNeeded();
    }

    public void startServicesIfNeeded() {
        startServicesIfNeeded(getResources().getStringArray(C0001R$array.config_systemUIServiceComponents));
    }

    /* access modifiers changed from: package-private */
    public void startSecondaryUserServicesIfNeeded() {
        startServicesIfNeeded(getResources().getStringArray(C0001R$array.config_systemUIServiceComponentsPerUser));
    }

    private void startServicesIfNeeded(String[] strArr) {
        if (!this.mServicesStarted) {
            this.mServices = new SystemUI[strArr.length];
            if (!this.mBootCompleted && "1".equals(SystemProperties.get("sys.boot_completed"))) {
                this.mBootCompleted = true;
            }
            Log.v("SystemUIService", "Starting SystemUI services for user " + Process.myUserHandle().getIdentifier() + ".");
            TimingsTraceLog timingsTraceLog = new TimingsTraceLog("SystemUIBootTiming", 4096);
            timingsTraceLog.traceBegin("StartServices");
            int length = strArr.length;
            for (int i = 0; i < length; i++) {
                String str = strArr[i];
                timingsTraceLog.traceBegin("StartServices" + str);
                long currentTimeMillis = System.currentTimeMillis();
                try {
                    Class<?> cls = Class.forName(str);
                    Object newInstance = cls.newInstance();
                    if (newInstance instanceof SystemUI.Injector) {
                        newInstance = ((SystemUI.Injector) newInstance).apply(this);
                    }
                    this.mServices[i] = (SystemUI) newInstance;
                    SystemUI[] systemUIArr = this.mServices;
                    systemUIArr[i].mContext = this;
                    systemUIArr[i].mComponents = this.mComponents;
                    systemUIArr[i].start();
                    timingsTraceLog.traceEnd();
                    long currentTimeMillis2 = System.currentTimeMillis() - currentTimeMillis;
                    if (currentTimeMillis2 > 1000) {
                        Log.w("SystemUIService", "Initialization of " + cls.getName() + " took " + currentTimeMillis2 + " ms");
                    }
                    if (this.mBootCompleted) {
                        this.mServices[i].onBootCompleted();
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e2) {
                    throw new RuntimeException(e2);
                } catch (InstantiationException e3) {
                    throw new RuntimeException(e3);
                }
            }
            ((InitController) Dependency.get(InitController.class)).executePostInitTasks();
            timingsTraceLog.traceEnd();
            final Handler handler = new Handler(Looper.getMainLooper());
            ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener) new PluginListener<OverlayPlugin>() {
                /* class com.android.systemui.SystemUIApplication.AnonymousClass3 */
                private ArraySet<OverlayPlugin> mOverlays = new ArraySet<>();

                public void onPluginConnected(final OverlayPlugin overlayPlugin, Context context) {
                    handler.post(new Runnable() {
                        /* class com.android.systemui.SystemUIApplication.AnonymousClass3.AnonymousClass1 */

                        public void run() {
                            StatusBar statusBar = (StatusBar) SystemUIApplication.this.getComponent(StatusBar.class);
                            if (statusBar != null) {
                                overlayPlugin.setup(statusBar.getStatusBarWindow(), statusBar.getNavigationBarView(), new Callback(overlayPlugin));
                            }
                        }
                    });
                }

                public void onPluginDisconnected(final OverlayPlugin overlayPlugin) {
                    handler.post(new Runnable() {
                        /* class com.android.systemui.SystemUIApplication.AnonymousClass3.AnonymousClass2 */

                        public void run() {
                            AnonymousClass3.this.mOverlays.remove(overlayPlugin);
                            ((StatusBarWindowController) Dependency.get(StatusBarWindowController.class)).setForcePluginOpen(AnonymousClass3.this.mOverlays.size() != 0);
                        }
                    });
                }

                /* access modifiers changed from: package-private */
                /* renamed from: com.android.systemui.SystemUIApplication$3$Callback */
                public class Callback implements OverlayPlugin.Callback {
                    private final OverlayPlugin mPlugin;

                    Callback(OverlayPlugin overlayPlugin) {
                        this.mPlugin = overlayPlugin;
                    }

                    @Override // com.android.systemui.plugins.OverlayPlugin.Callback
                    public void onHoldStatusBarOpenChange() {
                        if (this.mPlugin.holdStatusBarOpen()) {
                            AnonymousClass3.this.mOverlays.add(this.mPlugin);
                        } else {
                            AnonymousClass3.this.mOverlays.remove(this.mPlugin);
                        }
                        handler.post(new Runnable() {
                            /* class com.android.systemui.SystemUIApplication.AnonymousClass3.Callback.AnonymousClass1 */

                            public void run() {
                                ((StatusBarWindowController) Dependency.get(StatusBarWindowController.class)).setStateListener(new StatusBarWindowController.OtherwisedCollapsedListener() {
                                    /* class com.android.systemui.$$Lambda$SystemUIApplication$3$Callback$1$sx3y3YDR9PfTcBFpqL5skj6JDUg */

                                    @Override // com.android.systemui.statusbar.phone.StatusBarWindowController.OtherwisedCollapsedListener
                                    public final void setWouldOtherwiseCollapse(boolean z) {
                                        SystemUIApplication.AnonymousClass3.Callback.AnonymousClass1.this.lambda$run$1$SystemUIApplication$3$Callback$1(z);
                                    }
                                });
                                ((StatusBarWindowController) Dependency.get(StatusBarWindowController.class)).setForcePluginOpen(AnonymousClass3.this.mOverlays.size() != 0);
                            }

                            public /* synthetic */ void lambda$run$1$SystemUIApplication$3$Callback$1(boolean z) {
                                AnonymousClass3.this.mOverlays.forEach(new Consumer(z) {
                                    /* class com.android.systemui.$$Lambda$SystemUIApplication$3$Callback$1$BwolTXxR8lk33KXtnn_kk1xKxjQ */
                                    private final /* synthetic */ boolean f$0;

                                    {
                                        this.f$0 = r1;
                                    }

                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        SystemUIApplication.AnonymousClass3.Callback.AnonymousClass1.lambda$run$0(this.f$0, (OverlayPlugin) obj);
                                    }
                                });
                            }
                        });
                    }
                }
            }, OverlayPlugin.class, true);
            this.mServicesStarted = true;
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        if (this.mServicesStarted) {
            int length = this.mServices.length;
            for (int i = 0; i < length; i++) {
                SystemUI[] systemUIArr = this.mServices;
                if (systemUIArr[i] != null) {
                    systemUIArr[i].onConfigurationChanged(configuration);
                }
            }
        }
    }

    @Override // com.android.systemui.SysUiServiceProvider
    public <T> T getComponent(Class<T> cls) {
        return (T) this.mComponents.get(cls);
    }

    public SystemUI[] getServices() {
        return this.mServices;
    }
}
