package com.android.systemui.recents.misc;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.dreams.IDreamManager;
import android.util.Log;
import android.util.MutableBoolean;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.app.AssistUtils;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.statusbar.policy.UserInfoController;
import java.util.List;

public class SystemServicesProxy {
    static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    private static SystemServicesProxy sSystemServicesProxy;
    AccessibilityManager mAccm;
    ActivityManager mAm;
    AssistUtils mAssistUtils;
    Canvas mBgProtectionCanvas;
    Paint mBgProtectionPaint;
    private final Context mContext;
    private int mCurrentUserId;
    Display mDisplay;
    private final IDreamManager mDreamManager;
    int mDummyThumbnailHeight;
    int mDummyThumbnailWidth;
    private final Runnable mGcRunnable = new Runnable() {
        /* class com.android.systemui.recents.misc.SystemServicesProxy.AnonymousClass1 */

        public void run() {
            System.gc();
            System.runFinalization();
        }
    };
    IActivityManager mIam;
    IActivityTaskManager mIatm;
    IPackageManager mIpm;
    boolean mIsSafeMode;
    IWindowManager mIwm;
    private final UserInfoController.OnUserInfoChangedListener mOnUserInfoChangedListener = new UserInfoController.OnUserInfoChangedListener() {
        /* class com.android.systemui.recents.misc.$$Lambda$SystemServicesProxy$14WNoAPwhU0GwlQXHqE_l3lK1kI */

        @Override // com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener
        public final void onUserInfoChanged(String str, Drawable drawable, String str2) {
            SystemServicesProxy.this.lambda$new$0$SystemServicesProxy(str, drawable, str2);
        }
    };
    PackageManager mPm;
    String mRecentsPackage;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    UserManager mUm;
    WindowManager mWm;

    public boolean isSystemUser(int i) {
        return i == 0;
    }

