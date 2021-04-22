package com.android.keyguard.clock;

import com.android.keyguard.clock.ClockManager;
import java.util.function.BiConsumer;

/* renamed from: com.android.keyguard.clock.-$$Lambda$ClockManager$i436KHmxBKLRfCOA6rL_7pJbxgc  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$ClockManager$i436KHmxBKLRfCOA6rL_7pJbxgc implements BiConsumer {
    public static final /* synthetic */ $$Lambda$ClockManager$i436KHmxBKLRfCOA6rL_7pJbxgc INSTANCE = new $$Lambda$ClockManager$i436KHmxBKLRfCOA6rL_7pJbxgc();

    private /* synthetic */ $$Lambda$ClockManager$i436KHmxBKLRfCOA6rL_7pJbxgc() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ClockManager.lambda$reload$4((ClockManager.ClockChangedListener) obj, (ClockManager.AvailableClocks) obj2);
    }
}
