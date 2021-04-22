package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.MathUtils;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.internal.util.Preconditions;
import com.android.systemui.RegionInterceptingFrameLayout;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.phone.NavigationBarTransitions;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.tuner.TunablePadding;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.leak.RotationUtils;
import java.util.ArrayList;
import java.util.List;

public class ScreenDecorations extends SystemUI implements TunerService.Tunable, NavigationBarTransitions.DarkIntensityListener {
    private static final boolean DEBUG_SCREENSHOT_ROUNDED_CORNERS = SystemProperties.getBoolean("debug.screenshot_rounded_corners", false);
    private boolean mAssistHintBlocked = false;
    private boolean mAssistHintVisible;
    private View mBottomOverlay;
    private SecureSetting mColorInversionSetting;
    private DisplayCutoutView mCutoutBottom;
    private DisplayCutoutView mCutoutTop;
    private float mDensity;
    private DisplayManager.DisplayListener mDisplayListener;
    private DisplayManager mDisplayManager;
    private Handler mHandler;
    private boolean mInGesturalMode;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.ScreenDecorations.AnonymousClass4 */

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_SWITCHED")) {
                ScreenDecorations.this.mColorInversionSetting.setUserId(intent.getIntExtra("android.intent.extra.user_handle", ActivityManager.getCurrentUser()));
                ScreenDecorations screenDecorations = ScreenDecorations.this;
                screenDecorations.updateColorInversion(screenDecorations.mColorInversionSetting.getValue());
            }
        }
    };
    private boolean mIsReceivingNavBarColor = false;
    private View mOverlay;
    private boolean mPendingRotationChange;
    private int mRotation;
    protected int mRoundedDefault;
    protected int mRoundedDefaultBottom;
    protected int mRoundedDefaultTop;
    private WindowManager mWindowManager;

    private boolean isLandscape(int i) {
        return i == 1 || i == 2;
    }

    public static Region rectsToRegion(List<Rect> list) {
        Region obtain = Region.obtain();
        if (list != null) {
            for (Rect rect : list) {
                if (rect != null && !rect.isEmpty()) {
                    obtain.op(rect, Region.Op.UNION);
                }
            }
        }
        return obtain;
    }

    @Override // com.android.systemui.SystemUI
    public void start() {
        this.mHandler = startHandlerThread();
        this.mHandler.post(new Runnable() {
            /* class com.android.systemui.$$Lambda$ScreenDecorations$IfAux2ksmJXT9o9i38WaSEQXJTQ */

            public final void run() {
                ScreenDecorations.lambda$IfAux2ksmJXT9o9i38WaSEQXJTQ(ScreenDecorations.this);
            }
        });
        setupStatusBarPaddingIfNeeded();
        putComponent(ScreenDecorations.class, this);
        this.mInGesturalMode = QuickStepContract.isGesturalMode(((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(new NavigationModeController.ModeChangedListener() {
            /* class com.android.systemui.$$Lambda$60rw5ghsFNPB4OvLwvmy1hJgGv0 */

            @Override // com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener
            public final void onNavigationModeChanged(int i) {
                ScreenDecorations.this.lambda$handleNavigationModeChange$0$ScreenDecorations(i);
            }
        }));
    }

    /* access modifiers changed from: package-private */
    /* renamed from: handleNavigationModeChange */
    public void lambda$handleNavigationModeChange$0$ScreenDecorations(int i) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(i) {
                /* class com.android.systemui.$$Lambda$ScreenDecorations$4F6CKqAgtSx8ZovPRT6WEWEYS4E */
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$handleNavigationModeChange$0$ScreenDecorations(this.f$1);
                }
            });
            return;
        }
        boolean isGesturalMode = QuickStepContract.isGesturalMode(i);
        if (this.mInGesturalMode != isGesturalMode) {
            this.mInGesturalMode = isGesturalMode;
            if (this.mInGesturalMode && this.mOverlay == null) {
                setupDecorations();
                if (this.mOverlay != null) {
                    updateLayoutParams();
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public Animator getHandleAnimator(View view, float f, float f2, boolean z, long j, Interpolator interpolator) {
        float lerp = MathUtils.lerp(2.0f, 1.0f, f);
        float lerp2 = MathUtils.lerp(2.0f, 1.0f, f2);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.SCALE_X, lerp, lerp2);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(view, View.SCALE_Y, lerp, lerp2);
        float lerp3 = MathUtils.lerp(0.2f, 0.0f, f);
        float lerp4 = MathUtils.lerp(0.2f, 0.0f, f2);
        float f3 = (float) (z ? -1 : 1);
        ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, f3 * lerp3 * ((float) view.getWidth()), f3 * lerp4 * ((float) view.getWidth()));
        ObjectAnimator ofFloat4 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, lerp3 * ((float) view.getHeight()), lerp4 * ((float) view.getHeight()));
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(ofFloat).with(ofFloat2);
        animatorSet.play(ofFloat).with(ofFloat3);
        animatorSet.play(ofFloat).with(ofFloat4);
        animatorSet.setDuration(j);
        animatorSet.setInterpolator(interpolator);
        return animatorSet;
    }

    private void fade(View view, boolean z, boolean z2) {
        if (z) {
            view.animate().cancel();
            view.setAlpha(1.0f);
            view.setVisibility(0);
            AnimatorSet animatorSet = new AnimatorSet();
            Animator handleAnimator = getHandleAnimator(view, 0.0f, 1.1f, z2, 750, new PathInterpolator(0.0f, 0.45f, 0.67f, 1.0f));
            PathInterpolator pathInterpolator = new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);
            Animator handleAnimator2 = getHandleAnimator(view, 1.1f, 0.97f, z2, 400, pathInterpolator);
            Animator handleAnimator3 = getHandleAnimator(view, 0.97f, 1.02f, z2, 400, pathInterpolator);
            Animator handleAnimator4 = getHandleAnimator(view, 1.02f, 1.0f, z2, 400, pathInterpolator);
            animatorSet.play(handleAnimator).before(handleAnimator2);
            animatorSet.play(handleAnimator2).before(handleAnimator3);
            animatorSet.play(handleAnimator3).before(handleAnimator4);
            animatorSet.start();
            return;
        }
        view.animate().cancel();
        view.animate().setInterpolator(new AccelerateInterpolator(1.5f)).setDuration(250).alpha(0.0f);
    }

    /* renamed from: setAssistHintVisible */
    public void lambda$setAssistHintVisible$1$ScreenDecorations(boolean z) {
        View view;
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(z) {
                /* class com.android.systemui.$$Lambda$ScreenDecorations$v4VgfK79EV22k9HdjvuSqrLHx4 */
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$setAssistHintVisible$1$ScreenDecorations(this.f$1);
                }
            });
        } else if ((!this.mAssistHintBlocked || !z) && (view = this.mOverlay) != null && this.mBottomOverlay != null) {
            if (this.mAssistHintVisible != z) {
                this.mAssistHintVisible = z;
                CornerHandleView cornerHandleView = (CornerHandleView) view.findViewById(C0007R$id.assist_hint_left);
                CornerHandleView cornerHandleView2 = (CornerHandleView) this.mOverlay.findViewById(C0007R$id.assist_hint_right);
                CornerHandleView cornerHandleView3 = (CornerHandleView) this.mBottomOverlay.findViewById(C0007R$id.assist_hint_left);
                CornerHandleView cornerHandleView4 = (CornerHandleView) this.mBottomOverlay.findViewById(C0007R$id.assist_hint_right);
                int i = this.mRotation;
                if (i == 0) {
                    fade(cornerHandleView3, this.mAssistHintVisible, true);
                    fade(cornerHandleView4, this.mAssistHintVisible, false);
                } else if (i == 1) {
                    fade(cornerHandleView2, this.mAssistHintVisible, true);
                    fade(cornerHandleView4, this.mAssistHintVisible, false);
                } else if (i == 2) {
                    fade(cornerHandleView, this.mAssistHintVisible, false);
                    fade(cornerHandleView3, this.mAssistHintVisible, true);
                } else if (i == 3) {
                    fade(cornerHandleView, this.mAssistHintVisible, false);
                    fade(cornerHandleView2, this.mAssistHintVisible, true);
                }
            }
            updateWindowVisibilities();
        }
    }

    /* renamed from: setAssistHintBlocked */
    public void lambda$setAssistHintBlocked$2$ScreenDecorations(boolean z) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(z) {
                /* class com.android.systemui.$$Lambda$ScreenDecorations$X65dAPl3paBdNr5xrYJHzDmgROE */
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$setAssistHintBlocked$2$ScreenDecorations(this.f$1);
                }
            });
            return;
        }
        this.mAssistHintBlocked = z;
        if (this.mAssistHintVisible && this.mAssistHintBlocked) {
            lambda$setAssistHintVisible$1$ScreenDecorations(false);
        }
    }

    /* access modifiers changed from: package-private */
    public Handler startHandlerThread() {
        HandlerThread handlerThread = new HandlerThread("ScreenDecorations");
        handlerThread.start();
        return handlerThread.getThreadHandler();
    }

    private boolean shouldHostHandles() {
        return this.mInGesturalMode;
    }

    /* access modifiers changed from: private */
    public void startOnScreenDecorationsThread() {
        this.mRotation = RotationUtils.getExactRotation(this.mContext);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        updateRoundedCornerRadii();
        if (hasRoundedCorners() || shouldDrawCutout() || shouldHostHandles()) {
            setupDecorations();
        }
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            /* class com.android.systemui.ScreenDecorations.AnonymousClass1 */

            public void onDisplayAdded(int i) {
            }

            public void onDisplayRemoved(int i) {
            }

            public void onDisplayChanged(int i) {
                int exactRotation = RotationUtils.getExactRotation(ScreenDecorations.this.mContext);
                if (!(ScreenDecorations.this.mOverlay == null || ScreenDecorations.this.mBottomOverlay == null || ScreenDecorations.this.mRotation == exactRotation)) {
                    ScreenDecorations.this.mPendingRotationChange = true;
                    ViewTreeObserver viewTreeObserver = ScreenDecorations.this.mOverlay.getViewTreeObserver();
                    ScreenDecorations screenDecorations = ScreenDecorations.this;
                    viewTreeObserver.addOnPreDrawListener(new RestartingPreDrawListener(screenDecorations.mOverlay, exactRotation));
                    ViewTreeObserver viewTreeObserver2 = ScreenDecorations.this.mBottomOverlay.getViewTreeObserver();
                    ScreenDecorations screenDecorations2 = ScreenDecorations.this;
                    viewTreeObserver2.addOnPreDrawListener(new RestartingPreDrawListener(screenDecorations2.mBottomOverlay, exactRotation));
                }
                ScreenDecorations.this.updateOrientation();
            }
        };
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
        updateOrientation();
    }

    private void setupDecorations() {
        this.mOverlay = LayoutInflater.from(this.mContext).inflate(C0010R$layout.rounded_corners, (ViewGroup) null);
        this.mCutoutTop = new DisplayCutoutView(this.mContext, true, new Runnable() {
            /* class com.android.systemui.$$Lambda$ScreenDecorations$aq1MVJyy_LkZ11q5t5cPVZOqbG0 */

            public final void run() {
                ScreenDecorations.lambda$aq1MVJyy_LkZ11q5t5cPVZOqbG0(ScreenDecorations.this);
            }
        }, this);
        ((ViewGroup) this.mOverlay).addView(this.mCutoutTop);
        this.mBottomOverlay = LayoutInflater.from(this.mContext).inflate(C0010R$layout.rounded_corners, (ViewGroup) null);
        this.mCutoutBottom = new DisplayCutoutView(this.mContext, false, new Runnable() {
            /* class com.android.systemui.$$Lambda$ScreenDecorations$aq1MVJyy_LkZ11q5t5cPVZOqbG0 */

            public final void run() {
                ScreenDecorations.lambda$aq1MVJyy_LkZ11q5t5cPVZOqbG0(ScreenDecorations.this);
            }
        }, this);
        ((ViewGroup) this.mBottomOverlay).addView(this.mCutoutBottom);
        this.mOverlay.setSystemUiVisibility(256);
        this.mOverlay.setAlpha(0.0f);
        this.mOverlay.setForceDarkAllowed(false);
        this.mBottomOverlay.setSystemUiVisibility(256);
        this.mBottomOverlay.setAlpha(0.0f);
        this.mBottomOverlay.setForceDarkAllowed(false);
        updateViews();
        this.mWindowManager.addView(this.mOverlay, getWindowLayoutParams());
        this.mWindowManager.addView(this.mBottomOverlay, getBottomLayoutParams());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.mDensity = displayMetrics.density;
        ((Handler) Dependency.get(Dependency.MAIN_HANDLER)).post(new Runnable() {
            /* class com.android.systemui.$$Lambda$ScreenDecorations$80y3DVvpOo5hQVDW3EJlUKhPsU */

            public final void run() {
                ScreenDecorations.this.lambda$setupDecorations$3$ScreenDecorations();
            }
        });
        this.mColorInversionSetting = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") {
            /* class com.android.systemui.ScreenDecorations.AnonymousClass2 */

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.qs.SecureSetting
            public void handleValueChanged(int i, boolean z) {
                ScreenDecorations.this.updateColorInversion(i);
            }
        };
        this.mColorInversionSetting.setListening(true);
        this.mColorInversionSetting.onChange(false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter, null, this.mHandler);
        this.mOverlay.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            /* class com.android.systemui.ScreenDecorations.AnonymousClass3 */

            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                ScreenDecorations.this.mOverlay.removeOnLayoutChangeListener(this);
                ScreenDecorations.this.mOverlay.animate().alpha(1.0f).setDuration(1000).start();
                ScreenDecorations.this.mBottomOverlay.animate().alpha(1.0f).setDuration(1000).start();
            }
        });
        this.mOverlay.getViewTreeObserver().addOnPreDrawListener(new ValidatingPreDrawListener(this.mOverlay));
        this.mBottomOverlay.getViewTreeObserver().addOnPreDrawListener(new ValidatingPreDrawListener(this.mBottomOverlay));
    }

    public /* synthetic */ void lambda$setupDecorations$3$ScreenDecorations() {
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_rounded_size");
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateColorInversion(int i) {
        int i2 = i != 0 ? -1 : -16777216;
        ColorStateList valueOf = ColorStateList.valueOf(i2);
        ((ImageView) this.mOverlay.findViewById(C0007R$id.left)).setImageTintList(valueOf);
        ((ImageView) this.mOverlay.findViewById(C0007R$id.right)).setImageTintList(valueOf);
        ((ImageView) this.mBottomOverlay.findViewById(C0007R$id.left)).setImageTintList(valueOf);
        ((ImageView) this.mBottomOverlay.findViewById(C0007R$id.right)).setImageTintList(valueOf);
        this.mCutoutTop.setColor(i2);
        this.mCutoutBottom.setColor(i2);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.SystemUI
    public void onConfigurationChanged(Configuration configuration) {
        this.mHandler.post(new Runnable() {
            /* class com.android.systemui.$$Lambda$ScreenDecorations$ZcXfKcloCI7zxM2FCddGg2uzM */

            public final void run() {
                ScreenDecorations.this.lambda$onConfigurationChanged$4$ScreenDecorations();
            }
        });
    }

    public /* synthetic */ void lambda$onConfigurationChanged$4$ScreenDecorations() {
        this.mPendingRotationChange = false;
        updateOrientation();
        updateRoundedCornerRadii();
        if (shouldDrawCutout() && this.mOverlay == null) {
            setupDecorations();
        }
        if (this.mOverlay != null) {
            updateLayoutParams();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateOrientation() {
        int exactRotation;
        boolean z = this.mHandler.getLooper().getThread() == Thread.currentThread();
        Preconditions.checkState(z, "must call on " + this.mHandler.getLooper().getThread() + ", but was " + Thread.currentThread());
        if (!this.mPendingRotationChange && (exactRotation = RotationUtils.getExactRotation(this.mContext)) != this.mRotation) {
            this.mRotation = exactRotation;
            if (this.mOverlay != null) {
                updateLayoutParams();
                updateViews();
                if (this.mAssistHintVisible) {
                    hideAssistHandles();
                    lambda$setAssistHintVisible$1$ScreenDecorations(true);
                }
            }
        }
    }

    private void hideAssistHandles() {
        View view = this.mOverlay;
        if (view != null && this.mBottomOverlay != null) {
            view.findViewById(C0007R$id.assist_hint_left).setVisibility(8);
            this.mOverlay.findViewById(C0007R$id.assist_hint_right).setVisibility(8);
            this.mBottomOverlay.findViewById(C0007R$id.assist_hint_left).setVisibility(8);
            this.mBottomOverlay.findViewById(C0007R$id.assist_hint_right).setVisibility(8);
            this.mAssistHintVisible = false;
        }
    }

    private void updateRoundedCornerRadii() {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(17105405);
        int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(17105407);
        int dimensionPixelSize3 = this.mContext.getResources().getDimensionPixelSize(17105406);
        if ((this.mRoundedDefault == dimensionPixelSize && this.mRoundedDefaultBottom == dimensionPixelSize3 && this.mRoundedDefaultTop == dimensionPixelSize2) ? false : true) {
            this.mRoundedDefault = dimensionPixelSize;
            this.mRoundedDefaultTop = dimensionPixelSize2;
            this.mRoundedDefaultBottom = dimensionPixelSize3;
            onTuningChanged("sysui_rounded_size", null);
        }
    }

    private void updateViews() {
        View findViewById = this.mOverlay.findViewById(C0007R$id.left);
        View findViewById2 = this.mOverlay.findViewById(C0007R$id.right);
        View findViewById3 = this.mBottomOverlay.findViewById(C0007R$id.left);
        View findViewById4 = this.mBottomOverlay.findViewById(C0007R$id.right);
        int i = this.mRotation;
        if (i == 0) {
            updateView(findViewById, 51, 0);
            updateView(findViewById2, 53, 90);
            updateView(findViewById3, 83, 270);
            updateView(findViewById4, 85, 180);
        } else if (i == 1) {
            updateView(findViewById, 51, 0);
            updateView(findViewById2, 83, 270);
            updateView(findViewById3, 53, 90);
            updateView(findViewById4, 85, 180);
        } else if (i == 3) {
            updateView(findViewById, 83, 270);
            updateView(findViewById2, 85, 180);
            updateView(findViewById3, 51, 0);
            updateView(findViewById4, 53, 90);
        } else if (i == 2) {
            updateView(findViewById, 85, 180);
            updateView(findViewById2, 53, 90);
            updateView(findViewById3, 83, 270);
            updateView(findViewById4, 51, 0);
        }
        updateAssistantHandleViews();
        this.mCutoutTop.setRotation(this.mRotation);
        this.mCutoutBottom.setRotation(this.mRotation);
        updateWindowVisibilities();
    }

    private void updateAssistantHandleViews() {
        View findViewById = this.mOverlay.findViewById(C0007R$id.assist_hint_left);
        View findViewById2 = this.mOverlay.findViewById(C0007R$id.assist_hint_right);
        View findViewById3 = this.mBottomOverlay.findViewById(C0007R$id.assist_hint_left);
        View findViewById4 = this.mBottomOverlay.findViewById(C0007R$id.assist_hint_right);
        int i = this.mAssistHintVisible ? 0 : 4;
        int i2 = this.mRotation;
        if (i2 == 0) {
            findViewById.setVisibility(8);
            findViewById2.setVisibility(8);
            findViewById3.setVisibility(i);
            findViewById4.setVisibility(i);
            updateView(findViewById3, 83, 270);
            updateView(findViewById4, 85, 180);
        } else if (i2 == 1) {
            findViewById.setVisibility(8);
            findViewById2.setVisibility(i);
            findViewById3.setVisibility(8);
            findViewById4.setVisibility(i);
            updateView(findViewById2, 83, 270);
            updateView(findViewById4, 85, 180);
        } else if (i2 == 3) {
            findViewById.setVisibility(i);
            findViewById2.setVisibility(i);
            findViewById3.setVisibility(8);
            findViewById4.setVisibility(8);
            updateView(findViewById, 83, 270);
            updateView(findViewById2, 85, 180);
        } else if (i2 == 2) {
            findViewById.setVisibility(i);
            findViewById2.setVisibility(8);
            findViewById3.setVisibility(i);
            findViewById4.setVisibility(8);
            updateView(findViewById, 85, 180);
            updateView(findViewById3, 83, 270);
        }
    }

    private void updateView(View view, int i, int i2) {
        ((FrameLayout.LayoutParams) view.getLayoutParams()).gravity = i;
        view.setRotation((float) i2);
    }

    /* access modifiers changed from: private */
    public void updateWindowVisibilities() {
        updateWindowVisibility(this.mOverlay);
        updateWindowVisibility(this.mBottomOverlay);
    }

    private void updateWindowVisibility(View view) {
        boolean z = true;
        int i = 0;
        boolean z2 = shouldDrawCutout() && view.findViewById(C0007R$id.display_cutout).getVisibility() == 0;
        boolean hasRoundedCorners = hasRoundedCorners();
        if (!(view.findViewById(C0007R$id.assist_hint_left).getVisibility() == 0 || view.findViewById(C0007R$id.assist_hint_right).getVisibility() == 0)) {
            z = false;
        }
        if (!z2 && !hasRoundedCorners && !z) {
            i = 8;
        }
        view.setVisibility(i);
    }

    private boolean hasRoundedCorners() {
        return this.mRoundedDefault > 0 || this.mRoundedDefaultBottom > 0 || this.mRoundedDefaultTop > 0;
    }

    private boolean shouldDrawCutout() {
        return shouldDrawCutout(this.mContext);
    }

    static boolean shouldDrawCutout(Context context) {
        return context.getResources().getBoolean(17891462);
    }

    private void setupStatusBarPaddingIfNeeded() {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.rounded_corner_content_padding);
        if (dimensionPixelSize != 0) {
            setupStatusBarPadding(dimensionPixelSize);
        }
    }

    private void setupStatusBarPadding(int i) {
        StatusBar statusBar = (StatusBar) getComponent(StatusBar.class);
        StatusBarWindowView statusBarWindow = statusBar != null ? statusBar.getStatusBarWindow() : null;
        if (statusBarWindow != null) {
            TunablePadding.addTunablePadding(statusBarWindow.findViewById(C0007R$id.keyguard_header), "sysui_rounded_content_padding", i, 2);
            FragmentHostManager fragmentHostManager = FragmentHostManager.get(statusBarWindow);
            fragmentHostManager.addTagListener("CollapsedStatusBarFragment", new TunablePaddingTagListener(i, C0007R$id.status_bar));
            fragmentHostManager.addTagListener(QS.TAG, new TunablePaddingTagListener(i, C0007R$id.header));
        }
    }

    /* access modifiers changed from: package-private */
    public WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -2, 2024, 545259816, -3);
        layoutParams.privateFlags |= 80;
        if (!DEBUG_SCREENSHOT_ROUNDED_CORNERS) {
            layoutParams.privateFlags |= 1048576;
        }
        layoutParams.setTitle("ScreenDecorOverlay");
        int i = this.mRotation;
        if (i == 2 || i == 3) {
            layoutParams.gravity = 85;
        } else {
            layoutParams.gravity = 51;
        }
        layoutParams.layoutInDisplayCutoutMode = 1;
        if (isLandscape(this.mRotation)) {
            layoutParams.width = -2;
            layoutParams.height = -1;
        }
        layoutParams.privateFlags |= 16777216;
        return layoutParams;
    }

    private WindowManager.LayoutParams getBottomLayoutParams() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.setTitle("ScreenDecorOverlayBottom");
        int i = this.mRotation;
        if (i == 2 || i == 3) {
            windowLayoutParams.gravity = 51;
        } else {
            windowLayoutParams.gravity = 85;
        }
        return windowLayoutParams;
    }

    private void updateLayoutParams() {
        this.mWindowManager.updateViewLayout(this.mOverlay, getWindowLayoutParams());
        this.mWindowManager.updateViewLayout(this.mBottomOverlay, getBottomLayoutParams());
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        this.mHandler.post(new Runnable(str, str2) {
            /* class com.android.systemui.$$Lambda$ScreenDecorations$mdf60Bg4ecefimWHJ4lSsesAIU */
            private final /* synthetic */ String f$1;
            private final /* synthetic */ String f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                ScreenDecorations.this.lambda$onTuningChanged$5$ScreenDecorations(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$onTuningChanged$5$ScreenDecorations(String str, String str2) {
        if (this.mOverlay != null && "sysui_rounded_size".equals(str)) {
            int i = this.mRoundedDefault;
            int i2 = this.mRoundedDefaultTop;
            int i3 = this.mRoundedDefaultBottom;
            if (str2 != null) {
                try {
                    i = (int) (((float) Integer.parseInt(str2)) * this.mDensity);
                } catch (Exception unused) {
                }
            }
            if (i2 == 0) {
                i2 = i;
            }
            if (i3 != 0) {
                i = i3;
            }
            setSize(this.mOverlay.findViewById(C0007R$id.left), i2);
            setSize(this.mOverlay.findViewById(C0007R$id.right), i2);
            setSize(this.mBottomOverlay.findViewById(C0007R$id.left), i);
            setSize(this.mBottomOverlay.findViewById(C0007R$id.right), i);
        }
    }

    private void setSize(View view, int i) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = i;
        layoutParams.height = i;
        view.setLayoutParams(layoutParams);
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarTransitions.DarkIntensityListener
    /* renamed from: onDarkIntensity */
    public void lambda$onDarkIntensity$6$ScreenDecorations(float f) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(f) {
                /* class com.android.systemui.$$Lambda$ScreenDecorations$0LxH4_gyyT9LgXM946gQ6FsGA7o */
                private final /* synthetic */ float f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$onDarkIntensity$6$ScreenDecorations(this.f$1);
                }
            });
            return;
        }
        View view = this.mOverlay;
        if (view != null) {
            ((CornerHandleView) view.findViewById(C0007R$id.assist_hint_left)).updateDarkness(f);
            ((CornerHandleView) this.mOverlay.findViewById(C0007R$id.assist_hint_right)).updateDarkness(f);
        }
        View view2 = this.mBottomOverlay;
        if (view2 != null) {
            ((CornerHandleView) view2.findViewById(C0007R$id.assist_hint_left)).updateDarkness(f);
            ((CornerHandleView) this.mBottomOverlay.findViewById(C0007R$id.assist_hint_right)).updateDarkness(f);
        }
    }

    /* access modifiers changed from: package-private */
    public static class TunablePaddingTagListener implements FragmentHostManager.FragmentListener {
        private final int mId;
        private final int mPadding;
        private TunablePadding mTunablePadding;

        public TunablePaddingTagListener(int i, int i2) {
            this.mPadding = i;
            this.mId = i2;
        }

        @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
        public void onFragmentViewCreated(String str, Fragment fragment) {
            TunablePadding tunablePadding = this.mTunablePadding;
            if (tunablePadding != null) {
                tunablePadding.destroy();
            }
            View view = fragment.getView();
            int i = this.mId;
            if (i != 0) {
                view = view.findViewById(i);
            }
            this.mTunablePadding = TunablePadding.addTunablePadding(view, "sysui_rounded_content_padding", this.mPadding, 3);
        }
    }

    public static class DisplayCutoutView extends View implements DisplayManager.DisplayListener, RegionInterceptingFrameLayout.RegionInterceptableView {
        private final Path mBoundingPath = new Path();
        private final Rect mBoundingRect = new Rect();
        private final List<Rect> mBounds = new ArrayList();
        private int mColor = -16777216;
        private final ScreenDecorations mDecorations;
        private final DisplayInfo mInfo = new DisplayInfo();
        private final boolean mInitialStart;
        private final int[] mLocation = new int[2];
        private final Paint mPaint = new Paint();
        private int mRotation;
        private boolean mStart;
        private final Runnable mVisibilityChangedListener;

        public void onDisplayAdded(int i) {
        }

        public void onDisplayRemoved(int i) {
        }

        public DisplayCutoutView(Context context, boolean z, Runnable runnable, ScreenDecorations screenDecorations) {
            super(context);
            this.mInitialStart = z;
            this.mVisibilityChangedListener = runnable;
            this.mDecorations = screenDecorations;
            setId(C0007R$id.display_cutout);
        }

        public void setColor(int i) {
            this.mColor = i;
            invalidate();
        }

        /* access modifiers changed from: protected */
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            ((DisplayManager) ((View) this).mContext.getSystemService(DisplayManager.class)).registerDisplayListener(this, getHandler());
            update();
        }

        /* access modifiers changed from: protected */
        public void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            ((DisplayManager) ((View) this).mContext.getSystemService(DisplayManager.class)).unregisterDisplayListener(this);
        }

        /* access modifiers changed from: protected */
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            getLocationOnScreen(this.mLocation);
            int[] iArr = this.mLocation;
            canvas.translate((float) (-iArr[0]), (float) (-iArr[1]));
            if (!this.mBoundingPath.isEmpty()) {
                this.mPaint.setColor(this.mColor);
                this.mPaint.setStyle(Paint.Style.FILL);
                this.mPaint.setAntiAlias(true);
                canvas.drawPath(this.mBoundingPath, this.mPaint);
            }
        }

        public void onDisplayChanged(int i) {
            if (i == getDisplay().getDisplayId()) {
                update();
            }
        }

        public void setRotation(int i) {
            this.mRotation = i;
            update();
        }

        private boolean isStart() {
            int i = this.mRotation;
            if (i == 2 || i == 3) {
                return !this.mInitialStart;
            }
            return this.mInitialStart;
        }

        private void update() {
            int i;
            if (isAttachedToWindow() && !this.mDecorations.mPendingRotationChange) {
                this.mStart = isStart();
                requestLayout();
                getDisplay().getDisplayInfo(this.mInfo);
                this.mBounds.clear();
                this.mBoundingRect.setEmpty();
                this.mBoundingPath.reset();
                if (!ScreenDecorations.shouldDrawCutout(getContext()) || !hasCutout()) {
                    i = 8;
                } else {
                    this.mBounds.addAll(this.mInfo.displayCutout.getBoundingRects());
                    localBounds(this.mBoundingRect);
                    updateGravity();
                    updateBoundingPath();
                    invalidate();
                    i = 0;
                }
                if (i != getVisibility()) {
                    setVisibility(i);
                    this.mVisibilityChangedListener.run();
                }
            }
        }

        private void updateBoundingPath() {
            DisplayInfo displayInfo = this.mInfo;
            int i = displayInfo.logicalWidth;
            int i2 = displayInfo.logicalHeight;
            int i3 = displayInfo.rotation;
            boolean z = true;
            if (!(i3 == 1 || i3 == 3)) {
                z = false;
            }
            int i4 = z ? i2 : i;
            if (!z) {
                i = i2;
            }
            this.mBoundingPath.set(DisplayCutout.pathFromResources(getResources(), i4, i));
            Matrix matrix = new Matrix();
            transformPhysicalToLogicalCoordinates(this.mInfo.rotation, i4, i, matrix);
            this.mBoundingPath.transform(matrix);
        }

        private static void transformPhysicalToLogicalCoordinates(int i, int i2, int i3, Matrix matrix) {
            if (i == 0) {
                matrix.reset();
            } else if (i == 1) {
                matrix.setRotate(270.0f);
                matrix.postTranslate(0.0f, (float) i2);
            } else if (i == 2) {
                matrix.setRotate(180.0f);
                matrix.postTranslate((float) i2, (float) i3);
            } else if (i == 3) {
                matrix.setRotate(90.0f);
                matrix.postTranslate((float) i3, 0.0f);
            } else {
                throw new IllegalArgumentException("Unknown rotation: " + i);
            }
        }

        private void updateGravity() {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) layoutParams;
                int gravity = getGravity(this.mInfo.displayCutout);
                if (layoutParams2.gravity != gravity) {
                    layoutParams2.gravity = gravity;
                    setLayoutParams(layoutParams2);
                }
            }
        }

        private boolean hasCutout() {
            DisplayCutout displayCutout = this.mInfo.displayCutout;
            if (displayCutout == null) {
                return false;
            }
            if (this.mStart) {
                if (displayCutout.getSafeInsetLeft() > 0 || displayCutout.getSafeInsetTop() > 0) {
                    return true;
                }
                return false;
            } else if (displayCutout.getSafeInsetRight() > 0 || displayCutout.getSafeInsetBottom() > 0) {
                return true;
            } else {
                return false;
            }
        }

        /* access modifiers changed from: protected */
        public void onMeasure(int i, int i2) {
            if (this.mBounds.isEmpty()) {
                super.onMeasure(i, i2);
            } else {
                setMeasuredDimension(View.resolveSizeAndState(this.mBoundingRect.width(), i, 0), View.resolveSizeAndState(this.mBoundingRect.height(), i2, 0));
            }
        }

        public static void boundsFromDirection(DisplayCutout displayCutout, int i, Rect rect) {
            if (i == 3) {
                rect.set(displayCutout.getBoundingRectLeft());
            } else if (i == 5) {
                rect.set(displayCutout.getBoundingRectRight());
            } else if (i == 48) {
                rect.set(displayCutout.getBoundingRectTop());
            } else if (i != 80) {
                rect.setEmpty();
            } else {
                rect.set(displayCutout.getBoundingRectBottom());
            }
        }

        private void localBounds(Rect rect) {
            DisplayCutout displayCutout = this.mInfo.displayCutout;
            boundsFromDirection(displayCutout, getGravity(displayCutout), rect);
        }

        private int getGravity(DisplayCutout displayCutout) {
            if (this.mStart) {
                if (displayCutout.getSafeInsetLeft() > 0) {
                    return 3;
                }
                if (displayCutout.getSafeInsetTop() > 0) {
                    return 48;
                }
                return 0;
            } else if (displayCutout.getSafeInsetRight() > 0) {
                return 5;
            } else {
                return displayCutout.getSafeInsetBottom() > 0 ? 80 : 0;
            }
        }

        @Override // com.android.systemui.RegionInterceptingFrameLayout.RegionInterceptableView
        public boolean shouldInterceptTouch() {
            return this.mInfo.displayCutout != null && getVisibility() == 0;
        }

        @Override // com.android.systemui.RegionInterceptingFrameLayout.RegionInterceptableView
        public Region getInterceptRegion() {
            if (this.mInfo.displayCutout == null) {
                return null;
            }
            View rootView = getRootView();
            Region rectsToRegion = ScreenDecorations.rectsToRegion(this.mInfo.displayCutout.getBoundingRects());
            rootView.getLocationOnScreen(this.mLocation);
            int[] iArr = this.mLocation;
            rectsToRegion.translate(-iArr[0], -iArr[1]);
            rectsToRegion.op(rootView.getLeft(), rootView.getTop(), rootView.getRight(), rootView.getBottom(), Region.Op.INTERSECT);
            return rectsToRegion;
        }
    }

    private class RestartingPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        private final int mTargetRotation;
        private final View mView;

        private RestartingPreDrawListener(View view, int i) {
            this.mView = view;
            this.mTargetRotation = i;
        }

        public boolean onPreDraw() {
            this.mView.getViewTreeObserver().removeOnPreDrawListener(this);
            if (this.mTargetRotation == ScreenDecorations.this.mRotation) {
                return true;
            }
            ScreenDecorations.this.mPendingRotationChange = false;
            ScreenDecorations.this.updateOrientation();
            this.mView.invalidate();
            return false;
        }
    }

    /* access modifiers changed from: private */
    public class ValidatingPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        private final View mView;

        public ValidatingPreDrawListener(View view) {
            this.mView = view;
        }

        public boolean onPreDraw() {
            if (RotationUtils.getExactRotation(ScreenDecorations.this.mContext) == ScreenDecorations.this.mRotation || ScreenDecorations.this.mPendingRotationChange) {
                return true;
            }
            this.mView.invalidate();
            return false;
        }
    }
}
