package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.collection.ArrayMap;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;

public class NotificationIconAreaController implements DarkIconDispatcher.DarkReceiver, StatusBarStateController.StateListener {
    private boolean mAnimationsEnabled;
    private NotificationIconContainer mCenteredIcon;
    protected View mCenteredIconArea;
    private int mCenteredIconTint = -1;
    private StatusBarIconView mCenteredIconView;
    private Context mContext;
    private final ContrastColorUtil mContrastColorUtil;
    private float mDarkAmount;
    private final NotificationEntryManager mEntryManager;
    private boolean mFullyDark;
    private int mIconHPadding;
    private int mIconSize;
    private int mIconTint = -1;
    private final NotificationMediaManager mMediaManager;
    protected View mNotificationIconArea;
    private NotificationIconContainer mNotificationIcons;
    private ViewGroup mNotificationScrollLayout;
    private NotificationIconContainer mShelfIcons;
    private StatusBar mStatusBar;
    private final StatusBarStateController mStatusBarStateController;
    private final Rect mTintArea = new Rect();
    private final Runnable mUpdateStatusBarIcons = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$NWCrb8vzuopzf5kAygkNeXndtBo */

        public final void run() {
            NotificationIconAreaController.this.updateStatusBarIcons();
        }
    };

    public NotificationIconAreaController(Context context, StatusBar statusBar, StatusBarStateController statusBarStateController, NotificationMediaManager notificationMediaManager) {
        this.mStatusBar = statusBar;
        this.mContrastColorUtil = ContrastColorUtil.getInstance(context);
        this.mContext = context;
        this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        this.mStatusBarStateController = statusBarStateController;
        this.mStatusBarStateController.addCallback(this);
        this.mMediaManager = notificationMediaManager;
        initializeNotificationAreaViews(context);
    }

    /* access modifiers changed from: protected */
    public View inflateIconArea(LayoutInflater layoutInflater) {
        return layoutInflater.inflate(C0010R$layout.notification_icon_area, (ViewGroup) null);
    }

    /* access modifiers changed from: protected */
    public void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);
        LayoutInflater from = LayoutInflater.from(context);
        this.mNotificationIconArea = inflateIconArea(from);
        this.mNotificationIcons = (NotificationIconContainer) this.mNotificationIconArea.findViewById(C0007R$id.notificationIcons);
        this.mNotificationScrollLayout = this.mStatusBar.getNotificationScrollLayout();
        this.mCenteredIconArea = from.inflate(C0010R$layout.center_icon_area, (ViewGroup) null);
        this.mCenteredIcon = (NotificationIconContainer) this.mCenteredIconArea.findViewById(C0007R$id.centeredIcon);
    }

    public void setupShelf(NotificationShelf notificationShelf) {
        this.mShelfIcons = notificationShelf.getShelfIcons();
        notificationShelf.setCollapsedIcons(this.mNotificationIcons);
    }

    public void onDensityOrFontScaleChanged(Context context) {
        reloadDimens(context);
        FrameLayout.LayoutParams generateIconLayoutParams = generateIconLayoutParams();
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            this.mNotificationIcons.getChildAt(i).setLayoutParams(generateIconLayoutParams);
        }
        for (int i2 = 0; i2 < this.mShelfIcons.getChildCount(); i2++) {
            this.mShelfIcons.getChildAt(i2).setLayoutParams(generateIconLayoutParams);
        }
        for (int i3 = 0; i3 < this.mCenteredIcon.getChildCount(); i3++) {
            this.mCenteredIcon.getChildAt(i3).setLayoutParams(generateIconLayoutParams);
        }
    }

    private FrameLayout.LayoutParams generateIconLayoutParams() {
        return new FrameLayout.LayoutParams(this.mIconSize + (this.mIconHPadding * 2), getHeight());
    }

    private void reloadDimens(Context context) {
        Resources resources = context.getResources();
        this.mIconSize = resources.getDimensionPixelSize(17105430);
        this.mIconHPadding = resources.getDimensionPixelSize(C0005R$dimen.status_bar_icon_padding);
    }

    public View getNotificationInnerAreaView() {
        return this.mNotificationIconArea;
    }

    public View getCenteredNotificationAreaView() {
        return this.mCenteredIconArea;
    }

    @Override // com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver
    public void onDarkChanged(Rect rect, float f, int i) {
        if (rect == null) {
            this.mTintArea.setEmpty();
        } else {
            this.mTintArea.set(rect);
        }
        View view = this.mNotificationIconArea;
        if (view == null) {
            this.mIconTint = i;
        } else if (DarkIconDispatcher.isInArea(rect, view)) {
            this.mIconTint = i;
        }
        View view2 = this.mCenteredIconArea;
        if (view2 == null) {
            this.mCenteredIconTint = i;
        } else if (DarkIconDispatcher.isInArea(rect, view2)) {
            this.mCenteredIconTint = i;
        }
        applyNotificationIconsTint();
    }

    /* access modifiers changed from: protected */
    public int getHeight() {
        return this.mStatusBar.getStatusBarHeight();
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowNotificationIcon(NotificationEntry notificationEntry, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) {
        StatusBarIconView statusBarIconView = notificationEntry.centeredIcon;
        if (z6 == (statusBarIconView != null && Objects.equals(statusBarIconView, this.mCenteredIconView))) {
            return false;
        }
        if (this.mEntryManager.getNotificationData().isAmbient(notificationEntry.key) && !z) {
            return false;
        }
        if (z5 && notificationEntry.key.equals(this.mMediaManager.getMediaNotificationKey())) {
            return false;
        }
        if ((!z2 && !notificationEntry.isHighPriority()) || !notificationEntry.isTopLevelChild() || notificationEntry.getRow().getVisibility() == 8) {
            return false;
        }
        if (notificationEntry.isRowDismissed() && z3) {
            return false;
        }
        if (!z4 || !notificationEntry.isLastMessageFromReply()) {
            return (z && !this.mFullyDark) || !notificationEntry.shouldSuppressStatusBar();
        }
        return false;
    }

    public void updateNotificationIcons() {
        updateStatusBarIcons();
        updateShelfIcons();
        updateCenterIcon();
        applyNotificationIconsTint();
    }

    private void updateShelfIcons() {
        $$Lambda$NotificationIconAreaController$afpYK1wAP1i0HTFHOa1jb1wzzAQ r1 = $$Lambda$NotificationIconAreaController$afpYK1wAP1i0HTFHOa1jb1wzzAQ.INSTANCE;
        NotificationIconContainer notificationIconContainer = this.mShelfIcons;
        boolean z = this.mFullyDark;
        updateIconsForLayout(r1, notificationIconContainer, true, true, false, z, z, true);
    }

    public void updateStatusBarIcons() {
        updateIconsForLayout($$Lambda$NotificationIconAreaController$ujxUrqwlryo8PHBzga56kRshsA.INSTANCE, this.mNotificationIcons, false, true, true, true, false, true);
    }

    private void updateCenterIcon() {
        updateIconsForLayout($$Lambda$NotificationIconAreaController$S6CJ2tXrA2ieNVmUpwBa8v9eeEY.INSTANCE, this.mCenteredIcon, false, true, false, false, this.mFullyDark, false);
    }

    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
        updateAnimations();
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        updateAnimations();
    }

    private void updateAnimations() {
        boolean z = true;
        boolean z2 = this.mStatusBarStateController.getState() == 0;
        this.mCenteredIcon.setAnimationsEnabled(this.mAnimationsEnabled && z2);
        NotificationIconContainer notificationIconContainer = this.mNotificationIcons;
        if (!this.mAnimationsEnabled || !z2) {
            z = false;
        }
        notificationIconContainer.setAnimationsEnabled(z);
    }

    private void updateIconsForLayout(Function<NotificationEntry, StatusBarIconView> function, NotificationIconContainer notificationIconContainer, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) {
        ArrayList arrayList = new ArrayList(this.mNotificationScrollLayout.getChildCount());
        for (int i = 0; i < this.mNotificationScrollLayout.getChildCount(); i++) {
            View childAt = this.mNotificationScrollLayout.getChildAt(i);
            if (childAt instanceof ExpandableNotificationRow) {
                NotificationEntry entry = ((ExpandableNotificationRow) childAt).getEntry();
                if (shouldShowNotificationIcon(entry, z, z2, z3, z4, z5, z6)) {
                    StatusBarIconView apply = function.apply(entry);
                    if (apply != null) {
                        arrayList.add(apply);
                    }
                }
            }
        }
        ArrayMap<String, ArrayList<StatusBarIcon>> arrayMap = new ArrayMap<>();
        ArrayList arrayList2 = new ArrayList();
        for (int i2 = 0; i2 < notificationIconContainer.getChildCount(); i2++) {
            View childAt2 = notificationIconContainer.getChildAt(i2);
            if ((childAt2 instanceof StatusBarIconView) && !arrayList.contains(childAt2)) {
                StatusBarIconView statusBarIconView = (StatusBarIconView) childAt2;
                String groupKey = statusBarIconView.getNotification().getGroupKey();
                int i3 = 0;
                boolean z7 = false;
                while (true) {
                    if (i3 >= arrayList.size()) {
                        break;
                    }
                    StatusBarIconView statusBarIconView2 = (StatusBarIconView) arrayList.get(i3);
                    if (statusBarIconView2.getSourceIcon().sameAs(statusBarIconView.getSourceIcon()) && statusBarIconView2.getNotification().getGroupKey().equals(groupKey)) {
                        if (z7) {
                            z7 = false;
                            break;
                        }
                        z7 = true;
                    }
                    i3++;
                }
                if (z7) {
                    ArrayList<StatusBarIcon> arrayList3 = arrayMap.get(groupKey);
                    if (arrayList3 == null) {
                        arrayList3 = new ArrayList<>();
                        arrayMap.put(groupKey, arrayList3);
                    }
                    arrayList3.add(statusBarIconView.getStatusBarIcon());
                }
                arrayList2.add(statusBarIconView);
            }
        }
        ArrayList arrayList4 = new ArrayList();
        for (String str : arrayMap.keySet()) {
            if (arrayMap.get(str).size() != 1) {
                arrayList4.add(str);
            }
        }
        arrayMap.removeAll(arrayList4);
        notificationIconContainer.setReplacingIcons(arrayMap);
        int size = arrayList2.size();
        for (int i4 = 0; i4 < size; i4++) {
            notificationIconContainer.removeView((View) arrayList2.get(i4));
        }
        ViewGroup.LayoutParams generateIconLayoutParams = generateIconLayoutParams();
        for (int i5 = 0; i5 < arrayList.size(); i5++) {
            StatusBarIconView statusBarIconView3 = (StatusBarIconView) arrayList.get(i5);
            notificationIconContainer.removeTransientView(statusBarIconView3);
            if (statusBarIconView3.getParent() == null) {
                if (z3) {
                    statusBarIconView3.setOnDismissListener(this.mUpdateStatusBarIcons);
                }
                notificationIconContainer.addView(statusBarIconView3, i5, generateIconLayoutParams);
            }
        }
        notificationIconContainer.setChangingViewPositions(true);
        int childCount = notificationIconContainer.getChildCount();
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt3 = notificationIconContainer.getChildAt(i6);
            View view = (StatusBarIconView) arrayList.get(i6);
            if (childAt3 != view) {
                notificationIconContainer.removeView(view);
                notificationIconContainer.addView(view, i6);
            }
        }
        notificationIconContainer.setChangingViewPositions(false);
        notificationIconContainer.setReplacingIcons(null);
    }

    private void applyNotificationIconsTint() {
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            StatusBarIconView statusBarIconView = (StatusBarIconView) this.mNotificationIcons.getChildAt(i);
            if (statusBarIconView.getWidth() != 0) {
                updateTintForIcon(statusBarIconView, this.mIconTint);
            } else {
                statusBarIconView.executeOnLayout(new Runnable(statusBarIconView) {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$NotificationIconAreaController$LHvVP8ZKDxtcx6Sj3gf4ttey2ho */
                    private final /* synthetic */ StatusBarIconView f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NotificationIconAreaController.this.lambda$applyNotificationIconsTint$3$NotificationIconAreaController(this.f$1);
                    }
                });
            }
        }
        for (int i2 = 0; i2 < this.mCenteredIcon.getChildCount(); i2++) {
            StatusBarIconView statusBarIconView2 = (StatusBarIconView) this.mCenteredIcon.getChildAt(i2);
            if (statusBarIconView2.getWidth() != 0) {
                updateTintForIcon(statusBarIconView2, this.mCenteredIconTint);
            } else {
                statusBarIconView2.executeOnLayout(new Runnable(statusBarIconView2) {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$NotificationIconAreaController$kEHcYKNlJqRNuom7zI__dD3YiUQ */
                    private final /* synthetic */ StatusBarIconView f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NotificationIconAreaController.this.lambda$applyNotificationIconsTint$4$NotificationIconAreaController(this.f$1);
                    }
                });
            }
        }
    }

    public /* synthetic */ void lambda$applyNotificationIconsTint$3$NotificationIconAreaController(StatusBarIconView statusBarIconView) {
        updateTintForIcon(statusBarIconView, this.mIconTint);
    }

    public /* synthetic */ void lambda$applyNotificationIconsTint$4$NotificationIconAreaController(StatusBarIconView statusBarIconView) {
        updateTintForIcon(statusBarIconView, this.mCenteredIconTint);
    }

    private void updateTintForIcon(StatusBarIconView statusBarIconView, int i) {
        int i2 = 0;
        if (!Boolean.TRUE.equals(statusBarIconView.getTag(C0007R$id.icon_is_pre_L)) || NotificationUtils.isGrayscale(statusBarIconView, this.mContrastColorUtil)) {
            i2 = DarkIconDispatcher.getTint(this.mTintArea, statusBarIconView, i);
        }
        statusBarIconView.setStaticDrawableColor(i2);
        statusBarIconView.setDecorColor(i);
    }

    public void showIconIsolated(StatusBarIconView statusBarIconView, boolean z) {
        this.mNotificationIcons.showIconIsolated(statusBarIconView, z);
    }

    public void setIsolatedIconLocation(Rect rect, boolean z) {
        this.mNotificationIcons.setIsolatedIconLocation(rect, z);
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozeAmountChanged(float f, float f2) {
        this.mDarkAmount = f;
        boolean z = this.mDarkAmount == 1.0f;
        if (this.mFullyDark != z) {
            this.mFullyDark = z;
            updateShelfIcons();
        }
    }
}
