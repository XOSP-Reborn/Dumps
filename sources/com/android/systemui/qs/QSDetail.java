package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.statusbar.CommandQueue;
import java.util.Objects;

public class QSDetail extends LinearLayout {
    private boolean mAnimatingOpen;
    private QSDetailClipper mClipper;
    private boolean mClosingDetail;
    private DetailAdapter mDetailAdapter;
    private ViewGroup mDetailContent;
    protected TextView mDetailDoneButton;
    protected TextView mDetailSettingsButton;
    private final SparseArray<View> mDetailViews = new SparseArray<>();
    private View mFooter;
    private boolean mFullyExpanded;
    private QuickStatusBarHeader mHeader;
    private final AnimatorListenerAdapter mHideGridContentWhenDone = new AnimatorListenerAdapter() {
        /* class com.android.systemui.qs.QSDetail.AnonymousClass4 */

        public void onAnimationCancel(Animator animator) {
            animator.removeListener(this);
            QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }

        public void onAnimationEnd(Animator animator) {
            if (QSDetail.this.mDetailAdapter != null) {
                QSDetail.this.mQsPanel.setGridContentVisibility(false);
                QSDetail.this.mHeader.setVisibility(4);
                QSDetail.this.mFooter.setVisibility(4);
            }
            QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }
    };
    protected QSTileHost mHost;
    private int mOpenX;
    private int mOpenY;
    protected View mQsDetailHeader;
    protected ImageView mQsDetailHeaderProgress;
    protected Switch mQsDetailHeaderSwitch;
    protected TextView mQsDetailHeaderTitle;
    private QSPanel mQsPanel;
    protected Callback mQsPanelCallback = new Callback() {
        /* class com.android.systemui.qs.QSDetail.AnonymousClass3 */

        @Override // com.android.systemui.qs.QSDetail.Callback
        public void onToggleStateChanged(final boolean z) {
            QSDetail.this.post(new Runnable() {
                /* class com.android.systemui.qs.QSDetail.AnonymousClass3.AnonymousClass1 */

                public void run() {
                    QSDetail qSDetail = QSDetail.this;
                    qSDetail.handleToggleStateChanged(z, qSDetail.mDetailAdapter != null && QSDetail.this.mDetailAdapter.getToggleEnabled());
                }
            });
        }

        @Override // com.android.systemui.qs.QSDetail.Callback
        public void onShowingDetail(final DetailAdapter detailAdapter, final int i, final int i2) {
            QSDetail.this.post(new Runnable() {
                /* class com.android.systemui.qs.QSDetail.AnonymousClass3.AnonymousClass2 */

                public void run() {
                    if (QSDetail.this.isAttachedToWindow()) {
                        QSDetail.this.handleShowingDetail(detailAdapter, i, i2, false);
                    }
                }
            });
        }

        @Override // com.android.systemui.qs.QSDetail.Callback
        public void onScanStateChanged(final boolean z) {
            QSDetail.this.post(new Runnable() {
                /* class com.android.systemui.qs.QSDetail.AnonymousClass3.AnonymousClass3 */

                public void run() {
                    QSDetail.this.handleScanStateChanged(z);
                }
            });
        }
    };
    private boolean mScanState;
    private boolean mSwitchState;
    private final AnimatorListenerAdapter mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
        /* class com.android.systemui.qs.QSDetail.AnonymousClass5 */

