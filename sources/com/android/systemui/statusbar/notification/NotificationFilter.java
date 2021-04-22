package com.android.systemui.statusbar.notification;

import android.app.AppGlobals;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBar;

public class NotificationFilter {
    private NotificationData.KeyguardEnvironment mEnvironment;
    private ForegroundServiceController mFsc;
    private final NotificationGroupManager mGroupManager = ((NotificationGroupManager) Dependency.get(NotificationGroupManager.class));
    private boolean mNotificationsHidden;
    private ShadeController mShadeController;
    private long mTimeNotificationsWereLastViewed = 0;
    private NotificationLockscreenUserManager mUserManager;

    private NotificationData.KeyguardEnvironment getEnvironment() {
        if (this.mEnvironment == null) {
            this.mEnvironment = (NotificationData.KeyguardEnvironment) Dependency.get(NotificationData.KeyguardEnvironment.class);
        }
        return this.mEnvironment;
    }

    private ShadeController getShadeController() {
        if (this.mShadeController == null) {
            this.mShadeController = (ShadeController) Dependency.get(ShadeController.class);
        }
        return this.mShadeController;
    }

    private ForegroundServiceController getFsc() {
        if (this.mFsc == null) {
            this.mFsc = (ForegroundServiceController) Dependency.get(ForegroundServiceController.class);
        }
        return this.mFsc;
    }

    private NotificationLockscreenUserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
        }
        return this.mUserManager;
    }

    public boolean shouldFilterOut(NotificationEntry notificationEntry) {
        return shouldFilterOut(notificationEntry, false);
    }

    public boolean shouldFilterOut(NotificationEntry notificationEntry, boolean z) {
        String[] stringArray;
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if ((!getEnvironment().isDeviceProvisioned() && !showNotificationEvenIfUnprovisioned(statusBarNotification)) || !getEnvironment().isNotificationForCurrentProfiles(statusBarNotification)) {
            return true;
        }
        if (isMediaNotification(statusBarNotification) || isWfcEmergencyCallLimitationDialogNotification(statusBarNotification)) {
            return false;
        }
        if (getUserManager().isLockscreenPublicMode(statusBarNotification.getUserId()) && (statusBarNotification.getNotification().visibility == -1 || getUserManager().shouldHideNotifications(statusBarNotification.getUserId()) || getUserManager().shouldHideNotifications(statusBarNotification.getKey()))) {
            return true;
        }
        if (getShadeController().isDozing() && notificationEntry.shouldSuppressAmbient()) {
            return true;
        }
        if ((!getShadeController().isDozing() && notificationEntry.shouldSuppressNotificationList()) || notificationEntry.suspended) {
            return true;
        }
        if (!StatusBar.ENABLE_CHILD_NOTIFICATIONS && this.mGroupManager.isChildInGroupWithSummary(statusBarNotification)) {
            return true;
        }
        if (getFsc().isDisclosureNotification(statusBarNotification) && !getFsc().isDisclosureNeededForUser(statusBarNotification.getUserId())) {
            return true;
        }
        if (getFsc().isSystemAlertNotification(statusBarNotification) && (stringArray = statusBarNotification.getNotification().extras.getStringArray("android.foregroundApps")) != null && stringArray.length >= 1 && !getFsc().isSystemAlertWarningNeeded(statusBarNotification.getUserId(), stringArray[0])) {
            return true;
        }
        if (notificationEntry.isBubble() && !notificationEntry.showInShadeWhenBubble()) {
            return true;
        }
        if (!z || this.mTimeNotificationsWereLastViewed <= statusBarNotification.getPostTime()) {
            return false;
        }
        this.mNotificationsHidden = true;
        return true;
    }

    public boolean hasHiddenNotifications() {
        return this.mNotificationsHidden;
    }

    public void resetHideNotificationState() {
        this.mNotificationsHidden = false;
    }

    public void updateNotificationViewedTime() {
        this.mTimeNotificationsWereLastViewed = System.currentTimeMillis();
    }

    private boolean isMediaNotification(StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().isMediaNotification();
    }

    private boolean isWfcEmergencyCallLimitationDialogNotification(StatusBarNotification statusBarNotification) {
        String tag = statusBarNotification.getTag();
        return tag != null && tag.equals("EmergencyNetworkNotification");
    }

    private static boolean showNotificationEvenIfUnprovisioned(StatusBarNotification statusBarNotification) {
        return showNotificationEvenIfUnprovisioned(AppGlobals.getPackageManager(), statusBarNotification);
    }

    @VisibleForTesting
    static boolean showNotificationEvenIfUnprovisioned(IPackageManager iPackageManager, StatusBarNotification statusBarNotification) {
        return checkUidPermission(iPackageManager, "android.permission.NOTIFICATION_DURING_SETUP", statusBarNotification.getUid()) == 0 && statusBarNotification.getNotification().extras.getBoolean("android.allowDuringSetup");
    }

    private static int checkUidPermission(IPackageManager iPackageManager, String str, int i) {
        try {
            return iPackageManager.checkUidPermission(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
