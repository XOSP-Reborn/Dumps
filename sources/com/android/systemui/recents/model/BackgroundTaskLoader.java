package com.android.systemui.recents.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.systemui.shared.recents.model.IconLoader;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.system.ActivityManagerWrapper;

/* access modifiers changed from: package-private */
public class BackgroundTaskLoader implements Runnable {
    static boolean DEBUG = false;
    static String TAG = "BackgroundTaskLoader";
    private boolean mCancelled;
    private Context mContext;
    private final IconLoader mIconLoader;
    private final TaskResourceLoadQueue mLoadQueue;
    private final HandlerThread mLoadThread;
    private final Handler mLoadThreadHandler;
    private final Handler mMainThreadHandler = new Handler();
    private final OnIdleChangedListener mOnIdleChangedListener;
    private boolean mStarted;
    private boolean mWaitingOnLoadQueue;

    /* access modifiers changed from: package-private */
    public interface OnIdleChangedListener {
        void onIdleChanged(boolean z);
    }

    public BackgroundTaskLoader(TaskResourceLoadQueue taskResourceLoadQueue, IconLoader iconLoader, OnIdleChangedListener onIdleChangedListener) {
        this.mLoadQueue = taskResourceLoadQueue;
        this.mIconLoader = iconLoader;
        this.mOnIdleChangedListener = onIdleChangedListener;
        this.mLoadThread = new HandlerThread("Recents-TaskResourceLoader", 10);
        this.mLoadThread.start();
        this.mLoadThreadHandler = new Handler(this.mLoadThread.getLooper());
    }

    /* access modifiers changed from: package-private */
    public void start(Context context) {
        this.mContext = context;
        this.mCancelled = false;
        if (!this.mStarted) {
            this.mStarted = true;
            this.mLoadThreadHandler.post(this);
            return;
        }
        synchronized (this.mLoadThread) {
            this.mLoadThread.notifyAll();
        }
    }

    /* access modifiers changed from: package-private */
    public void stop() {
        this.mCancelled = true;
        if (this.mWaitingOnLoadQueue) {
            this.mContext = null;
        }
    }

    public void run() {
        while (true) {
            if (this.mCancelled) {
                this.mContext = null;
                synchronized (this.mLoadThread) {
                    try {
                        this.mLoadThread.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                processLoadQueueItem();
                if (!this.mCancelled && this.mLoadQueue.isEmpty()) {
                    synchronized (this.mLoadQueue) {
                        try {
                            this.mWaitingOnLoadQueue = true;
                            this.mMainThreadHandler.post(new Runnable() {
                                /* class com.android.systemui.recents.model.BackgroundTaskLoader.AnonymousClass1 */

                                public void run() {
                                    BackgroundTaskLoader.this.mOnIdleChangedListener.onIdleChanged(true);
                                }
                            });
                            this.mLoadQueue.wait();
                            this.mMainThreadHandler.post(new Runnable() {
                                /* class com.android.systemui.recents.model.BackgroundTaskLoader.AnonymousClass2 */

                                public void run() {
                                    BackgroundTaskLoader.this.mOnIdleChangedListener.onIdleChanged(false);
                                }
                            });
                            this.mWaitingOnLoadQueue = false;
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void processLoadQueueItem() {
        final Task nextTask = this.mLoadQueue.nextTask();
        if (nextTask != null) {
            final Drawable icon = this.mIconLoader.getIcon(nextTask);
            if (DEBUG) {
                String str = TAG;
                Log.d(str, "Loading thumbnail: " + nextTask.key);
            }
            final ThumbnailData taskThumbnail = ActivityManagerWrapper.getInstance().getTaskThumbnail(nextTask.key.id, true);
            if (!this.mCancelled) {
                this.mMainThreadHandler.post(new Runnable() {
                    /* class com.android.systemui.recents.model.BackgroundTaskLoader.AnonymousClass3 */

                    public void run() {
                        nextTask.notifyTaskDataLoaded(taskThumbnail, icon);
                    }
                });
            }
        }
    }
}
