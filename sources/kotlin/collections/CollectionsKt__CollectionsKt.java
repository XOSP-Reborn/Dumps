package kotlin.collections;

import java.util.List;
import kotlin.jvm.internal.Intrinsics;

/* access modifiers changed from: package-private */
/* compiled from: Collections.kt */
public class CollectionsKt__CollectionsKt extends CollectionsKt__CollectionsJVMKt {
    public static <T> List<T> emptyList() {
        return EmptyList.INSTANCE;
    }

    public static final <T> int getLastIndex(List<? extends T> list) {
        Intrinsics.checkParameterIsNotNull(list, "receiver$0");
        return list.size() - 1;
    }
}
