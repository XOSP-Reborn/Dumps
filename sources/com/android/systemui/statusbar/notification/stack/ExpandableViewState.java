package com.android.systemui.statusbar.notification.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import com.android.systemui.C0007R$id;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.ExpandableView;

public class ExpandableViewState extends ViewState {
    private static final int TAG_ANIMATOR_HEIGHT = C0007R$id.height_animator_tag;
    private static final int TAG_ANIMATOR_TOP_INSET = C0007R$id.top_inset_animator_tag;
    private static final int TAG_END_HEIGHT = C0007R$id.height_animator_end_value_tag;
    private static final int TAG_END_TOP_INSET = C0007R$id.top_inset_animator_end_value_tag;
    private static final int TAG_START_HEIGHT = C0007R$id.height_animator_start_value_tag;
    private static final int TAG_START_TOP_INSET = C0007R$id.top_inset_animator_start_value_tag;
    public boolean belowSpeedBump;
    public int clipTopAmount;
    public boolean dark;
    public boolean dimmed;
    public boolean headsUpIsVisible;
    public int height;
    public boolean hideSensitive;
    public boolean inShelf;
    public int location;
    public int notGoneIndex;

    @Override // com.android.systemui.statusbar.notification.stack.ViewState
    public void copyFrom(ViewState viewState) {
        super.copyFrom(viewState);
        if (viewState instanceof ExpandableViewState) {
            ExpandableViewState expandableViewState = (ExpandableViewState) viewState;
            this.height = expandableViewState.height;
            this.dimmed = expandableViewState.dimmed;
            this.dark = expandableViewState.dark;
            this.hideSensitive = expandableViewState.hideSensitive;
            this.belowSpeedBump = expandableViewState.belowSpeedBump;
            this.clipTopAmount = expandableViewState.clipTopAmount;
            this.notGoneIndex = expandableViewState.notGoneIndex;
            this.location = expandableViewState.location;
            this.headsUpIsVisible = expandableViewState.headsUpIsVisible;
        }
    }

    @Override // com.android.systemui.statusbar.notification.stack.ViewState
    public void applyToView(View view) {
        super.applyToView(view);
        if (view instanceof ExpandableView) {
            ExpandableView expandableView = (ExpandableView) view;
            int actualHeight = expandableView.getActualHeight();
            int i = this.height;
            if (actualHeight != i) {
                expandableView.setActualHeight(i, false);
            }
            expandableView.setDimmed(this.dimmed, false);
            expandableView.setHideSensitive(this.hideSensitive, false, 0, 0);
            expandableView.setBelowSpeedBump(this.belowSpeedBump);
            expandableView.setDark(this.dark, false, 0);
            int i2 = this.clipTopAmount;
            if (((float) expandableView.getClipTopAmount()) != ((float) i2)) {
                expandableView.setClipTopAmount(i2);
            }
            expandableView.setTransformingInShelf(false);
            expandableView.setInShelf(this.inShelf);
            if (this.headsUpIsVisible) {
                expandableView.setHeadsUpIsVisible();
            }
        }
    }

    @Override // com.android.systemui.statusbar.notification.stack.ViewState
    public void animateTo(View view, AnimationProperties animationProperties) {
        super.animateTo(view, animationProperties);
        if (view instanceof ExpandableView) {
            ExpandableView expandableView = (ExpandableView) view;
            AnimationFilter animationFilter = animationProperties.getAnimationFilter();
            if (this.height != expandableView.getActualHeight()) {
                startHeightAnimation(expandableView, animationProperties);
            } else {
                abortAnimation(view, TAG_ANIMATOR_HEIGHT);
            }
            if (this.clipTopAmount != expandableView.getClipTopAmount()) {
                startInsetAnimation(expandableView, animationProperties);
            } else {
                abortAnimation(view, TAG_ANIMATOR_TOP_INSET);
            }
            expandableView.setDimmed(this.dimmed, animationFilter.animateDimmed);
            expandableView.setBelowSpeedBump(this.belowSpeedBump);
            expandableView.setHideSensitive(this.hideSensitive, animationFilter.animateHideSensitive, animationProperties.delay, animationProperties.duration);
            expandableView.setDark(this.dark, animationFilter.animateDark, animationProperties.delay);
            if (animationProperties.wasAdded(view) && !this.hidden) {
                expandableView.performAddAnimation(animationProperties.delay, animationProperties.duration, false);
            }
            if (!expandableView.isInShelf() && this.inShelf) {
                expandableView.setTransformingInShelf(true);
            }
            expandableView.setInShelf(this.inShelf);
            if (this.headsUpIsVisible) {
                expandableView.setHeadsUpIsVisible();
            }
        }
    }

