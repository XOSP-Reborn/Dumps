package com.android.systemui.statusbar.notification.stack;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.Preconditions;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;

public class SectionHeaderView extends ActivatableNotificationView {
    private ImageView mClearAllButton;
    private ViewGroup mContents;
    private TextView mLabelView;
    private View.OnClickListener mOnClearClickListener = null;
    private final RectF mTmpRect = new RectF();

    public SectionHeaderView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContents = (ViewGroup) Preconditions.checkNotNull((ViewGroup) findViewById(C0007R$id.content));
        bindContents();
    }

    private void bindContents() {
        this.mLabelView = (TextView) Preconditions.checkNotNull((TextView) findViewById(C0007R$id.header_label));
        this.mClearAllButton = (ImageView) Preconditions.checkNotNull((ImageView) findViewById(C0007R$id.btn_clear_all));
        View.OnClickListener onClickListener = this.mOnClearClickListener;
        if (onClickListener != null) {
            this.mClearAllButton.setOnClickListener(onClickListener);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public View getContentView() {
        return this.mContents;
    }

    /* access modifiers changed from: package-private */
    public void reinflateContents() {
        this.mContents.removeAllViews();
        LayoutInflater.from(getContext()).inflate(C0010R$layout.status_bar_notification_section_header_contents, this.mContents);
        bindContents();
    }

    /* access modifiers changed from: package-private */
    public void onUiModeChanged() {
        updateBackgroundColors();
        this.mLabelView.setTextColor(getContext().getColor(C0004R$color.notification_section_header_label_color));
        this.mClearAllButton.setImageResource(C0006R$drawable.status_bar_notification_section_header_clear_btn);
    }

    /* access modifiers changed from: package-private */
    public void setAreThereDismissableGentleNotifs(boolean z) {
        this.mClearAllButton.setVisibility(z ? 0 : 8);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean disallowSingleClick(MotionEvent motionEvent) {
        this.mTmpRect.set((float) this.mClearAllButton.getLeft(), (float) this.mClearAllButton.getTop(), (float) (this.mClearAllButton.getLeft() + this.mClearAllButton.getWidth()), (float) (this.mClearAllButton.getTop() + this.mClearAllButton.getHeight()));
        return this.mTmpRect.contains(motionEvent.getX(), motionEvent.getY());
    }

    /* access modifiers changed from: package-private */
    public void setOnHeaderClickListener(View.OnClickListener onClickListener) {
        this.mContents.setOnClickListener(onClickListener);
    }

    /* access modifiers changed from: package-private */
    public void setOnClearAllClickListener(View.OnClickListener onClickListener) {
        this.mOnClearClickListener = onClickListener;
        this.mClearAllButton.setOnClickListener(onClickListener);
    }
}
