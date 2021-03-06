package com.android.systemui.recents.model;

import com.android.systemui.shared.recents.model.Task;
import java.util.concurrent.ConcurrentLinkedQueue;

/* access modifiers changed from: package-private */
public class TaskResourceLoadQueue {
    private final ConcurrentLinkedQueue<Task> mQueue = new ConcurrentLinkedQueue<>();

    TaskResourceLoadQueue() {
    }

    /* access modifiers changed from: package-private */
    public void addTask(Task task) {
        if (!this.mQueue.contains(task)) {
            this.mQueue.add(task);
        }
        synchronized (this) {
            notifyAll();
        }
    }

    /* access modifiers changed from: package-private */
    public Task nextTask() {
        return this.mQueue.poll();
    }

    /* access modifiers changed from: package-private */
    public void removeTask(Task task) {
        this.mQueue.remove(task);
    }

    /* access modifiers changed from: package-private */
    public void clearTasks() {
        this.mQueue.clear();
    }

    /* access modifiers changed from: package-private */
    public boolean isEmpty() {
        return this.mQueue.isEmpty();
    }
}
