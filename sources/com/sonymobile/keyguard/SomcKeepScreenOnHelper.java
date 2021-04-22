package com.sonymobile.keyguard;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import java.lang.ref.WeakReference;

public class SomcKeepScreenOnHelper {
    private Handler mKeepScreenOnInLockscreenHandler;
    private final SomcUserActivityPoker mUserActivityPoker;

    public SomcKeepScreenOnHelper(SomcUserActivityPoker somcUserActivityPoker) {
        this.mUserActivityPoker = somcUserActivityPoker;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            /* class com.sonymobile.keyguard.SomcKeepScreenOnHelper.AnonymousClass1 */

            public void run() {
                SomcKeepScreenOnHelper somcKeepScreenOnHelper = SomcKeepScreenOnHelper.this;
                somcKeepScreenOnHelper.mKeepScreenOnInLockscreenHandler = new KeepScreenOnHandler(somcKeepScreenOnHelper);
            }
        });
    }

    public final void checkIfMotionEventShouldKeepScreenOn(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 1) {
            if (actionMasked != 2) {
                if (actionMasked != 3) {
                    return;
                }
            } else if (!this.mKeepScreenOnInLockscreenHandler.hasMessages(1001)) {
                this.mKeepScreenOnInLockscreenHandler.sendEmptyMessageDelayed(1001, 1000);
                return;
            } else {
                return;
            }
        }
        this.mKeepScreenOnInLockscreenHandler.removeMessages(1001);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleKeepScreenOnMessage(Message message) {
        if (message != null && message.what == 1001) {
            this.mUserActivityPoker.poke();
        }
    }

    private static class KeepScreenOnHandler extends Handler {
        private final WeakReference<SomcKeepScreenOnHelper> mSomcKeepScreenOnHelper;

        public KeepScreenOnHandler(SomcKeepScreenOnHelper somcKeepScreenOnHelper) {
            this.mSomcKeepScreenOnHelper = new WeakReference<>(somcKeepScreenOnHelper);
        }

        public void handleMessage(Message message) {
            SomcKeepScreenOnHelper somcKeepScreenOnHelper = this.mSomcKeepScreenOnHelper.get();
            if (somcKeepScreenOnHelper != null) {
                somcKeepScreenOnHelper.handleKeepScreenOnMessage(message);
            }
        }
    }
}
