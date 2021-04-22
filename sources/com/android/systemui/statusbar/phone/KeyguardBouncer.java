package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.UserManager;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.StatsLog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSecurityView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R$id;
import com.android.keyguard.R$layout;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.DejankUtils;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import java.io.PrintWriter;

public class KeyguardBouncer {
    private int mBouncerPromptReason;
    protected final ViewMediatorCallback mCallback;
    protected final ViewGroup mContainer;
    protected final Context mContext;
    private final DismissCallbackRegistry mDismissCallbackRegistry;
    private float mExpansion = 1.0f;
    private final BouncerExpansionCallback mExpansionCallback;
    private final FalsingManager mFalsingManager;
    private final Handler mHandler;
    private boolean mIsAnimatingAway;
    private boolean mIsScrimmed;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    protected KeyguardHostView mKeyguardView;
    private ViewGroup mLockIconContainer;
    protected final LockPatternUtils mLockPatternUtils;
    private final Runnable mRemoveViewRunnable = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$iQsniWdIxLGqyYwRi09kQAh02M */

        public final void run() {
            KeyguardBouncer.this.removeView();
        }
    };
    private final Runnable mResetRunnable = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$KeyguardBouncer$Y9Hvfk0n3yPK2FQ39O1Z5j49gj0 */

        public final void run() {
            KeyguardBouncer.this.lambda$new$0$KeyguardBouncer();
        }
    };
    protected ViewGroup mRoot;
    private final Runnable mShowRunnable = new Runnable() {
        /* class com.android.systemui.statusbar.phone.KeyguardBouncer.AnonymousClass2 */

        public void run() {
            KeyguardBouncer.this.mRoot.setVisibility(0);
            KeyguardBouncer keyguardBouncer = KeyguardBouncer.this;
            keyguardBouncer.showPromptReason(keyguardBouncer.mBouncerPromptReason);
            CharSequence consumeCustomMessage = KeyguardBouncer.this.mCallback.consumeCustomMessage();
            if (consumeCustomMessage != null) {
                KeyguardBouncer.this.mKeyguardView.showErrorMessage(consumeCustomMessage);
            }
            if (KeyguardBouncer.this.mKeyguardView.getHeight() == 0 || KeyguardBouncer.this.mKeyguardView.getHeight() == KeyguardBouncer.this.mStatusBarHeight) {
                KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    /* class com.android.systemui.statusbar.phone.KeyguardBouncer.AnonymousClass2.AnonymousClass1 */

                    public boolean onPreDraw() {
                        KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().removeOnPreDrawListener(this);
                        KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
                        return true;
                    }
                });
                KeyguardBouncer.this.mKeyguardView.requestLayout();
            } else {
                KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
            }
            KeyguardBouncer.this.mShowingSoon = false;
            if (KeyguardBouncer.this.mExpansion == 0.0f) {
                KeyguardBouncer.this.mKeyguardView.onResume();
                KeyguardBouncer.this.mKeyguardView.resetSecurityContainer();
            }
            StatsLog.write(63, 2);
        }
    };
    private boolean mShowingSoon;
    private int mStatusBarHeight;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.android.systemui.statusbar.phone.KeyguardBouncer.AnonymousClass1 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStrongAuthStateChanged(int i) {
            KeyguardBouncer keyguardBouncer = KeyguardBouncer.this;
            keyguardBouncer.mBouncerPromptReason = keyguardBouncer.mCallback.getBouncerPromptReason();
        }
    };

    public interface BouncerExpansionCallback {
        void onFullyHidden();

        void onFullyShown();

        void onStartingToHide();

        void onStartingToShow();
    }

    public /* synthetic */ void lambda$new$0$KeyguardBouncer() {
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView != null) {
            keyguardHostView.resetSecurityContainer();
        }
    }

    public KeyguardBouncer(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils, ViewGroup viewGroup, DismissCallbackRegistry dismissCallbackRegistry, FalsingManager falsingManager, BouncerExpansionCallback bouncerExpansionCallback, KeyguardUpdateMonitor keyguardUpdateMonitor, Handler handler) {
        this.mContext = context;
        this.mCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mContainer = viewGroup;
        this.mKeyguardUpdateMonitor = keyguardUpdateMonitor;
        this.mFalsingManager = falsingManager;
        this.mDismissCallbackRegistry = dismissCallbackRegistry;
        this.mExpansionCallback = bouncerExpansionCallback;
        this.mHandler = handler;
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
    }

    public void show(boolean z) {
        show(z, true);
    }

    public void show(boolean z, boolean z2) {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        if (currentUser != 0 || !UserManager.isSplitSystemUser()) {
            ensureView();
            this.mIsScrimmed = z2;
            if (z2) {
                setExpansion(0.0f);
            }
            if (z) {
                showPrimarySecurityScreen();
            }
            if (this.mRoot.getVisibility() != 0 && !this.mShowingSoon) {
                int currentUser2 = KeyguardUpdateMonitor.getCurrentUser();
                boolean z3 = false;
                if (!(UserManager.isSplitSystemUser() && currentUser2 == 0) && currentUser2 == currentUser) {
                    z3 = true;
                }
                if (!z3 || !this.mKeyguardView.dismiss(currentUser2)) {
                    if (!z3) {
                        Slog.w("KeyguardBouncer", "User can't dismiss keyguard: " + currentUser2 + " != " + currentUser);
                    }
                    this.mShowingSoon = true;
                    DejankUtils.removeCallbacks(this.mResetRunnable);
                    if (this.mKeyguardUpdateMonitor.isFaceDetectionRunning()) {
                        this.mHandler.postDelayed(this.mShowRunnable, 800);
                    } else {
                        DejankUtils.postAfterTraversal(this.mShowRunnable);
                    }
                    this.mCallback.onBouncerVisiblityChanged(true);
                    this.mExpansionCallback.onStartingToShow();
                }
            }
        }
    }

    public boolean isScrimmed() {
        return this.mIsScrimmed;
    }

    private void onFullyShown() {
        this.mFalsingManager.onBouncerShown();
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView == null) {
            Log.wtf("KeyguardBouncer", "onFullyShown when view was null");
        } else {
            keyguardHostView.onResume();
        }
    }

    private void onFullyHidden() {
        if (!this.mShowingSoon) {
            cancelShowRunnable();
            ViewGroup viewGroup = this.mRoot;
            if (viewGroup != null) {
                viewGroup.setVisibility(4);
            }
            this.mFalsingManager.onBouncerHidden();
            DejankUtils.postAfterTraversal(this.mResetRunnable);
        }
    }

    public void showPromptReason(int i) {
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView != null) {
            keyguardHostView.showPromptReason(i);
        } else {
            Log.w("KeyguardBouncer", "Trying to show prompt reason on empty bouncer");
        }
    }

    public void showMessage(String str, ColorStateList colorStateList) {
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView != null) {
            keyguardHostView.showMessage(str, colorStateList);
        } else {
            Log.w("KeyguardBouncer", "Trying to show message on empty bouncer");
        }
    }

    private void cancelShowRunnable() {
        DejankUtils.removeCallbacks(this.mShowRunnable);
        this.mHandler.removeCallbacks(this.mShowRunnable);
        this.mShowingSoon = false;
    }

    public void showWithDismissAction(ActivityStarter.OnDismissAction onDismissAction, Runnable runnable) {
        ensureView();
        this.mKeyguardView.setOnDismissAction(onDismissAction, runnable);
        show(false);
    }

    public void hide(boolean z) {
        if (isShowing()) {
            StatsLog.write(63, 1);
            this.mDismissCallbackRegistry.notifyDismissCancelled();
        }
        this.mIsScrimmed = false;
        this.mFalsingManager.onBouncerHidden();
        this.mCallback.onBouncerVisiblityChanged(false);
        cancelShowRunnable();
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView != null) {
            keyguardHostView.cancelDismissAction();
            this.mKeyguardView.cleanUp();
        }
        this.mIsAnimatingAway = false;
        ViewGroup viewGroup = this.mRoot;
        if (viewGroup != null) {
            viewGroup.setVisibility(4);
            if (z) {
                this.mHandler.postDelayed(this.mRemoveViewRunnable, 50);
            }
        }
    }

    public void startPreHideAnimation(Runnable runnable) {
        this.mIsAnimatingAway = true;
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView != null) {
            keyguardHostView.startDisappearAnimation(runnable);
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public void onScreenTurnedOff() {
        ViewGroup viewGroup;
        if (this.mKeyguardView != null && (viewGroup = this.mRoot) != null && viewGroup.getVisibility() == 0) {
            this.mKeyguardView.onPause();
        }
    }

    public boolean isShowing() {
        ViewGroup viewGroup;
        return (this.mShowingSoon || ((viewGroup = this.mRoot) != null && viewGroup.getVisibility() == 0)) && this.mExpansion == 0.0f && !isAnimatingAway();
    }

    public boolean isPartiallyVisible() {
        ViewGroup viewGroup;
        return (this.mShowingSoon || ((viewGroup = this.mRoot) != null && viewGroup.getVisibility() == 0)) && this.mExpansion != 1.0f && !isAnimatingAway();
    }

    public boolean isAnimatingAway() {
        return this.mIsAnimatingAway;
    }

    public void prepare() {
        boolean z = this.mRoot != null;
        ensureView();
        if (z) {
            showPrimarySecurityScreen();
        }
        this.mBouncerPromptReason = this.mCallback.getBouncerPromptReason();
    }

    private void showPrimarySecurityScreen() {
        this.mKeyguardView.showPrimarySecurityScreen();
        KeyguardSecurityView currentSecurityView = this.mKeyguardView.getCurrentSecurityView();
        if (currentSecurityView != null) {
            this.mLockIconContainer = (ViewGroup) ((ViewGroup) currentSecurityView).findViewById(R$id.lock_icon_container);
        }
    }

    public void setExpansion(float f) {
        float f2 = this.mExpansion;
        this.mExpansion = f;
        if (this.mKeyguardView != null && !this.mIsAnimatingAway) {
            this.mKeyguardView.setAlpha(MathUtils.constrain(MathUtils.map(0.95f, 1.0f, 1.0f, 0.0f, f), 0.0f, 1.0f));
            KeyguardHostView keyguardHostView = this.mKeyguardView;
            keyguardHostView.setTranslationY(((float) keyguardHostView.getHeight()) * f);
        }
        int i = (f > 0.0f ? 1 : (f == 0.0f ? 0 : -1));
        if (i == 0 && f2 != 0.0f) {
            onFullyShown();
            this.mExpansionCallback.onFullyShown();
        } else if (f == 1.0f && f2 != 1.0f) {
            onFullyHidden();
            this.mExpansionCallback.onFullyHidden();
        } else if (i != 0 && f2 == 0.0f) {
            this.mExpansionCallback.onStartingToHide();
        }
    }

    public boolean willDismissWithAction() {
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        return keyguardHostView != null && keyguardHostView.hasDismissActions();
    }

    /* access modifiers changed from: protected */
    public void ensureView() {
        boolean hasCallbacks = this.mHandler.hasCallbacks(this.mRemoveViewRunnable);
        if (this.mRoot == null || hasCallbacks) {
            inflateView();
        }
    }

    /* access modifiers changed from: protected */
    public void inflateView() {
        removeView();
        this.mHandler.removeCallbacks(this.mRemoveViewRunnable);
        this.mRoot = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R$layout.keyguard_bouncer, (ViewGroup) null);
        this.mKeyguardView = (KeyguardHostView) this.mRoot.findViewById(R$id.keyguard_host_view);
        this.mKeyguardView.setLockPatternUtils(this.mLockPatternUtils);
        this.mKeyguardView.setViewMediatorCallback(this.mCallback);
        ViewGroup viewGroup = this.mContainer;
        viewGroup.addView(this.mRoot, viewGroup.getChildCount());
        this.mStatusBarHeight = this.mRoot.getResources().getDimensionPixelOffset(C0005R$dimen.status_bar_height);
        this.mRoot.setVisibility(4);
        this.mRoot.setAccessibilityPaneTitle(this.mKeyguardView.getAccessibilityTitleForCurrentMode());
        WindowInsets rootWindowInsets = this.mRoot.getRootWindowInsets();
        if (rootWindowInsets != null) {
            this.mRoot.dispatchApplyWindowInsets(rootWindowInsets);
        }
    }

    /* access modifiers changed from: protected */
    public void removeView() {
        ViewGroup viewGroup;
        ViewGroup viewGroup2 = this.mRoot;
        if (viewGroup2 != null && viewGroup2.getParent() == (viewGroup = this.mContainer)) {
            viewGroup.removeView(this.mRoot);
            this.mRoot = null;
        }
    }

    public boolean needsFullscreenBouncer() {
        ensureView();
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode securityMode = keyguardHostView.getSecurityMode();
        if (securityMode == KeyguardSecurityModel.SecurityMode.SimPin || securityMode == KeyguardSecurityModel.SecurityMode.SimPuk) {
            return true;
        }
        return false;
    }

    public boolean isFullscreenBouncer() {
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        if (keyguardHostView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode currentSecurityMode = keyguardHostView.getCurrentSecurityMode();
        if (currentSecurityMode == KeyguardSecurityModel.SecurityMode.SimPin || currentSecurityMode == KeyguardSecurityModel.SecurityMode.SimPuk) {
            return true;
        }
        return false;
    }

    public boolean isSecure() {
        KeyguardHostView keyguardHostView = this.mKeyguardView;
        return keyguardHostView == null || keyguardHostView.getSecurityMode() != KeyguardSecurityModel.SecurityMode.None;
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mKeyguardView.shouldEnableMenuKey();
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        ensureView();
        return this.mKeyguardView.interceptMediaKey(keyEvent);
    }

    public void notifyKeyguardAuthenticated(boolean z) {
        ensureView();
        this.mKeyguardView.finish(z, KeyguardUpdateMonitor.getCurrentUser());
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("KeyguardBouncer");
        printWriter.println("  isShowing(): " + isShowing());
        printWriter.println("  mStatusBarHeight: " + this.mStatusBarHeight);
        printWriter.println("  mExpansion: " + this.mExpansion);
        printWriter.println("  mKeyguardView; " + this.mKeyguardView);
        printWriter.println("  mShowingSoon: " + this.mKeyguardView);
        printWriter.println("  mBouncerPromptReason: " + this.mBouncerPromptReason);
        printWriter.println("  mIsAnimatingAway: " + this.mIsAnimatingAway);
    }
}
