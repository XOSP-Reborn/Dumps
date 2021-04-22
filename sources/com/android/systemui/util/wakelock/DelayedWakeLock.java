package com.android.systemui.util.wakelock;

import android.os.Handler;

public class DelayedWakeLock implements WakeLock {
    private final Handler mHandler;
    private final WakeLock mInner;

    public DelayedWakeLock(Handler handler, WakeLock wakeLock) {
        this.mHandler = handler;
        this.mInner = wakeLock;
    }

    @Override // com.android.systemui.util.wakelock.WakeLock
    public void acquire(String str) {
        this.mInner.acquire(str);
    }

    public /* synthetic */ void lambda$release$0$DelayedWakeLock(String str) {
        this.mInner.release(str);
    }

    @Override // com.android.systemui.util.wakelock.WakeLock
    public void release(String str) {
        this.mHandler.postDelayed(new Runnable(str) {
            /* class com.android.systemui.util.wakelock.$$Lambda$DelayedWakeLock$aTG9u0wfrNahXJF_VixBxfvFqfg */
            private final /* synthetic */ String f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                DelayedWakeLock.this.lambda$release$0$DelayedWakeLock(this.f$1);
            }
        }, 100);
    }

    @Override // com.android.systemui.util.wakelock.WakeLock
    public Runnable wrap(Runnable runnable) {
        return WakeLock.wrapImpl(this, runnable);
    }

    public String toString() {
        return "[DelayedWakeLock] " + this.mInner;
    }
}
