package kotlin.sequences;

import java.util.Iterator;
import java.util.NoSuchElementException;
import kotlin.jvm.internal.markers.KMappedMarker;

/* compiled from: Sequences.kt */
public final class TakeSequence$iterator$1 implements Iterator<T>, KMappedMarker {
    private final Iterator<T> iterator;
    private int left;
    final /* synthetic */ TakeSequence this$0;

    public void remove() {
        throw new UnsupportedOperationException("Operation is not supported for read-only collection");
    }

    /* JADX WARN: Incorrect args count in method signature: ()V */
    TakeSequence$iterator$1(TakeSequence takeSequence) {
        this.this$0 = takeSequence;
        this.left = takeSequence.count;
        this.iterator = takeSequence.sequence.iterator();
    }

    @Override // java.util.Iterator
    public T next() {
        int i = this.left;
        if (i != 0) {
            this.left = i - 1;
            return this.iterator.next();
        }
        throw new NoSuchElementException();
    }

    public boolean hasNext() {
        return this.left > 0 && this.iterator.hasNext();
    }
}
