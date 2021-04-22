package com.android.systemui.assist.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.metrics.LogMaker;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0010R$layout;
import com.android.systemui.Dependency;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.AssistManager;

public class DefaultUiController implements AssistManager.UiController {
    private boolean mAttached = false;
    private ValueAnimator mInvocationAnimator = new ValueAnimator();
    private boolean mInvocationInProgress = false;
    protected InvocationLightsView mInvocationLightsView;
    private float mLastInvocationProgress = 0.0f;
    private final WindowManager.LayoutParams mLayoutParams;
    private final PathInterpolator mProgressInterpolator = new PathInterpolator(0.83f, 0.0f, 0.84f, 1.0f);
    protected final FrameLayout mRoot;
    private final WindowManager mWindowManager;

    public DefaultUiController(Context context) {
        this.mRoot = new FrameLayout(context);
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mLayoutParams = new WindowManager.LayoutParams(-1, -2, 0, 0, 2024, 808, -3);
        WindowManager.LayoutParams layoutParams = this.mLayoutParams;
        layoutParams.privateFlags = 64;
        layoutParams.gravity = 80;
        layoutParams.setTitle("Assist");
        this.mInvocationLightsView = (InvocationLightsView) LayoutInflater.from(context).inflate(C0010R$layout.invocation_lights, (ViewGroup) this.mRoot, false);
        this.mRoot.addView(this.mInvocationLightsView);
    }

    @Override // com.android.systemui.assist.AssistManager.UiController
    public void onInvocationProgress(int i, float f) {
        boolean z = this.mInvocationInProgress;
        if (f == 1.0f) {
            animateInvocationCompletion(i, 0.0f);
        } else if (f == 0.0f) {
            hide();
        } else {
            if (!z) {
                attach();
                this.mInvocationInProgress = true;
                updateAssistHandleVisibility();
            }
            setProgressInternal(i, f);
        }
        this.mLastInvocationProgress = f;
        logInvocationProgressMetrics(i, f, z);
    }

    @Override // com.android.systemui.assist.AssistManager.UiController
    public void onGestureCompletion(float f) {
        animateInvocationCompletion(1, f);
    }

    public void hide() {
        ((AssistManager) Dependency.get(AssistManager.class)).hideAssist();
        detach();
        if (this.mInvocationAnimator.isRunning()) {
            this.mInvocationAnimator.cancel();
        }
        this.mInvocationLightsView.hide();
        this.mInvocationInProgress = false;
        updateAssistHandleVisibility();
    }

    protected static void logInvocationProgressMetrics(int i, float f, boolean z) {
        if (!z && f > 0.0f) {
            MetricsLogger.action(new LogMaker(1716).setType(4).setSubtype(((AssistManager) Dependency.get(AssistManager.class)).toLoggingSubType(i)));
        }
        if (z && f == 0.0f) {
            MetricsLogger.action(new LogMaker(1716).setType(5).setSubtype(1));
        }
    }

    private void updateAssistHandleVisibility() {
        ((ScreenDecorations) SysUiServiceProvider.getComponent(this.mRoot.getContext(), ScreenDecorations.class)).lambda$setAssistHintBlocked$2$ScreenDecorations(this.mInvocationInProgress);
    }

    private void attach() {
        if (!this.mAttached) {
            this.mWindowManager.addView(this.mRoot, this.mLayoutParams);
            this.mAttached = true;
        }
    }

    private void detach() {
        if (this.mAttached) {
            this.mWindowManager.removeViewImmediate(this.mRoot);
            this.mAttached = false;
        }
    }

    private void setProgressInternal(int i, float f) {
        this.mInvocationLightsView.onInvocationProgress(this.mProgressInterpolator.getInterpolation(f));
    }

    private void animateInvocationCompletion(int i, float f) {
        this.mInvocationAnimator = ValueAnimator.ofFloat(this.mLastInvocationProgress, 1.0f);
        this.mInvocationAnimator.setStartDelay(1);
        this.mInvocationAnimator.setDuration(200L);
        this.mInvocationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(i) {
            /* class com.android.systemui.assist.ui.$$Lambda$DefaultUiController$DsyFMixn8vpgo7pkqARg9d_ZEVw */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                DefaultUiController.this.lambda$animateInvocationCompletion$0$DefaultUiController(this.f$1, valueAnimator);
            }
        });
        this.mInvocationAnimator.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.assist.ui.DefaultUiController.AnonymousClass1 */

            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                DefaultUiController.this.mInvocationInProgress = false;
                DefaultUiController.this.mLastInvocationProgress = 0.0f;
                DefaultUiController.this.hide();
            }
        });
        this.mInvocationAnimator.start();
    }

    public /* synthetic */ void lambda$animateInvocationCompletion$0$DefaultUiController(int i, ValueAnimator valueAnimator) {
        setProgressInternal(i, ((Float) valueAnimator.getAnimatedValue()).floatValue());
    }
}
