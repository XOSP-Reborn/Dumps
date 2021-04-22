package com.android.systemui.pip.phone;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.pip.phone.PipMediaController;
import com.android.systemui.pip.phone.PipMenuActivityController;
import com.android.systemui.shared.system.InputConsumerController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PipMenuActivityController {
    private IActivityManager mActivityManager;
    private ParceledListSlice mAppActions;
    private Context mContext;
    private Handler mHandler = new Handler() {
        /* class com.android.systemui.pip.phone.PipMenuActivityController.AnonymousClass1 */

        public void handleMessage(Message message) {
            switch (message.what) {
                case 100:
                    PipMenuActivityController.this.onMenuStateChanged(message.arg1, true);
                    return;
                case 101:
                    PipMenuActivityController.this.mListeners.forEach($$Lambda$PipMenuActivityController$1$8btqC3E6FFjbjLWUhiNmbnKUlfI.INSTANCE);
                    return;
                case 102:
                    PipMenuActivityController.this.mListeners.forEach($$Lambda$PipMenuActivityController$1$o9fLqvuiKIYwdsSexRT0X4Ty0V4.INSTANCE);
                    return;
                case 103:
                    PipMenuActivityController.this.mListeners.forEach($$Lambda$PipMenuActivityController$1$rDXDKqpw1CLC0fwevwYEng68Bps.INSTANCE);
                    return;
                case 104:
                    PipMenuActivityController.this.mToActivityMessenger = message.replyTo;
                    PipMenuActivityController.this.setStartActivityRequested(false);
                    if (PipMenuActivityController.this.mOnAnimationEndRunnable != null) {
                        PipMenuActivityController.this.mOnAnimationEndRunnable.run();
                        PipMenuActivityController.this.mOnAnimationEndRunnable = null;
                    }
                    if (PipMenuActivityController.this.mToActivityMessenger == null) {
                        PipMenuActivityController.this.onMenuStateChanged(0, true);
                        return;
                    }
                    return;
                case 105:
                    PipMenuActivityController.this.mInputConsumerController.registerInputConsumer();
                    return;
                case 106:
                    PipMenuActivityController.this.mInputConsumerController.unregisterInputConsumer();
                    return;
                case 107:
                    PipMenuActivityController.this.mListeners.forEach($$Lambda$PipMenuActivityController$1$nEDJFK5X9H1WAx_9S8qUwV6KLY.INSTANCE);
                    return;
                default:
                    return;
            }
        }
    };
    private InputConsumerController mInputConsumerController;
    private ArrayList<Listener> mListeners = new ArrayList<>();
    private PipMediaController.ActionListener mMediaActionListener = new PipMediaController.ActionListener() {
        /* class com.android.systemui.pip.phone.PipMenuActivityController.AnonymousClass2 */

        @Override // com.android.systemui.pip.phone.PipMediaController.ActionListener
        public void onMediaActionsChanged(List<RemoteAction> list) {
            PipMenuActivityController.this.mMediaActions = new ParceledListSlice(list);
            PipMenuActivityController.this.updateMenuActions();
        }
    };
    private ParceledListSlice mMediaActions;
    private PipMediaController mMediaController;
    private int mMenuState;
    private Messenger mMessenger = new Messenger(this.mHandler);
    private Runnable mOnAnimationEndRunnable;
    private boolean mStartActivityRequested;
    private long mStartActivityRequestedTime;
    private Runnable mStartActivityRequestedTimeoutRunnable = new Runnable() {
        /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivityController$46Yr3xVHMZsGyZiGhSKF_IPBnzk */

        public final void run() {
            PipMenuActivityController.this.lambda$new$0$PipMenuActivityController();
        }
    };
    private Bundle mTmpDismissFractionData = new Bundle();
    private Messenger mToActivityMessenger;

    public interface Listener {
        void onPipDismiss();

        void onPipExpand();

        void onPipMenuStateChanged(int i, boolean z);

        void onPipMinimize();

        void onPipShowMenu();
    }

    public /* synthetic */ void lambda$new$0$PipMenuActivityController() {
        setStartActivityRequested(false);
        Runnable runnable = this.mOnAnimationEndRunnable;
        if (runnable != null) {
            runnable.run();
            this.mOnAnimationEndRunnable = null;
        }
        Log.e("PipMenuActController", "Expected start menu activity request timed out");
    }

    public PipMenuActivityController(Context context, IActivityManager iActivityManager, PipMediaController pipMediaController, InputConsumerController inputConsumerController) {
        this.mContext = context;
        this.mActivityManager = iActivityManager;
        this.mMediaController = pipMediaController;
        this.mInputConsumerController = inputConsumerController;
    }

    public boolean isMenuActivityVisible() {
        return this.mToActivityMessenger != null;
    }

    public void onActivityPinned() {
        if (this.mMenuState == 0) {
            this.mInputConsumerController.registerInputConsumer();
        }
    }

    public void onActivityUnpinned() {
        hideMenu();
        setStartActivityRequested(false);
    }

    public void onPinnedStackAnimationEnded() {
        if (this.mToActivityMessenger != null) {
            Message obtain = Message.obtain();
            obtain.what = 6;
            try {
                this.mToActivityMessenger.send(obtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu pinned animation ended", e);
            }
        }
    }

    public void addListener(Listener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    public void setDismissFraction(float f) {
        if (this.mToActivityMessenger != null) {
            this.mTmpDismissFractionData.clear();
            this.mTmpDismissFractionData.putFloat("dismiss_fraction", f);
            Message obtain = Message.obtain();
            obtain.what = 5;
            obtain.obj = this.mTmpDismissFractionData;
            try {
                this.mToActivityMessenger.send(obtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu to update dismiss fraction", e);
            }
        } else if (!this.mStartActivityRequested || isStartActivityRequestedElapsed()) {
            startMenuActivity(0, null, null, false, false);
        }
    }

    public void showMenu(int i, Rect rect, Rect rect2, boolean z, boolean z2) {
        if (this.mToActivityMessenger != null) {
            Bundle bundle = new Bundle();
            bundle.putInt("menu_state", i);
            bundle.putParcelable("stack_bounds", rect);
            bundle.putParcelable("movement_bounds", rect2);
            bundle.putBoolean("allow_timeout", z);
            bundle.putBoolean("resize_menu_on_show", z2);
            Message obtain = Message.obtain();
            obtain.what = 1;
            obtain.obj = bundle;
            try {
                this.mToActivityMessenger.send(obtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu to show", e);
            }
        } else if (!this.mStartActivityRequested || isStartActivityRequestedElapsed()) {
            startMenuActivity(i, rect, rect2, z, z2);
        }
    }

    public void pokeMenu() {
        if (this.mToActivityMessenger != null) {
            Message obtain = Message.obtain();
            obtain.what = 2;
            try {
                this.mToActivityMessenger.send(obtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify poke menu", e);
            }
        }
    }

    public void hideMenu() {
        if (this.mToActivityMessenger != null) {
            Message obtain = Message.obtain();
            obtain.what = 3;
            try {
                this.mToActivityMessenger.send(obtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu to hide", e);
            }
        }
    }

    public void hideMenu(Runnable runnable, Runnable runnable2) {
        if (this.mStartActivityRequested) {
            this.mOnAnimationEndRunnable = runnable2;
            runnable.run();
            this.mHandler.removeCallbacks(this.mStartActivityRequestedTimeoutRunnable);
            this.mHandler.postDelayed(this.mStartActivityRequestedTimeoutRunnable, 300);
        } else if (this.mMenuState != 0 && this.mToActivityMessenger != null) {
            runnable.run();
            Message obtain = Message.obtain();
            obtain.what = 3;
            obtain.obj = runnable2;
            try {
                this.mToActivityMessenger.send(obtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify hide menu", e);
            }
        }
    }

    public void hideMenuWithoutResize() {
        onMenuStateChanged(0, false);
    }

    public void setAppActions(ParceledListSlice parceledListSlice) {
        this.mAppActions = parceledListSlice;
        updateMenuActions();
    }

    private ParceledListSlice resolveMenuActions() {
        if (isValidActions(this.mAppActions)) {
            return this.mAppActions;
        }
        return this.mMediaActions;
    }

    private void startMenuActivity(int i, Rect rect, Rect rect2, boolean z, boolean z2) {
        try {
            ActivityManager.StackInfo stackInfo = ActivityTaskManager.getService().getStackInfo(2, 0);
            if (stackInfo == null || stackInfo.taskIds == null || stackInfo.taskIds.length <= 0) {
                Log.e("PipMenuActController", "No PIP tasks found");
                return;
            }
            Intent intent = new Intent(this.mContext, PipMenuActivity.class);
            intent.putExtra("messenger", this.mMessenger);
            intent.putExtra("actions", (Parcelable) resolveMenuActions());
            if (rect != null) {
                intent.putExtra("stack_bounds", rect);
            }
            if (rect2 != null) {
                intent.putExtra("movement_bounds", rect2);
            }
            intent.putExtra("menu_state", i);
            intent.putExtra("allow_timeout", z);
            intent.putExtra("resize_menu_on_show", z2);
            ActivityOptions makeCustomAnimation = ActivityOptions.makeCustomAnimation(this.mContext, 0, 0);
            makeCustomAnimation.setLaunchTaskId(stackInfo.taskIds[stackInfo.taskIds.length - 1]);
            makeCustomAnimation.setTaskOverlay(true, true);
            this.mContext.startActivityAsUser(intent, makeCustomAnimation.toBundle(), UserHandle.CURRENT);
            setStartActivityRequested(true);
        } catch (RemoteException e) {
            setStartActivityRequested(false);
            Log.e("PipMenuActController", "Error showing PIP menu activity", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateMenuActions() {
        if (this.mToActivityMessenger != null) {
            Rect rect = null;
            try {
                ActivityManager.StackInfo stackInfo = ActivityTaskManager.getService().getStackInfo(2, 0);
                if (stackInfo != null) {
                    rect = stackInfo.bounds;
                }
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Error showing PIP menu activity", e);
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable("stack_bounds", rect);
            bundle.putParcelable("actions", resolveMenuActions());
            Message obtain = Message.obtain();
            obtain.what = 4;
            obtain.obj = bundle;
            try {
                this.mToActivityMessenger.send(obtain);
            } catch (RemoteException e2) {
                Log.e("PipMenuActController", "Could not notify menu activity to update actions", e2);
            }
        }
    }

    private boolean isValidActions(ParceledListSlice parceledListSlice) {
        return parceledListSlice != null && parceledListSlice.getList().size() > 0;
    }

    private boolean isStartActivityRequestedElapsed() {
        return SystemClock.uptimeMillis() - this.mStartActivityRequestedTime >= 300;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onMenuStateChanged(int i, boolean z) {
        if (i == 0) {
            this.mInputConsumerController.registerInputConsumer();
        } else {
            this.mInputConsumerController.unregisterInputConsumer();
        }
        if (i != this.mMenuState) {
            this.mListeners.forEach(new Consumer(i, z) {
                /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivityController$oZuzXTzYX29YiUgUX8q8QZcGtw */
                private final /* synthetic */ int f$0;
                private final /* synthetic */ boolean f$1;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                }

                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((PipMenuActivityController.Listener) obj).onPipMenuStateChanged(this.f$0, this.f$1);
                }
            });
            if (i == 2) {
                this.mMediaController.addListener(this.mMediaActionListener);
            } else {
                this.mMediaController.removeListener(this.mMediaActionListener);
            }
        }
        this.mMenuState = i;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setStartActivityRequested(boolean z) {
        this.mHandler.removeCallbacks(this.mStartActivityRequestedTimeoutRunnable);
        this.mStartActivityRequested = z;
        this.mStartActivityRequestedTime = z ? SystemClock.uptimeMillis() : 0;
    }

    public void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        printWriter.println(str + "PipMenuActController");
        printWriter.println(str2 + "mMenuState=" + this.mMenuState);
        printWriter.println(str2 + "mToActivityMessenger=" + this.mToActivityMessenger);
        printWriter.println(str2 + "mListeners=" + this.mListeners.size());
        printWriter.println(str2 + "mStartActivityRequested=" + this.mStartActivityRequested);
        printWriter.println(str2 + "mStartActivityRequestedTime=" + this.mStartActivityRequestedTime);
    }
}
