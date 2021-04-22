package com.android.systemui.recents.model;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.recents.model.BackgroundTaskLoader;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class HighResThumbnailLoader implements Task.TaskCallbacks, BackgroundTaskLoader.OnIdleChangedListener {
    private final ActivityManagerWrapper mActivityManager;
    private boolean mFlingingFast;
    private final boolean mIsLowRamDevice;
    @GuardedBy({"mLoadQueue"})
    private final ArrayDeque<Task> mLoadQueue = new ArrayDeque<>();
    private final Thread mLoadThread;
    private final Runnable mLoader = new Runnable() {
        /* class com.android.systemui.recents.model.HighResThumbnailLoader.AnonymousClass1 */

        public void run() {
            Process.setThreadPriority(11);
            while (true) {
                Task task = null;
                synchronized (HighResThumbnailLoader.this.mLoadQueue) {
                    if (HighResThumbnailLoader.this.mLoading) {
                        if (!HighResThumbnailLoader.this.mLoadQueue.isEmpty()) {
                            task = (Task) HighResThumbnailLoader.this.mLoadQueue.poll();
                            if (task != null) {
                                HighResThumbnailLoader.this.mLoadingTasks.add(task);
                            }
                        }
                    }
                    try {
                        HighResThumbnailLoader.this.mLoaderIdling = true;
                        HighResThumbnailLoader.this.mLoadQueue.wait();
                        HighResThumbnailLoader.this.mLoaderIdling = false;
                    } catch (InterruptedException unused) {
                    }
                }
                if (task != null) {
                    loadTask(task);
                }
            }
        }

        private void loadTask(final Task task) {
            final ThumbnailData taskThumbnail = HighResThumbnailLoader.this.mActivityManager.getTaskThumbnail(task.key.id, false);
            HighResThumbnailLoader.this.mMainThreadHandler.post(new Runnable() {
                /* class com.android.systemui.recents.model.HighResThumbnailLoader.AnonymousClass1.AnonymousClass1 */

                public void run() {
                    synchronized (HighResThumbnailLoader.this.mLoadQueue) {
                        HighResThumbnailLoader.this.mLoadingTasks.remove(task);
                    }
                    if (HighResThumbnailLoader.this.mVisibleTasks.contains(task)) {
                        Task task = task;
                        task.notifyTaskDataLoaded(taskThumbnail, task.icon);
                    }
                }
            });
        }
    };
    @GuardedBy({"mLoadQueue"})
    private boolean mLoaderIdling;
    private boolean mLoading;
    @GuardedBy({"mLoadQueue"})
    private final ArraySet<Task> mLoadingTasks = new ArraySet<>();
    private final Handler mMainThreadHandler;
    private boolean mTaskLoadQueueIdle;
    private boolean mVisible;
    private final ArrayList<Task> mVisibleTasks = new ArrayList<>();

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataUnloaded() {
    }

    public HighResThumbnailLoader(ActivityManagerWrapper activityManagerWrapper, Looper looper, boolean z) {
        this.mActivityManager = activityManagerWrapper;
        this.mMainThreadHandler = new Handler(looper);
        this.mLoadThread = new Thread(this.mLoader, "Recents-HighResThumbnailLoader");
        this.mLoadThread.start();
        this.mIsLowRamDevice = z;
    }

    public void setVisible(boolean z) {
        if (!this.mIsLowRamDevice) {
            this.mVisible = z;
            updateLoading();
        }
    }

    public void setFlingingFast(boolean z) {
        if (this.mFlingingFast != z && !this.mIsLowRamDevice) {
            this.mFlingingFast = z;
            updateLoading();
        }
    }

    @Override // com.android.systemui.recents.model.BackgroundTaskLoader.OnIdleChangedListener
    public void onIdleChanged(boolean z) {
        setTaskLoadQueueIdle(z);
    }

    public void setTaskLoadQueueIdle(boolean z) {
        if (!this.mIsLowRamDevice) {
            this.mTaskLoadQueueIdle = z;
            updateLoading();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean isLoading() {
        return this.mLoading;
    }

    private void updateLoading() {
        setLoading(this.mVisible && !this.mFlingingFast && this.mTaskLoadQueueIdle);
    }

    private void setLoading(boolean z) {
        if (z != this.mLoading) {
            synchronized (this.mLoadQueue) {
                this.mLoading = z;
                if (!z) {
                    stopLoading();
                } else {
                    startLoading();
                }
            }
        }
    }

    @GuardedBy({"mLoadQueue"})
    private void startLoading() {
        for (int size = this.mVisibleTasks.size() - 1; size >= 0; size--) {
            Task task = this.mVisibleTasks.get(size);
            ThumbnailData thumbnailData = task.thumbnail;
            if ((thumbnailData == null || thumbnailData.reducedResolution) && !this.mLoadQueue.contains(task) && !this.mLoadingTasks.contains(task)) {
                this.mLoadQueue.add(task);
            }
        }
        this.mLoadQueue.notifyAll();
    }

    @GuardedBy({"mLoadQueue"})
    private void stopLoading() {
        this.mLoadQueue.clear();
        this.mLoadQueue.notifyAll();
    }

    public void onTaskVisible(Task task) {
        task.addCallback(this);
        this.mVisibleTasks.add(task);
        ThumbnailData thumbnailData = task.thumbnail;
        if ((thumbnailData == null || thumbnailData.reducedResolution) && this.mLoading) {
            synchronized (this.mLoadQueue) {
                this.mLoadQueue.add(task);
                this.mLoadQueue.notifyAll();
            }
        }
    }

    public void onTaskInvisible(Task task) {
        task.removeCallback(this);
        this.mVisibleTasks.remove(task);
        synchronized (this.mLoadQueue) {
            this.mLoadQueue.remove(task);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void waitForLoaderIdle() {
        while (true) {
            synchronized (this.mLoadQueue) {
                if (this.mLoadQueue.isEmpty() && this.mLoaderIdling) {
                    return;
                }
            }
            SystemClock.sleep(100);
        }
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataLoaded(Task task, ThumbnailData thumbnailData) {
        if (thumbnailData != null && !thumbnailData.reducedResolution) {
            synchronized (this.mLoadQueue) {
                this.mLoadQueue.remove(task);
            }
        }
    }
}
