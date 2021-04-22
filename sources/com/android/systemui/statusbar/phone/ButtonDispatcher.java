package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import java.util.ArrayList;

public class ButtonDispatcher {
    private View.AccessibilityDelegate mAccessibilityDelegate;
    private Float mAlpha;
    private final ValueAnimator.AnimatorUpdateListener mAlphaListener = new ValueAnimator.AnimatorUpdateListener() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$ButtonDispatcher$YQ5xchhAskLzgLUT3UrgvCxrRAQ */

        public final void onAnimationUpdate(ValueAnimator valueAnimator) {
            ButtonDispatcher.this.lambda$new$0$ButtonDispatcher(valueAnimator);
        }
    };
    private View.OnClickListener mClickListener;
    private View mCurrentView;
    private Float mDarkIntensity;
    private Boolean mDelayTouchFeedback;
    private ValueAnimator mFadeAnimator;
    private final AnimatorListenerAdapter mFadeListener = new AnimatorListenerAdapter() {
        /* class com.android.systemui.statusbar.phone.ButtonDispatcher.AnonymousClass1 */

        public void onAnimationEnd(Animator animator) {
            ButtonDispatcher.this.mFadeAnimator = null;
            ButtonDispatcher buttonDispatcher = ButtonDispatcher.this;
            buttonDispatcher.setVisibility(buttonDispatcher.getAlpha() == 1.0f ? 0 : 4);
        }
    };
    private final int mId;
    private KeyButtonDrawable mImageDrawable;
    private View.OnLongClickListener mLongClickListener;
    private Boolean mLongClickable;
    private View.OnHoverListener mOnHoverListener;
    private View.OnTouchListener mTouchListener;
    private boolean mVertical;
    private final ArrayList<View> mViews = new ArrayList<>();
    private Integer mVisibility = 0;

    /* access modifiers changed from: protected */
    public void onDestroy() {
    }

    public /* synthetic */ void lambda$new$0$ButtonDispatcher(ValueAnimator valueAnimator) {
        setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    public ButtonDispatcher(int i) {
        this.mId = i;
    }

    /* access modifiers changed from: package-private */
    public void clear() {
        this.mViews.clear();
    }

    /* access modifiers changed from: package-private */
    public void addView(View view) {
        this.mViews.add(view);
        view.setOnClickListener(this.mClickListener);
        view.setOnTouchListener(this.mTouchListener);
        view.setOnLongClickListener(this.mLongClickListener);
        view.setOnHoverListener(this.mOnHoverListener);
        Boolean bool = this.mLongClickable;
        if (bool != null) {
            view.setLongClickable(bool.booleanValue());
        }
        Float f = this.mAlpha;
        if (f != null) {
            view.setAlpha(f.floatValue());
        }
        Integer num = this.mVisibility;
        if (num != null) {
            view.setVisibility(num.intValue());
        }
        View.AccessibilityDelegate accessibilityDelegate = this.mAccessibilityDelegate;
        if (accessibilityDelegate != null) {
            view.setAccessibilityDelegate(accessibilityDelegate);
        }
        if (view instanceof ButtonInterface) {
            ButtonInterface buttonInterface = (ButtonInterface) view;
            Float f2 = this.mDarkIntensity;
            if (f2 != null) {
                buttonInterface.setDarkIntensity(f2.floatValue());
            }
            KeyButtonDrawable keyButtonDrawable = this.mImageDrawable;
            if (keyButtonDrawable != null) {
                buttonInterface.setImageDrawable(keyButtonDrawable);
            }
            Boolean bool2 = this.mDelayTouchFeedback;
            if (bool2 != null) {
                buttonInterface.setDelayTouchFeedback(bool2.booleanValue());
            }
            buttonInterface.setVertical(this.mVertical);
        }
    }

    public int getId() {
        return this.mId;
    }

    public int getVisibility() {
        Integer num = this.mVisibility;
        if (num != null) {
            return num.intValue();
        }
        return 0;
    }

    public boolean isVisible() {
        return getVisibility() == 0;
    }

    public float getAlpha() {
        Float f = this.mAlpha;
        if (f != null) {
            return f.floatValue();
        }
        return 1.0f;
    }

    public KeyButtonDrawable getImageDrawable() {
        return this.mImageDrawable;
    }

    public void setImageDrawable(KeyButtonDrawable keyButtonDrawable) {
        this.mImageDrawable = keyButtonDrawable;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            if (this.mViews.get(i) instanceof ButtonInterface) {
                ((ButtonInterface) this.mViews.get(i)).setImageDrawable(this.mImageDrawable);
            }
        }
        KeyButtonDrawable keyButtonDrawable2 = this.mImageDrawable;
        if (keyButtonDrawable2 != null) {
            keyButtonDrawable2.setCallback(this.mCurrentView);
        }
    }

    public void setVisibility(int i) {
        if (this.mVisibility.intValue() != i) {
            ValueAnimator valueAnimator = this.mFadeAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            this.mVisibility = Integer.valueOf(i);
            int size = this.mViews.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mViews.get(i2).setVisibility(this.mVisibility.intValue());
            }
        }
    }

    public void abortCurrentGesture() {
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            if (this.mViews.get(i) instanceof ButtonInterface) {
                ((ButtonInterface) this.mViews.get(i)).abortCurrentGesture();
            }
        }
    }

    public void setAlpha(float f) {
        setAlpha(f, false);
    }

    public void setAlpha(float f, boolean z) {
        setAlpha(f, z, getAlpha() < f ? 150 : 100);
    }

    public void setAlpha(float f, boolean z, long j) {
        if (z) {
            ValueAnimator valueAnimator = this.mFadeAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            setVisibility(0);
            this.mFadeAnimator = ValueAnimator.ofFloat(getAlpha(), f);
            this.mFadeAnimator.setDuration(j);
            this.mFadeAnimator.setInterpolator(getAlpha() < f ? Interpolators.ALPHA_IN : Interpolators.ALPHA_OUT);
            this.mFadeAnimator.addListener(this.mFadeListener);
            this.mFadeAnimator.addUpdateListener(this.mAlphaListener);
            this.mFadeAnimator.start();
            return;
        }
        this.mAlpha = Float.valueOf(f);
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setAlpha(f);
        }
    }

    public void setDarkIntensity(float f) {
        this.mDarkIntensity = Float.valueOf(f);
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            if (this.mViews.get(i) instanceof ButtonInterface) {
                ((ButtonInterface) this.mViews.get(i)).setDarkIntensity(f);
            }
        }
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mClickListener = onClickListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnClickListener(this.mClickListener);
        }
    }

    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        this.mTouchListener = onTouchListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnTouchListener(this.mTouchListener);
        }
    }

    public void setLongClickable(boolean z) {
        this.mLongClickable = Boolean.valueOf(z);
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setLongClickable(this.mLongClickable.booleanValue());
        }
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.mLongClickListener = onLongClickListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnLongClickListener(this.mLongClickListener);
        }
    }

    public void setOnHoverListener(View.OnHoverListener onHoverListener) {
        this.mOnHoverListener = onHoverListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnHoverListener(this.mOnHoverListener);
        }
    }

    public void setAccessibilityDelegate(View.AccessibilityDelegate accessibilityDelegate) {
        this.mAccessibilityDelegate = accessibilityDelegate;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setAccessibilityDelegate(accessibilityDelegate);
        }
    }

    public ArrayList<View> getViews() {
        return this.mViews;
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public void setCurrentView(View view) {
        this.mCurrentView = view.findViewById(this.mId);
        KeyButtonDrawable keyButtonDrawable = this.mImageDrawable;
        if (keyButtonDrawable != null) {
            keyButtonDrawable.setCallback(this.mCurrentView);
        }
        View view2 = this.mCurrentView;
        if (view2 != null) {
            view2.setTranslationX(0.0f);
            this.mCurrentView.setTranslationY(0.0f);
            this.mCurrentView.setTranslationZ(0.0f);
        }
    }

    public void setVertical(boolean z) {
        this.mVertical = z;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            View view = this.mViews.get(i);
            if (view instanceof ButtonInterface) {
                ((ButtonInterface) view).setVertical(z);
            }
        }
    }
}
