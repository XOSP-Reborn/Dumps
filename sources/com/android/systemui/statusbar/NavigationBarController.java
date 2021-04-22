package com.android.systemui.statusbar;

import android.app.Fragment;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.RegisterStatusBarResult;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.AutoHideController;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarLockController;
import com.android.systemui.statusbar.phone.NavigationBarLockControllerImpl;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.sonymobile.runtimeskinning.SkinningBridge;

public class NavigationBarController implements CommandQueue.Callbacks {
    private static final String TAG = "NavigationBarController";
    private final Context mContext;
    private final DisplayManager mDisplayManager;
    private final Handler mHandler;
    private View mNavigationBarView;
    @VisibleForTesting
    SparseArray<NavigationBarFragment> mNavigationBars = new SparseArray<>();

    public NavigationBarController(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        CommandQueue commandQueue = (CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class);
        if (commandQueue != null) {
            commandQueue.addCallback((CommandQueue.Callbacks) this);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onDisplayRemoved(int i) {
        removeNavigationBar(i);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onDisplayReady(int i) {
        createNavigationBar(this.mDisplayManager.getDisplay(i), null);
    }

    public void createNavigationBars(boolean z, RegisterStatusBarResult registerStatusBarResult) {
        Display[] displays = this.mDisplayManager.getDisplays();
        for (Display display : displays) {
            if (z || display.getDisplayId() != 0) {
                createNavigationBar(display, registerStatusBarResult);
            }
        }
        View view = this.mNavigationBarView;
        if (view != null) {
            SkinningBridge.init(view, C0007R$id.back, C0007R$id.home, this.mContext.getResources().getIdentifier("home_button", "id", "com.android.systemui"), C0007R$id.recent_apps, C0007R$id.menu, C0007R$id.ime_switcher, C0007R$id.accessibility_button, C0007R$id.rotate_suggestion);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void createNavigationBar(Display display, RegisterStatusBarResult registerStatusBarResult) {
        Context context;
        if (display != null) {
            int displayId = display.getDisplayId();
            boolean z = displayId == 0;
            try {
                if (WindowManagerGlobal.getWindowManagerService().hasNavigationBar(displayId)) {
                    if (z) {
                        context = this.mContext;
                    } else {
                        context = this.mContext.createDisplayContext(display);
                    }
                    this.mNavigationBarView = NavigationBarFragment.create(context, new FragmentHostManager.FragmentListener(z, context, displayId, registerStatusBarResult, display) {
                        /* class com.android.systemui.statusbar.$$Lambda$NavigationBarController$oyTONslWMHHQSXiga3Vs0njIek8 */
                        private final /* synthetic */ boolean f$1;
                        private final /* synthetic */ Context f$2;
                        private final /* synthetic */ int f$3;
                        private final /* synthetic */ RegisterStatusBarResult f$4;
                        private final /* synthetic */ Display f$5;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                            this.f$3 = r4;
                            this.f$4 = r5;
                            this.f$5 = r6;
                        }

                        @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
                        public final void onFragmentViewCreated(String str, Fragment fragment) {
                            NavigationBarController.this.lambda$createNavigationBar$0$NavigationBarController(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, str, fragment);
                        }
                    });
                }
            } catch (RemoteException unused) {
                Log.w(TAG, "Cannot get WindowManager.");
            }
        }
    }

    public /* synthetic */ void lambda$createNavigationBar$0$NavigationBarController(boolean z, Context context, int i, RegisterStatusBarResult registerStatusBarResult, Display display, String str, Fragment fragment) {
        NavigationBarLockController navigationBarLockController;
        LightBarController lightBarController;
        AutoHideController autoHideController;
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) fragment;
        NavigationBarView navigationBarView = (NavigationBarView) navigationBarFragment.getView();
        if (z) {
            navigationBarLockController = (NavigationBarLockController) Dependency.get(NavigationBarLockController.class);
        } else {
            navigationBarLockController = new NavigationBarLockControllerImpl(context);
        }
        navigationBarView.setLockController(navigationBarLockController);
        navigationBarLockController.setNavigationBarView(navigationBarView);
        if (z) {
            lightBarController = (LightBarController) Dependency.get(LightBarController.class);
        } else {
            lightBarController = new LightBarController(context, (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class), (BatteryController) Dependency.get(BatteryController.class));
        }
        navigationBarFragment.setLightBarController(lightBarController);
        if (z) {
            autoHideController = (AutoHideController) Dependency.get(AutoHideController.class);
        } else {
            autoHideController = new AutoHideController(context, this.mHandler);
        }
        navigationBarFragment.setAutoHideController(autoHideController);
        navigationBarFragment.restoreSystemUiVisibilityState();
        this.mNavigationBars.append(i, navigationBarFragment);
        if (registerStatusBarResult != null) {
            navigationBarFragment.setImeWindowStatus(display.getDisplayId(), registerStatusBarResult.mImeToken, registerStatusBarResult.mImeWindowVis, registerStatusBarResult.mImeBackDisposition, registerStatusBarResult.mShowImeSwitcher);
        }
    }

    private void removeNavigationBar(int i) {
        NavigationBarFragment navigationBarFragment = this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            ((NavigationBarView) navigationBarFragment.getView()).getLockController().destroy();
            WindowManagerGlobal.getInstance().removeView(navigationBarFragment.getView().getRootView(), true);
            this.mNavigationBars.remove(i);
        }
    }

    public void checkNavBarModes(int i) {
        NavigationBarFragment navigationBarFragment = this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.checkNavBarModes();
        }
    }

    public void finishBarAnimations(int i) {
        NavigationBarFragment navigationBarFragment = this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.finishBarAnimations();
        }
    }

    public void touchAutoDim(int i) {
        NavigationBarFragment navigationBarFragment = this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.touchAutoDim();
        }
    }

    public void transitionTo(int i, int i2, boolean z) {
        NavigationBarFragment navigationBarFragment = this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.transitionTo(i2, z);
        }
    }

    public void disableAnimationsDuringHide(int i, long j) {
        NavigationBarFragment navigationBarFragment = this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.disableAnimationsDuringHide(j);
        }
    }

    public NavigationBarView getDefaultNavigationBarView() {
        return getNavigationBarView(0);
    }

    public NavigationBarView getNavigationBarView(int i) {
        NavigationBarFragment navigationBarFragment = this.mNavigationBars.get(i);
        if (navigationBarFragment == null) {
            return null;
        }
        return (NavigationBarView) navigationBarFragment.getView();
    }

    public NavigationBarFragment getDefaultNavigationBarFragment() {
        return this.mNavigationBars.get(0);
    }
}
