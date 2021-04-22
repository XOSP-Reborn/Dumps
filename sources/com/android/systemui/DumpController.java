package com.android.systemui;

import com.android.internal.util.Preconditions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: DumpController.kt */
public final class DumpController implements Dumpable {
    public static final Companion Companion = new Companion(null);
    private final List<WeakReference<Dumpable>> listeners = new ArrayList();

    /* compiled from: DumpController.kt */
    public static final class Companion {
        private Companion() {
        }

        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public final void addListener(Dumpable dumpable) {
        Intrinsics.checkParameterIsNotNull(dumpable, "listener");
        boolean z = false;
        Preconditions.checkNotNull(dumpable, "The listener to be added cannot be null", new Object[0]);
        synchronized (this.listeners) {
            List<WeakReference<Dumpable>> list = this.listeners;
            if (!(list instanceof Collection) || !list.isEmpty()) {
                Iterator<T> it = list.iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (Intrinsics.areEqual((Dumpable) it.next().get(), dumpable)) {
                            z = true;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (!z) {
                this.listeners.add(new WeakReference<>(dumpable));
            }
            Unit unit = Unit.INSTANCE;
        }
    }

    public final void removeListener(Dumpable dumpable) {
        Intrinsics.checkParameterIsNotNull(dumpable, "listener");
        synchronized (this.listeners) {
            CollectionsKt.removeAll(this.listeners, new DumpController$removeListener$$inlined$synchronized$lambda$1(this, dumpable));
        }
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Intrinsics.checkParameterIsNotNull(printWriter, "pw");
        printWriter.println("DumpController state:");
        synchronized (this.listeners) {
            Iterator<T> it = this.listeners.iterator();
            while (it.hasNext()) {
                Dumpable dumpable = (Dumpable) it.next().get();
                if (dumpable != null) {
                    dumpable.dump(fileDescriptor, printWriter, strArr);
                }
            }
            Unit unit = Unit.INSTANCE;
        }
    }
}
