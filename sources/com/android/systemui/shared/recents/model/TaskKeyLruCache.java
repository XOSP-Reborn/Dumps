package com.android.systemui.shared.recents.model;

import android.util.LruCache;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;
import java.io.PrintWriter;

public class TaskKeyLruCache<V> extends TaskKeyCache<V> {
    private final LruCache<Integer, V> mCache;
    private final EvictionCallback mEvictionCallback;

    public interface EvictionCallback {
        void onEntryEvicted(Task.TaskKey taskKey);
    }

    public TaskKeyLruCache(int i, EvictionCallback evictionCallback) {
        this.mEvictionCallback = evictionCallback;
        this.mCache = new LruCache<Integer, V>(i) {
            /* class com.android.systemui.shared.recents.model.TaskKeyLruCache.AnonymousClass1 */

            /* access modifiers changed from: protected */
            public void entryRemoved(boolean z, Integer num, V v, V v2) {
                if (TaskKeyLruCache.this.mEvictionCallback != null) {
                    TaskKeyLruCache.this.mEvictionCallback.onEntryEvicted(TaskKeyLruCache.this.mKeys.get(num.intValue()));
                }
                TaskKeyLruCache.this.mKeys.remove(num.intValue());
            }
        };
    }

    public final void trimToSize(int i) {
        this.mCache.trimToSize(i);
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
        this.mCache.evictAll();
    }
}
