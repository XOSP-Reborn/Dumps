package kotlin.collections;

import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.Sequence;

/* access modifiers changed from: package-private */
/* compiled from: _Collections.kt */
public class CollectionsKt___CollectionsKt extends CollectionsKt___CollectionsJvmKt {
    public static <T> Sequence<T> asSequence(Iterable<? extends T> iterable) {
        Intrinsics.checkParameterIsNotNull(iterable, "receiver$0");
        return new CollectionsKt___CollectionsKt$asSequence$$inlined$Sequence$1(iterable);
    }
}
