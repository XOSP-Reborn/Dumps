package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0015R$style;
import com.android.systemui.Interpolators;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.util.InjectionInflationController;
import com.android.systemui.util.LifecycleFragment;

public class QSFragment extends LifecycleFragment implements QS, CommandQueue.Callbacks {
    private final Animator.AnimatorListener mAnimateHeaderSlidingInListener = new AnimatorListenerAdapter() {
        /* class com.android.systemui.qs.QSFragment.AnonymousClass3 */

        public void onAnimationEnd(Animator animator) {
            QSFragment.this.mHeaderAnimating = false;
            QSFragment.this.updateQsState();
        }
    };
    private QSContainerImpl mContainer;
    private long mDelay;
    private QSFooter mFooter;
    protected QuickStatusBarHeader mHeader;
    private boolean mHeaderAnimating;
    private final QSTileHost mHost;
    private final InjectionInflationController mInjectionInflater;
    private boolean mKeyguardShowing;
    private float mLastQSExpansion = -1.0f;
    private int mLayoutDirection;
    private boolean mListening;
    private QS.HeightListener mPanelView;
    private QSAnimator mQSAnimator;
    private QSCustomizer mQSCustomizer;
    private QSDetail mQSDetail;
    protected QSPanel mQSPanel;
    private final Rect mQsBounds = new Rect();
    private boolean mQsDisabled;
    private boolean mQsExpanded;
    private final RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler;
    private boolean mStackScrollerOverscrolling;
    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn = new ViewTreeObserver.OnPreDrawListener() {
        /* class com.android.systemui.qs.QSFragment.AnonymousClass2 */

        public boolean onPreDraw() {
            QSFragment.this.getView().getViewTreeObserver().removeOnPreDrawListener(this);
            QSFragment.this.getView().animate().translationY(0.0f).setStartDelay(QSFragment.this.mDelay).setDuration(448).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(QSFragment.this.mAnimateHeaderSlidingInListener).start();
            QSFragment.this.getView().setY((float) (-QSFragment.this.mHeader.getHeight()));
            return true;
        }
    };

