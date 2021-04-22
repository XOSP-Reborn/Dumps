package kotlin.sequences;

import java.util.Iterator;
import kotlin.collections.EmptyIterator;

/* compiled from: Sequences.kt */
final class EmptySequence implements Sequence, DropTakeSequence {
    public static final EmptySequence INSTANCE = new EmptySequence();

    private EmptySequence() {
    }

    @Override // kotlin.sequences.Sequence
    public Iterator iterator() {
        return EmptyIterator.INSTANCE;
    }

    @Override // kotlin.sequences.DropTakeSequence
    public EmptySequence take(int i) {
        return INSTANCE;
    }
}
