package com.android.systemui.shared.recents.model;

import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;

public abstract class TaskKeyCache<V> {
    protected final SparseArray<Task.TaskKey> mKeys = new SparseArray<>();

    /* access modifiers changed from: protected */
    public abstract void evictAllCache();

    /* access modifiers changed from: protected */
    public abstract V getCacheEntry(int i);

    /* access modifiers changed from: protected */
    public abstract void putCacheEntry(int i, V v);

    /* access modifiers changed from: protected */
    public abstract void removeCacheEntry(int i);

    public final synchronized V get(Task.TaskKey taskKey) {
        return getCacheEntry(taskKey.id);
    }

    public final synchronized V getAndInvalidateIfModified(Task.TaskKey taskKey) {
        Task.TaskKey taskKey2 = this.mKeys.get(taskKey.id);
        if (taskKey2 == null || (taskKey2.windowingMode == taskKey.windowingMode && taskKey2.lastActiveTime == taskKey.lastActiveTime)) {
            return getCacheEntry(taskKey.id);
        }
        remove(taskKey);
        return null;
    }

    public final synchronized void put(Task.TaskKey taskKey, V v) {
        if (taskKey == null || v == null) {
            Log.e("TaskKeyCache", "Unexpected null key or value: " + taskKey + ", " + ((Object) v));
            return;
        }
        this.mKeys.put(taskKey.id, taskKey);
        putCacheEntry(taskKey.id, v);
    }

    public final synchronized void remove(Task.TaskKey taskKey) {
        removeCacheEntry(taskKey.id);
        this.mKeys.remove(taskKey.id);
    }

    public final synchronized void evictAll() {
        evictAllCache();
        this.mKeys.clear();
    }
}
