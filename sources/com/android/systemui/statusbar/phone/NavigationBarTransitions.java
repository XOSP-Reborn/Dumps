package com.android.systemui.statusbar.phone;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.SparseArray;
import android.view.IWallpaperVisibilityListener;
import android.view.IWindowManager;
import android.view.View;
import androidx.appcompat.R$styleable;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.LightBarTransitionsController;
import com.android.systemui.statusbar.phone.NavigationBarTransitions;
import java.util.ArrayList;
import java.util.List;

public final class NavigationBarTransitions extends BarTransitions implements LightBarTransitionsController.DarkIntensityApplier {
    private final boolean mAllowAutoDimWallpaperNotVisible;
    private boolean mAutoDim;
    private final int mAutoDimDuration;
    private final IStatusBarService mBarService;
    private List<DarkIntensityListener> mDarkIntensityListeners;
    private final float mDimDarkButtonAlpha;
    private final float mDimLightButtonAlpha;
    private final Handler mHandler = Handler.getMain();
    private final LightBarTransitionsController mLightTransitionsController;
    private boolean mLightsOut;
    private int mNavBarMode = 0;
    private View mNavButtons;
    private View mNavLockButtons;
    private final NavigationBarView mView;
    private final IWallpaperVisibilityListener mWallpaperVisibilityListener = new IWallpaperVisibilityListener.Stub() {
        /* class com.android.systemui.statusbar.phone.NavigationBarTransitions.AnonymousClass1 */

        public void onWallpaperVisibilityChanged(boolean z, int i) throws RemoteException {
            NavigationBarTransitions.this.mWallpaperVisible = z;
            NavigationBarTransitions.this.mHandler.post(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarTransitions$1$5foY_Yygo1gW25mVBRpPSQRb_g */

                public final void run() {
                    NavigationBarTransitions.AnonymousClass1.this.lambda$onWallpaperVisibilityChanged$0$NavigationBarTransitions$1();
                }
            });
        }

