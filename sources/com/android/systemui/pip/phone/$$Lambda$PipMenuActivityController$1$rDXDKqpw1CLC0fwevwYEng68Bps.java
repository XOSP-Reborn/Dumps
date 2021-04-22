package com.android.systemui.pip.phone;

import com.android.systemui.pip.phone.PipMenuActivityController;
import java.util.function.Consumer;

/* renamed from: com.android.systemui.pip.phone.-$$Lambda$PipMenuActivityController$1$rDXDKqpw1CLC0fwevwYEng68Bps  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PipMenuActivityController$1$rDXDKqpw1CLC0fwevwYEng68Bps implements Consumer {
    public static final /* synthetic */ $$Lambda$PipMenuActivityController$1$rDXDKqpw1CLC0fwevwYEng68Bps INSTANCE = new $$Lambda$PipMenuActivityController$1$rDXDKqpw1CLC0fwevwYEng68Bps();

    private /* synthetic */ $$Lambda$PipMenuActivityController$1$rDXDKqpw1CLC0fwevwYEng68Bps() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((PipMenuActivityController.Listener) obj).onPipDismiss();
    }
}
