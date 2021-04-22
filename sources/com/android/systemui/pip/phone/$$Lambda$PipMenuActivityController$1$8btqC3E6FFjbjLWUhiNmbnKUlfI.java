package com.android.systemui.pip.phone;

import com.android.systemui.pip.phone.PipMenuActivityController;
import java.util.function.Consumer;

/* renamed from: com.android.systemui.pip.phone.-$$Lambda$PipMenuActivityController$1$8btqC3E6FFjbjLWUhiNmbnKUlfI  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PipMenuActivityController$1$8btqC3E6FFjbjLWUhiNmbnKUlfI implements Consumer {
    public static final /* synthetic */ $$Lambda$PipMenuActivityController$1$8btqC3E6FFjbjLWUhiNmbnKUlfI INSTANCE = new $$Lambda$PipMenuActivityController$1$8btqC3E6FFjbjLWUhiNmbnKUlfI();

    private /* synthetic */ $$Lambda$PipMenuActivityController$1$8btqC3E6FFjbjLWUhiNmbnKUlfI() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((PipMenuActivityController.Listener) obj).onPipExpand();
    }
}
