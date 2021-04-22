package com.sonymobile.keyguard.aod;

import android.app.Notification;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.sonymobile.lockscreen.notifications.SomcLockscreenNotifications;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationsBatteryView extends LinearLayout {
    private static final String TAG = "NotificationsBatteryView";
    private Context mContext;
    private boolean mIsDozing;
    private List<View> mNewArrivalViews = new ArrayList();
    private List<NotificationAnimation> mNotifAnimations = new ArrayList();
    private NotificationData mNotificationData;

    public NotificationsBatteryView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    public void setNotificationData(NotificationData notificationData) {
        this.mNotificationData = notificationData;
    }

    public void setDozing(boolean z) {
        if (this.mIsDozing != z) {
            this.mIsDozing = z;
            if (this.mIsDozing) {
                boolean isKeyguardShowing = ((StatusBar) ((SystemUIApplication) this.mContext).getComponent(StatusBar.class)).isKeyguardShowing();
                boolean shouldFilterOutOldNotifications = SomcLockscreenNotifications.shouldFilterOutOldNotifications(this.mContext);
                if (!isKeyguardShowing && shouldFilterOutOldNotifications) {
                    this.mNotificationData.filterAndSort(true);
                }
                refresh();
                return;
            }
            removeAllViews();
        }
    }

    public void refresh() {
        removeAllViews();
        addNotificationsView();
        addBatteryMeterView();
        for (int i = 0; i < this.mNotifAnimations.size(); i++) {
            this.mNotifAnimations.get(i).stopNotificationAnimation();
        }
        this.mNotifAnimations.clear();
        for (int i2 = 0; i2 < this.mNewArrivalViews.size(); i2++) {
            NotificationAnimation notificationAnimation = new NotificationAnimation(this.mContext);
            this.mNotifAnimations.add(notificationAnimation);
            notificationAnimation.startNotificationAnimation(this.mNewArrivalViews.get(i2));
        }
    }

    private void addNotificationsView() {
        ArrayList<NotificationEntry> activeNotifications = this.mNotificationData.getActiveNotifications();
        NotificationGroupManager notificationGroupManager = (NotificationGroupManager) Dependency.get(NotificationGroupManager.class);
        this.mNewArrivalViews.clear();
        Iterator<NotificationEntry> it = activeNotifications.iterator();
        int i = 0;
        boolean z = false;
        while (it.hasNext()) {
            NotificationEntry next = it.next();
            StatusBarNotification statusBarNotification = next.notification;
            Notification notification = statusBarNotification.getNotification();
            if (!next.getRow().isRemoved() && next.getRow().getVisibility() != 8) {
                if (!statusBarNotification.isGroup() || !notificationGroupManager.isChildInGroupWithSummary(statusBarNotification) || !notification.isGroupChild()) {
                    Icon smallIcon = notification.getSmallIcon();
                    if (smallIcon == null) {
                        Log.e(TAG, "small icon in notification from " + statusBarNotification.getPackageName());
                    } else {
                        long currentTimeMillis = System.currentTimeMillis();
                        long postTime = statusBarNotification.getPostTime();
                        boolean z2 = currentTimeMillis > postTime && currentTimeMillis - postTime < 500;
                        Drawable loadDrawable = smallIcon.loadDrawable(this.mContext);
                        if (loadDrawable != null) {
                            loadDrawable.mutate().setColorFilter(-1, PorterDuff.Mode.SRC_ATOP);
                            ImageView imageView = new ImageView(this.mContext);
                            imageView.setImageDrawable(loadDrawable);
                            int dimensionPixelSize = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_notification_heightwidth);
                            int dimensionPixelSize2 = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_notification_margin);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dimensionPixelSize, dimensionPixelSize);
                            layoutParams.setMargins(dimensionPixelSize2, dimensionPixelSize2, dimensionPixelSize2, dimensionPixelSize2);
                            if (i < 3) {
                                addView(imageView, layoutParams);
                                if (z2) {
                                    this.mNewArrivalViews.add(imageView);
                                }
                            } else if (z2) {
                                z = true;
                            }
                            i++;
                        }
                    }
                }
            }
        }
        addRestNotificationNumView(i, z);
    }

    private void addRestNotificationNumView(int i, boolean z) {
        TextView textView = new TextView(this.mContext);
        int i2 = i - 3;
        if (i2 > 0) {
            int dimensionPixelSize = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_notification_margin);
            int dimensionPixelSize2 = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_notification_textsize);
            textView.setText("+" + i2);
            textView.setTextColor(-1);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextSize(0, (float) dimensionPixelSize2);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
            layoutParams.setMargins(dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
            layoutParams.gravity = 16;
            addView(textView, layoutParams);
            if (z) {
                this.mNewArrivalViews.add(textView);
            }
        }
    }

    private void addBatteryMeterView() {
        int dimensionPixelSize = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_notification_heightwidth);
        int dimensionPixelSize2 = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_notification_margin);
        BatteryMeterView batteryMeterView = new BatteryMeterView(this.mContext);
        batteryMeterView.setForceShowPercent(true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, dimensionPixelSize);
        layoutParams.setMargins(dimensionPixelSize2, dimensionPixelSize2, dimensionPixelSize2, dimensionPixelSize2);
        layoutParams.gravity = 16;
        addView(batteryMeterView, layoutParams);
    }

    public void onUpdateNotifications() {
        if (this.mIsDozing) {
            refresh();
        }
    }
}
