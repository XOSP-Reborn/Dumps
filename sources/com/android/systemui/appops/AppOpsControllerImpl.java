package com.android.systemui.appops;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dumpable;
import com.android.systemui.appops.AppOpsController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppOpsControllerImpl implements AppOpsController, AppOpsManager.OnOpActiveChangedListener, AppOpsManager.OnOpNotedListener, Dumpable {
    protected static final int[] OPS = {26, 24, 27, 0, 1};
    @GuardedBy({"mActiveItems"})
    private final List<AppOpItem> mActiveItems = new ArrayList();
    private final AppOpsManager mAppOps;
    private H mBGHandler;
    private final List<AppOpsController.Callback> mCallbacks = new ArrayList();
    private final ArrayMap<Integer, Set<AppOpsController.Callback>> mCallbacksByCode = new ArrayMap<>();
    private final Context mContext;
    @GuardedBy({"mNotedItems"})
    private final List<AppOpItem> mNotedItems = new ArrayList();

    public AppOpsControllerImpl(Context context, Looper looper) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mBGHandler = new H(looper);
        int length = OPS.length;
        for (int i = 0; i < length; i++) {
            this.mCallbacksByCode.put(Integer.valueOf(OPS[i]), new ArraySet());
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setBGHandler(H h) {
        this.mBGHandler = h;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setListening(boolean z) {
        if (z) {
            this.mAppOps.startWatchingActive(OPS, this);
            this.mAppOps.startWatchingNoted(OPS, this);
            return;
        }
        this.mAppOps.stopWatchingActive(this);
        this.mAppOps.stopWatchingNoted(this);
        this.mBGHandler.removeCallbacksAndMessages(null);
        synchronized (this.mActiveItems) {
            this.mActiveItems.clear();
        }
        synchronized (this.mNotedItems) {
            this.mNotedItems.clear();
        }
    }

    @Override // com.android.systemui.appops.AppOpsController
    public void addCallback(int[] iArr, AppOpsController.Callback callback) {
        int length = iArr.length;
        boolean z = false;
        for (int i = 0; i < length; i++) {
            if (this.mCallbacksByCode.containsKey(Integer.valueOf(iArr[i]))) {
                this.mCallbacksByCode.get(Integer.valueOf(iArr[i])).add(callback);
                z = true;
            }
        }
        if (z) {
            this.mCallbacks.add(callback);
        }
        if (!this.mCallbacks.isEmpty()) {
            setListening(true);
        }
    }

    private AppOpItem getAppOpItem(List<AppOpItem> list, int i, int i2, String str) {
        int size = list.size();
        for (int i3 = 0; i3 < size; i3++) {
            AppOpItem appOpItem = list.get(i3);
            if (appOpItem.getCode() == i && appOpItem.getUid() == i2 && appOpItem.getPackageName().equals(str)) {
                return appOpItem;
            }
        }
        return null;
    }

    private boolean updateActives(int i, int i2, String str, boolean z) {
        synchronized (this.mActiveItems) {
            AppOpItem appOpItem = getAppOpItem(this.mActiveItems, i, i2, str);
            if (appOpItem == null && z) {
                this.mActiveItems.add(new AppOpItem(i, i2, str, System.currentTimeMillis()));
                return true;
            } else if (appOpItem == null || z) {
                return false;
            } else {
                this.mActiveItems.remove(appOpItem);
                return true;
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void removeNoted(int i, int i2, String str) {
        synchronized (this.mNotedItems) {
            AppOpItem appOpItem = getAppOpItem(this.mNotedItems, i, i2, str);
            if (appOpItem != null) {
                this.mNotedItems.remove(appOpItem);
                notifySuscribers(i, i2, str, false);
            }
        }
    }

    private void addNoted(int i, int i2, String str) {
        AppOpItem appOpItem;
        synchronized (this.mNotedItems) {
            appOpItem = getAppOpItem(this.mNotedItems, i, i2, str);
            if (appOpItem == null) {
                appOpItem = new AppOpItem(i, i2, str, System.currentTimeMillis());
                this.mNotedItems.add(appOpItem);
            }
        }
        this.mBGHandler.scheduleRemoval(appOpItem, 5000);
    }

    public void onOpActiveChanged(int i, int i2, String str, boolean z) {
        if (updateActives(i, i2, str, z)) {
            notifySuscribers(i, i2, str, z);
        }
    }

    public void onOpNoted(int i, int i2, String str, int i3) {
        if (i3 == 0) {
            addNoted(i, i2, str);
            notifySuscribers(i, i2, str, true);
        }
    }

    private void notifySuscribers(int i, int i2, String str, boolean z) {
        if (this.mCallbacksByCode.containsKey(Integer.valueOf(i))) {
            for (AppOpsController.Callback callback : this.mCallbacksByCode.get(Integer.valueOf(i))) {
                callback.onActiveStateChanged(i, i2, str, z);
            }
        }
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("AppOpsController state:");
        printWriter.println("  Active Items:");
        for (int i = 0; i < this.mActiveItems.size(); i++) {
            printWriter.print("    ");
            printWriter.println(this.mActiveItems.get(i).toString());
        }
        printWriter.println("  Noted Items:");
        for (int i2 = 0; i2 < this.mNotedItems.size(); i2++) {
            printWriter.print("    ");
            printWriter.println(this.mNotedItems.get(i2).toString());
        }
    }

    /* access modifiers changed from: protected */
    public final class H extends Handler {
        H(Looper looper) {
            super(looper);
        }

        public void scheduleRemoval(final AppOpItem appOpItem, long j) {
            removeCallbacksAndMessages(appOpItem);
            postDelayed(new Runnable() {
                /* class com.android.systemui.appops.AppOpsControllerImpl.H.AnonymousClass1 */

                public void run() {
                    AppOpsControllerImpl.this.removeNoted(appOpItem.getCode(), appOpItem.getUid(), appOpItem.getPackageName());
                }
            }, appOpItem, j);
        }
    }
}
