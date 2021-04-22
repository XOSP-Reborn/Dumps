package com.android.systemui.stackdivider;

import android.app.ActivityTaskManager;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.GuardedBy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WindowManagerProxy {
    private static final WindowManagerProxy sInstance = new WindowManagerProxy();
    private float mDimLayerAlpha;
    private final Runnable mDimLayerRunnable = new Runnable() {
        /* class com.android.systemui.stackdivider.WindowManagerProxy.AnonymousClass4 */

        public void run() {
            try {
                WindowManagerGlobal.getWindowManagerService().setResizeDimLayer(WindowManagerProxy.this.mDimLayerVisible, WindowManagerProxy.this.mDimLayerTargetWindowingMode, WindowManagerProxy.this.mDimLayerAlpha);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private int mDimLayerTargetWindowingMode;
    private boolean mDimLayerVisible;
    private final Runnable mDismissRunnable = new Runnable() {
        /* class com.android.systemui.stackdivider.WindowManagerProxy.AnonymousClass2 */

        public void run() {
            try {
                ActivityTaskManager.getService().dismissSplitScreenMode(false);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to remove stack: " + e);
            }
        }
    };
    @GuardedBy({"mDockedRect"})
    private final Rect mDockedRect = new Rect();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Runnable mMaximizeRunnable = new Runnable() {
        /* class com.android.systemui.stackdivider.WindowManagerProxy.AnonymousClass3 */

        public void run() {
            try {
                ActivityTaskManager.getService().dismissSplitScreenMode(true);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private final Runnable mResizeRunnable = new Runnable() {
        /* class com.android.systemui.stackdivider.WindowManagerProxy.AnonymousClass1 */

        public void run() {
            synchronized (WindowManagerProxy.this.mDockedRect) {
                WindowManagerProxy.this.mTmpRect1.set(WindowManagerProxy.this.mDockedRect);
                WindowManagerProxy.this.mTmpRect2.set(WindowManagerProxy.this.mTempDockedTaskRect);
                WindowManagerProxy.this.mTmpRect3.set(WindowManagerProxy.this.mTempDockedInsetRect);
                WindowManagerProxy.this.mTmpRect4.set(WindowManagerProxy.this.mTempOtherTaskRect);
                WindowManagerProxy.this.mTmpRect5.set(WindowManagerProxy.this.mTempOtherInsetRect);
            }
            try {
                ActivityTaskManager.getService().resizeDockedStack(WindowManagerProxy.this.mTmpRect1, WindowManagerProxy.this.mTmpRect2.isEmpty() ? null : WindowManagerProxy.this.mTmpRect2, WindowManagerProxy.this.mTmpRect3.isEmpty() ? null : WindowManagerProxy.this.mTmpRect3, WindowManagerProxy.this.mTmpRect4.isEmpty() ? null : WindowManagerProxy.this.mTmpRect4, WindowManagerProxy.this.mTmpRect5.isEmpty() ? null : WindowManagerProxy.this.mTmpRect5);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private final Runnable mSetTouchableRegionRunnable = new Runnable() {
        /* class com.android.systemui.stackdivider.WindowManagerProxy.AnonymousClass5 */

        public void run() {
            try {
                synchronized (WindowManagerProxy.this.mDockedRect) {
                    WindowManagerProxy.this.mTmpRect1.set(WindowManagerProxy.this.mTouchableRegion);
                }
                WindowManagerGlobal.getWindowManagerService().setDockedStackDividerTouchRegion(WindowManagerProxy.this.mTmpRect1);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to set touchable region: " + e);
            }
        }
    };
    private final Rect mTempDockedInsetRect = new Rect();
    private final Rect mTempDockedTaskRect = new Rect();
    private final Rect mTempOtherInsetRect = new Rect();
    private final Rect mTempOtherTaskRect = new Rect();
    private final Rect mTmpRect1 = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final Rect mTmpRect3 = new Rect();
    private final Rect mTmpRect4 = new Rect();
    private final Rect mTmpRect5 = new Rect();
    @GuardedBy({"mDockedRect"})
    private final Rect mTouchableRegion = new Rect();

    private WindowManagerProxy() {
    }

    public static WindowManagerProxy getInstance() {
        return sInstance;
    }

    public void resizeDockedStack(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5) {
        synchronized (this.mDockedRect) {
            this.mDockedRect.set(rect);
            if (rect2 != null) {
                this.mTempDockedTaskRect.set(rect2);
            } else {
                this.mTempDockedTaskRect.setEmpty();
            }
            if (rect3 != null) {
                this.mTempDockedInsetRect.set(rect3);
            } else {
                this.mTempDockedInsetRect.setEmpty();
            }
            if (rect4 != null) {
                this.mTempOtherTaskRect.set(rect4);
            } else {
                this.mTempOtherTaskRect.setEmpty();
            }
            if (rect5 != null) {
                this.mTempOtherInsetRect.set(rect5);
            } else {
                this.mTempOtherInsetRect.setEmpty();
            }
        }
        this.mExecutor.execute(this.mResizeRunnable);
    }

    public void dismissDockedStack() {
        this.mExecutor.execute(this.mDismissRunnable);
    }

    public void maximizeDockedStack() {
        this.mExecutor.execute(this.mMaximizeRunnable);
    }

    public void setResizing(final boolean z) {
        this.mExecutor.execute(new Runnable() {
            /* class com.android.systemui.stackdivider.WindowManagerProxy.AnonymousClass6 */

            public void run() {
                try {
                    ActivityTaskManager.getService().setSplitScreenResizing(z);
                } catch (RemoteException e) {
                    Log.w("WindowManagerProxy", "Error calling setDockedStackResizing: " + e);
                }
            }
        });
    }

    public int getDockSide() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getDockedStackSide();
        } catch (RemoteException e) {
            Log.w("WindowManagerProxy", "Failed to get dock side: " + e);
            return -1;
        }
    }

    public void setResizeDimLayer(boolean z, int i, float f) {
        this.mDimLayerVisible = z;
        this.mDimLayerTargetWindowingMode = i;
        this.mDimLayerAlpha = f;
        this.mExecutor.execute(this.mDimLayerRunnable);
    }

    public void setTouchRegion(Rect rect) {
        synchronized (this.mDockedRect) {
            this.mTouchableRegion.set(rect);
        }
        this.mExecutor.execute(this.mSetTouchableRegionRunnable);
    }
}
