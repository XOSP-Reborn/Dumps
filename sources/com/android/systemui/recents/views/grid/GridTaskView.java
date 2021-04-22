package com.android.systemui.recents.views.grid;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.systemui.recents.views.AnimateableViewBounds;
import com.android.systemui.recents.views.TaskView;

public class GridTaskView extends TaskView {
    private int mHeaderHeight;

    public GridTaskView(Context context) {
        this(context, null);
    }

    public GridTaskView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GridTaskView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public GridTaskView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mHeaderHeight = context.getResources().getDimensionPixelSize(2131166224);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.recents.views.TaskView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mThumbnailView.setSizeToFit(true);
        this.mThumbnailView.setOverlayHeaderOnThumbnailActionBar(false);
        this.mThumbnailView.updateThumbnailMatrix();
        this.mThumbnailView.setTranslationY((float) this.mHeaderHeight);
        this.mHeaderView.setShouldDarkenBackgroundColor(true);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.recents.views.TaskView
    public AnimateableViewBounds createOutlineProvider() {
        return new AnimateableGridViewBounds(this, ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(2131166262));
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.recents.views.TaskView
    public void onConfigurationChanged() {
        super.onConfigurationChanged();
        this.mHeaderHeight = ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(2131166224);
        this.mThumbnailView.setTranslationY((float) this.mHeaderHeight);
    }
}
