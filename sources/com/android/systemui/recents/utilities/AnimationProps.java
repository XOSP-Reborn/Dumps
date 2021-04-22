package com.android.systemui.recents.utilities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import java.util.List;

public class AnimationProps {
    public static final AnimationProps IMMEDIATE = new AnimationProps(0, LINEAR_INTERPOLATOR);
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private Animator.AnimatorListener mListener;
    private SparseLongArray mPropDuration;
    private SparseArray<Interpolator> mPropInterpolators;
    private SparseLongArray mPropStartDelay;

    public AnimationProps() {
    }

    public AnimationProps(int i, Interpolator interpolator) {
        this(0, i, interpolator, null);
    }

    public AnimationProps(int i, Interpolator interpolator, Animator.AnimatorListener animatorListener) {
        this(0, i, interpolator, animatorListener);
    }

    public AnimationProps(int i, int i2, Interpolator interpolator) {
        this(i, i2, interpolator, null);
    }

    public AnimationProps(int i, int i2, Interpolator interpolator, Animator.AnimatorListener animatorListener) {
        setStartDelay(0, i);
        setDuration(0, i2);
        setInterpolator(0, interpolator);
        setListener(animatorListener);
    }

    public AnimatorSet createAnimator(List<Animator> list) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator.AnimatorListener animatorListener = this.mListener;
        if (animatorListener != null) {
            animatorSet.addListener(animatorListener);
        }
        animatorSet.playTogether(list);
        return animatorSet;
    }

    public <T extends ValueAnimator> T apply(int i, T t) {
        t.setStartDelay(getStartDelay(i));
        t.setDuration(getDuration(i));
        t.setInterpolator(getInterpolator(i));
        return t;
    }

    public AnimationProps setStartDelay(int i, int i2) {
        if (this.mPropStartDelay == null) {
            this.mPropStartDelay = new SparseLongArray();
        }
        this.mPropStartDelay.append(i, (long) i2);
        return this;
    }

    public long getStartDelay(int i) {
        SparseLongArray sparseLongArray = this.mPropStartDelay;
        if (sparseLongArray == null) {
            return 0;
        }
        long j = sparseLongArray.get(i, -1);
        if (j != -1) {
            return j;
        }
        return this.mPropStartDelay.get(0, 0);
    }

    public AnimationProps setDuration(int i, int i2) {
        if (this.mPropDuration == null) {
            this.mPropDuration = new SparseLongArray();
        }
        this.mPropDuration.append(i, (long) i2);
        return this;
    }

    public long getDuration(int i) {
        SparseLongArray sparseLongArray = this.mPropDuration;
        if (sparseLongArray == null) {
            return 0;
        }
        long j = sparseLongArray.get(i, -1);
        if (j != -1) {
            return j;
        }
        return this.mPropDuration.get(0, 0);
    }

    public AnimationProps setInterpolator(int i, Interpolator interpolator) {
        if (this.mPropInterpolators == null) {
            this.mPropInterpolators = new SparseArray<>();
        }
        this.mPropInterpolators.append(i, interpolator);
        return this;
    }

    public Interpolator getInterpolator(int i) {
        SparseArray<Interpolator> sparseArray = this.mPropInterpolators;
        if (sparseArray == null) {
            return LINEAR_INTERPOLATOR;
        }
        Interpolator interpolator = sparseArray.get(i);
        if (interpolator != null) {
            return interpolator;
        }
        return this.mPropInterpolators.get(0, LINEAR_INTERPOLATOR);
    }

    public AnimationProps setListener(Animator.AnimatorListener animatorListener) {
        this.mListener = animatorListener;
        return this;
    }

    public Animator.AnimatorListener getListener() {
        return this.mListener;
    }

    public boolean isImmediate() {
        int size = this.mPropDuration.size();
        for (int i = 0; i < size; i++) {
            if (this.mPropDuration.valueAt(i) > 0) {
                return false;
            }
        }
        return true;
    }
}