        public void onAnimationEnd(Animator animator) {
            QSDetail.this.mDetailContent.removeAllViews();
            QSDetail.this.setVisibility(4);
            QSDetail.this.mClosingDetail = false;
        }
    };
    private boolean mTriggeredExpand;

    public interface Callback {
        void onScanStateChanged(boolean z);

        void onShowingDetail(DetailAdapter detailAdapter, int i, int i2);

        void onToggleStateChanged(boolean z);
    }

    public QSDetail(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        FontSizeUtils.updateFontSize(this.mDetailDoneButton, C0005R$dimen.qs_detail_button_text_size);
        FontSizeUtils.updateFontSize(this.mDetailSettingsButton, C0005R$dimen.qs_detail_button_text_size);
        for (int i = 0; i < this.mDetailViews.size(); i++) {
            this.mDetailViews.valueAt(i).dispatchConfigurationChanged(configuration);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDetailContent = (ViewGroup) findViewById(16908290);
        this.mDetailSettingsButton = (TextView) findViewById(16908314);
        this.mDetailDoneButton = (TextView) findViewById(16908313);
        this.mQsDetailHeader = findViewById(C0007R$id.qs_detail_header);
        this.mQsDetailHeaderTitle = (TextView) this.mQsDetailHeader.findViewById(16908310);
        this.mQsDetailHeaderSwitch = (Switch) this.mQsDetailHeader.findViewById(16908311);
        this.mQsDetailHeaderProgress = (ImageView) findViewById(C0007R$id.qs_detail_header_progress);
        updateDetailText();
        this.mClipper = new QSDetailClipper(this);
        this.mDetailDoneButton.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.qs.QSDetail.AnonymousClass1 */

            public void onClick(View view) {
                QSDetail qSDetail = QSDetail.this;
                qSDetail.announceForAccessibility(((LinearLayout) qSDetail).mContext.getString(C0014R$string.accessibility_desc_quick_settings));
                QSDetail.this.mQsPanel.closeDetail();
            }
        });
    }

    public void setQsPanel(QSPanel qSPanel, QuickStatusBarHeader quickStatusBarHeader, View view) {
        this.mQsPanel = qSPanel;
        this.mHeader = quickStatusBarHeader;
        this.mFooter = view;
        this.mHeader.setCallback(this.mQsPanelCallback);
        this.mQsPanel.setCallback(this.mQsPanelCallback);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public boolean isShowingDetail() {
        return this.mDetailAdapter != null;
    }

    public void setFullyExpanded(boolean z) {
        this.mFullyExpanded = z;
    }

    public void setExpanded(boolean z) {
        if (!z) {
            this.mTriggeredExpand = false;
        }
    }

    private void updateDetailText() {
        this.mDetailDoneButton.setText(C0014R$string.quick_settings_done);
        this.mDetailSettingsButton.setText(C0014R$string.quick_settings_more_settings);
    }

    public boolean isClosingDetail() {
        return this.mClosingDetail;
    }

    public void handleShowingDetail(DetailAdapter detailAdapter, int i, int i2, boolean z) {
        AnimatorListenerAdapter animatorListenerAdapter;
        boolean z2 = detailAdapter != null;
        setClickable(z2);
        if (z2) {
            setupDetailHeader(detailAdapter);
            if (!z || this.mFullyExpanded) {
                this.mTriggeredExpand = false;
            } else {
                this.mTriggeredExpand = true;
                ((CommandQueue) SysUiServiceProvider.getComponent(((LinearLayout) this).mContext, CommandQueue.class)).animateExpandSettingsPanel(null);
            }
            this.mOpenX = i;
            this.mOpenY = i2;
        } else {
            i = this.mOpenX;
            i2 = this.mOpenY;
            if (z && this.mTriggeredExpand) {
                ((CommandQueue) SysUiServiceProvider.getComponent(((LinearLayout) this).mContext, CommandQueue.class)).animateCollapsePanels();
                this.mTriggeredExpand = false;
            }
        }
        boolean z3 = (this.mDetailAdapter != null) != (detailAdapter != null);
        if (z3 || this.mDetailAdapter != detailAdapter) {
            if (detailAdapter != null) {
                int metricsCategory = detailAdapter.getMetricsCategory();
                View createDetailView = detailAdapter.createDetailView(((LinearLayout) this).mContext, this.mDetailViews.get(metricsCategory), this.mDetailContent);
                if (createDetailView != null) {
                    setupDetailFooter(detailAdapter);
                    this.mDetailContent.removeAllViews();
                    this.mDetailContent.addView(createDetailView);
                    this.mDetailViews.put(metricsCategory, createDetailView);
                    ((MetricsLogger) Dependency.get(MetricsLogger.class)).visible(detailAdapter.getMetricsCategory());
                    announceForAccessibility(((LinearLayout) this).mContext.getString(C0014R$string.accessibility_quick_settings_detail, detailAdapter.getTitle()));
                    this.mDetailAdapter = detailAdapter;
                    animatorListenerAdapter = this.mHideGridContentWhenDone;
                    setVisibility(0);
                } else {
                    throw new IllegalStateException("Must return detail view");
                }
            } else {
                if (this.mDetailAdapter != null) {
                    ((MetricsLogger) Dependency.get(MetricsLogger.class)).hidden(this.mDetailAdapter.getMetricsCategory());
                }
                this.mClosingDetail = true;
                this.mDetailAdapter = null;
                animatorListenerAdapter = this.mTeardownDetailWhenDone;
                this.mHeader.setVisibility(0);
                this.mFooter.setVisibility(0);
                this.mQsPanel.setGridContentVisibility(true);
                this.mQsPanelCallback.onScanStateChanged(false);
            }
            sendAccessibilityEvent(32);
            animateDetailVisibleDiff(i, i2, z3, animatorListenerAdapter);
        }
    }

    /* access modifiers changed from: protected */
    public void animateDetailVisibleDiff(int i, int i2, boolean z, Animator.AnimatorListener animatorListener) {
        if (z) {
            boolean z2 = true;
            this.mAnimatingOpen = this.mDetailAdapter != null;
            if (this.mFullyExpanded || this.mDetailAdapter != null) {
                setAlpha(1.0f);
                QSDetailClipper qSDetailClipper = this.mClipper;
                if (this.mDetailAdapter == null) {
                    z2 = false;
                }
                qSDetailClipper.animateCircularClip(i, i2, z2, animatorListener);
                return;
            }
            animate().alpha(0.0f).setDuration(300).setListener(animatorListener).start();
        }
    }

    /* access modifiers changed from: protected */
    public void setupDetailFooter(DetailAdapter detailAdapter) {
        Intent settingsIntent = detailAdapter.getSettingsIntent();
        this.mDetailSettingsButton.setVisibility(settingsIntent != null ? 0 : 8);
        this.mDetailSettingsButton.setOnClickListener(new View.OnClickListener(settingsIntent) {
            /* class com.android.systemui.qs.$$Lambda$QSDetail$NHQwfesA2Z6J0e0FBlLg3IIEATQ */
            private final /* synthetic */ Intent f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                QSDetail.lambda$setupDetailFooter$0(DetailAdapter.this, this.f$1, view);
            }
        });
    }

    static /* synthetic */ void lambda$setupDetailFooter$0(DetailAdapter detailAdapter, Intent intent, View view) {
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).action(929, detailAdapter.getMetricsCategory());
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
    }

    /* access modifiers changed from: protected */
    public void setupDetailHeader(final DetailAdapter detailAdapter) {
        this.mQsDetailHeaderTitle.setText(detailAdapter.getTitle());
        Boolean toggleState = detailAdapter.getToggleState();
        if (toggleState == null) {
            this.mQsDetailHeaderSwitch.setVisibility(4);
            this.mQsDetailHeader.setClickable(false);
            return;
        }
        this.mQsDetailHeaderSwitch.setVisibility(0);
        handleToggleStateChanged(toggleState.booleanValue(), detailAdapter.getToggleEnabled());
        this.mQsDetailHeader.setClickable(true);
        this.mQsDetailHeader.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.qs.QSDetail.AnonymousClass2 */

            public void onClick(View view) {
                boolean z = !QSDetail.this.mQsDetailHeaderSwitch.isChecked();
                QSDetail.this.mQsDetailHeaderSwitch.setChecked(z);
                detailAdapter.setToggleState(z);
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleToggleStateChanged(boolean z, boolean z2) {
        this.mSwitchState = z;
        if (!this.mAnimatingOpen) {
            this.mQsDetailHeaderSwitch.setChecked(z);
            this.mQsDetailHeader.setEnabled(z2);
            this.mQsDetailHeaderSwitch.setEnabled(z2);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleScanStateChanged(boolean z) {
        if (this.mScanState != z) {
            this.mScanState = z;
            Animatable animatable = (Animatable) this.mQsDetailHeaderProgress.getDrawable();
            if (z) {
                this.mQsDetailHeaderProgress.animate().cancel();
                ViewPropertyAnimator alpha = this.mQsDetailHeaderProgress.animate().alpha(1.0f);
                Objects.requireNonNull(animatable);
                alpha.withEndAction(new Runnable(animatable) {
                    /* class com.android.systemui.qs.$$Lambda$dWuG3P2xqsast1TFpf_9V5OJbdM */
                    private final /* synthetic */ Animatable f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final void run() {
                        this.f$0.start();
                    }
                }).start();
                return;
            }
            this.mQsDetailHeaderProgress.animate().cancel();
            ViewPropertyAnimator alpha2 = this.mQsDetailHeaderProgress.animate().alpha(0.0f);
            Objects.requireNonNull(animatable);
            alpha2.withEndAction(new Runnable(animatable) {
                /* class com.android.systemui.qs.$$Lambda$uWzoJtW0gRQtylxIzOBLYDei0eA */
                private final /* synthetic */ Animatable f$0;

                {
                    this.f$0 = r1;
                }

                public final void run() {
                    this.f$0.stop();
                }
            }).start();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void checkPendingAnimations() {
        boolean z = this.mSwitchState;
        DetailAdapter detailAdapter = this.mDetailAdapter;
        handleToggleStateChanged(z, detailAdapter != null && detailAdapter.getToggleEnabled());
    }
}
