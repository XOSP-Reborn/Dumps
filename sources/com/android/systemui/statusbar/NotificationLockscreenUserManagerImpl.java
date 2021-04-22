package com.android.systemui.statusbar;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class NotificationLockscreenUserManagerImpl implements Dumpable, NotificationLockscreenUserManager, StatusBarStateController.StateListener {
    protected final BroadcastReceiver mAllUsersReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction()) && NotificationLockscreenUserManagerImpl.this.isCurrentProfile(getSendingUserId())) {
                NotificationLockscreenUserManagerImpl.this.mUsersAllowingPrivateNotifications.clear();
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                NotificationLockscreenUserManagerImpl.this.getEntryManager().updateNotifications();
            }
        }
    };
    private boolean mAllowLockscreenRemoteInput;
    private final IStatusBarService mBarService;
    protected final BroadcastReceiver mBaseBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                NotificationLockscreenUserManagerImpl.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                NotificationLockscreenUserManagerImpl.this.updateCurrentProfilesCache();
                Log.v("LockscreenUserManager", "userId " + NotificationLockscreenUserManagerImpl.this.mCurrentUserId + " is in the house");
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                NotificationLockscreenUserManagerImpl.this.updatePublicMode();
                NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().filterAndSort();
                NotificationLockscreenUserManagerImpl notificationLockscreenUserManagerImpl = NotificationLockscreenUserManagerImpl.this;
                notificationLockscreenUserManagerImpl.mPresenter.onUserSwitched(notificationLockscreenUserManagerImpl.mCurrentUserId);
                for (NotificationLockscreenUserManager.UserChangedListener userChangedListener : NotificationLockscreenUserManagerImpl.this.mListeners) {
                    userChangedListener.onUserChanged(NotificationLockscreenUserManagerImpl.this.mCurrentUserId);
                }
            } else if ("android.intent.action.USER_ADDED".equals(action)) {
                NotificationLockscreenUserManagerImpl.this.updateCurrentProfilesCache();
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).startConnectionToCurrentUser();
            } else if ("com.android.systemui.statusbar.work_challenge_unlocked_notification_action".equals(action)) {
                IntentSender intentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
                String stringExtra = intent.getStringExtra("android.intent.extra.INDEX");
                if (intentSender != null) {
                    try {
                        NotificationLockscreenUserManagerImpl.this.mContext.startIntentSender(intentSender, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException unused) {
                    }
                }
                if (stringExtra != null) {
                    try {
                        NotificationLockscreenUserManagerImpl.this.mBarService.onNotificationClick(stringExtra, NotificationVisibility.obtain(stringExtra, NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().getRank(stringExtra), NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().getActiveNotifications().size(), true, NotificationLogger.getNotificationLocation(NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().get(stringExtra))));
                    } catch (RemoteException unused2) {
                    }
                }
            }
        }
    };
    protected final Context mContext;
    protected final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();
    protected int mCurrentUserId = 0;
    private final DevicePolicyManager mDevicePolicyManager;
    private final DeviceProvisionedController mDeviceProvisionedController = ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    private NotificationEntryManager mEntryManager;
    private boolean mIsDisableSecureNotificationsByDpm;
    protected KeyguardManager mKeyguardManager;
    private final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    private final List<NotificationLockscreenUserManager.UserChangedListener> mListeners = new ArrayList();
    private LockPatternUtils mLockPatternUtils;
    private final SparseBooleanArray mLockscreenPublicMode = new SparseBooleanArray();
    protected ContentObserver mLockscreenSettingsObserver;
    protected NotificationPresenter mPresenter;
    protected ContentObserver mSettingsObserver;
    private boolean mShowLockscreenMediaNotifications;
    private boolean mShowLockscreenNotifications;
    private int mState = 0;
    private final UserManager mUserManager;
    private final SparseBooleanArray mUsersAllowingNotifications = new SparseBooleanArray();
    private final SparseBooleanArray mUsersAllowingPrivateNotifications = new SparseBooleanArray();
    private final SparseBooleanArray mUsersWithSeperateWorkChallenge = new SparseBooleanArray();

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private NotificationEntryManager getEntryManager() {
        if (this.mEntryManager == null) {
            this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        }
        return this.mEntryManager;
    }

    public NotificationLockscreenUserManagerImpl(Context context) {
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mKeyguardManager = (KeyguardManager) context.getSystemService(KeyguardManager.class);
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public void setUpWithPresenter(NotificationPresenter notificationPresenter) {
        this.mPresenter = notificationPresenter;
        this.mLockscreenSettingsObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
            /* class com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl.AnonymousClass3 */

            public void onChange(boolean z) {
                NotificationLockscreenUserManagerImpl.this.mUsersAllowingPrivateNotifications.clear();
                NotificationLockscreenUserManagerImpl.this.mUsersAllowingNotifications.clear();
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                NotificationLockscreenUserManagerImpl.this.getEntryManager().updateNotifications();
            }
        };
        this.mSettingsObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
            /* class com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl.AnonymousClass4 */

            public void onChange(boolean z) {
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                if (NotificationLockscreenUserManagerImpl.this.mDeviceProvisionedController.isDeviceProvisioned()) {
                    NotificationLockscreenUserManagerImpl.this.getEntryManager().updateNotifications();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_show_notifications"), false, this.mLockscreenSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_allow_private_notifications"), true, this.mLockscreenSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, this.mSettingsObserver);
        this.mContext.registerReceiverAsUser(this.mAllUsersReceiver, UserHandle.ALL, new IntentFilter("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED"), null, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiver(this.mBaseBroadcastReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.systemui.statusbar.work_challenge_unlocked_notification_action");
        this.mContext.registerReceiver(this.mBaseBroadcastReceiver, intentFilter2, "com.android.systemui.permission.SELF", null);
        updateCurrentProfilesCache();
        this.mSettingsObserver.onChange(false);
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean shouldShowLockscreenNotifications() {
        return this.mShowLockscreenNotifications;
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean shouldShowLockscreenMediaNotifications() {
        return this.mShowLockscreenMediaNotifications;
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean shouldAllowLockscreenRemoteInput() {
        return this.mAllowLockscreenRemoteInput;
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean isCurrentProfile(int i) {
        boolean z;
        synchronized (this.mCurrentProfiles) {
            if (i != -1) {
                try {
                    if (this.mCurrentProfiles.get(i) == null) {
                        z = false;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            z = true;
        }
        return z;
    }

    private boolean shouldTemporarilyHideNotifications(int i) {
        if (i == -1) {
            i = this.mCurrentUserId;
        }
        return KeyguardUpdateMonitor.getInstance(this.mContext).isUserInLockdown(i);
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean shouldHideNotifications(int i) {
        int i2;
        return (isLockscreenPublicMode(i) && !userAllowsNotificationsInPublic(i)) || (i != (i2 = this.mCurrentUserId) && shouldHideNotifications(i2)) || shouldTemporarilyHideNotifications(i);
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean shouldHideNotifications(String str) {
        if (getEntryManager() == null) {
            Log.wtf("LockscreenUserManager", "mEntryManager was null!", new Throwable());
            return true;
        } else if (!isLockscreenPublicMode(this.mCurrentUserId) || getEntryManager().getNotificationData().getVisibilityOverride(str) != -1) {
            return false;
        } else {
            return true;
        }
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean shouldShowOnKeyguard(NotificationEntry notificationEntry) {
        boolean z;
        if (getEntryManager() == null) {
            Log.wtf("LockscreenUserManager", "mEntryManager was null!", new Throwable());
            return false;
        }
        if (!NotificationUtils.useNewInterruptionModel(this.mContext) || !hideSilentNotificationsOnLockscreen()) {
            z = !getEntryManager().getNotificationData().isAmbient(notificationEntry.key);
        } else {
            z = notificationEntry.isTopBucket();
        }
        this.mShowLockscreenMediaNotifications = this.mShowLockscreenNotifications;
        if (this.mIsDisableSecureNotificationsByDpm) {
            this.mShowLockscreenMediaNotifications = false;
        } else if (getEntryManager().getNotificationData().get(notificationEntry.key) != null && getEntryManager().getNotificationData().get(notificationEntry.key).isMediaNotification()) {
            this.mShowLockscreenMediaNotifications = true;
        }
        if (!this.mShowLockscreenMediaNotifications || !z) {
            return false;
        }
        return true;
    }

    private boolean hideSilentNotificationsOnLockscreen() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "lock_screen_show_silent_notifications", 1) == 0;
    }

    private void setShowLockscreenNotifications(boolean z) {
        this.mShowLockscreenNotifications = z;
    }

    private void setIsDisableSecureNotificationsByDpm(boolean z) {
        this.mIsDisableSecureNotificationsByDpm = !z;
    }

    private void setLockscreenAllowRemoteInput(boolean z) {
        this.mAllowLockscreenRemoteInput = z;
    }

    /* access modifiers changed from: protected */
    public void updateLockscreenNotificationSetting() {
        boolean z = true;
        boolean z2 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, this.mCurrentUserId) != 0;
        boolean z3 = (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mCurrentUserId) & 4) == 0;
        if (!z2 || !z3) {
            z = false;
        }
        setShowLockscreenNotifications(z);
        setIsDisableSecureNotificationsByDpm(z3);
        setLockscreenAllowRemoteInput(false);
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean userAllowsPrivateNotificationsInPublic(int i) {
        boolean z = true;
        if (i == -1) {
            return true;
        }
        if (this.mUsersAllowingPrivateNotifications.indexOfKey(i) >= 0) {
            return this.mUsersAllowingPrivateNotifications.get(i);
        }
        boolean z2 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0, i) != 0;
        boolean adminAllowsKeyguardFeature = adminAllowsKeyguardFeature(i, 8);
        if (!z2 || !adminAllowsKeyguardFeature) {
            z = false;
        }
        this.mUsersAllowingPrivateNotifications.append(i, z);
        return z;
    }

    private boolean adminAllowsKeyguardFeature(int i, int i2) {
        if (i == -1 || (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, i) & i2) == 0) {
            return true;
        }
        return false;
    }

    public void setLockscreenPublicMode(boolean z, int i) {
        this.mLockscreenPublicMode.put(i, z);
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean isLockscreenPublicMode(int i) {
        if (i == -1) {
            return this.mLockscreenPublicMode.get(this.mCurrentUserId, false);
        }
        return this.mLockscreenPublicMode.get(i, false);
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean needsSeparateWorkChallenge(int i) {
        return this.mUsersWithSeperateWorkChallenge.get(i, false);
    }

    private boolean userAllowsNotificationsInPublic(int i) {
        boolean z = true;
        if (isCurrentProfile(i) && i != this.mCurrentUserId) {
            return true;
        }
        if (this.mUsersAllowingNotifications.indexOfKey(i) >= 0) {
            return this.mUsersAllowingNotifications.get(i);
        }
        boolean z2 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 0, i) != 0;
        boolean adminAllowsKeyguardFeature = adminAllowsKeyguardFeature(i, 4);
        boolean privateNotificationsAllowed = this.mKeyguardManager.getPrivateNotificationsAllowed();
        if (!z2 || !adminAllowsKeyguardFeature || !privateNotificationsAllowed) {
            z = false;
        }
        this.mUsersAllowingNotifications.append(i, z);
        return z;
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean needsRedaction(NotificationEntry notificationEntry) {
        boolean z = (userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) ^ true) || (userAllowsPrivateNotificationsInPublic(notificationEntry.notification.getUserId()) ^ true);
        boolean z2 = notificationEntry.notification.getNotification().visibility == 0;
        if (packageHasVisibilityOverride(notificationEntry.notification.getKey())) {
            return true;
        }
        if (!z2 || !z) {
            return false;
        }
        return true;
    }

    private boolean packageHasVisibilityOverride(String str) {
        if (getEntryManager() == null) {
            Log.wtf("LockscreenUserManager", "mEntryManager was null!", new Throwable());
            return true;
        } else if (getEntryManager().getNotificationData().getVisibilityOverride(str) == 0) {
            return true;
        } else {
            return false;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateCurrentProfilesCache() {
        synchronized (this.mCurrentProfiles) {
            this.mCurrentProfiles.clear();
            if (this.mUserManager != null) {
                for (UserInfo userInfo : this.mUserManager.getProfiles(this.mCurrentUserId)) {
                    this.mCurrentProfiles.put(userInfo.id, userInfo);
                }
            }
        }
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public boolean isAnyProfilePublicMode() {
        for (int size = this.mCurrentProfiles.size() - 1; size >= 0; size--) {
            if (isLockscreenPublicMode(this.mCurrentProfiles.valueAt(size).id)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public int getCurrentUserId() {
        return this.mCurrentUserId;
    }

    public SparseArray<UserInfo> getCurrentProfiles() {
        return this.mCurrentProfiles;
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        this.mState = i;
        updatePublicMode();
    }

    @Override // com.android.systemui.statusbar.NotificationLockscreenUserManager
    public void updatePublicMode() {
        boolean z = this.mState != 0 || this.mKeyguardMonitor.isShowing();
        boolean z2 = z && isSecure(getCurrentUserId());
        SparseArray<UserInfo> currentProfiles = getCurrentProfiles();
        this.mUsersWithSeperateWorkChallenge.clear();
        for (int size = currentProfiles.size() - 1; size >= 0; size--) {
            int i = currentProfiles.valueAt(size).id;
            boolean isSeparateProfileChallengeEnabled = this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i);
            setLockscreenPublicMode((z2 || i == getCurrentUserId() || !isSeparateProfileChallengeEnabled || !isSecure(i)) ? z2 : z || this.mKeyguardManager.isDeviceLocked(i), i);
            this.mUsersWithSeperateWorkChallenge.put(i, isSeparateProfileChallengeEnabled);
        }
        getEntryManager().updateNotifications();
    }

    private boolean isSecure(int i) {
        return this.mKeyguardMonitor.isSecure() || this.mLockPatternUtils.isSecure(i);
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NotificationLockscreenUserManager state:");
        printWriter.print("  mCurrentUserId=");
        printWriter.println(this.mCurrentUserId);
        printWriter.print("  mShowLockscreenNotifications=");
        printWriter.println(this.mShowLockscreenNotifications);
        printWriter.print("  mAllowLockscreenRemoteInput=");
        printWriter.println(this.mAllowLockscreenRemoteInput);
        printWriter.print("  mCurrentProfiles=");
        for (int size = this.mCurrentProfiles.size() - 1; size >= 0; size += -1) {
            printWriter.print("" + this.mCurrentProfiles.valueAt(size).id + " ");
        }
        printWriter.println();
    }
}