    @Override // com.android.systemui.plugins.qs.QS
    public void setHasNotifications(boolean z) {
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setHeaderClickable(boolean z) {
    }

    public QSFragment(RemoteInputQuickSettingsDisabler remoteInputQuickSettingsDisabler, InjectionInflationController injectionInflationController, Context context, QSTileHost qSTileHost) {
        this.mRemoteInputQuickSettingsDisabler = remoteInputQuickSettingsDisabler;
        this.mInjectionInflater = injectionInflationController;
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).observe(getLifecycle(), this);
        this.mHost = qSTileHost;
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return this.mInjectionInflater.injectable(layoutInflater.cloneInContext(new ContextThemeWrapper(getContext(), C0015R$style.qs_theme))).inflate(C0010R$layout.qs_panel, viewGroup, false);
    }

    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mQSPanel = (QSPanel) view.findViewById(C0007R$id.quick_settings_panel);
        this.mQSDetail = (QSDetail) view.findViewById(C0007R$id.qs_detail);
        this.mHeader = (QuickStatusBarHeader) view.findViewById(C0007R$id.header);
        this.mFooter = (QSFooter) view.findViewById(C0007R$id.qs_footer);
        this.mContainer = (QSContainerImpl) view.findViewById(C0007R$id.quick_settings_container);
        this.mQSDetail.setQsPanel(this.mQSPanel, this.mHeader, (View) this.mFooter);
        this.mQSAnimator = new QSAnimator(this, (QuickQSPanel) this.mHeader.findViewById(C0007R$id.quick_qs_panel), this.mQSPanel);
        this.mQSCustomizer = (QSCustomizer) view.findViewById(C0007R$id.qs_customize);
        this.mQSCustomizer.setQs(this);
        if (bundle != null) {
            setExpanded(bundle.getBoolean("expanded"));
            setListening(bundle.getBoolean("listening"));
            setEditLocation(view);
            this.mQSCustomizer.restoreInstanceState(bundle);
            if (this.mQsExpanded) {
                this.mQSPanel.getTileLayout().restoreInstanceState(bundle);
            }
        }
        setHost(this.mHost);
    }

    @Override // com.android.systemui.util.LifecycleFragment
    public void onDestroy() {
        super.onDestroy();
        if (this.mListening) {
            setListening(false);
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("expanded", this.mQsExpanded);
        bundle.putBoolean("listening", this.mListening);
        this.mQSCustomizer.saveInstanceState(bundle);
        if (this.mQsExpanded) {
            this.mQSPanel.getTileLayout().saveInstanceState(bundle);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isListening() {
        return this.mListening;
    }

    /* access modifiers changed from: package-private */
    public boolean isExpanded() {
        return this.mQsExpanded;
    }

    @Override // com.android.systemui.plugins.qs.QS
    public View getHeader() {
        return this.mHeader;
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setPanelView(QS.HeightListener heightListener) {
        this.mPanelView = heightListener;
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        setEditLocation(getView());
        if (configuration.getLayoutDirection() != this.mLayoutDirection) {
            this.mLayoutDirection = configuration.getLayoutDirection();
            QSAnimator qSAnimator = this.mQSAnimator;
            if (qSAnimator != null) {
                qSAnimator.onRtlChanged();
            }
        }
    }

    private void setEditLocation(View view) {
        View findViewById = view.findViewById(16908291);
        int[] locationOnScreen = findViewById.getLocationOnScreen();
        this.mQSCustomizer.setEditLocation(locationOnScreen[0] + (findViewById.getWidth() / 2), locationOnScreen[1] + (findViewById.getHeight() / 2));
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setContainer(ViewGroup viewGroup) {
        if (viewGroup instanceof NotificationsQuickSettingsContainer) {
            this.mQSCustomizer.setContainer((NotificationsQuickSettingsContainer) viewGroup);
        }
    }

    @Override // com.android.systemui.plugins.qs.QS
    public boolean isCustomizing() {
        return this.mQSCustomizer.isCustomizing();
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mQSPanel.setHost(qSTileHost, this.mQSCustomizer);
        this.mHeader.setQSPanel(this.mQSPanel);
        this.mFooter.setQSPanel(this.mQSPanel);
        this.mQSDetail.setHost(qSTileHost);
        QSAnimator qSAnimator = this.mQSAnimator;
        if (qSAnimator != null) {
            qSAnimator.setHost(qSTileHost);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void disable(int i, int i2, int i3, boolean z) {
        if (i == getContext().getDisplayId()) {
            int adjustDisableFlags = this.mRemoteInputQuickSettingsDisabler.adjustDisableFlags(i3);
            boolean z2 = (adjustDisableFlags & 1) != 0;
            if (z2 != this.mQsDisabled) {
                this.mQsDisabled = z2;
                this.mContainer.disable(i2, adjustDisableFlags, z);
                this.mHeader.disable(i2, adjustDisableFlags, z);
                this.mFooter.disable(i2, adjustDisableFlags, z);
                updateQsState();
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateQsState() {
        boolean z = true;
        int i = 0;
        boolean z2 = this.mQsExpanded || this.mStackScrollerOverscrolling || this.mHeaderAnimating;
        this.mQSPanel.setExpanded(this.mQsExpanded);
        this.mQSDetail.setExpanded(this.mQsExpanded);
        this.mHeader.setVisibility((this.mQsExpanded || !this.mKeyguardShowing || this.mHeaderAnimating) ? 0 : 4);
        this.mHeader.setExpanded((this.mKeyguardShowing && !this.mHeaderAnimating) || (this.mQsExpanded && !this.mStackScrollerOverscrolling));
        this.mFooter.setVisibility((this.mQsDisabled || (!this.mQsExpanded && this.mKeyguardShowing && !this.mHeaderAnimating)) ? 4 : 0);
        QSFooter qSFooter = this.mFooter;
        if ((!this.mKeyguardShowing || this.mHeaderAnimating) && (!this.mQsExpanded || this.mStackScrollerOverscrolling)) {
            z = false;
        }
        qSFooter.setExpanded(z);
        QSPanel qSPanel = this.mQSPanel;
        if (this.mQsDisabled || !z2) {
            i = 4;
        }
        qSPanel.setVisibility(i);
    }

    public QSPanel getQsPanel() {
        return this.mQSPanel;
    }

    @Override // com.android.systemui.plugins.qs.QS
    public boolean isShowingDetail() {
        return this.mQSPanel.isShowingCustomize() || this.mQSDetail.isShowingDetail();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return isCustomizing();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setExpanded(boolean z) {
        this.mQsExpanded = z;
        this.mQSPanel.setListening(this.mListening, this.mQsExpanded);
        updateQsState();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setKeyguardShowing(boolean z) {
        this.mKeyguardShowing = z;
        this.mLastQSExpansion = -1.0f;
        QSAnimator qSAnimator = this.mQSAnimator;
        if (qSAnimator != null) {
            qSAnimator.setOnKeyguard(z);
        }
        this.mFooter.setKeyguardShowing(z);
        updateQsState();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setOverscrolling(boolean z) {
        this.mStackScrollerOverscrolling = z;
        updateQsState();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setListening(boolean z) {
        this.mListening = z;
        this.mHeader.setListening(z);
        this.mFooter.setListening(z);
        this.mQSPanel.setListening(this.mListening, this.mQsExpanded);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setHeaderListening(boolean z) {
        this.mHeader.setListening(z);
        this.mFooter.setListening(z);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setQsExpansion(float f, float f2) {
        this.mContainer.setExpansion(f);
        float f3 = 1.0f;
        float f4 = f - 1.0f;
        if (!this.mHeaderAnimating) {
            View view = getView();
            if (this.mKeyguardShowing) {
                f2 = ((float) this.mHeader.getHeight()) * f4;
            }
            view.setTranslationY(f2);
        }
        if (f != this.mLastQSExpansion) {
            this.mLastQSExpansion = f;
            boolean z = f == 1.0f;
            float bottom = f4 * ((float) ((this.mQSPanel.getBottom() - this.mHeader.getBottom()) + this.mHeader.getPaddingBottom() + this.mFooter.getHeight()));
            this.mHeader.setExpansion(this.mKeyguardShowing, f, bottom);
            QSFooter qSFooter = this.mFooter;
            if (!this.mKeyguardShowing) {
                f3 = f;
            }
            qSFooter.setExpansion(f3);
            this.mQSPanel.getQsTileRevealController().setExpansion(f);
            this.mQSPanel.getTileLayout().setExpansion(f);
            this.mQSPanel.setTranslationY(bottom);
            this.mQSDetail.setFullyExpanded(z);
            if (z) {
                this.mQSPanel.setClipBounds(null);
            } else {
                this.mQsBounds.top = (int) (-this.mQSPanel.getTranslationY());
                this.mQsBounds.right = this.mQSPanel.getWidth();
                this.mQsBounds.bottom = this.mQSPanel.getHeight();
                this.mQSPanel.setClipBounds(this.mQsBounds);
            }
            QSAnimator qSAnimator = this.mQSAnimator;
            if (qSAnimator != null) {
                qSAnimator.setPosition(f);
            }
        }
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void animateHeaderSlidingIn(long j) {
        if (!this.mQsExpanded) {
            this.mHeaderAnimating = true;
            this.mDelay = j;
            getView().getViewTreeObserver().addOnPreDrawListener(this.mStartHeaderSlidingIn);
        }
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void animateHeaderSlidingOut() {
        this.mHeaderAnimating = true;
        getView().animate().y((float) (-this.mHeader.getHeight())).setStartDelay(0).setDuration(360).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.qs.QSFragment.AnonymousClass1 */

            public void onAnimationEnd(Animator animator) {
                if (QSFragment.this.getView() != null) {
                    QSFragment.this.getView().animate().setListener(null);
                }
                QSFragment.this.mHeaderAnimating = false;
                QSFragment.this.updateQsState();
            }
        }).start();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setExpandClickListener(View.OnClickListener onClickListener) {
        this.mFooter.setExpandClickListener(onClickListener);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void closeDetail() {
        this.mQSPanel.closeDetail();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void notifyCustomizeChanged() {
        this.mContainer.updateExpansion();
        int i = 0;
        this.mQSPanel.setVisibility(!this.mQSCustomizer.isCustomizing() ? 0 : 4);
        QSFooter qSFooter = this.mFooter;
        if (this.mQSCustomizer.isCustomizing()) {
            i = 4;
        }
        qSFooter.setVisibility(i);
        this.mPanelView.onQsHeightChanged();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public int getDesiredHeight() {
        if (this.mQSCustomizer.isCustomizing()) {
            return getView().getHeight();
        }
        if (!this.mQSDetail.isClosingDetail()) {
            return getView().getMeasuredHeight();
        }
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mQSPanel.getLayoutParams();
        return layoutParams.topMargin + layoutParams.bottomMargin + this.mQSPanel.getMeasuredHeight() + getView().getPaddingBottom();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void setHeightOverride(int i) {
        this.mContainer.setHeightOverride(i);
    }

    @Override // com.android.systemui.plugins.qs.QS
    public int getQsMinExpansionHeight() {
        return this.mHeader.getHeight();
    }

    @Override // com.android.systemui.plugins.qs.QS
    public void hideImmediately() {
        getView().animate().cancel();
        getView().setY((float) (-this.mHeader.getHeight()));
    }
}
