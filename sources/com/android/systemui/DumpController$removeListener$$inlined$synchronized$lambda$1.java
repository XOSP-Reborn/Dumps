package com.android.systemui;

import java.lang.ref.WeakReference;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;

/* access modifiers changed from: package-private */
/* compiled from: DumpController.kt */
public final class DumpController$removeListener$$inlined$synchronized$lambda$1 extends Lambda implements Function1<WeakReference<Dumpable>, Boolean> {
    final /* synthetic */ Dumpable $listener$inlined;
    final /* synthetic */ DumpController this$0;

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    DumpController$removeListener$$inlined$synchronized$lambda$1(DumpController dumpController, Dumpable dumpable) {
        super(1);
        this.this$0 = dumpController;
        this.$listener$inlined = dumpable;
    }

    /* Return type fixed from 'java.lang.Object' to match base method */
    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.lang.Object] */
    @Override // kotlin.jvm.functions.Function1
    public /* bridge */ /* synthetic */ Boolean invoke(WeakReference<Dumpable> weakReference) {
        return Boolean.valueOf(invoke(weakReference));
    }

    public final boolean invoke(WeakReference<Dumpable> weakReference) {
        Intrinsics.checkParameterIsNotNull(weakReference, "it");
        return Intrinsics.areEqual(weakReference.get(), this.$listener$inlined) || weakReference.get() == null;
    }
}
