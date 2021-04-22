package com.google.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0015R$style;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.statusbar.phone.ButtonInterface;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.android.systemui.statusbar.policy.KeyButtonView;
import java.util.ArrayList;

public class OpaLayout extends FrameLayout implements ButtonInterface {
    static final Interpolator INTERPOLATOR_40_40 = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    static final Interpolator INTERPOLATOR_40_OUT = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    private final Interpolator HOME_DISAPPEAR_INTERPOLATOR;
    private final ArrayList<View> mAnimatedViews;
    private int mAnimationState;
    private View mBlue;
    private View mBottom;
    private final Runnable mCheckLongPress;
    private final Interpolator mCollapseInterpolator;
    private final ArraySet<Animator> mCurrentAnimators;
    private boolean mDelayTouchFeedback;
    private final Runnable mDiamondAnimation;
    private boolean mDiamondAnimationDelayed;
    private final Interpolator mDiamondInterpolator;
    private final Interpolator mDotsFullSizeInterpolator;
    private final Interpolator mFastOutSlowInInterpolator;
    private View mGreen;
    private ImageView mHalo;
    private int mHaloDiameter;
    private KeyButtonView mHome;
    private final Interpolator mHomeDisappearInterpolator;
    private boolean mIsPressed;
    private boolean mIsVertical;
    private View mLeft;
    private boolean mLongClicked;
    private boolean mOpaEnabled;
    private boolean mOpaEnabledNeedsUpdate;
    private final OverviewProxyService.OverviewProxyListener mOverviewProxyListener;
    private OverviewProxyService mOverviewProxyService;
    private View mRed;
    private Resources mResources;
    private final Runnable mRetract;
    private final Interpolator mRetractInterpolator;
    private View mRight;
    private int mScrollTouchSlop;
    private long mStartTime;
    private View mTop;
    private int mTouchDownX;
    private int mTouchDownY;
    private ImageView mWhite;
    private ImageView mWhiteCutout;
    private boolean mWindowVisible;
    private View mYellow;

    public OpaLayout(Context context) {
        this(context, null);
    }