    static {
        BitmapFactory.Options options = sBitmapOptions;
        options.inMutable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    public /* synthetic */ void lambda$new$0$SystemServicesProxy(String str, Drawable drawable, String str2) {
        this.mCurrentUserId = ActivityManager.getCurrentUser();
    }

    private SystemServicesProxy(Context context) {
        this.mContext = context.getApplicationContext();
        this.mAccm = AccessibilityManager.getInstance(context);
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mIam = ActivityManager.getService();
        this.mIatm = ActivityTaskManager.getService();
        this.mPm = context.getPackageManager();
        this.mIpm = AppGlobals.getPackageManager();
        this.mAssistUtils = new AssistUtils(context);
        this.mWm = (WindowManager) context.getSystemService("window");
        this.mIwm = WindowManagerGlobal.getWindowManagerService();
        this.mUm = UserManager.get(context);
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
        this.mDisplay = this.mWm.getDefaultDisplay();
        this.mRecentsPackage = context.getPackageName();
        this.mIsSafeMode = this.mPm.isSafeMode();
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        Resources resources = context.getResources();
        this.mDummyThumbnailWidth = resources.getDimensionPixelSize(17104898);
        this.mDummyThumbnailHeight = resources.getDimensionPixelSize(17104897);
        this.mBgProtectionPaint = new Paint();
        this.mBgProtectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        this.mBgProtectionPaint.setColor(-1);
        this.mBgProtectionCanvas = new Canvas();
        ((UserInfoController) Dependency.get(UserInfoController.class)).addCallback(this.mOnUserInfoChangedListener);
    }

    public static synchronized SystemServicesProxy getInstance(Context context) {
        SystemServicesProxy systemServicesProxy;
        synchronized (SystemServicesProxy.class) {
            if (sSystemServicesProxy == null) {
                sSystemServicesProxy = new SystemServicesProxy(context);
            }
            systemServicesProxy = sSystemServicesProxy;
        }
        return systemServicesProxy;
    }

    public void gc() {
        BackgroundThread.getHandler().post(this.mGcRunnable);
    }

    public boolean isRecentsActivityVisible() {
        return isRecentsActivityVisible(null);
    }

    public boolean isRecentsActivityVisible(MutableBoolean mutableBoolean) {
        if (this.mIam == null) {
            return false;
        }
        try {
            List allStackInfos = this.mIatm.getAllStackInfos();
            ComponentName componentName = null;
            ActivityManager.StackInfo stackInfo = null;
            ActivityManager.StackInfo stackInfo2 = null;
            ActivityManager.StackInfo stackInfo3 = null;
            for (int i = 0; i < allStackInfos.size(); i++) {
                ActivityManager.StackInfo stackInfo4 = (ActivityManager.StackInfo) allStackInfos.get(i);
                WindowConfiguration windowConfiguration = stackInfo4.configuration.windowConfiguration;
                int activityType = windowConfiguration.getActivityType();
                int windowingMode = windowConfiguration.getWindowingMode();
                if (stackInfo == null && activityType == 2) {
                    stackInfo = stackInfo4;
                } else if (stackInfo2 == null && activityType == 1 && (windowingMode == 1 || windowingMode == 4)) {
                    stackInfo2 = stackInfo4;
                } else if (stackInfo3 == null && activityType == 3) {
                    stackInfo3 = stackInfo4;
                }
            }
            boolean isStackNotOccluded = isStackNotOccluded(stackInfo, stackInfo2);
            boolean isStackNotOccluded2 = isStackNotOccluded(stackInfo3, stackInfo2);
            if (mutableBoolean != null) {
                mutableBoolean.value = isStackNotOccluded;
            }
            if (stackInfo3 != null) {
                componentName = stackInfo3.topActivity;
            }
            if (!isStackNotOccluded2 || componentName == null || !componentName.getPackageName().equals("com.android.systemui") || !LegacyRecentsImpl.RECENTS_ACTIVITIES.contains(componentName.getClassName())) {
                return false;
            }
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isStackNotOccluded(ActivityManager.StackInfo stackInfo, ActivityManager.StackInfo stackInfo2) {
        boolean z = false;
        boolean z2 = stackInfo == null || stackInfo.visible;
        if (stackInfo2 == null || stackInfo == null) {
            return z2;
        }
        if (stackInfo2.visible && stackInfo2.position > stackInfo.position) {
            z = true;
        }
        return z2 & (!z);
    }

    public boolean isInSafeMode() {
        return this.mIsSafeMode;
    }

    public boolean setTaskWindowingModeSplitScreenPrimary(int i, int i2, Rect rect) {
        IActivityTaskManager iActivityTaskManager = this.mIatm;
        if (iActivityTaskManager == null) {
            return false;
        }
        try {
            return iActivityTaskManager.setTaskWindowingModeSplitScreenPrimary(i, i2, true, false, rect, true);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ActivityManager.StackInfo getSplitScreenPrimaryStack() {
        try {
            return this.mIatm.getStackInfo(3, 0);
        } catch (RemoteException unused) {
            return null;
        }
    }

    public boolean hasDockedTask() {
        ActivityManager.StackInfo splitScreenPrimaryStack;
        if (this.mIam == null || (splitScreenPrimaryStack = getSplitScreenPrimaryStack()) == null) {
            return false;
        }
        int currentUser = getCurrentUser();
        boolean z = false;
        for (int length = splitScreenPrimaryStack.taskUserIds.length - 1; length >= 0 && !z; length--) {
            z = splitScreenPrimaryStack.taskUserIds[length] == currentUser;
        }
        return z;
    }

    public boolean hasTransposedNavigationBar() {
        Rect rect = new Rect();
        getStableInsets(rect);
        return rect.right > 0;
    }

    public int getCurrentUser() {
        return this.mCurrentUserId;
    }

    public int getProcessUser() {
        UserManager userManager = this.mUm;
        if (userManager == null) {
            return 0;
        }
        return userManager.getUserHandle();
    }

    public boolean isTouchExplorationEnabled() {
        AccessibilityManager accessibilityManager = this.mAccm;
        if (accessibilityManager != null && accessibilityManager.isEnabled() && this.mAccm.isTouchExplorationEnabled()) {
            return true;
        }
        return false;
    }

    public int getDeviceSmallestWidth() {
        if (this.mDisplay == null) {
            return 0;
        }
        Point point = new Point();
        this.mDisplay.getCurrentSizeRange(point, new Point());
        return point.x;
    }

    public Rect getDisplayRect() {
        Rect rect = new Rect();
        if (this.mDisplay == null) {
            return rect;
        }
        Point point = new Point();
        this.mDisplay.getRealSize(point);
        rect.set(0, 0, point.x, point.y);
        return rect;
    }

    public Rect getWindowRect() {
        Rect rect = new Rect();
        if (this.mIam == null) {
            return rect;
        }
        try {
            ActivityManager.StackInfo stackInfo = this.mIatm.getStackInfo(0, 3);
            if (stackInfo == null) {
                stackInfo = this.mIatm.getStackInfo(1, 1);
            }
            if (stackInfo != null) {
                rect.set(stackInfo.bounds);
            }
            return rect;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Throwable unused) {
        }
        return rect;
    }

    public /* synthetic */ void lambda$startActivityAsUserAsync$1$SystemServicesProxy(Intent intent, ActivityOptions activityOptions) {
        this.mContext.startActivityAsUser(intent, activityOptions != null ? activityOptions.toBundle() : null, UserHandle.CURRENT);
    }

    public void startActivityAsUserAsync(Intent intent, ActivityOptions activityOptions) {
        this.mUiOffloadThread.submit(new Runnable(intent, activityOptions) {
            /* class com.android.systemui.recents.misc.$$Lambda$SystemServicesProxy$N7nq4D_yvcF7wooCA6t2HP24UJI */
            private final /* synthetic */ Intent f$1;
            private final /* synthetic */ ActivityOptions f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                SystemServicesProxy.this.lambda$startActivityAsUserAsync$1$SystemServicesProxy(this.f$1, this.f$2);
            }
        });
    }

    public void startInPlaceAnimationOnFrontMostApplication(ActivityOptions activityOptions) {
        Bundle bundle;
        if (this.mIam != null) {
            try {
                IActivityTaskManager iActivityTaskManager = this.mIatm;
                if (activityOptions == null) {
                    bundle = null;
                } else {
                    bundle = activityOptions.toBundle();
                }
                iActivityTaskManager.startInPlaceAnimationOnFrontMostApplication(bundle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getDockedDividerSize(Context context) {
        Resources resources = context.getResources();
        return resources.getDimensionPixelSize(17105147) - (resources.getDimensionPixelSize(17105146) * 2);
    }

    public void getStableInsets(Rect rect) {
        if (this.mWm != null) {
            try {
                this.mIwm.getStableInsets(0, rect);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setRecentsVisibility(boolean z) {
        this.mUiOffloadThread.submit(new Runnable(z) {
            /* class com.android.systemui.recents.misc.$$Lambda$SystemServicesProxy$ve6L74feVQWkpgaS7KU2FyhUuE */
            private final /* synthetic */ boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                SystemServicesProxy.this.lambda$setRecentsVisibility$2$SystemServicesProxy(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$setRecentsVisibility$2$SystemServicesProxy(boolean z) {
        try {
            this.mIwm.setRecentsVisibility(z);
        } catch (RemoteException e) {
            Log.e("SystemServicesProxy", "Unable to reach window manager", e);
        }
    }
}
