package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.UserIcons;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.Utils;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dumpable;
import com.android.systemui.GuestResumeSessionReceiver;
import com.android.systemui.Prefs;
import com.android.systemui.SystemUISecondaryUserService;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.qs.tiles.UserDetailView;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserSwitcherController implements Dumpable {
    private final ActivityStarter mActivityStarter;
    private final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList<>();
    private Dialog mAddUserDialog;
    private boolean mAddUsersWhenLocked;
    private final KeyguardMonitor.Callback mCallback = new KeyguardMonitor.Callback() {
        /* class com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass7 */

        @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
        public void onKeyguardShowingChanged() {
            if (!UserSwitcherController.this.mKeyguardMonitor.isShowing()) {
                UserSwitcherController userSwitcherController = UserSwitcherController.this;
                userSwitcherController.mHandler.post(new Runnable() {
                    /* class com.android.systemui.statusbar.policy.$$Lambda$UserSwitcherController$7$pQr4FiWnaYmK1LUVjgYnvNV4vI */

                    public final void run() {
                        UserSwitcherController.this.notifyAdapters();
                    }
                });
                return;
            }
            UserSwitcherController.this.notifyAdapters();
        }
    };
    protected final Context mContext;
    private Dialog mExitGuestDialog;
    private SparseBooleanArray mForcePictureLoadForUserId = new SparseBooleanArray(2);
    private final GuestResumeSessionReceiver mGuestResumeSessionReceiver = new GuestResumeSessionReceiver();
    protected final Handler mHandler;
    private final boolean mIsCustomDialogShown;
    private final KeyguardMonitor mKeyguardMonitor;
    private int mLastNonGuestUser = 0;
    private boolean mPauseRefreshUsers;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /* class com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass2 */
        private int mCallState;

        public void onCallStateChanged(int i, String str) {
            if (this.mCallState != i) {
                this.mCallState = i;
                UserSwitcherController.this.refreshUsers(-10000);
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            boolean z = false;
            int i = -10000;
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (UserSwitcherController.this.mExitGuestDialog != null && UserSwitcherController.this.mExitGuestDialog.isShowing()) {
                    UserSwitcherController.this.mExitGuestDialog.cancel();
                    UserSwitcherController.this.mExitGuestDialog = null;
                }
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
                UserInfo userInfo = UserSwitcherController.this.mUserManager.getUserInfo(intExtra);
                int size = UserSwitcherController.this.mUsers.size();
                int i2 = 0;
                while (i2 < size) {
                    UserRecord userRecord = (UserRecord) UserSwitcherController.this.mUsers.get(i2);
                    UserInfo userInfo2 = userRecord.info;
                    if (userInfo2 != null) {
                        boolean z2 = userInfo2.id == intExtra;
                        if (userRecord.isCurrent != z2) {
                            UserSwitcherController.this.mUsers.set(i2, userRecord.copyWithIsCurrent(z2));
                        }
                        if (z2 && !userRecord.isGuest) {
                            UserSwitcherController.this.mLastNonGuestUser = userRecord.info.id;
                        }
                        if ((userInfo == null || !userInfo.isAdmin()) && userRecord.isRestricted) {
                            UserSwitcherController.this.mUsers.remove(i2);
                            i2--;
                        }
                    }
                    i2++;
                }
                UserSwitcherController.this.notifyAdapters();
                if (UserSwitcherController.this.mSecondaryUser != -10000) {
                    context.stopServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandle.of(UserSwitcherController.this.mSecondaryUser));
                    UserSwitcherController.this.mSecondaryUser = -10000;
                }
                if (!(userInfo == null || userInfo.id == 0)) {
                    context.startServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandle.of(userInfo.id));
                    UserSwitcherController.this.mSecondaryUser = userInfo.id;
                }
                z = true;
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(intent.getAction())) {
                i = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            } else if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction()) && intent.getIntExtra("android.intent.extra.user_handle", -10000) != 0) {
                return;
            }
            UserSwitcherController.this.refreshUsers(i);
            if (z) {
                UserSwitcherController.this.mUnpauseRefreshUsers.run();
            }
        }
    };
    private boolean mResumeUserOnGuestLogout = true;
    private int mSecondaryUser = -10000;
    private Intent mSecondaryUserServiceIntent;
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        /* class com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass5 */

        public void onChange(boolean z) {
            UserSwitcherController userSwitcherController = UserSwitcherController.this;
            boolean z2 = false;
            userSwitcherController.mSimpleUserSwitcher = Settings.Global.getInt(userSwitcherController.mContext.getContentResolver(), "lockscreenSimpleUserSwitcher", 0) != 0;
            UserSwitcherController userSwitcherController2 = UserSwitcherController.this;
            if (Settings.Global.getInt(userSwitcherController2.mContext.getContentResolver(), "add_users_when_locked", 0) != 0) {
                z2 = true;
            }
            userSwitcherController2.mAddUsersWhenLocked = z2;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    private boolean mSimpleUserSwitcher;
    private final Runnable mUnpauseRefreshUsers = new Runnable() {
        /* class com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass4 */

        public void run() {
            UserSwitcherController.this.mHandler.removeCallbacks(this);
            UserSwitcherController.this.mPauseRefreshUsers = false;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    protected final UserManager mUserManager;
    private ArrayList<UserRecord> mUsers = new ArrayList<>();
    public final DetailAdapter userDetailAdapter = new DetailAdapter() {
        /* class com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass6 */
        private final Intent USER_SETTINGS_INTENT = new Intent("android.settings.USER_SETTINGS");

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public int getMetricsCategory() {
            return 125;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Boolean getToggleState() {
            return null;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public void setToggleState(boolean z) {
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public CharSequence getTitle() {
            return UserSwitcherController.this.mContext.getString(C0014R$string.quick_settings_user_title);
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            UserDetailView userDetailView;
            if (!(view instanceof UserDetailView)) {
                userDetailView = UserDetailView.inflate(context, viewGroup, false);
                userDetailView.createAndSetAdapter(UserSwitcherController.this);
            } else {
                userDetailView = (UserDetailView) view;
            }
            userDetailView.refreshAdapter();
            return userDetailView;
        }

        @Override // com.android.systemui.plugins.qs.DetailAdapter
        public Intent getSettingsIntent() {
            return this.USER_SETTINGS_INTENT;
        }
    };

    public UserSwitcherController(Context context, KeyguardMonitor keyguardMonitor, Handler handler, ActivityStarter activityStarter) {
        this.mContext = context;
        if (!UserManager.isGuestUserEphemeral()) {
            this.mGuestResumeSessionReceiver.register(context);
        }
        this.mKeyguardMonitor = keyguardMonitor;
        this.mHandler = handler;
        this.mActivityStarter = activityStarter;
        this.mUserManager = UserManager.get(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, intentFilter, null, null);
        this.mSecondaryUserServiceIntent = new Intent(context, SystemUISecondaryUserService.class);
        this.mIsCustomDialogShown = context.getResources().getBoolean(C0003R$bool.config_showWarningDialogWhenCreatingUser);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, new IntentFilter(), "com.android.systemui.permission.SELF", null);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("lockscreenSimpleUserSwitcher"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("add_users_when_locked"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("allow_user_switching_when_system_user_locked"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
        keyguardMonitor.addCallback(this.mCallback);
        listenForCallState();
        refreshUsers(-10000);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void refreshUsers(int i) {
        UserInfo userInfo;
        if (i != -10000) {
            this.mForcePictureLoadForUserId.put(i, true);
        }
        if (!this.mPauseRefreshUsers) {
            boolean z = this.mForcePictureLoadForUserId.get(-1);
            SparseArray sparseArray = new SparseArray(this.mUsers.size());
            int size = this.mUsers.size();
            for (int i2 = 0; i2 < size; i2++) {
                UserRecord userRecord = this.mUsers.get(i2);
                if (!(userRecord == null || userRecord.picture == null || (userInfo = userRecord.info) == null || z || this.mForcePictureLoadForUserId.get(userInfo.id))) {
                    sparseArray.put(userRecord.info.id, userRecord.picture);
                }
            }
            this.mForcePictureLoadForUserId.clear();
            final boolean z2 = this.mAddUsersWhenLocked;
            new AsyncTask<SparseArray<Bitmap>, Void, ArrayList<UserRecord>>() {
                /* class com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass1 */

                /* access modifiers changed from: protected */
                public ArrayList<UserRecord> doInBackground(SparseArray<Bitmap>... sparseArrayArr) {
                    int i = 0;
                    SparseArray<Bitmap> sparseArray = sparseArrayArr[0];
                    List<UserInfo> users = UserSwitcherController.this.mUserManager.getUsers(true);
                    UserRecord userRecord = null;
                    if (users == null) {
                        return null;
                    }
                    ArrayList<UserRecord> arrayList = new ArrayList<>(users.size());
                    int currentUser = ActivityManager.getCurrentUser();
                    boolean canSwitchUsers = UserSwitcherController.this.mUserManager.canSwitchUsers();
                    UserInfo userInfo = null;
                    for (UserInfo userInfo2 : users) {
                        boolean z = currentUser == userInfo2.id;
                        UserInfo userInfo3 = z ? userInfo2 : userInfo;
                        boolean z2 = canSwitchUsers || z;
                        if (userInfo2.isEnabled()) {
                            if (userInfo2.isGuest()) {
                                userRecord = new UserRecord(userInfo2, null, true, z, false, false, canSwitchUsers);
                            } else if (userInfo2.supportsSwitchToByUser()) {
                                Bitmap bitmap = sparseArray.get(userInfo2.id);
                                if (bitmap == null && (bitmap = UserSwitcherController.this.mUserManager.getUserIcon(userInfo2.id)) != null) {
                                    int dimensionPixelSize = UserSwitcherController.this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.max_avatar_size);
                                    bitmap = Bitmap.createScaledBitmap(bitmap, dimensionPixelSize, dimensionPixelSize, true);
                                }
                                arrayList.add(z ? 0 : arrayList.size(), new UserRecord(userInfo2, bitmap, false, z, false, false, z2));
                            }
                        }
                        userInfo = userInfo3;
                    }
                    if (arrayList.size() > 1 || userRecord != null) {
                        Prefs.putBoolean(UserSwitcherController.this.mContext, "HasSeenMultiUser", true);
                    }
                    boolean z3 = !UserSwitcherController.this.mUserManager.hasBaseUserRestriction("no_add_user", UserHandle.SYSTEM);
                    boolean z4 = userInfo != null && (userInfo.isAdmin() || userInfo.id == 0) && z3;
                    boolean z5 = z3 && z2;
                    boolean z6 = (z4 || z5) && userRecord == null;
                    boolean z7 = (z4 || z5) && UserSwitcherController.this.mUserManager.canAddMoreUsers();
                    boolean z8 = !z2;
                    if (!UserSwitcherController.this.mSimpleUserSwitcher) {
                        if (userRecord != null) {
                            if (!userRecord.isCurrent) {
                                i = arrayList.size();
                            }
                            arrayList.add(i, userRecord);
                        } else if (z6) {
                            UserRecord userRecord2 = new UserRecord(null, null, true, false, false, z8, canSwitchUsers);
                            UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(userRecord2);
                            arrayList.add(userRecord2);
                        }
                    }
                    if (!UserSwitcherController.this.mSimpleUserSwitcher && z7) {
                        UserRecord userRecord3 = new UserRecord(null, null, false, false, true, z8, canSwitchUsers);
                        UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(userRecord3);
                        arrayList.add(userRecord3);
                    }
                    return arrayList;
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(ArrayList<UserRecord> arrayList) {
                    if (arrayList != null) {
                        UserSwitcherController.this.mUsers = arrayList;
                        UserSwitcherController.this.notifyAdapters();
                    }
                }
            }.execute(sparseArray);
        }
    }

    private void pauseRefreshUsers() {
        if (!this.mPauseRefreshUsers) {
            this.mHandler.postDelayed(this.mUnpauseRefreshUsers, 3000);
            this.mPauseRefreshUsers = true;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    public void notifyAdapters() {
        for (int size = this.mAdapters.size() - 1; size >= 0; size--) {
            BaseUserAdapter baseUserAdapter = this.mAdapters.get(size).get();
            if (baseUserAdapter != null) {
                baseUserAdapter.notifyDataSetChanged();
            } else {
                this.mAdapters.remove(size);
            }
        }
    }

    public boolean isSimpleUserSwitcher() {
        return this.mSimpleUserSwitcher;
    }

    public boolean useFullscreenUserSwitcher() {
        int i = Settings.System.getInt(this.mContext.getContentResolver(), "enable_fullscreen_user_switcher", -1);
        if (i != -1) {
            return i != 0;
        }
        return this.mContext.getResources().getBoolean(C0003R$bool.config_enableFullscreenUserSwitcher);
    }

    public void switchTo(UserRecord userRecord) {
        int i;
        UserInfo userInfo;
        if (!userRecord.isGuest || userRecord.info != null) {
            if (userRecord.isAddUser) {
                showAddUserDialog();
                return;
            }
            i = userRecord.info.id;
        } else if (!showAddUserDialog(true)) {
            UserManager userManager = this.mUserManager;
            Context context = this.mContext;
            UserInfo createGuest = userManager.createGuest(context, context.getString(C0014R$string.guest_nickname));
            if (createGuest != null) {
                i = createGuest.id;
            } else {
                return;
            }
        } else {
            return;
        }
        int currentUser = ActivityManager.getCurrentUser();
        if (currentUser == i) {
            if (userRecord.isGuest) {
                showExitGuestDialog(i);
            }
        } else if (!UserManager.isGuestUserEphemeral() || (userInfo = this.mUserManager.getUserInfo(currentUser)) == null || !userInfo.isGuest()) {
            switchToUserId(i);
        } else {
            showExitGuestDialog(currentUser, userRecord.resolveId());
        }
    }

    public int getSwitchableUserCount() {
        int size = this.mUsers.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = this.mUsers.get(i2).info;
            if (userInfo != null && userInfo.supportsSwitchToByUser()) {
                i++;
            }
        }
        return i;
    }

    /* access modifiers changed from: protected */
    public void switchToUserId(int i) {
        try {
            pauseRefreshUsers();
            ActivityManager.getService().switchUser(i);
        } catch (RemoteException e) {
            Log.e("UserSwitcherController", "Couldn't switch user.", e);
        }
    }

    private void showExitGuestDialog(int i) {
        int i2;
        UserInfo userInfo;
        showExitGuestDialog(i, (!this.mResumeUserOnGuestLogout || (i2 = this.mLastNonGuestUser) == 0 || (userInfo = this.mUserManager.getUserInfo(i2)) == null || !userInfo.isEnabled() || !userInfo.supportsSwitchToByUser()) ? 0 : userInfo.id);
    }

    /* access modifiers changed from: protected */
    public void showExitGuestDialog(int i, int i2) {
        Dialog dialog = this.mExitGuestDialog;
        if (dialog != null && dialog.isShowing()) {
            this.mExitGuestDialog.cancel();
        }
        this.mExitGuestDialog = new ExitGuestDialog(this.mContext, i, i2);
        this.mExitGuestDialog.show();
    }

    public void showAddUserDialog() {
        showAddUserDialog(false);
    }

    public boolean showAddUserDialog(boolean z) {
        Dialog dialog = this.mAddUserDialog;
        if (dialog != null && dialog.isShowing()) {
            this.mAddUserDialog.cancel();
        }
        if (this.mIsCustomDialogShown) {
            this.mAddUserDialog = new AddUserDialog(this.mContext, z);
        } else if (z) {
            return false;
        } else {
            this.mAddUserDialog = new AddUserDialog(this.mContext);
        }
        this.mAddUserDialog.show();
        return true;
    }

    /* access modifiers changed from: protected */
    public void exitGuest(int i, int i2) {
        switchToUserId(i2);
        this.mUserManager.removeUser(i);
    }

    private void listenForCallState() {
        TelephonyManager.from(this.mContext).listen(this.mPhoneStateListener, 32);
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("UserSwitcherController state:");
        printWriter.println("  mLastNonGuestUser=" + this.mLastNonGuestUser);
        printWriter.print("  mUsers.size=");
        printWriter.println(this.mUsers.size());
        for (int i = 0; i < this.mUsers.size(); i++) {
            printWriter.print("    ");
            printWriter.println(this.mUsers.get(i).toString());
        }
    }

    public String getCurrentUserName(Context context) {
        UserRecord userRecord;
        UserInfo userInfo;
        if (this.mUsers.isEmpty() || (userRecord = this.mUsers.get(0)) == null || (userInfo = userRecord.info) == null) {
            return null;
        }
        if (userRecord.isGuest) {
            return context.getString(C0014R$string.guest_nickname);
        }
        return userInfo.name;
    }

    public void onDensityOrFontScaleChanged() {
        refreshUsers(-1);
    }

    @VisibleForTesting
    public void addAdapter(WeakReference<BaseUserAdapter> weakReference) {
        this.mAdapters.add(weakReference);
    }

    @VisibleForTesting
    public ArrayList<UserRecord> getUsers() {
        return this.mUsers;
    }

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final UserSwitcherController mController;
        private final KeyguardMonitor mKeyguardMonitor;
        private final UnlockMethodCache mUnlockMethodCache;

        public long getItemId(int i) {
            return (long) i;
        }

        protected BaseUserAdapter(UserSwitcherController userSwitcherController) {
            this.mController = userSwitcherController;
            this.mKeyguardMonitor = userSwitcherController.mKeyguardMonitor;
            this.mUnlockMethodCache = UnlockMethodCache.getInstance(userSwitcherController.mContext);
            userSwitcherController.addAdapter(new WeakReference<>(this));
        }

        public int getCount() {
            int i = 0;
            if (!(this.mKeyguardMonitor.isShowing() && this.mKeyguardMonitor.isSecure() && !this.mUnlockMethodCache.canSkipBouncer())) {
                return this.mController.getUsers().size();
            }
            int size = this.mController.getUsers().size();
            int i2 = 0;
            while (i < size && !this.mController.getUsers().get(i).isRestricted) {
                i2++;
                i++;
            }
            return i2;
        }

        public UserRecord getItem(int i) {
            return this.mController.getUsers().get(i);
        }

        public void switchTo(UserRecord userRecord) {
            this.mController.switchTo(userRecord);
        }

        public String getName(Context context, UserRecord userRecord) {
            if (userRecord.isGuest) {
                if (userRecord.isCurrent) {
                    return context.getString(C0014R$string.guest_exit_guest);
                }
                return context.getString(userRecord.info == null ? C0014R$string.guest_new_guest : C0014R$string.guest_nickname);
            } else if (userRecord.isAddUser) {
                return context.getString(C0014R$string.user_add_user);
            } else {
                return userRecord.info.name;
            }
        }

        public Drawable getDrawable(Context context, UserRecord userRecord) {
            if (userRecord.isAddUser) {
                return context.getDrawable(C0006R$drawable.ic_add_circle_qs);
            }
            Drawable defaultUserIcon = UserIcons.getDefaultUserIcon(context.getResources(), userRecord.resolveId(), false);
            if (userRecord.isGuest) {
                defaultUserIcon.setColorFilter(Utils.getColorAttrDefaultColor(context, 16842800), PorterDuff.Mode.SRC_IN);
            }
            return defaultUserIcon;
        }

        public void refresh() {
            this.mController.refreshUsers(-10000);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void checkIfAddUserDisallowedByAdminOnly(UserRecord userRecord) {
        RestrictedLockUtils.EnforcedAdmin checkIfRestrictionEnforced = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(this.mContext, "no_add_user", ActivityManager.getCurrentUser());
        if (checkIfRestrictionEnforced == null || RestrictedLockUtilsInternal.hasBaseUserRestriction(this.mContext, "no_add_user", ActivityManager.getCurrentUser())) {
            userRecord.isDisabledByAdmin = false;
            userRecord.enforcedAdmin = null;
            return;
        }
        userRecord.isDisabledByAdmin = true;
        userRecord.enforcedAdmin = checkIfRestrictionEnforced;
    }

    public void startActivity(Intent intent) {
        this.mActivityStarter.startActivity(intent, true);
    }

    public static final class UserRecord {
        public RestrictedLockUtils.EnforcedAdmin enforcedAdmin;
        public final UserInfo info;
        public final boolean isAddUser;
        public final boolean isCurrent;
        public boolean isDisabledByAdmin;
        public final boolean isGuest;
        public final boolean isRestricted;
        public boolean isSwitchToEnabled;
        public final Bitmap picture;

        public UserRecord(UserInfo userInfo, Bitmap bitmap, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
            this.info = userInfo;
            this.picture = bitmap;
            this.isGuest = z;
            this.isCurrent = z2;
            this.isAddUser = z3;
            this.isRestricted = z4;
            this.isSwitchToEnabled = z5;
        }

        public UserRecord copyWithIsCurrent(boolean z) {
            return new UserRecord(this.info, this.picture, this.isGuest, z, this.isAddUser, this.isRestricted, this.isSwitchToEnabled);
        }

        public int resolveId() {
            UserInfo userInfo;
            if (this.isGuest || (userInfo = this.info) == null) {
                return -10000;
            }
            return userInfo.id;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UserRecord(");
            if (this.info != null) {
                sb.append("name=\"");
                sb.append(this.info.name);
                sb.append("\" id=");
                sb.append(this.info.id);
            } else if (this.isGuest) {
                sb.append("<add guest placeholder>");
            } else if (this.isAddUser) {
                sb.append("<add user placeholder>");
            }
            if (this.isGuest) {
                sb.append(" <isGuest>");
            }
            if (this.isAddUser) {
                sb.append(" <isAddUser>");
            }
            if (this.isCurrent) {
                sb.append(" <isCurrent>");
            }
            if (this.picture != null) {
                sb.append(" <hasPicture>");
            }
            if (this.isRestricted) {
                sb.append(" <isRestricted>");
            }
            if (this.isDisabledByAdmin) {
                sb.append(" <isDisabledByAdmin>");
                sb.append(" enforcedAdmin=");
                sb.append(this.enforcedAdmin);
            }
            if (this.isSwitchToEnabled) {
                sb.append(" <isSwitchToEnabled>");
            }
            sb.append(')');
            return sb.toString();
        }
    }

    /* access modifiers changed from: private */
    public final class ExitGuestDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        private final int mGuestId;
        private final int mTargetId;

        public ExitGuestDialog(Context context, int i, int i2) {
            super(context);
            setTitle(C0014R$string.guest_exit_guest_dialog_title);
            setMessage(context.getString(C0014R$string.guest_exit_guest_dialog_message));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(C0014R$string.guest_exit_guest_dialog_remove), this);
            SystemUIDialog.setWindowOnTop(this);
            setCanceledOnTouchOutside(false);
            this.mGuestId = i;
            this.mTargetId = i2;
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                cancel();
                return;
            }
            dismiss();
            UserSwitcherController.this.exitGuest(this.mGuestId, this.mTargetId);
        }
    }

    /* access modifiers changed from: private */
    public final class AddUserDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        private View mAddUserMessageView;
        private View mAddUserScrollView;
        private boolean mIsGuest;
        private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
        private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

        public AddUserDialog(Context context) {
            super(context);
            setTitle(C0014R$string.user_add_user_title);
            setMessage(context.getString(C0014R$string.user_add_user_message_short));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(17039370), this);
            SystemUIDialog.setWindowOnTop(this);
        }

        public AddUserDialog(Context context, boolean z) {
            super(context);
            this.mIsGuest = z;
            setTitle(C0014R$string.user_add_user_dialog_title_txt);
            setMessage(context.getString(C0014R$string.user_add_user_dialog_message_txt));
            setButton(-2, context.getString(C0014R$string.user_dialog_cancel_txt), this);
            setButton(-1, context.getString(C0014R$string.user_dialog_ok_txt), this);
            SystemUIDialog.setWindowOnTop(this);
            setOnDismissListener(new DialogInterface.OnDismissListener(UserSwitcherController.this) {
                /* class com.android.systemui.statusbar.policy.UserSwitcherController.AddUserDialog.AnonymousClass1 */

                public void onDismiss(DialogInterface dialogInterface) {
                    AddUserDialog.this.removeListenersForScrolling();
                }
            });
        }

        /* access modifiers changed from: protected */
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            this.mAddUserScrollView = findViewById(16909317);
            this.mAddUserMessageView = findViewById(16908299);
            getButton(-1).setEnabled(false);
            ViewTreeObserver viewTreeObserver = this.mAddUserScrollView.getViewTreeObserver();
            this.mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                /* class com.android.systemui.statusbar.policy.UserSwitcherController.AddUserDialog.AnonymousClass2 */

                public void onGlobalLayout() {
                    if (AddUserDialog.this.isScreenFullyScrolled()) {
                        AddUserDialog.this.getButton(-1).setEnabled(true);
                        AddUserDialog.this.removeListenersForScrolling();
                    }
                }
            };
            viewTreeObserver.addOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
            this.mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
                /* class com.android.systemui.statusbar.policy.UserSwitcherController.AddUserDialog.AnonymousClass3 */

                public void onScrollChanged() {
                    if (AddUserDialog.this.isScreenFullyScrolled()) {
                        AddUserDialog.this.getButton(-1).setEnabled(true);
                        AddUserDialog.this.removeListenersForScrolling();
                    }
                }
            };
            viewTreeObserver.addOnScrollChangedListener(this.mOnScrollChangedListener);
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                cancel();
                return;
            }
            dismiss();
            if (this.mIsGuest) {
                UserSwitcherController userSwitcherController = UserSwitcherController.this;
                UserManager userManager = userSwitcherController.mUserManager;
                Context context = userSwitcherController.mContext;
                UserInfo createGuest = userManager.createGuest(context, context.getString(C0014R$string.guest_nickname));
                if (createGuest != null) {
                    UserSwitcherController.this.switchToUserId(createGuest.id);
                }
            } else if (!ActivityManager.isUserAMonkey()) {
                UserSwitcherController userSwitcherController2 = UserSwitcherController.this;
                UserInfo createUser = userSwitcherController2.mUserManager.createUser(userSwitcherController2.mContext.getString(C0014R$string.user_new_user_name), 0);
                if (createUser != null) {
                    int i2 = createUser.id;
                    UserSwitcherController.this.mUserManager.setUserIcon(i2, UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(UserSwitcherController.this.mContext.getResources(), i2, false)));
                    UserSwitcherController.this.switchToUserId(i2);
                }
            }
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private void removeListenersForScrolling() {
            ViewTreeObserver viewTreeObserver = this.mAddUserScrollView.getViewTreeObserver();
            ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = this.mOnGlobalLayoutListener;
            if (onGlobalLayoutListener != null) {
                viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener);
                this.mOnGlobalLayoutListener = null;
            }
            ViewTreeObserver.OnScrollChangedListener onScrollChangedListener = this.mOnScrollChangedListener;
            if (onScrollChangedListener != null) {
                viewTreeObserver.removeOnScrollChangedListener(onScrollChangedListener);
                this.mOnScrollChangedListener = null;
            }
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean isScreenFullyScrolled() {
            return (this.mAddUserScrollView.getScrollY() + this.mAddUserScrollView.getHeight()) - this.mAddUserScrollView.getPaddingTop() >= this.mAddUserMessageView.getHeight();
        }
    }
}