    private void startHeightAnimation(final ExpandableView expandableView, AnimationProperties animationProperties) {
        Integer num = (Integer) ViewState.getChildTag(expandableView, TAG_START_HEIGHT);
        Integer num2 = (Integer) ViewState.getChildTag(expandableView, TAG_END_HEIGHT);
        int i = this.height;
        if (num2 == null || num2.intValue() != i) {
            ValueAnimator valueAnimator = (ValueAnimator) ViewState.getChildTag(expandableView, TAG_ANIMATOR_HEIGHT);
            if (animationProperties.getAnimationFilter().animateHeight) {
                ValueAnimator ofInt = ValueAnimator.ofInt(expandableView.getActualHeight(), i);
                ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    /* class com.android.systemui.statusbar.notification.stack.ExpandableViewState.AnonymousClass1 */

                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        expandableView.setActualHeight(((Integer) valueAnimator.getAnimatedValue()).intValue(), false);
                    }
                });
                ofInt.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                ofInt.setDuration(ViewState.cancelAnimatorAndGetNewDuration(animationProperties.duration, valueAnimator));
                if (animationProperties.delay > 0 && (valueAnimator == null || valueAnimator.getAnimatedFraction() == 0.0f)) {
                    ofInt.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter animationFinishListener = animationProperties.getAnimationFinishListener();
                if (animationFinishListener != null) {
                    ofInt.addListener(animationFinishListener);
                }
                ofInt.addListener(new AnimatorListenerAdapter() {
                    /* class com.android.systemui.statusbar.notification.stack.ExpandableViewState.AnonymousClass2 */
                    boolean mWasCancelled;

                    public void onAnimationEnd(Animator animator) {
                        expandableView.setTag(ExpandableViewState.TAG_ANIMATOR_HEIGHT, null);
                        expandableView.setTag(ExpandableViewState.TAG_START_HEIGHT, null);
                        expandableView.setTag(ExpandableViewState.TAG_END_HEIGHT, null);
                        expandableView.setActualHeightAnimating(false);
                        if (!this.mWasCancelled) {
                            ExpandableView expandableView = expandableView;
                            if (expandableView instanceof ExpandableNotificationRow) {
                                ((ExpandableNotificationRow) expandableView).setGroupExpansionChanging(false);
                            }
                        }
                    }

                    public void onAnimationStart(Animator animator) {
                        this.mWasCancelled = false;
                    }

                    public void onAnimationCancel(Animator animator) {
                        this.mWasCancelled = true;
                    }
                });
                ViewState.startAnimator(ofInt, animationFinishListener);
                expandableView.setTag(TAG_ANIMATOR_HEIGHT, ofInt);
                expandableView.setTag(TAG_START_HEIGHT, Integer.valueOf(expandableView.getActualHeight()));
                expandableView.setTag(TAG_END_HEIGHT, Integer.valueOf(i));
                expandableView.setActualHeightAnimating(true);
            } else if (valueAnimator != null) {
                PropertyValuesHolder[] values = valueAnimator.getValues();
                int intValue = num.intValue() + (i - num2.intValue());
                values[0].setIntValues(intValue, i);
                expandableView.setTag(TAG_START_HEIGHT, Integer.valueOf(intValue));
                expandableView.setTag(TAG_END_HEIGHT, Integer.valueOf(i));
                valueAnimator.setCurrentPlayTime(valueAnimator.getCurrentPlayTime());
            } else {
                expandableView.setActualHeight(i, false);
            }
        }
    }

    private void startInsetAnimation(final ExpandableView expandableView, AnimationProperties animationProperties) {
        Integer num = (Integer) ViewState.getChildTag(expandableView, TAG_START_TOP_INSET);
        Integer num2 = (Integer) ViewState.getChildTag(expandableView, TAG_END_TOP_INSET);
        int i = this.clipTopAmount;
        if (num2 == null || num2.intValue() != i) {
            ValueAnimator valueAnimator = (ValueAnimator) ViewState.getChildTag(expandableView, TAG_ANIMATOR_TOP_INSET);
            if (animationProperties.getAnimationFilter().animateTopInset) {
                ValueAnimator ofInt = ValueAnimator.ofInt(expandableView.getClipTopAmount(), i);
                ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    /* class com.android.systemui.statusbar.notification.stack.ExpandableViewState.AnonymousClass3 */

                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        expandableView.setClipTopAmount(((Integer) valueAnimator.getAnimatedValue()).intValue());
                    }
                });
                ofInt.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                ofInt.setDuration(ViewState.cancelAnimatorAndGetNewDuration(animationProperties.duration, valueAnimator));
                if (animationProperties.delay > 0 && (valueAnimator == null || valueAnimator.getAnimatedFraction() == 0.0f)) {
                    ofInt.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter animationFinishListener = animationProperties.getAnimationFinishListener();
                if (animationFinishListener != null) {
                    ofInt.addListener(animationFinishListener);
                }
                ofInt.addListener(new AnimatorListenerAdapter() {
                    /* class com.android.systemui.statusbar.notification.stack.ExpandableViewState.AnonymousClass4 */

                    public void onAnimationEnd(Animator animator) {
                        expandableView.setTag(ExpandableViewState.TAG_ANIMATOR_TOP_INSET, null);
                        expandableView.setTag(ExpandableViewState.TAG_START_TOP_INSET, null);
                        expandableView.setTag(ExpandableViewState.TAG_END_TOP_INSET, null);
                    }
                });
                ViewState.startAnimator(ofInt, animationFinishListener);
                expandableView.setTag(TAG_ANIMATOR_TOP_INSET, ofInt);
                expandableView.setTag(TAG_START_TOP_INSET, Integer.valueOf(expandableView.getClipTopAmount()));
                expandableView.setTag(TAG_END_TOP_INSET, Integer.valueOf(i));
            } else if (valueAnimator != null) {
                PropertyValuesHolder[] values = valueAnimator.getValues();
                int intValue = num.intValue() + (i - num2.intValue());
                values[0].setIntValues(intValue, i);
                expandableView.setTag(TAG_START_TOP_INSET, Integer.valueOf(intValue));
                expandableView.setTag(TAG_END_TOP_INSET, Integer.valueOf(i));
                valueAnimator.setCurrentPlayTime(valueAnimator.getCurrentPlayTime());
            } else {
                expandableView.setClipTopAmount(i);
            }
        }
    }

    public static int getFinalActualHeight(ExpandableView expandableView) {
        if (expandableView == null) {
            return 0;
        }
        if (((ValueAnimator) ViewState.getChildTag(expandableView, TAG_ANIMATOR_HEIGHT)) == null) {
            return expandableView.getActualHeight();
        }
        return ((Integer) ViewState.getChildTag(expandableView, TAG_END_HEIGHT)).intValue();
    }

    @Override // com.android.systemui.statusbar.notification.stack.ViewState
    public void cancelAnimations(View view) {
        super.cancelAnimations(view);
        Animator animator = (Animator) ViewState.getChildTag(view, TAG_ANIMATOR_HEIGHT);
        if (animator != null) {
            animator.cancel();
        }
        Animator animator2 = (Animator) ViewState.getChildTag(view, TAG_ANIMATOR_TOP_INSET);
        if (animator2 != null) {
            animator2.cancel();
        }
    }
}