        public /* synthetic */ void lambda$onWallpaperVisibilityChanged$0$NavigationBarTransitions$1() {
            NavigationBarTransitions.this.applyLightsOut(true, false);
        }
    };
    private boolean mWallpaperVisible;

    public interface DarkIntensityListener {
        void onDarkIntensity(float f);
    }

    public NavigationBarTransitions(NavigationBarView navigationBarView) {
        super(navigationBarView, C0006R$drawable.nav_background);
        this.mView = navigationBarView;
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mLightTransitionsController = new LightBarTransitionsController(navigationBarView.getContext(), this);
        this.mAllowAutoDimWallpaperNotVisible = navigationBarView.getContext().getResources().getBoolean(C0003R$bool.config_navigation_bar_enable_auto_dim_no_visible_wallpaper);
        this.mDarkIntensityListeners = new ArrayList();
        try {
            this.mWallpaperVisible = ((IWindowManager) Dependency.get(IWindowManager.class)).registerWallpaperVisibilityListener(this.mWallpaperVisibilityListener, 0);
        } catch (RemoteException unused) {
        }
        this.mView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarTransitions$XJcD0ZRW4UO2juvu7uZcSTj_ILk */

            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                NavigationBarTransitions.this.lambda$new$0$NavigationBarTransitions(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        View currentView = this.mView.getCurrentView();
        if (currentView != null) {
            this.mNavButtons = currentView.findViewById(C0007R$id.nav_buttons);
            this.mNavLockButtons = currentView.findViewById(C0007R$id.nav_lock_buttons);
        }
        Resources resources = navigationBarView.getContext().getResources();
        this.mAutoDimDuration = resources.getInteger(C0008R$integer.navbar_auto_dim_duration);
        this.mDimDarkButtonAlpha = resources.getFloat(C0008R$integer.navbar_dim_dark_button_alpha);
        this.mDimLightButtonAlpha = resources.getFloat(C0008R$integer.navbar_dim_light_button_alpha);
    }

    public /* synthetic */ void lambda$new$0$NavigationBarTransitions(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        View currentView = this.mView.getCurrentView();
        if (currentView != null) {
            this.mNavButtons = currentView.findViewById(C0007R$id.nav_buttons);
            this.mNavLockButtons = currentView.findViewById(C0007R$id.nav_lock_buttons);
            applyLightsOut(false, true);
        }
    }

    public void init() {
        applyModeBackground(-1, getMode(), false);
        applyLightsOut(false, true);
    }

    public void destroy() {
        try {
            ((IWindowManager) Dependency.get(IWindowManager.class)).unregisterWallpaperVisibilityListener(this.mWallpaperVisibilityListener, 0);
        } catch (RemoteException unused) {
        }
    }

    public void setAutoDim(boolean z) {
        if ((!z || !NavBarTintController.isEnabled(this.mView.getContext(), this.mNavBarMode)) && this.mAutoDim != z) {
            this.mAutoDim = z;
            applyLightsOut(true, false);
        }
    }

    /* access modifiers changed from: package-private */
    public void setBackgroundFrame(Rect rect) {
        this.mBarBackground.setFrame(rect);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.phone.BarTransitions
    public boolean isLightsOut(int i) {
        return super.isLightsOut(i) || (this.mAllowAutoDimWallpaperNotVisible && this.mAutoDim && !this.mWallpaperVisible && i != 5);
    }

    public LightBarTransitionsController getLightTransitionsController() {
        return this.mLightTransitionsController;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.phone.BarTransitions
    public void onTransition(int i, int i2, boolean z) {
        super.onTransition(i, i2, z);
        applyLightsOut(z, false);
        this.mView.onBarTransition(i2);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void applyLightsOut(boolean z, boolean z2) {
        applyLightsOut(isLightsOut(getMode()), z, z2);
    }

    private void applyLightsOut(boolean z, boolean z2, boolean z3) {
        if (z3 || z != this.mLightsOut) {
            this.mLightsOut = z;
            View view = this.mNavButtons;
            if (view != null && this.mNavLockButtons != null) {
                view.animate().cancel();
                float currentDarkIntensity = z ? this.mDimLightButtonAlpha + ((this.mDimDarkButtonAlpha - this.mDimLightButtonAlpha) * this.mLightTransitionsController.getCurrentDarkIntensity()) : 1.0f;
                if (!z2) {
                    this.mNavButtons.setAlpha(currentDarkIntensity);
                    this.mNavLockButtons.setAlpha(currentDarkIntensity);
                    return;
                }
                long j = (long) (z ? this.mAutoDimDuration : 250);
                this.mNavButtons.animate().alpha(currentDarkIntensity).setDuration(j).start();
                this.mNavLockButtons.animate().alpha(currentDarkIntensity).setDuration(j).start();
            }
        }
    }

    public void reapplyDarkIntensity() {
        applyDarkIntensity(this.mLightTransitionsController.getCurrentDarkIntensity());
    }

    @Override // com.android.systemui.statusbar.phone.LightBarTransitionsController.DarkIntensityApplier
    public void applyDarkIntensity(float f) {
        SparseArray<ButtonDispatcher> buttonDispatchers = this.mView.getButtonDispatchers();
        for (int size = buttonDispatchers.size() - 1; size >= 0; size--) {
            buttonDispatchers.valueAt(size).setDarkIntensity(f);
        }
        this.mView.getRotationButtonController().setDarkIntensity(f);
        for (DarkIntensityListener darkIntensityListener : this.mDarkIntensityListeners) {
            darkIntensityListener.onDarkIntensity(f);
        }
        if (this.mAutoDim) {
            applyLightsOut(false, true);
        }
    }

    @Override // com.android.systemui.statusbar.phone.LightBarTransitionsController.DarkIntensityApplier
    public int getTintAnimationDuration() {
        return NavBarTintController.isEnabled(this.mView.getContext(), this.mNavBarMode) ? Math.max(1700, 400) : R$styleable.AppCompatTheme_windowFixedWidthMajor;
    }

    public void onNavigationModeChanged(int i) {
        this.mNavBarMode = i;
    }

    public float addDarkIntensityListener(DarkIntensityListener darkIntensityListener) {
        this.mDarkIntensityListeners.add(darkIntensityListener);
        return this.mLightTransitionsController.getCurrentDarkIntensity();
    }

    public void removeDarkIntensityListener(DarkIntensityListener darkIntensityListener) {
        this.mDarkIntensityListeners.remove(darkIntensityListener);
    }
}
