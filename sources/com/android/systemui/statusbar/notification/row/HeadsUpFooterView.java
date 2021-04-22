package com.android.systemui.statusbar.notification.row;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0007R$id;
import com.android.systemui.statusbar.notification.MultiWindowButtonManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.stack.ExpandableViewState;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sonymobile.systemui.statusbar.MultiWindowButtonLogger;

public class HeadsUpFooterView extends StackScrollerDecorView {
    private HeadsUpManager mHeadsUpManager;
    private boolean mIsNotificationExpanded = false;
    private boolean mIsTargetEntryValid = false;
    private FloatingActionButton mMultiWindowButton;
    private MultiWindowButtonManager mMultiWindowButtonManager;

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.StackScrollerDecorView
    public View findSecondaryView() {
        return null;
    }

    public HeadsUpFooterView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMultiWindowButtonManager = new MultiWindowButtonManager(context);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.StackScrollerDecorView
    public View findContentView() {
        return findViewById(C0007R$id.heads_up_footer);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.StackScrollerDecorView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mMultiWindowButton = (FloatingActionButton) findViewById(C0007R$id.heads_up_multiwindow_button);
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public ExpandableViewState createExpandableViewState() {
        return new HeadsUpFooterState();
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void setIsNotificationExpanded(boolean z) {
        if (MultiWindowButtonManager.DEBUG_MW) {
            String str = MultiWindowButtonManager.DEBUG_MW_TAG;
            Log.d(str, "setIsNotificationExpanded: " + z);
        }
        this.mIsNotificationExpanded = z;
        setVisible();
    }

    public void setEntry(NotificationEntry notificationEntry, boolean z) {
        HeadsUpManager headsUpManager;
        Runnable multiWindowButtonInvoker;
        if (this.mMultiWindowButton == null || (headsUpManager = this.mHeadsUpManager) == null) {
            setVisible(false, false);
            return;
        }
        NotificationEntry topEntry = headsUpManager.getTopEntry();
        if (MultiWindowButtonManager.DEBUG_MW) {
            String str = MultiWindowButtonManager.DEBUG_MW_TAG;
            Log.d(str, "setEntry " + notificationEntry + " " + z + " targetEntry=" + topEntry);
        }
        this.mIsTargetEntryValid = false;
        if ((notificationEntry != topEntry || z) && this.mMultiWindowButtonManager.isForegroundResizeable() && (multiWindowButtonInvoker = this.mMultiWindowButtonManager.getMultiWindowButtonInvoker(topEntry)) != null) {
            this.mMultiWindowButton.setOnClickListener(new View.OnClickListener(multiWindowButtonInvoker, topEntry) {
                /* class com.android.systemui.statusbar.notification.row.$$Lambda$HeadsUpFooterView$hDnr4vx3BZ5H0vWiU2PCG1wvs */
                private final /* synthetic */ Runnable f$1;
                private final /* synthetic */ NotificationEntry f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void onClick(View view) {
                    HeadsUpFooterView.this.lambda$setEntry$0$HeadsUpFooterView(this.f$1, this.f$2, view);
                }
            });
            setColor(topEntry.getRow().getShowingLayout().shouldUseDark());
            this.mIsTargetEntryValid = true;
            if (z && !this.mIsNotificationExpanded) {
                MultiWindowButtonLogger.logEvent(((FrameLayout) this).mContext, "headsup_show");
            }
        }
        setVisible();
    }

    public /* synthetic */ void lambda$setEntry$0$HeadsUpFooterView(Runnable runnable, NotificationEntry notificationEntry, View view) {
        runnable.run();
        HeadsUpManager headsUpManager = this.mHeadsUpManager;
        if (headsUpManager != null) {
            headsUpManager.removeNotification(notificationEntry.key, true);
        }
        MultiWindowButtonLogger.logEvent(((FrameLayout) this).mContext, "headsup_click");
    }

    public boolean isValid() {
        return !this.mIsNotificationExpanded && this.mIsTargetEntryValid;
    }

    private void setVisible() {
        if (isValid()) {
            setVisible(true, true);
        } else {
            setVisible(false, false);
        }
    }

    private void setColor(boolean z) {
        int i;
        int i2;
        if (z) {
            i = ((FrameLayout) this).mContext.getColor(C0004R$color.notification_button_light_background_color);
            i2 = ((FrameLayout) this).mContext.getColor(C0004R$color.notification_button_dark_image_color);
        } else {
            i = ((FrameLayout) this).mContext.getColor(C0004R$color.notification_button_dark_background_color);
            i2 = ((FrameLayout) this).mContext.getColor(C0004R$color.notification_button_light_image_color);
        }
        this.mMultiWindowButton.setBackgroundTintList(ColorStateList.valueOf(i));
        this.mMultiWindowButton.setImageTintList(ColorStateList.valueOf(i2));
    }

    public class HeadsUpFooterState extends ExpandableViewState {
        public HeadsUpFooterState() {
        }

        @Override // com.android.systemui.statusbar.notification.stack.ViewState, com.android.systemui.statusbar.notification.stack.ExpandableViewState
        public void applyToView(View view) {
            if (MultiWindowButtonManager.DEBUG_MW) {
                String str = MultiWindowButtonManager.DEBUG_MW_TAG;
                Log.d(str, "applyToView " + view + " / yTrans=" + this.yTranslation + " clipTop=" + this.clipTopAmount + " h=" + this.height);
            }
            super.applyToView(view);
            if (view instanceof HeadsUpFooterView) {
                HeadsUpFooterView headsUpFooterView = (HeadsUpFooterView) view;
                HeadsUpFooterView.this.mMultiWindowButton.setEnabled(headsUpFooterView.isVisible());
                HeadsUpFooterView.this.mMultiWindowButton.setClickable(headsUpFooterView.isVisible());
            }
        }
    }
}
