package com.android.systemui.statusbar.phone;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.ViewClippingUtil;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.HeadsUpStatusBarView;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HeadsUpAppearanceController implements OnHeadsUpChangedListener, DarkIconDispatcher.DarkReceiver {
    private boolean mAnimationsEnabled;
    private final View mCenteredIconView;
    private final View mClockView;
    private final DarkIconDispatcher mDarkIconDispatcher;
    @VisibleForTesting
    float mExpandFraction;
    @VisibleForTesting
    float mExpandedHeight;
    private final HeadsUpManagerPhone mHeadsUpManager;
    private final HeadsUpStatusBarView mHeadsUpStatusBarView;
    @VisibleForTesting
    boolean mIsExpanded;
    private final NotificationIconAreaController mNotificationIconAreaController;
    private final View mOperatorNameView;
    private final NotificationPanelView mPanelView;
    private final ViewClippingUtil.ClippingParameters mParentClippingParams;
    Point mPoint;
    private final BiConsumer<Float, Float> mSetExpandedHeight;
    private final Consumer<ExpandableNotificationRow> mSetTrackingHeadsUp;
    private boolean mShown;
    private final View.OnLayoutChangeListener mStackScrollLayoutChangeListener;
    private final NotificationStackScrollLayout mStackScroller;
    private ExpandableNotificationRow mTrackedChild;
    private final Runnable mUpdatePanelTranslation;

    public /* synthetic */ void lambda$new$0$HeadsUpAppearanceController(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updatePanelTranslation();
    }

    public HeadsUpAppearanceController(NotificationIconAreaController notificationIconAreaController, HeadsUpManagerPhone headsUpManagerPhone, View view) {
        this(notificationIconAreaController, headsUpManagerPhone, (HeadsUpStatusBarView) view.findViewById(C0007R$id.heads_up_status_bar_view), (NotificationStackScrollLayout) view.findViewById(C0007R$id.notification_stack_scroller), (NotificationPanelView) view.findViewById(C0007R$id.notification_panel), view.findViewById(C0007R$id.clock), view.findViewById(C0007R$id.operator_name_frame), view.findViewById(C0007R$id.centered_icon_area));
    }

    @VisibleForTesting
    public HeadsUpAppearanceController(NotificationIconAreaController notificationIconAreaController, HeadsUpManagerPhone headsUpManagerPhone, HeadsUpStatusBarView headsUpStatusBarView, NotificationStackScrollLayout notificationStackScrollLayout, NotificationPanelView notificationPanelView, View view, View view2, View view3) {
        this.mSetTrackingHeadsUp = new Consumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$u27UVgFXO2FqgY8QI0m_qAQyl8 */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                HeadsUpAppearanceController.this.setTrackingHeadsUp((ExpandableNotificationRow) obj);
            }
        };
        this.mUpdatePanelTranslation = new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$22QZFjoGlQJQoKOrFebHbZltB4 */

            public final void run() {
                HeadsUpAppearanceController.this.updatePanelTranslation();
            }
        };
        this.mSetExpandedHeight = new BiConsumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$DialNTWPBOn27MISeLu6p9klZxI */

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HeadsUpAppearanceController.this.setExpandedHeight(((Float) obj).floatValue(), ((Float) obj2).floatValue());
            }
        };
        this.mStackScrollLayoutChangeListener = new View.OnLayoutChangeListener() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$HeadsUpAppearanceController$hwNOwOgXItDjQM7QwL00pigpnrk */

            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                HeadsUpAppearanceController.this.lambda$new$0$HeadsUpAppearanceController(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        };
        this.mParentClippingParams = new ViewClippingUtil.ClippingParameters() {
            /* class com.android.systemui.statusbar.phone.HeadsUpAppearanceController.AnonymousClass1 */

            public boolean shouldFinish(View view) {
                return view.getId() == C0007R$id.status_bar;
            }
        };
        this.mAnimationsEnabled = true;
        this.mNotificationIconAreaController = notificationIconAreaController;
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpStatusBarView = headsUpStatusBarView;
        this.mCenteredIconView = view3;
        headsUpStatusBarView.setOnDrawingRectChangedListener(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$HeadsUpAppearanceController$1d3l5klDiH8maZOdHwrJBKgigPE */

            public final void run() {
                HeadsUpAppearanceController.this.lambda$new$1$HeadsUpAppearanceController();
            }
        });
        this.mStackScroller = notificationStackScrollLayout;
        this.mPanelView = notificationPanelView;
        notificationPanelView.addTrackingHeadsUpListener(this.mSetTrackingHeadsUp);
        notificationPanelView.addVerticalTranslationListener(this.mUpdatePanelTranslation);
        notificationPanelView.setHeadsUpAppearanceController(this);
        this.mStackScroller.addOnExpandedHeightListener(this.mSetExpandedHeight);
        this.mStackScroller.addOnLayoutChangeListener(this.mStackScrollLayoutChangeListener);
        this.mStackScroller.setHeadsUpAppearanceController(this);
        this.mClockView = view;
        this.mOperatorNameView = view2;
        this.mDarkIconDispatcher = (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class);
        this.mDarkIconDispatcher.addDarkReceiver(this);
        this.mHeadsUpStatusBarView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            /* class com.android.systemui.statusbar.phone.HeadsUpAppearanceController.AnonymousClass2 */

            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                if (HeadsUpAppearanceController.this.shouldBeVisible()) {
                    HeadsUpAppearanceController.this.updateTopEntry();
                    HeadsUpAppearanceController.this.mStackScroller.requestLayout();
                }
                HeadsUpAppearanceController.this.mHeadsUpStatusBarView.removeOnLayoutChangeListener(this);
            }
        });
    }

    public /* synthetic */ void lambda$new$1$HeadsUpAppearanceController() {
        updateIsolatedIconLocation(true);
    }

    public void destroy() {
        this.mHeadsUpManager.removeListener(this);
        this.mHeadsUpStatusBarView.setOnDrawingRectChangedListener(null);
        this.mPanelView.removeTrackingHeadsUpListener(this.mSetTrackingHeadsUp);
        this.mPanelView.removeVerticalTranslationListener(this.mUpdatePanelTranslation);
        this.mPanelView.setHeadsUpAppearanceController(null);
        this.mStackScroller.removeOnExpandedHeightListener(this.mSetExpandedHeight);
        this.mStackScroller.removeOnLayoutChangeListener(this.mStackScrollLayoutChangeListener);
        this.mDarkIconDispatcher.removeDarkReceiver(this);
    }

    private void updateIsolatedIconLocation(boolean z) {
        this.mNotificationIconAreaController.setIsolatedIconLocation(this.mHeadsUpStatusBarView.getIconDrawingRect(), z);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinned(NotificationEntry notificationEntry) {
        updateTopEntry();
        lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(notificationEntry);
    }

    private int getRtlTranslation() {
        int i;
        if (this.mPoint == null) {
            this.mPoint = new Point();
        }
        int i2 = 0;
        if (this.mStackScroller.getDisplay() != null) {
            this.mStackScroller.getDisplay().getRealSize(this.mPoint);
            i = this.mPoint.x;
        } else {
            i = 0;
        }
        WindowInsets rootWindowInsets = this.mStackScroller.getRootWindowInsets();
        DisplayCutout displayCutout = rootWindowInsets != null ? rootWindowInsets.getDisplayCutout() : null;
        int stableInsetLeft = rootWindowInsets != null ? rootWindowInsets.getStableInsetLeft() : 0;
        int stableInsetRight = rootWindowInsets != null ? rootWindowInsets.getStableInsetRight() : 0;
        int safeInsetLeft = displayCutout != null ? displayCutout.getSafeInsetLeft() : 0;
        if (displayCutout != null) {
            i2 = displayCutout.getSafeInsetRight();
        }
        return ((Math.max(stableInsetLeft, safeInsetLeft) + this.mStackScroller.getRight()) + Math.max(stableInsetRight, i2)) - i;
    }

    public void updatePanelTranslation() {
        int i;
        if (this.mStackScroller.isLayoutRtl()) {
            i = getRtlTranslation();
        } else {
            i = this.mStackScroller.getLeft();
        }
        this.mHeadsUpStatusBarView.setPanelTranslation(((float) i) + this.mStackScroller.getTranslationX());
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x003e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateTopEntry() {
        /*
            r5 = this;
            boolean r0 = r5.mIsExpanded
            r1 = 0
            if (r0 != 0) goto L_0x0014
            com.android.systemui.statusbar.phone.HeadsUpManagerPhone r0 = r5.mHeadsUpManager
            boolean r0 = r0.hasPinnedHeadsUp()
            if (r0 == 0) goto L_0x0014
            com.android.systemui.statusbar.phone.HeadsUpManagerPhone r0 = r5.mHeadsUpManager
            com.android.systemui.statusbar.notification.collection.NotificationEntry r0 = r0.getTopEntry()
            goto L_0x0015
        L_0x0014:
            r0 = r1
        L_0x0015:
            com.android.systemui.statusbar.HeadsUpStatusBarView r2 = r5.mHeadsUpStatusBarView
            com.android.systemui.statusbar.notification.collection.NotificationEntry r2 = r2.getShowingEntry()
            com.android.systemui.statusbar.HeadsUpStatusBarView r3 = r5.mHeadsUpStatusBarView
            r3.setEntry(r0)
            if (r0 == r2) goto L_0x0043
            r3 = 1
            r4 = 0
            if (r0 != 0) goto L_0x002d
            r5.setShown(r4)
            boolean r2 = r5.mIsExpanded
        L_0x002b:
            r2 = r2 ^ r3
            goto L_0x0036
        L_0x002d:
            if (r2 != 0) goto L_0x0035
            r5.setShown(r3)
            boolean r2 = r5.mIsExpanded
            goto L_0x002b
        L_0x0035:
            r2 = r4
        L_0x0036:
            r5.updateIsolatedIconLocation(r4)
            com.android.systemui.statusbar.phone.NotificationIconAreaController r5 = r5.mNotificationIconAreaController
            if (r0 != 0) goto L_0x003e
            goto L_0x0040
        L_0x003e:
            com.android.systemui.statusbar.StatusBarIconView r1 = r0.icon
        L_0x0040:
            r5.showIconIsolated(r1, r2)
        L_0x0043:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.HeadsUpAppearanceController.updateTopEntry():void");
    }

    private void setShown(boolean z) {
        if (this.mShown != z) {
            this.mShown = z;
            if (z) {
                updateParentClipping(false);
                this.mHeadsUpStatusBarView.setVisibility(0);
                show(this.mHeadsUpStatusBarView);
                hide(this.mClockView, 4);
                if (this.mCenteredIconView.getVisibility() != 8) {
                    hide(this.mCenteredIconView, 4);
                }
                View view = this.mOperatorNameView;
                if (view != null) {
                    hide(view, 4);
                    return;
                }
                return;
            }
            show(this.mClockView);
            if (this.mCenteredIconView.getVisibility() != 8) {
                show(this.mCenteredIconView);
            }
            View view2 = this.mOperatorNameView;
            if (view2 != null) {
                show(view2);
            }
            hide(this.mHeadsUpStatusBarView, 8, new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$HeadsUpAppearanceController$iMPD_cMpkAUOLIdQAujzNCdyYQ */

                public final void run() {
                    HeadsUpAppearanceController.this.lambda$setShown$2$HeadsUpAppearanceController();
                }
            });
        }
    }

    public /* synthetic */ void lambda$setShown$2$HeadsUpAppearanceController() {
        updateParentClipping(true);
    }

    private void updateParentClipping(boolean z) {
        ViewClippingUtil.setClippingDeactivated(this.mHeadsUpStatusBarView, !z, this.mParentClippingParams);
    }

    private void hide(View view, int i) {
        hide(view, i, null);
    }

    private void hide(View view, int i, Runnable runnable) {
        if (this.mAnimationsEnabled) {
            CrossFadeHelper.fadeOut(view, 110, 0, new Runnable(view, i, runnable) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$HeadsUpAppearanceController$6jWM7O8t5p3KhJ2lcC8glbZxW9w */
                private final /* synthetic */ View f$0;
                private final /* synthetic */ int f$1;
                private final /* synthetic */ Runnable f$2;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    HeadsUpAppearanceController.lambda$hide$3(this.f$0, this.f$1, this.f$2);
                }
            });
            return;
        }
        view.setVisibility(i);
        if (runnable != null) {
            runnable.run();
        }
    }

    static /* synthetic */ void lambda$hide$3(View view, int i, Runnable runnable) {
        view.setVisibility(i);
        if (runnable != null) {
            runnable.run();
        }
    }

    private void show(View view) {
        if (this.mAnimationsEnabled) {
            CrossFadeHelper.fadeIn(view, 110, 100);
        } else {
            view.setVisibility(0);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
    }

    @VisibleForTesting
    public boolean isShown() {
        return this.mShown;
    }

    public boolean shouldBeVisible() {
        return !this.mIsExpanded && this.mHeadsUpManager.hasPinnedHeadsUp();
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpUnPinned(NotificationEntry notificationEntry) {
        updateTopEntry();
        lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(notificationEntry);
    }

    public void setExpandedHeight(float f, float f2) {
        boolean z = true;
        boolean z2 = f != this.mExpandedHeight;
        this.mExpandedHeight = f;
        this.mExpandFraction = f2;
        if (f <= 0.0f) {
            z = false;
        }
        if (z2) {
            updateHeadsUpHeaders();
        }
        if (z != this.mIsExpanded) {
            this.mIsExpanded = z;
            updateTopEntry();
        }
    }

    public void setTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        ExpandableNotificationRow expandableNotificationRow2 = this.mTrackedChild;
        this.mTrackedChild = expandableNotificationRow;
        if (expandableNotificationRow2 != null) {
            lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(expandableNotificationRow2.getEntry());
        }
    }

    private void updateHeadsUpHeaders() {
        this.mHeadsUpManager.getAllEntries().forEach(new Consumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$HeadsUpAppearanceController$r_oAtsVltLEqS4w4SiU08R_o1A */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                HeadsUpAppearanceController.this.lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController((NotificationEntry) obj);
            }
        });
    }

    /* renamed from: updateHeader */
    public void lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(NotificationEntry notificationEntry) {
        float f;
        ExpandableNotificationRow row = notificationEntry.getRow();
        if (row.isPinned() || row.isHeadsUpAnimatingAway() || row == this.mTrackedChild) {
            f = this.mExpandFraction;
        } else {
            f = 1.0f;
        }
        row.setHeaderVisibleAmount(f);
    }

    @Override // com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver
    public void onDarkChanged(Rect rect, float f, int i) {
        this.mHeadsUpStatusBarView.onDarkChanged(rect, f, i);
    }

    public void setPublicMode(boolean z) {
        this.mHeadsUpStatusBarView.setPublicMode(z);
        updateTopEntry();
    }

    /* access modifiers changed from: package-private */
    public void readFrom(HeadsUpAppearanceController headsUpAppearanceController) {
        if (headsUpAppearanceController != null) {
            this.mTrackedChild = headsUpAppearanceController.mTrackedChild;
            this.mExpandedHeight = headsUpAppearanceController.mExpandedHeight;
            this.mIsExpanded = headsUpAppearanceController.mIsExpanded;
            this.mExpandFraction = headsUpAppearanceController.mExpandFraction;
        }
    }
}
