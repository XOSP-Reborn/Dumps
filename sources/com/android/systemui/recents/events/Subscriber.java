package com.android.systemui.recents.events;

import java.lang.ref.WeakReference;

/* access modifiers changed from: package-private */
/* compiled from: EventBus */
public class Subscriber {
    private WeakReference<Object> mSubscriber;
    long registrationTime;

    Subscriber(Object obj, long j) {
        this.mSubscriber = new WeakReference<>(obj);
        this.registrationTime = j;
    }

    public String toString(int i) {
        Object obj = this.mSubscriber.get();
        String hexString = Integer.toHexString(System.identityHashCode(obj));
        return obj.getClass().getSimpleName() + " [0x" + hexString + ", P" + i + "]";
    }

    public Object getReference() {
        return this.mSubscriber.get();
    }
}