    public OpaLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpaLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mFastOutSlowInInterpolator = Interpolators.FAST_OUT_SLOW_IN;
        this.mHomeDisappearInterpolator = new PathInterpolator(0.65f, 0.0f, 1.0f, 1.0f);
        this.mCollapseInterpolator = Interpolators.FAST_OUT_LINEAR_IN;
        this.mDotsFullSizeInterpolator = new PathInterpolator(0.4f, 0.0f, 0.0f, 1.0f);
        this.mRetractInterpolator = new PathInterpolator(0.4f, 0.0f, 0.0f, 1.0f);
        this.mDiamondInterpolator = new PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f);
        this.HOME_DISAPPEAR_INTERPOLATOR = new PathInterpolator(0.65f, 0.0f, 1.0f, 1.0f);
        this.mCurrentAnimators = new ArraySet<>();
        this.mAnimatedViews = new ArrayList<>();
        this.mAnimationState = 0;
        this.mRetract = new Runnable() {
            /* class com.google.android.systemui.OpaLayout.AnonymousClass1 */

            public void run() {
                OpaLayout.this.cancelCurrentAnimation();
                OpaLayout.this.startRetractAnimation();
            }
        };
        this.mCheckLongPress = new Runnable() {
            /* class com.google.android.systemui.OpaLayout.AnonymousClass2 */

            public void run() {
                if (OpaLayout.this.mIsPressed) {
                    OpaLayout.this.mLongClicked = true;
                }
            }
        };
        this.mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() {
            /* class com.google.android.systemui.OpaLayout.AnonymousClass3 */

            @Override // com.android.systemui.recents.OverviewProxyService.OverviewProxyListener
            public void onConnectionChanged(boolean z) {
                OpaLayout.this.updateOpaLayout();
            }
        };
        this.mDiamondAnimation = new Runnable() {
            /* class com.google.android.systemui.$$Lambda$OpaLayout$4_BG8NBMX8f4CM36AHgbLewodcE */

            public final void run() {
                OpaLayout.this.lambda$new$1$OpaLayout();
            }
        };
        this.mScrollTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public OpaLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mResources = getResources();
        this.mBlue = findViewById(C0007R$id.blue);
        this.mRed = findViewById(C0007R$id.red);
        this.mYellow = findViewById(C0007R$id.yellow);
        this.mGreen = findViewById(C0007R$id.green);
        this.mWhite = (ImageView) findViewById(C0007R$id.white);
        this.mWhiteCutout = (ImageView) findViewById(C0007R$id.white_cutout);
        this.mHalo = (ImageView) findViewById(C0007R$id.halo);
        this.mHome = (KeyButtonView) findViewById(C0007R$id.home_button);
        this.mHalo.setImageDrawable(KeyButtonDrawable.create(new ContextThemeWrapper(getContext(), C0015R$style.DualToneLightTheme), new ContextThemeWrapper(getContext(), C0015R$style.DualToneDarkTheme), C0006R$drawable.halo, false, null));
        this.mHaloDiameter = this.mResources.getDimensionPixelSize(C0005R$dimen.halo_diameter);
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        this.mWhiteCutout.setLayerType(2, paint);
        this.mAnimatedViews.add(this.mBlue);
        this.mAnimatedViews.add(this.mRed);
        this.mAnimatedViews.add(this.mYellow);
        this.mAnimatedViews.add(this.mGreen);
        this.mAnimatedViews.add(this.mWhite);
        this.mAnimatedViews.add(this.mWhiteCutout);
        this.mAnimatedViews.add(this.mHalo);
        this.mOpaEnabledNeedsUpdate = true;
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        setOpaEnabled(UserSettingsUtils.load(getContext().getContentResolver()));
    }

    public void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        this.mWindowVisible = i == 0;
        if (i == 0) {
            updateOpaLayout();
            return;
        }
        cancelCurrentAnimation();
        skipToStartingValue();
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.mHome.setOnLongClickListener(new View.OnLongClickListener(onLongClickListener) {
            /* class com.google.android.systemui.$$Lambda$OpaLayout$o_3Yb82bT_aVEO6XXM_b1dhZ9o */
            private final /* synthetic */ View.OnLongClickListener f$1;

            {
                this.f$1 = r2;
            }

            public final boolean onLongClick(View view) {
                return OpaLayout.this.lambda$setOnLongClickListener$0$OpaLayout(this.f$1, view);
            }
        });
    }

    public /* synthetic */ boolean lambda$setOnLongClickListener$0$OpaLayout(View.OnLongClickListener onLongClickListener, View view) {
        return onLongClickListener.onLongClick(this.mHome);
    }

    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        this.mHome.setOnTouchListener(onTouchListener);
    }

    public /* synthetic */ void lambda$new$1$OpaLayout() {
        if (this.mCurrentAnimators.isEmpty()) {
            startDiamondAnimation();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:9:0x001a, code lost:
        if (r0 != 3) goto L_0x010a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onInterceptTouchEvent(android.view.MotionEvent r9) {
        /*
        // Method dump skipped, instructions count: 267
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.systemui.OpaLayout.onInterceptTouchEvent(android.view.MotionEvent):boolean");
    }

    public void setAccessibilityDelegate(View.AccessibilityDelegate accessibilityDelegate) {
        super.setAccessibilityDelegate(accessibilityDelegate);
        this.mHome.setAccessibilityDelegate(accessibilityDelegate);
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setImageDrawable(Drawable drawable) {
        this.mWhite.setImageDrawable(drawable);
        this.mWhiteCutout.setImageDrawable(drawable);
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void abortCurrentGesture() {
        Log.w("OpaLayout", "***Called abortCurrentGesture");
        this.mHome.abortCurrentGesture();
        this.mIsPressed = false;
        this.mLongClicked = false;
        this.mDiamondAnimationDelayed = false;
        removeCallbacks(this.mDiamondAnimation);
        removeCallbacks(this.mCheckLongPress);
        int i = this.mAnimationState;
        if (i == 3 || i == 1) {
            this.mRetract.run();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateOpaLayout();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
        updateOpaLayout();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
    }

    private void startDiamondAnimation() {
        if (allowAnimations()) {
            this.mCurrentAnimators.clear();
            setDotsVisible();
            this.mCurrentAnimators.addAll((ArraySet<? extends Animator>) getDiamondAnimatorSet());
            this.mAnimationState = 1;
            startAll(this.mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startRetractAnimation() {
        if (allowAnimations()) {
            this.mCurrentAnimators.clear();
            this.mCurrentAnimators.addAll((ArraySet<? extends Animator>) getRetractAnimatorSet());
            this.mAnimationState = 2;
            startAll(this.mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startLineAnimation() {
        if (allowAnimations()) {
            this.mCurrentAnimators.clear();
            this.mCurrentAnimators.addAll((ArraySet<? extends Animator>) getLineAnimatorSet());
            this.mAnimationState = 3;
            startAll(this.mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startCollapseAnimation() {
        if (allowAnimations()) {
            this.mCurrentAnimators.clear();
            this.mCurrentAnimators.addAll((ArraySet<? extends Animator>) getCollapseAnimatorSet());
            this.mAnimationState = 3;
            startAll(this.mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private Animator getScaleAnimatorX(View view, float f, int i, Interpolator interpolator) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(3, f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration((long) i);
        return renderNodeAnimator;
    }

    private Animator getScaleAnimatorY(View view, float f, int i, Interpolator interpolator) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(4, f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration((long) i);
        return renderNodeAnimator;
    }

    private Animator getDeltaAnimatorX(View view, Interpolator interpolator, float f, int i) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(8, view.getX() + f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration((long) i);
        return renderNodeAnimator;
    }

    private Animator getDeltaAnimatorY(View view, Interpolator interpolator, float f, int i) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(9, view.getY() + f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration((long) i);
        return renderNodeAnimator;
    }

    private Animator getTranslationAnimatorX(View view, Interpolator interpolator, int i) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(0, 0.0f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration((long) i);
        return renderNodeAnimator;
    }

    private Animator getTranslationAnimatorY(View view, Interpolator interpolator, int i) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(1, 0.0f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration((long) i);
        return renderNodeAnimator;
    }

    private Animator getAlphaAnimator(View view, float f, int i, Interpolator interpolator) {
        return getAlphaAnimator(view, f, i, 0, interpolator);
    }

    private Animator getAlphaAnimator(View view, float f, int i, int i2, Interpolator interpolator) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(11, f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration((long) i);
        renderNodeAnimator.setStartDelay((long) i2);
        return renderNodeAnimator;
    }

    private void startAll(ArraySet<Animator> arraySet) {
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            arraySet.valueAt(size).start();
        }
    }

    private boolean allowAnimations() {
        return isAttachedToWindow() && this.mWindowVisible;
    }

    private float getPxVal(int i) {
        return (float) getResources().getDimensionPixelOffset(i);
    }

    private ArraySet<Animator> getDiamondAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        arraySet.add(getDeltaAnimatorY(this.mTop, this.mDiamondInterpolator, -getPxVal(C0005R$dimen.opa_diamond_translation), 200));
        arraySet.add(getScaleAnimatorX(this.mTop, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mTop, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getAlphaAnimator(this.mTop, 1.0f, 50, Interpolators.LINEAR));
        arraySet.add(getDeltaAnimatorY(this.mBottom, this.mDiamondInterpolator, getPxVal(C0005R$dimen.opa_diamond_translation), 200));
        arraySet.add(getScaleAnimatorX(this.mBottom, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mBottom, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getAlphaAnimator(this.mBottom, 1.0f, 50, Interpolators.LINEAR));
        arraySet.add(getDeltaAnimatorX(this.mLeft, this.mDiamondInterpolator, -getPxVal(C0005R$dimen.opa_diamond_translation), 200));
        arraySet.add(getScaleAnimatorX(this.mLeft, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mLeft, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getAlphaAnimator(this.mLeft, 1.0f, 50, Interpolators.LINEAR));
        arraySet.add(getDeltaAnimatorX(this.mRight, this.mDiamondInterpolator, getPxVal(C0005R$dimen.opa_diamond_translation), 200));
        arraySet.add(getScaleAnimatorX(this.mRight, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mRight, 0.8f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getAlphaAnimator(this.mRight, 1.0f, 50, Interpolators.LINEAR));
        arraySet.add(getScaleAnimatorX(this.mWhite, 0.625f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mWhite, 0.625f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorX(this.mWhiteCutout, 0.625f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mWhiteCutout, 0.625f, 200, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorX(this.mHalo, 0.47619048f, 100, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mHalo, 0.47619048f, 100, this.mFastOutSlowInInterpolator));
        arraySet.add(getAlphaAnimator(this.mHalo, 0.0f, 100, this.mFastOutSlowInInterpolator));
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            /* class com.google.android.systemui.OpaLayout.AnonymousClass4 */

            public void onAnimationCancel(Animator animator) {
                OpaLayout.this.mCurrentAnimators.clear();
            }

            public void onAnimationEnd(Animator animator) {
                OpaLayout.this.startLineAnimation();
            }
        });
        return arraySet;
    }

    private ArraySet<Animator> getRetractAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        arraySet.add(getTranslationAnimatorX(this.mRed, this.mRetractInterpolator, 300));
        arraySet.add(getTranslationAnimatorY(this.mRed, this.mRetractInterpolator, 300));
        arraySet.add(getScaleAnimatorX(this.mRed, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getScaleAnimatorY(this.mRed, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getAlphaAnimator(this.mRed, 0.0f, 50, 50, Interpolators.LINEAR));
        arraySet.add(getTranslationAnimatorX(this.mBlue, this.mRetractInterpolator, 300));
        arraySet.add(getTranslationAnimatorY(this.mBlue, this.mRetractInterpolator, 300));
        arraySet.add(getScaleAnimatorX(this.mBlue, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getScaleAnimatorY(this.mBlue, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getAlphaAnimator(this.mBlue, 0.0f, 50, 50, Interpolators.LINEAR));
        arraySet.add(getTranslationAnimatorX(this.mGreen, this.mRetractInterpolator, 300));
        arraySet.add(getTranslationAnimatorY(this.mGreen, this.mRetractInterpolator, 300));
        arraySet.add(getScaleAnimatorX(this.mGreen, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getScaleAnimatorY(this.mGreen, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getAlphaAnimator(this.mGreen, 0.0f, 50, 50, Interpolators.LINEAR));
        arraySet.add(getTranslationAnimatorX(this.mYellow, this.mRetractInterpolator, 300));
        arraySet.add(getTranslationAnimatorY(this.mYellow, this.mRetractInterpolator, 300));
        arraySet.add(getScaleAnimatorX(this.mYellow, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getScaleAnimatorY(this.mYellow, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getAlphaAnimator(this.mYellow, 0.0f, 50, 50, Interpolators.LINEAR));
        arraySet.add(getScaleAnimatorX(this.mWhite, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getScaleAnimatorY(this.mWhite, 1.0f, 300, this.mRetractInterpolator));
        arraySet.add(getScaleAnimatorX(this.mWhiteCutout, 1.0f, 300, INTERPOLATOR_40_OUT));
        arraySet.add(getScaleAnimatorY(this.mWhiteCutout, 1.0f, 300, INTERPOLATOR_40_OUT));
        arraySet.add(getScaleAnimatorX(this.mHalo, 1.0f, 300, this.mFastOutSlowInInterpolator));
        arraySet.add(getScaleAnimatorY(this.mHalo, 1.0f, 300, this.mFastOutSlowInInterpolator));
        arraySet.add(getAlphaAnimator(this.mHalo, 1.0f, 300, this.mFastOutSlowInInterpolator));
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            /* class com.google.android.systemui.OpaLayout.AnonymousClass5 */

            public void onAnimationEnd(Animator animator) {
                OpaLayout.this.mCurrentAnimators.clear();
                OpaLayout.this.skipToStartingValue();
            }
        });
        return arraySet;
    }

    private ArraySet<Animator> getCollapseAnimatorSet() {
        Animator animator;
        Animator animator2;
        Animator animator3;
        Animator animator4;
        ArraySet<Animator> arraySet = new ArraySet<>();
        if (this.mIsVertical) {
            animator = getTranslationAnimatorY(this.mRed, INTERPOLATOR_40_OUT, 133);
        } else {
            animator = getTranslationAnimatorX(this.mRed, INTERPOLATOR_40_OUT, 133);
        }
        arraySet.add(animator);
        arraySet.add(getScaleAnimatorX(this.mRed, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getScaleAnimatorY(this.mRed, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getAlphaAnimator(this.mRed, 0.0f, 50, 33, Interpolators.LINEAR));
        if (this.mIsVertical) {
            animator2 = getTranslationAnimatorY(this.mBlue, INTERPOLATOR_40_OUT, 150);
        } else {
            animator2 = getTranslationAnimatorX(this.mBlue, INTERPOLATOR_40_OUT, 150);
        }
        arraySet.add(animator2);
        arraySet.add(getScaleAnimatorX(this.mBlue, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getScaleAnimatorY(this.mBlue, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getAlphaAnimator(this.mBlue, 0.0f, 50, 33, Interpolators.LINEAR));
        if (this.mIsVertical) {
            animator3 = getTranslationAnimatorY(this.mYellow, INTERPOLATOR_40_OUT, 133);
        } else {
            animator3 = getTranslationAnimatorX(this.mYellow, INTERPOLATOR_40_OUT, 133);
        }
        arraySet.add(animator3);
        arraySet.add(getScaleAnimatorX(this.mYellow, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getScaleAnimatorY(this.mYellow, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getAlphaAnimator(this.mYellow, 0.0f, 50, 33, Interpolators.LINEAR));
        if (this.mIsVertical) {
            animator4 = getTranslationAnimatorY(this.mGreen, INTERPOLATOR_40_OUT, 150);
        } else {
            animator4 = getTranslationAnimatorX(this.mGreen, INTERPOLATOR_40_OUT, 150);
        }
        arraySet.add(animator4);
        arraySet.add(getScaleAnimatorX(this.mGreen, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getScaleAnimatorY(this.mGreen, 1.0f, 200, INTERPOLATOR_40_OUT));
        arraySet.add(getAlphaAnimator(this.mGreen, 0.0f, 50, 33, Interpolators.LINEAR));
        Animator scaleAnimatorX = getScaleAnimatorX(this.mWhite, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator scaleAnimatorY = getScaleAnimatorY(this.mWhite, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator scaleAnimatorX2 = getScaleAnimatorX(this.mWhiteCutout, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator scaleAnimatorY2 = getScaleAnimatorY(this.mWhiteCutout, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator scaleAnimatorX3 = getScaleAnimatorX(this.mHalo, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator scaleAnimatorY3 = getScaleAnimatorY(this.mHalo, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator alphaAnimator = getAlphaAnimator(this.mHalo, 1.0f, 150, this.mFastOutSlowInInterpolator);
        scaleAnimatorX.setStartDelay(33);
        scaleAnimatorY.setStartDelay(33);
        scaleAnimatorX2.setStartDelay(33);
        scaleAnimatorY2.setStartDelay(33);
        scaleAnimatorX3.setStartDelay(33);
        scaleAnimatorY3.setStartDelay(33);
        alphaAnimator.setStartDelay(33);
        arraySet.add(scaleAnimatorX);
        arraySet.add(scaleAnimatorY);
        arraySet.add(scaleAnimatorX2);
        arraySet.add(scaleAnimatorY2);
        arraySet.add(scaleAnimatorX3);
        arraySet.add(scaleAnimatorY3);
        arraySet.add(alphaAnimator);
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            /* class com.google.android.systemui.OpaLayout.AnonymousClass6 */

            public void onAnimationEnd(Animator animator) {
                OpaLayout.this.mCurrentAnimators.clear();
                OpaLayout.this.skipToStartingValue();
            }
        });
        return arraySet;
    }

    private ArraySet<Animator> getLineAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        if (this.mIsVertical) {
            arraySet.add(getDeltaAnimatorY(this.mRed, this.mFastOutSlowInInterpolator, getPxVal(C0005R$dimen.opa_line_x_trans_ry), 225));
            arraySet.add(getDeltaAnimatorX(this.mRed, this.mFastOutSlowInInterpolator, getPxVal(C0005R$dimen.opa_line_y_translation), 133));
            arraySet.add(getDeltaAnimatorY(this.mBlue, this.mFastOutSlowInInterpolator, getPxVal(C0005R$dimen.opa_line_x_trans_bg), 225));
            arraySet.add(getDeltaAnimatorY(this.mYellow, this.mFastOutSlowInInterpolator, -getPxVal(C0005R$dimen.opa_line_x_trans_ry), 225));
            arraySet.add(getDeltaAnimatorX(this.mYellow, this.mFastOutSlowInInterpolator, -getPxVal(C0005R$dimen.opa_line_y_translation), 133));
            arraySet.add(getDeltaAnimatorY(this.mGreen, this.mFastOutSlowInInterpolator, -getPxVal(C0005R$dimen.opa_line_x_trans_bg), 225));
        } else {
            arraySet.add(getDeltaAnimatorX(this.mRed, this.mFastOutSlowInInterpolator, -getPxVal(C0005R$dimen.opa_line_x_trans_ry), 225));
            arraySet.add(getDeltaAnimatorY(this.mRed, this.mFastOutSlowInInterpolator, getPxVal(C0005R$dimen.opa_line_y_translation), 133));
            arraySet.add(getDeltaAnimatorX(this.mBlue, this.mFastOutSlowInInterpolator, -getPxVal(C0005R$dimen.opa_line_x_trans_bg), 225));
            arraySet.add(getDeltaAnimatorX(this.mYellow, this.mFastOutSlowInInterpolator, getPxVal(C0005R$dimen.opa_line_x_trans_ry), 225));
            arraySet.add(getDeltaAnimatorY(this.mYellow, this.mFastOutSlowInInterpolator, -getPxVal(C0005R$dimen.opa_line_y_translation), 133));
            arraySet.add(getDeltaAnimatorX(this.mGreen, this.mFastOutSlowInInterpolator, getPxVal(C0005R$dimen.opa_line_x_trans_bg), 225));
        }
        arraySet.add(getScaleAnimatorX(this.mWhite, 0.0f, 83, this.mHomeDisappearInterpolator));
        arraySet.add(getScaleAnimatorY(this.mWhite, 0.0f, 83, this.mHomeDisappearInterpolator));
        arraySet.add(getScaleAnimatorX(this.mWhiteCutout, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        arraySet.add(getScaleAnimatorY(this.mWhiteCutout, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        arraySet.add(getScaleAnimatorX(this.mHalo, 0.0f, 83, this.mHomeDisappearInterpolator));
        arraySet.add(getScaleAnimatorY(this.mHalo, 0.0f, 83, this.mHomeDisappearInterpolator));
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            /* class com.google.android.systemui.OpaLayout.AnonymousClass7 */

            public void onAnimationEnd(Animator animator) {
                OpaLayout.this.startCollapseAnimation();
            }

            public void onAnimationCancel(Animator animator) {
                OpaLayout.this.mCurrentAnimators.clear();
            }
        });
        return arraySet;
    }

    public void setOpaEnabled(boolean z) {
        Log.i("OpaLayout", "Setting opa enabled to " + z);
        this.mOpaEnabled = z;
        this.mOpaEnabledNeedsUpdate = false;
        updateOpaLayout();
    }

    public void updateOpaLayout() {
        int i;
        boolean shouldShowSwipeUpUI = this.mOverviewProxyService.shouldShowSwipeUpUI();
        int i2 = 0;
        boolean z = this.mOpaEnabled && !shouldShowSwipeUpUI;
        ImageView imageView = this.mHalo;
        if (!z) {
            i2 = 4;
        }
        imageView.setVisibility(i2);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mWhite.getLayoutParams();
        int i3 = -1;
        if (shouldShowSwipeUpUI) {
            i = -1;
        } else {
            i = this.mHaloDiameter;
        }
        layoutParams.width = i;
        if (!shouldShowSwipeUpUI) {
            i3 = this.mHaloDiameter;
        }
        layoutParams.height = i3;
        this.mWhite.setLayoutParams(layoutParams);
        this.mWhiteCutout.setLayoutParams(layoutParams);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void cancelCurrentAnimation() {
        if (!this.mCurrentAnimators.isEmpty()) {
            for (int size = this.mCurrentAnimators.size() - 1; size >= 0; size--) {
                Animator valueAt = this.mCurrentAnimators.valueAt(size);
                valueAt.removeAllListeners();
                valueAt.cancel();
            }
            this.mCurrentAnimators.clear();
            this.mAnimationState = 0;
        }
    }

    private void endCurrentAnimation() {
        if (!this.mCurrentAnimators.isEmpty()) {
            for (int size = this.mCurrentAnimators.size() - 1; size >= 0; size--) {
                Animator valueAt = this.mCurrentAnimators.valueAt(size);
                valueAt.removeAllListeners();
                valueAt.end();
            }
            this.mCurrentAnimators.clear();
        }
        this.mAnimationState = 0;
    }

    private Animator getLongestAnim(ArraySet<Animator> arraySet) {
        long j = Long.MIN_VALUE;
        Animator animator = null;
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            Animator valueAt = arraySet.valueAt(size);
            if (valueAt.getTotalDuration() > j) {
                j = valueAt.getTotalDuration();
                animator = valueAt;
            }
        }
        return animator;
    }

    private void setDotsVisible() {
        int size = this.mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            this.mAnimatedViews.get(i).setAlpha(1.0f);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void skipToStartingValue() {
        int size = this.mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            View view = this.mAnimatedViews.get(i);
            view.setScaleY(1.0f);
            view.setScaleX(1.0f);
            view.setTranslationY(0.0f);
            view.setTranslationX(0.0f);
            view.setAlpha(0.0f);
        }
        this.mHalo.setAlpha(1.0f);
        this.mWhite.setAlpha(1.0f);
        this.mWhiteCutout.setAlpha(1.0f);
        this.mAnimationState = 0;
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setVertical(boolean z) {
        this.mIsVertical = z;
        this.mHome.setVertical(z);
        if (this.mIsVertical) {
            this.mTop = this.mGreen;
            this.mBottom = this.mBlue;
            this.mRight = this.mYellow;
            this.mLeft = this.mRed;
            return;
        }
        this.mTop = this.mRed;
        this.mBottom = this.mYellow;
        this.mLeft = this.mBlue;
        this.mRight = this.mGreen;
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setDarkIntensity(float f) {
        if (this.mWhite.getDrawable() instanceof KeyButtonDrawable) {
            ((KeyButtonDrawable) this.mWhite.getDrawable()).setDarkIntensity(f);
        }
        ((KeyButtonDrawable) this.mHalo.getDrawable()).setDarkIntensity(f);
        this.mWhite.invalidate();
        this.mHalo.invalidate();
        this.mHome.setDarkIntensity(f);
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setDelayTouchFeedback(boolean z) {
        this.mHome.setDelayTouchFeedback(z);
        this.mDelayTouchFeedback = z;
    }
}
