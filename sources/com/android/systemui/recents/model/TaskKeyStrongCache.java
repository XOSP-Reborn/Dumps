package com.android.systemui.recents.model;

import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.TaskKeyCache;
import java.io.PrintWriter;

public class TaskKeyStrongCache<V> extends TaskKeyCache<V> {
    private final ArrayMap<Integer, V> mCache = new ArrayMap<>();

    public final void copyEntries(TaskKeyStrongCache<V> taskKeyStrongCache) {
        for (int size = taskKeyStrongCache.mKeys.size() - 1; size >= 0; size--) {
            Task.TaskKey valueAt = taskKeyStrongCache.mKeys.valueAt(size);
            put(valueAt, taskKeyStrongCache.mCache.get(Integer.valueOf(valueAt.id)));
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.print("TaskKeyCache");
        printWriter.print(" numEntries=");
        printWriter.print(this.mKeys.size());
        printWriter.println();
        int size = this.mKeys.size();
        for (int i = 0; i < size; i++) {
            printWriter.print(str2);
            SparseArray<Task.TaskKey> sparseArray = this.mKeys;
            printWriter.println(sparseArray.get(sparseArray.keyAt(i)));
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    public V getCacheEntry(int i) {
        return this.mCache.get(Integer.valueOf(i));
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    public void putCacheEntry(int i, V v) {
        this.mCache.put(Integer.valueOf(i), v);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    public void removeCacheEntry(int i) {
        this.mCache.remove(Integer.valueOf(i));
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    public void evictAllCache() {
        this.mCache.clear();
    }
}
