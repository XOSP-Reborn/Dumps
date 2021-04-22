package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.qs.customize.QSCustomizer;

public class QSContainerImpl extends FrameLayout {
    private View mBackground;
    private View mBackgroundGradient;
    private QuickStatusBarHeader mHeader;
    private int mHeightOverride = -1;
    private QSCustomizer mQSCustomizer;
    private View mQSDetail;
    private View mQSFooter;
    private QSPanel mQSPanel;
    private boolean mQsDisabled;
    private float mQsExpansion;
    private int mSideMargins;
    private final Point mSizePoint = new Point();
    private View mStatusBarBackground;

    public boolean performClick() {
        return true;
    }

    public QSContainerImpl(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mQSPanel = (QSPanel) findViewById(C0007R$id.quick_settings_panel);
        this.mQSDetail = findViewById(C0007R$id.qs_detail);
        this.mHeader = (QuickStatusBarHeader) findViewById(C0007R$id.header);
        this.mQSCustomizer = (QSCustomizer) findViewById(C0007R$id.qs_customize);
        this.mQSFooter = findViewById(C0007R$id.qs_footer);
        this.mBackground = findViewById(C0007R$id.quick_settings_background);
        this.mStatusBarBackground = findViewById(C0007R$id.quick_settings_status_bar_background);
        this.mBackgroundGradient = findViewById(C0007R$id.quick_settings_gradient_view);
        this.mSideMargins = getResources().getDimensionPixelSize(C0005R$dimen.notification_side_paddings);
        setImportantForAccessibility(2);
        setMargins();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        setBackgroundGradientVisibility(configuration);
        updateResources();
        this.mSizePoint.set(0, 0);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        Configuration configuration = getResources().getConfiguration();
        boolean z = configuration.smallestScreenWidthDp >= 600 || configuration.orientation != 2;
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mQSPanel.getLayoutParams();
        int displayHeight = ((getDisplayHeight() - marginLayoutParams.topMargin) - marginLayoutParams.bottomMargin) - getPaddingBottom();
        if (z) {
            displayHeight -= getResources().getDimensionPixelSize(C0005R$dimen.navigation_bar_height);
        }
        this.mQSPanel.measure(i, View.MeasureSpec.makeMeasureSpec(displayHeight, 1073741824));
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(this.mQSPanel.getMeasuredWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + this.mQSPanel.getMeasuredHeight() + getPaddingBottom(), 1073741824));
        this.mQSCustomizer.measure(i, View.MeasureSpec.makeMeasureSpec(getDisplayHeight(), 1073741824));
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateExpansion();
    }

    public void disable(int i, int i2, boolean z) {
        boolean z2 = true;
        int i3 = 0;
        if ((i2 & 1) == 0) {
            z2 = false;
        }
        if (z2 != this.mQsDisabled) {
            this.mQsDisabled = z2;
            setBackgroundGradientVisibility(getResources().getConfiguration());
            View view = this.mBackground;
            if (this.mQsDisabled) {
                i3 = 8;
            }
            view.setVisibility(i3);
        }
    }

    private void updateResources() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mQSPanel.getLayoutParams();
        layoutParams.topMargin = ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(17105397);
        this.mQSPanel.setLayoutParams(layoutParams);
    }

    public void setHeightOverride(int i) {
        this.mHeightOverride = i;
        updateExpansion();
    }

    public void updateExpansion() {
        int calculateContainerHeight = calculateContainerHeight();
        setBottom(getTop() + calculateContainerHeight);
        this.mQSDetail.setBottom(getTop() + calculateContainerHeight);
        View view = this.mQSFooter;
        view.setTranslationY((float) (calculateContainerHeight - view.getHeight()));
        this.mBackground.setTop(this.mQSPanel.getTop());
        this.mBackground.setBottom(calculateContainerHeight);
    }

    /* access modifiers changed from: protected */
    public int calculateContainerHeight() {
        int i = this.mHeightOverride;
        if (i == -1) {
            i = getMeasuredHeight();
        }
        if (this.mQSCustomizer.isCustomizing()) {
            return this.mQSCustomizer.getHeight();
        }
        return this.mHeader.getHeight() + Math.round(this.mQsExpansion * ((float) (i - this.mHeader.getHeight())));
    }

    private void setBackgroundGradientVisibility(Configuration configuration) {
        int i = 4;
        if (configuration.orientation == 2) {
            this.mBackgroundGradient.setVisibility(4);
            this.mStatusBarBackground.setVisibility(4);
            return;
        }
        View view = this.mBackgroundGradient;
        if (!this.mQsDisabled) {
            i = 0;
        }
        view.setVisibility(i);
        this.mStatusBarBackground.setVisibility(0);
    }

    public void setExpansion(float f) {
        this.mQsExpansion = f;
        updateExpansion();
    }

    private void setMargins() {
        setMargins(this.mQSDetail);
        setMargins(this.mBackground);
        setMargins(this.mQSFooter);
        this.mQSPanel.setMargins(this.mSideMargins);
        this.mHeader.setMargins(this.mSideMargins);
    }

    private void setMargins(View view) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        int i = this.mSideMargins;
        layoutParams.rightMargin = i;
        layoutParams.leftMargin = i;
    }

    private int getDisplayHeight() {
        if (this.mSizePoint.y == 0) {
            getDisplay().getRealSize(this.mSizePoint);
        }
        return this.mSizePoint.y;
    }
}
