package com.android.systemui.recents;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0014R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.pip.PipUI;
import com.android.systemui.recents.IRecentsSystemUserCallbacks;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.component.ExpandPipEvent;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
import com.android.systemui.recents.events.component.ShowUserToastEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.events.ui.RecentsGrowingEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LegacyRecentsImpl implements RecentsImplementation {
    public static final Set<String> RECENTS_ACTIVITIES = new HashSet();
    private static RecentsConfiguration sConfiguration;
    private static RecentsDebugFlags sDebugFlags;
    private static SystemServicesProxy sSystemServicesProxy;
    private static RecentsTaskLoader sTaskLoader;
    private Context mContext;
    private Handler mHandler;
    private RecentsImpl mImpl;
    private final ArrayList<Runnable> mOnConnectRunnables = new ArrayList<>();
    private SysUiServiceProvider mSysUiServiceProvider;
    private RecentsSystemUser mSystemToUserCallbacks;
    private IRecentsSystemUserCallbacks mUserToSystemCallbacks;
    private final IBinder.DeathRecipient mUserToSystemCallbacksDeathRcpt = new IBinder.DeathRecipient() {
        /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass1 */

        public void binderDied() {
            LegacyRecentsImpl.this.mUserToSystemCallbacks = null;
            EventLog.writeEvent(36060, 3, Integer.valueOf(LegacyRecentsImpl.sSystemServicesProxy.getProcessUser()));
            LegacyRecentsImpl.this.mHandler.postDelayed(new Runnable() {
                /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass1.AnonymousClass1 */

                public void run() {
                    LegacyRecentsImpl.this.registerWithSystemUser();
                }
            }, 5000);
        }
    };
    private final ServiceConnection mUserToSystemServiceConnection = new ServiceConnection() {
        /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass2 */

        public void onServiceDisconnected(ComponentName componentName) {
        }

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder != null) {
                LegacyRecentsImpl.this.mUserToSystemCallbacks = IRecentsSystemUserCallbacks.Stub.asInterface(iBinder);
                EventLog.writeEvent(36060, 2, Integer.valueOf(LegacyRecentsImpl.sSystemServicesProxy.getProcessUser()));
                try {
                    iBinder.linkToDeath(LegacyRecentsImpl.this.mUserToSystemCallbacksDeathRcpt, 0);
                } catch (RemoteException e) {
                    Log.e("Recents", "Lost connection to (System) SystemUI", e);
                }
                LegacyRecentsImpl.this.runAndFlushOnConnectRunnables();
            }
            LegacyRecentsImpl.this.mContext.unbindService(this);
        }
    };

    private static String getMetricsCounterForResizeMode(int i) {
        return (i == 1 || i == 2) ? "window_enter_supported" : i != 4 ? "window_enter_incompatible" : "window_enter_unsupported";
    }

    static {
        RECENTS_ACTIVITIES.add("com.android.systemui.recents.RecentsActivity");
    }

    public IBinder getSystemUserCallbacks() {
        return this.mSystemToUserCallbacks;
    }

    public static RecentsTaskLoader getTaskLoader() {
        return sTaskLoader;
    }

    public static SystemServicesProxy getSystemServices() {
        return sSystemServicesProxy;
    }

    public static RecentsConfiguration getConfiguration() {
        return sConfiguration;
    }

    public static RecentsDebugFlags getDebugFlags() {
        return sDebugFlags;
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void onStart(Context context, SysUiServiceProvider sysUiServiceProvider) {
        this.mContext = context;
        this.mSysUiServiceProvider = sysUiServiceProvider;
        Resources resources = this.mContext.getResources();
        int color = this.mContext.getColor(2131100040);
        int color2 = this.mContext.getColor(2131100044);
        ((Recents) getComponent(Recents.class)).putComponent(LegacyRecentsImpl.class, this);
        sDebugFlags = new RecentsDebugFlags();
        sSystemServicesProxy = SystemServicesProxy.getInstance(this.mContext);
        sConfiguration = new RecentsConfiguration(this.mContext);
        sTaskLoader = new RecentsTaskLoader(this.mContext, resources.getInteger(2131427350), resources.getInteger(2131427349), resources.getInteger(2131427448));
        sTaskLoader.setDefaultColors(color, color2);
        this.mHandler = new Handler();
        this.mImpl = new RecentsImpl(this.mContext);
        EventBus.getDefault().register(this, 1);
        EventBus.getDefault().register(sSystemServicesProxy, 1);
        EventBus.getDefault().register(sTaskLoader, 1);
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            this.mSystemToUserCallbacks = new RecentsSystemUser(this.mContext, this.mImpl);
        } else {
            registerWithSystemUser();
        }
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void onBootCompleted() {
        this.mImpl.onBootCompleted();
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void growRecents() {
        EventBus.getDefault().send(new RecentsGrowingEvent());
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void showRecentApps(boolean z) {
        ActivityManagerWrapper.getInstance().closeSystemWindows("recentapps");
        int growsRecents = ((Divider) getComponent(Divider.class)).getView().growsRecents();
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.showRecents(z, false, true, growsRecents);
            return;
        }
        RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
        if (recentsSystemUser != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.showRecents(z, false, true, growsRecents);
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void hideRecentApps(boolean z, boolean z2) {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.hideRecents(z, z2);
            return;
        }
        RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
        if (recentsSystemUser != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.hideRecents(z, z2);
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void toggleRecentApps() {
        int growsRecents = ((Divider) getComponent(Divider.class)).getView().growsRecents();
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.toggleRecents(growsRecents);
            return;
        }
        RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
        if (recentsSystemUser != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.toggleRecents(growsRecents);
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void preloadRecentApps() {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.preloadRecents();
            return;
        }
        RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
        if (recentsSystemUser != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.preloadRecents();
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void cancelPreloadRecentApps() {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.cancelPreloadingRecents();
            return;
        }
        RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
        if (recentsSystemUser != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.cancelPreloadingRecents();
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public boolean splitPrimaryTask(int i, Rect rect, int i2) {
        Point point = new Point();
        if (rect == null) {
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(0).getRealSize(point);
            rect = new Rect(0, 0, point.x, point.y);
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
        int activityType = runningTask != null ? runningTask.configuration.windowConfiguration.getActivityType() : 0;
        boolean isScreenPinningActive = ActivityManagerWrapper.getInstance().isScreenPinningActive();
        boolean z = activityType == 2 || activityType == 3;
        if (runningTask != null && !z && !isScreenPinningActive) {
            logDockAttempt(this.mContext, runningTask.topActivity, runningTask.resizeMode);
            if (runningTask.supportsSplitScreenMultiWindow) {
                if (i2 != -1) {
                    MetricsLogger.action(this.mContext, i2, runningTask.topActivity.flattenToShortString());
                }
                if (sSystemServicesProxy.isSystemUser(currentUser)) {
                    this.mImpl.splitPrimaryTask(runningTask.id, i, rect);
                } else {
                    RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
                    if (recentsSystemUser != null) {
                        IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
                        if (nonSystemUserRecentsForUser != null) {
                            try {
                                nonSystemUserRecentsForUser.splitPrimaryTask(runningTask.id, i, rect);
                            } catch (RemoteException e) {
                                Log.e("Recents", "Callback failed", e);
                            }
                        } else {
                            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                        }
                    }
                }
                return true;
            }
            EventBus.getDefault().send(new ShowUserToastEvent(C0014R$string.dock_non_resizeble_failed_to_dock_text, 0));
        }
        return false;
    }

    public static void logDockAttempt(Context context, ComponentName componentName, int i) {
        if (i == 0) {
            MetricsLogger.action(context, 391, componentName.flattenToShortString());
        }
        MetricsLogger.count(context, getMetricsCounterForResizeMode(i), 1);
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void onAppTransitionFinished() {
        if (!getConfiguration().isLowRamDevice) {
            EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
        }
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void onConfigurationChanged(Configuration configuration) {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.onConfigurationChanged();
            return;
        }
        RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
        if (recentsSystemUser != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.onConfigurationChanged();
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
    }

    public final void onBusEvent(final RecentsVisibilityChangedEvent recentsVisibilityChangedEvent) {
        SystemServicesProxy systemServices = getSystemServices();
        if (systemServices.isSystemUser(systemServices.getProcessUser())) {
            this.mImpl.onVisibilityChanged(recentsVisibilityChangedEvent.applicationContext, recentsVisibilityChangedEvent.visible);
        } else {
            postToSystemUser(new Runnable() {
                /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass3 */

                public void run() {
                    try {
                        LegacyRecentsImpl.this.mUserToSystemCallbacks.updateRecentsVisibility(recentsVisibilityChangedEvent.visible);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
        if (!recentsVisibilityChangedEvent.visible) {
            this.mImpl.setWaitingForTransitionStart(false);
        }
    }

    public final void onBusEvent(DockedFirstAnimationFrameEvent dockedFirstAnimationFrameEvent) {
        SystemServicesProxy systemServices = getSystemServices();
        if (systemServices.isSystemUser(systemServices.getProcessUser())) {
            Divider divider = (Divider) getComponent(Divider.class);
            if (divider != null) {
                divider.onDockedFirstAnimationFrame();
                return;
            }
            return;
        }
        postToSystemUser(new Runnable() {
            /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass4 */

            public void run() {
                try {
                    LegacyRecentsImpl.this.mUserToSystemCallbacks.sendDockedFirstAnimationFrameEvent();
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            }
        });
    }

    public final void onBusEvent(final ScreenPinningRequestEvent screenPinningRequestEvent) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            this.mImpl.onStartScreenPinning(screenPinningRequestEvent.applicationContext, screenPinningRequestEvent.taskId);
        } else {
            postToSystemUser(new Runnable() {
                /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass5 */

                public void run() {
                    try {
                        LegacyRecentsImpl.this.mUserToSystemCallbacks.startScreenPinning(screenPinningRequestEvent.taskId);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(RecentsDrawnEvent recentsDrawnEvent) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            Divider divider = (Divider) getComponent(Divider.class);
            if (divider != null) {
                divider.onRecentsDrawn();
                return;
            }
            return;
        }
        postToSystemUser(new Runnable() {
            /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass6 */

            public void run() {
                try {
                    LegacyRecentsImpl.this.mUserToSystemCallbacks.sendRecentsDrawnEvent();
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            }
        });
    }

    public final void onBusEvent(final DockedTopTaskEvent dockedTopTaskEvent) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            Divider divider = (Divider) getComponent(Divider.class);
            if (divider != null) {
                divider.onDockedTopTask();
                return;
            }
            return;
        }
        postToSystemUser(new Runnable() {
            /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass7 */

            public void run() {
                try {
                    LegacyRecentsImpl.this.mUserToSystemCallbacks.sendDockingTopTaskEvent(dockedTopTaskEvent.initialRect);
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            }
        });
    }

    public final void onBusEvent(RecentsActivityStartingEvent recentsActivityStartingEvent) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            Divider divider = (Divider) getComponent(Divider.class);
            if (divider != null) {
                divider.onRecentsActivityStarting();
                return;
            }
            return;
        }
        postToSystemUser(new Runnable() {
            /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass8 */

            public void run() {
                try {
                    LegacyRecentsImpl.this.mUserToSystemCallbacks.sendLaunchRecentsEvent();
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            }
        });
    }

    public final void onBusEvent(LaunchTaskFailedEvent launchTaskFailedEvent) {
        this.mImpl.setWaitingForTransitionStart(false);
    }

    public final void onBusEvent(ConfigurationChangedEvent configurationChangedEvent) {
        this.mImpl.onConfigurationChanged();
    }

    public final void onBusEvent(ShowUserToastEvent showUserToastEvent) {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.onShowCurrentUserToast(showUserToastEvent.msgResId, showUserToastEvent.msgLength);
            return;
        }
        RecentsSystemUser recentsSystemUser = this.mSystemToUserCallbacks;
        if (recentsSystemUser != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = recentsSystemUser.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.showCurrentUserToast(showUserToastEvent.msgResId, showUserToastEvent.msgLength);
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
    }

    public final void onBusEvent(final SetWaitingForTransitionStartEvent setWaitingForTransitionStartEvent) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            this.mImpl.setWaitingForTransitionStart(setWaitingForTransitionStartEvent.waitingForTransitionStart);
        } else {
            postToSystemUser(new Runnable() {
                /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass9 */

                public void run() {
                    try {
                        LegacyRecentsImpl.this.mUserToSystemCallbacks.setWaitingForTransitionStartEvent(setWaitingForTransitionStartEvent.waitingForTransitionStart);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(ExpandPipEvent expandPipEvent) {
        PipUI pipUI = (PipUI) getComponent(PipUI.class);
        if (pipUI != null) {
            pipUI.expandPip();
        }
    }

    public final void onBusEvent(HidePipMenuEvent hidePipMenuEvent) {
        PipUI pipUI = (PipUI) getComponent(PipUI.class);
        if (pipUI != null) {
            hidePipMenuEvent.getAnimationTrigger().increment();
            pipUI.hidePipMenu(new Runnable() {
                /* class com.android.systemui.recents.$$Lambda$LegacyRecentsImpl$xO5R_It_ITxWKfmHT1Sjzqunw6k */

                public final void run() {
                    LegacyRecentsImpl.lambda$onBusEvent$0(HidePipMenuEvent.this);
                }
            }, new Runnable() {
                /* class com.android.systemui.recents.$$Lambda$LegacyRecentsImpl$PEWxOvtbk_wgSc2hdB0_I8K7cyg */

                public final void run() {
                    LegacyRecentsImpl.lambda$onBusEvent$1(HidePipMenuEvent.this);
                }
            });
            hidePipMenuEvent.getAnimationTrigger().decrement();
        }
    }

    private void registerWithSystemUser() {
        final int processUser = sSystemServicesProxy.getProcessUser();
        postToSystemUser(new Runnable() {
            /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass10 */

            public void run() {
                try {
                    LegacyRecentsImpl.this.mUserToSystemCallbacks.registerNonSystemUserCallbacks(new RecentsImplProxy(LegacyRecentsImpl.this.mImpl), processUser);
                } catch (RemoteException e) {
                    Log.e("Recents", "Failed to register", e);
                }
            }
        });
    }

    private void postToSystemUser(Runnable runnable) {
        this.mOnConnectRunnables.add(runnable);
        if (this.mUserToSystemCallbacks == null) {
            Intent intent = new Intent();
            intent.setClass(this.mContext, RecentsSystemUserService.class);
            boolean bindServiceAsUser = this.mContext.bindServiceAsUser(intent, this.mUserToSystemServiceConnection, 1, UserHandle.SYSTEM);
            EventLog.writeEvent(36060, 1, Integer.valueOf(sSystemServicesProxy.getProcessUser()));
            if (!bindServiceAsUser) {
                this.mHandler.postDelayed(new Runnable() {
                    /* class com.android.systemui.recents.LegacyRecentsImpl.AnonymousClass11 */

                    public void run() {
                        LegacyRecentsImpl.this.registerWithSystemUser();
                    }
                }, 5000);
                return;
            }
            return;
        }
        runAndFlushOnConnectRunnables();
    }

    private void runAndFlushOnConnectRunnables() {
        Iterator<Runnable> it = this.mOnConnectRunnables.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
        this.mOnConnectRunnables.clear();
    }

    private <T> T getComponent(Class<T> cls) {
        return (T) this.mSysUiServiceProvider.getComponent(cls);
    }

    @Override // com.android.systemui.recents.RecentsImplementation
    public void dump(PrintWriter printWriter) {
        printWriter.println("Recents");
        printWriter.println("  currentUserId=" + SystemServicesProxy.getInstance(this.mContext).getCurrentUser());
    }
}
