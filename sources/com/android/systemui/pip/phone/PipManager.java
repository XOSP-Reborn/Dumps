package com.android.systemui.pip.phone;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.pip.BasePipManager;
import com.android.systemui.pip.phone.PipManager;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.InputConsumerController;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.shared.system.WindowManagerWrapper;
import java.io.PrintWriter;

public class PipManager implements BasePipManager {
    private static PipManager sPipController;
    private IActivityManager mActivityManager;
    private IActivityTaskManager mActivityTaskManager;
    private PipAppOpsListener mAppOpsListener;
    private Context mContext;
    private Handler mHandler = new Handler();
    private InputConsumerController mInputConsumerController;
    private PipMediaController mMediaController;
    private PipMenuActivityController mMenuController;
    private final PinnedStackListener mPinnedStackListener = new PinnedStackListener();
    TaskStackChangeListener mTaskStackListener = new TaskStackChangeListener() {
        /* class com.android.systemui.pip.phone.PipManager.AnonymousClass1 */

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onActivityPinned(String str, int i, int i2, int i3) {
            PipManager.this.mTouchHandler.onActivityPinned();
            PipManager.this.mMediaController.onActivityPinned();
            PipManager.this.mMenuController.onActivityPinned();
            PipManager.this.mAppOpsListener.onActivityPinned(str);
            ((UiOffloadThread) Dependency.get(UiOffloadThread.class)).submit($$Lambda$PipManager$1$GurLWXFKpAPDop_aRGndKBjZCWU.INSTANCE);
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onActivityUnpinned() {
            ComponentName componentName = (ComponentName) PipUtils.getTopPinnedActivity(PipManager.this.mContext, PipManager.this.mActivityManager).first;
            PipManager.this.mMenuController.onActivityUnpinned();
            PipManager.this.mTouchHandler.onActivityUnpinned(componentName);
            PipManager.this.mAppOpsListener.onActivityUnpinned();
            ((UiOffloadThread) Dependency.get(UiOffloadThread.class)).submit(new Runnable(componentName) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipManager$1$ngvLEQ68U0fQkcsOpQTOX3GlNKk */
                private final /* synthetic */ ComponentName f$0;

                {
                    this.f$0 = r1;
                }

                public final void run() {
                    PipManager.AnonymousClass1.lambda$onActivityUnpinned$1(this.f$0);
                }
            });
        }

        static /* synthetic */ void lambda$onActivityUnpinned$1(ComponentName componentName) {
            WindowManagerWrapper.getInstance().setPipVisibility(componentName != null);
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onPinnedStackAnimationStarted() {
            PipManager.this.mTouchHandler.setTouchEnabled(false);
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onPinnedStackAnimationEnded() {
            PipManager.this.mTouchHandler.setTouchEnabled(true);
            PipManager.this.mTouchHandler.onPinnedStackAnimationEnded();
            PipManager.this.mMenuController.onPinnedStackAnimationEnded();
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onPinnedActivityRestartAttempt(boolean z) {
            PipManager.this.mTouchHandler.getMotionHelper().expandPip(z);
        }
    };
    private PipTouchHandler mTouchHandler;

    /* access modifiers changed from: private */
    public class PinnedStackListener extends IPinnedStackListener.Stub {
        private PinnedStackListener() {
        }

        public void onListenerRegistered(IPinnedStackController iPinnedStackController) {
            PipManager.this.mHandler.post(new Runnable(iPinnedStackController) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipManager$PinnedStackListener$fsM0yPTeQnwLCmc8K2TS4ZFeBWc */
                private final /* synthetic */ IPinnedStackController f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PipManager.PinnedStackListener.this.lambda$onListenerRegistered$0$PipManager$PinnedStackListener(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$onListenerRegistered$0$PipManager$PinnedStackListener(IPinnedStackController iPinnedStackController) {
            PipManager.this.mTouchHandler.setPinnedStackController(iPinnedStackController);
        }

        public void onImeVisibilityChanged(boolean z, int i) {
            PipManager.this.mHandler.post(new Runnable(z, i) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipManager$PinnedStackListener$VBLjn70VeOT58ISp8JJdGGwiLRI */
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    PipManager.PinnedStackListener.this.lambda$onImeVisibilityChanged$1$PipManager$PinnedStackListener(this.f$1, this.f$2);
                }
            });
        }

        public /* synthetic */ void lambda$onImeVisibilityChanged$1$PipManager$PinnedStackListener(boolean z, int i) {
            PipManager.this.mTouchHandler.onImeVisibilityChanged(z, i);
        }

        public void onShelfVisibilityChanged(boolean z, int i) {
            PipManager.this.mHandler.post(new Runnable(z, i) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipManager$PinnedStackListener$bf4e5rlYRO_U_i4UtAT1QucT53g */
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    PipManager.PinnedStackListener.this.lambda$onShelfVisibilityChanged$2$PipManager$PinnedStackListener(this.f$1, this.f$2);
                }
            });
        }

        public /* synthetic */ void lambda$onShelfVisibilityChanged$2$PipManager$PinnedStackListener(boolean z, int i) {
            PipManager.this.mTouchHandler.onShelfVisibilityChanged(z, i);
        }

        public void onMinimizedStateChanged(boolean z) {
            PipManager.this.mHandler.post(new Runnable(z) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipManager$PinnedStackListener$BUR7BmLfjK0NpOw2OLHQV6xTO5k */
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PipManager.PinnedStackListener.this.lambda$onMinimizedStateChanged$3$PipManager$PinnedStackListener(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$onMinimizedStateChanged$3$PipManager$PinnedStackListener(boolean z) {
            PipManager.this.mTouchHandler.setMinimizedState(z, true);
        }

        public void onMovementBoundsChanged(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, int i) {
            PipManager.this.mHandler.post(new Runnable(rect, rect2, rect3, z, z2, i) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipManager$PinnedStackListener$qj7lqmu1a4XOuu8emxk_Cwvcxo */
                private final /* synthetic */ Rect f$1;
                private final /* synthetic */ Rect f$2;
                private final /* synthetic */ Rect f$3;
                private final /* synthetic */ boolean f$4;
                private final /* synthetic */ boolean f$5;
                private final /* synthetic */ int f$6;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                    this.f$5 = r6;
                    this.f$6 = r7;
                }

                public final void run() {
                    PipManager.PinnedStackListener.this.lambda$onMovementBoundsChanged$4$PipManager$PinnedStackListener(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
                }
            });
        }

        public /* synthetic */ void lambda$onMovementBoundsChanged$4$PipManager$PinnedStackListener(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, int i) {
            PipManager.this.mTouchHandler.onMovementBoundsChanged(rect, rect2, rect3, z, z2, i);
        }

        public void onActionsChanged(ParceledListSlice parceledListSlice) {
            PipManager.this.mHandler.post(new Runnable(parceledListSlice) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipManager$PinnedStackListener$JU_GjrpL4fTB9HLmwOZwFKXw */
                private final /* synthetic */ ParceledListSlice f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PipManager.PinnedStackListener.this.lambda$onActionsChanged$5$PipManager$PinnedStackListener(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$onActionsChanged$5$PipManager$PinnedStackListener(ParceledListSlice parceledListSlice) {
            PipManager.this.mMenuController.setAppActions(parceledListSlice);
        }
    }

    private PipManager() {
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void initialize(Context context) {
        this.mContext = context;
        this.mActivityManager = ActivityManager.getService();
        this.mActivityTaskManager = ActivityTaskManager.getService();
        try {
            WindowManagerWrapper.getInstance().addPinnedStackListener(this.mPinnedStackListener);
        } catch (RemoteException e) {
            Log.e("PipManager", "Failed to register pinned stack listener", e);
        }
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        this.mInputConsumerController = InputConsumerController.getPipInputConsumer();
        this.mInputConsumerController.registerInputConsumer();
        this.mMediaController = new PipMediaController(context, this.mActivityManager);
        this.mMenuController = new PipMenuActivityController(context, this.mActivityManager, this.mMediaController, this.mInputConsumerController);
        this.mTouchHandler = new PipTouchHandler(context, this.mActivityManager, this.mActivityTaskManager, this.mMenuController, this.mInputConsumerController);
        this.mAppOpsListener = new PipAppOpsListener(context, this.mActivityManager, this.mTouchHandler.getMotionHelper());
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void onConfigurationChanged(Configuration configuration) {
        this.mTouchHandler.onConfigurationChanged();
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void expandPip() {
        this.mTouchHandler.getMotionHelper().expandPip(false);
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void hidePipMenu(Runnable runnable, Runnable runnable2) {
        this.mMenuController.hideMenu(runnable, runnable2);
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void showPictureInPictureMenu() {
        this.mTouchHandler.showPictureInPictureMenu();
    }

    public static PipManager getInstance() {
        if (sPipController == null) {
            sPipController = new PipManager();
        }
        return sPipController;
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void dump(PrintWriter printWriter) {
        printWriter.println("PipManager");
        this.mInputConsumerController.dump(printWriter, "  ");
        this.mMenuController.dump(printWriter, "  ");
        this.mTouchHandler.dump(printWriter, "  ");
    }
}
