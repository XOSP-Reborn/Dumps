package com.android.systemui.statusbar.notification;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.C0001R$array;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.sonymobile.systemui.multiwindowcontroller.MultiWindowController;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MultiWindowButtonManager {
    public static final boolean DEBUG_MW = (Log.isLoggable(DEBUG_MW_TAG, 3) && Build.IS_DEBUGGABLE);
    public static final String DEBUG_MW_TAG = "MultiWindowButtonManager";
    private final String[] mBlacklist;
    private final Context mContext;

    public MultiWindowButtonManager(Context context) {
        this.mContext = context;
        this.mBlacklist = context.getResources().getStringArray(C0001R$array.multiwindow_button_pkg_blacklist);
    }

    public boolean isForegroundResizeable() {
        IActivityManager service = ActivityManager.getService();
        try {
            ActivityManager.StackInfo focusedStackInfo = service.getFocusedStackInfo();
            if (DEBUG_MW) {
                String str = DEBUG_MW_TAG;
                Log.d(str, "isForegroundResizeable stack=" + focusedStackInfo);
            }
            if (focusedStackInfo != null) {
                if (focusedStackInfo.configuration != null) {
                    int windowingMode = focusedStackInfo.configuration.windowConfiguration.getWindowingMode();
                    int activityType = focusedStackInfo.configuration.windowConfiguration.getActivityType();
                    if (windowingMode != 3) {
                        if (windowingMode != 4) {
                            if (activityType != 1) {
                                if (DEBUG_MW) {
                                    String str2 = DEBUG_MW_TAG;
                                    Log.d(str2, "Foreground activity is not standard: " + activityType);
                                }
                                return false;
                            }
                            int i = focusedStackInfo.userId;
                            int i2 = focusedStackInfo.stackId;
                            int i3 = -1;
                            if (focusedStackInfo.taskIds != null && focusedStackInfo.taskIds.length > 0) {
                                i3 = focusedStackInfo.taskIds[0];
                            }
                            if (i2 >= 0) {
                                if (i3 >= 0) {
                                    Iterator it = service.getRecentTasks(ActivityManager.getMaxRecentTasksStatic(), 1, i).getList().iterator();
                                    while (true) {
                                        if (!it.hasNext()) {
                                            break;
                                        }
                                        ActivityManager.RecentTaskInfo recentTaskInfo = (ActivityManager.RecentTaskInfo) it.next();
                                        if (DEBUG_MW) {
                                            String str3 = DEBUG_MW_TAG;
                                            Log.d(str3, "resentTask: " + recentTaskInfo.baseActivity + " stackId=" + recentTaskInfo.stackId + " id=" + recentTaskInfo.id + " persistentId=" + recentTaskInfo.persistentId + " resizeMode=" + recentTaskInfo.resizeMode);
                                        }
                                        if (recentTaskInfo.stackId == i2 && recentTaskInfo.persistentId == i3) {
                                            if (recentTaskInfo.resizeMode != 0) {
                                                return true;
                                            }
                                        }
                                    }
                                    return false;
                                }
                            }
                            if (DEBUG_MW) {
                                Log.d(DEBUG_MW_TAG, "Failed to resolve stackId and taskId");
                            }
                            return false;
                        }
                    }
                    if (DEBUG_MW) {
                        String str4 = DEBUG_MW_TAG;
                        Log.d(str4, "Split screen mode is activated: " + windowingMode);
                    }
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            if (DEBUG_MW) {
                String str5 = DEBUG_MW_TAG;
                Log.d(str5, "Failed to check foreground stack. " + e);
            }
        }
    }

    public Runnable getMultiWindowButtonInvoker(NotificationEntry notificationEntry) {
        if (notificationEntry != null) {
            PendingIntent pendingIntent = notificationEntry.notification.getNotification().contentIntent;
            if (pendingIntent == null) {
                pendingIntent = notificationEntry.notification.getNotification().fullScreenIntent;
            }
            return getMultiWindowButtonInvoker(pendingIntent);
        } else if (!DEBUG_MW) {
            return null;
        } else {
            Log.d(DEBUG_MW_TAG, "getMultiWindowButtonInvoker failed: No entry");
            return null;
        }
    }

    private Runnable getMultiWindowButtonInvoker(PendingIntent pendingIntent) {
        ActivityInfo activityInfo;
        if (pendingIntent == null) {
            if (DEBUG_MW) {
                Log.d(DEBUG_MW_TAG, "getMultiWindowButtonInvoker failed: No contentIntent and fullScreenIntent");
            }
            return null;
        } else if (!pendingIntent.isActivity()) {
            if (DEBUG_MW) {
                Log.d(DEBUG_MW_TAG, "getMultiWindowButtonInvoker failed: not Activity");
            }
            return null;
        } else {
            Intent intent = pendingIntent.getIntent();
            if (intent == null) {
                if (DEBUG_MW) {
                    Log.d(DEBUG_MW_TAG, "getMultiWindowButtonInvoker failed: get raw intent");
                }
                return null;
            }
            PackageManager packageManager = this.mContext.getPackageManager();
            ResolveInfo resolveActivity = packageManager.resolveActivity(intent, 131072);
            if (resolveActivity == null || (activityInfo = resolveActivity.activityInfo) == null) {
                if (DEBUG_MW) {
                    Log.d(DEBUG_MW_TAG, "getMultiWindowButtonInvoker failed: get activityInfo");
                }
                return null;
            } else if (activityInfo.resizeMode == 0) {
                if (DEBUG_MW) {
                    String str = DEBUG_MW_TAG;
                    Log.d(str, "getMultiWindowButtonInvoker failed: resizeMode=" + resolveActivity.activityInfo.resizeMode);
                }
                return null;
            } else {
                String str2 = activityInfo.packageName;
                if (str2 == null) {
                    if (DEBUG_MW) {
                        Log.d(DEBUG_MW_TAG, "getMultiWindowButtonInvoker failed: no packageName");
                    }
                    return null;
                }
                String[] strArr = this.mBlacklist;
                if (strArr == null || Arrays.binarySearch(strArr, str2) < 0) {
                    Intent intent2 = new Intent("android.intent.action.MAIN");
                    intent2.addCategory("android.intent.category.LAUNCHER");
                    intent2.setPackage(str2);
                    List<ResolveInfo> queryIntentActivities = packageManager.queryIntentActivities(intent2, 0);
                    if (queryIntentActivities == null || queryIntentActivities.size() <= 0) {
                        if (DEBUG_MW) {
                            String str3 = DEBUG_MW_TAG;
                            Log.d(str3, "getMultiWindowButtonInvoker no CATEGORY_LAUNCHER on " + str2);
                        }
                        return null;
                    }
                    if (DEBUG_MW) {
                        String str4 = DEBUG_MW_TAG;
                        Log.d(str4, "getMultiWindowButtonInvoker OK: " + intent + " resizeMode=" + resolveActivity.activityInfo.resizeMode);
                    }
                    return new Runnable(str2, pendingIntent) {
                        /* class com.android.systemui.statusbar.notification.$$Lambda$MultiWindowButtonManager$3HeVl_YZvGErTn1GwHwdulJyD8w */
                        private final /* synthetic */ String f$1;
                        private final /* synthetic */ PendingIntent f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            MultiWindowButtonManager.this.lambda$getMultiWindowButtonInvoker$1$MultiWindowButtonManager(this.f$1, this.f$2);
                        }
                    };
                }
                if (DEBUG_MW) {
                    String str5 = DEBUG_MW_TAG;
                    Log.d(str5, "getMultiWindowButtonInvoker failed: " + str2 + " is in blacklist.");
                }
                return null;
            }
        }
    }

    public /* synthetic */ void lambda$getMultiWindowButtonInvoker$1$MultiWindowButtonManager(String str, PendingIntent pendingIntent) {
        if (DEBUG_MW) {
            String str2 = DEBUG_MW_TAG;
            Log.d(str2, "MultiWindowButtonInvoker#run packageName:" + str + " pi:" + pendingIntent);
        }
        if (isUsmEnabled(this.mContext)) {
            Intent intent = new Intent("com.sonymobile.ULTRA_STAMINA_FEATURE_NOT_SUPPORTED");
            intent.addFlags(268435456);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            return;
        }
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postRunnableDismissingKeyguard(new Runnable(pendingIntent, str) {
            /* class com.android.systemui.statusbar.notification.$$Lambda$MultiWindowButtonManager$XVYOqr2PLwErjXOlXgtR1T91Uc */
            private final /* synthetic */ PendingIntent f$1;
            private final /* synthetic */ String f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                MultiWindowButtonManager.this.lambda$getMultiWindowButtonInvoker$0$MultiWindowButtonManager(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$getMultiWindowButtonInvoker$0$MultiWindowButtonManager(PendingIntent pendingIntent, String str) {
        new MultiWindowController(this.mContext).launchMultiWindow(pendingIntent, str);
    }

    public static boolean isSpecialHome(Context context) {
        return isUsmEnabled(context) || isEmergencyModeEnabled(context);
    }

    private static boolean isUsmEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "somc.ultrastamina_mode", 0) != 0;
    }

    private static boolean isEmergencyModeEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "somc.emergency_mode", 0) != 0;
    }
}
