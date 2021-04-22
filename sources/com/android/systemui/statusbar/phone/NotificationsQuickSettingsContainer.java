package com.android.systemui.statusbar.phone;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.statusbar.notification.AboveShelfObserver;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;

public class NotificationsQuickSettingsContainer extends FrameLayout implements ViewStub.OnInflateListener, FragmentHostManager.FragmentListener, AboveShelfObserver.HasViewAboveShelfChangedListener {
    private int mBottomPadding;
    private boolean mCustomizerAnimating;
    private boolean mHasViewsAboveShelf;
    private boolean mInflated;
    private View mKeyguardStatusBar;
    private boolean mQsExpanded;
    private FrameLayout mQsFrame;
    private NotificationStackScrollLayout mStackScroller;
    private int mStackScrollerMargin;
    private View mUserSwitcher;

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mQsFrame = (FrameLayout) findViewById(C0007R$id.qs_frame);
        this.mStackScroller = (NotificationStackScrollLayout) findViewById(C0007R$id.notification_stack_scroller);
        this.mStackScrollerMargin = ((FrameLayout.LayoutParams) this.mStackScroller.getLayoutParams()).bottomMargin;
        this.mKeyguardStatusBar = findViewById(C0007R$id.keyguard_header);
        ViewStub viewStub = (ViewStub) findViewById(C0007R$id.keyguard_user_switcher);
        viewStub.setOnInflateListener(this);
        this.mUserSwitcher = viewStub;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FragmentHostManager.get(this).addTagListener(QS.TAG, this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        FragmentHostManager.get(this).removeTagListener(QS.TAG, this);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        reloadWidth(this.mQsFrame, C0005R$dimen.qs_panel_width);
        reloadWidth(this.mStackScroller, C0005R$dimen.notification_panel_width);
    }

    private void reloadWidth(View view, int i) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.width = getResources().getDimensionPixelSize(i);
        view.setLayoutParams(layoutParams);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mBottomPadding = windowInsets.getStableInsetBottom();
        setPadding(0, 0, 0, this.mBottomPadding);
        return windowInsets;
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View view, long j) {
        boolean z = true;
        boolean z2 = this.mInflated && this.mUserSwitcher.getVisibility() == 0;
        if (this.mKeyguardStatusBar.getVisibility() != 0) {
            z = false;
        }
        boolean z3 = this.mHasViewsAboveShelf;
        View view2 = z3 ? this.mStackScroller : this.mQsFrame;
        View view3 = !z3 ? this.mStackScroller : this.mQsFrame;
        if (view == this.mQsFrame) {
            if (z2 && z) {
                view3 = this.mUserSwitcher;
            } else if (z) {
                view3 = this.mKeyguardStatusBar;
            } else if (z2) {
                view3 = this.mUserSwitcher;
            }
            return super.drawChild(canvas, view3, j);
        } else if (view == this.mStackScroller) {
            if (z2 && z) {
                view2 = this.mKeyguardStatusBar;
            } else if (z || z2) {
                view2 = view3;
            }
            return super.drawChild(canvas, view2, j);
        } else if (view == this.mUserSwitcher) {
            if (!z2 || !z) {
                view3 = view2;
            }
            return super.drawChild(canvas, view3, j);
        } else if (view == this.mKeyguardStatusBar) {
            return super.drawChild(canvas, view2, j);
        } else {
            return super.drawChild(canvas, view, j);
        }
    }

    public void onInflate(ViewStub viewStub, View view) {
        if (viewStub == this.mUserSwitcher) {
            this.mUserSwitcher = view;
            this.mInflated = true;
        }
    }

    @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
    public void onFragmentViewCreated(String str, Fragment fragment) {
        ((QS) fragment).setContainer(this);
    }

    public void setQsExpanded(boolean z) {
        if (this.mQsExpanded != z) {
            this.mQsExpanded = z;
            invalidate();
        }
    }

    public void setCustomizerAnimating(boolean z) {
        if (this.mCustomizerAnimating != z) {
            this.mCustomizerAnimating = z;
            invalidate();
        }
    }

    public void setCustomizerShowing(boolean z) {
        if (z) {
            setPadding(0, 0, 0, 0);
            setBottomMargin(this.mStackScroller, 0);
        } else {
            setPadding(0, 0, 0, this.mBottomPadding);
            setBottomMargin(this.mStackScroller, this.mStackScrollerMargin);
        }
        this.mStackScroller.setQsCustomizerShowing(z);
    }

    private void setBottomMargin(View view, int i) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.bottomMargin = i;
        view.setLayoutParams(layoutParams);
    }

    @Override // com.android.systemui.statusbar.notification.AboveShelfObserver.HasViewAboveShelfChangedListener
    public void onHasViewsAboveShelfChanged(boolean z) {
        this.mHasViewsAboveShelf = z;
        invalidate();
    }
}
