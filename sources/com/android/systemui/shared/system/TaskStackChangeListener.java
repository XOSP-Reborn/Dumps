package com.android.systemui.shared.system;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.shared.recents.model.ThumbnailData;

public abstract class TaskStackChangeListener {
    public void onActivityDismissingDockedStack() {
    }

    public void onActivityForcedResizable(String str, int i, int i2) {
    }

    public void onActivityLaunchOnSecondaryDisplayFailed() {
    }

    public void onActivityLaunchOnSecondaryDisplayRerouted() {
    }

    public void onActivityPinned(String str, int i, int i2, int i3) {
    }

    public void onActivityRequestedOrientationChanged(int i, int i2) {
    }

    public void onActivityUnpinned() {
    }

    public void onBackPressedOnTaskRoot(ActivityManager.RunningTaskInfo runningTaskInfo) {
    }

    public void onPinnedActivityRestartAttempt(boolean z) {
    }

    public void onPinnedStackAnimationEnded() {
    }

    public void onPinnedStackAnimationStarted() {
    }

    public void onSizeCompatModeActivityChanged(int i, IBinder iBinder) {
    }

    public void onTaskCreated(int i, ComponentName componentName) {
    }

    public void onTaskDisplayChanged(int i, int i2) {
    }

    public void onTaskMovedToFront(int i) {
    }

    public void onTaskProfileLocked(int i, int i2) {
    }

    public void onTaskRemoved(int i) {
    }

    public void onTaskSnapshotChanged(int i, ThumbnailData thumbnailData) {
    }

    public void onTaskStackChanged() {
    }

    public void onTaskStackChangedBackground() {
    }

    public void onActivityLaunchOnSecondaryDisplayFailed(ActivityManager.RunningTaskInfo runningTaskInfo) {
        onActivityLaunchOnSecondaryDisplayFailed();
    }

    public void onActivityLaunchOnSecondaryDisplayRerouted(ActivityManager.RunningTaskInfo runningTaskInfo) {
        onActivityLaunchOnSecondaryDisplayRerouted();
    }

    public void onTaskMovedToFront(ActivityManager.RunningTaskInfo runningTaskInfo) {
        onTaskMovedToFront(runningTaskInfo.taskId);
    }

    /* access modifiers changed from: protected */
    public final boolean checkCurrentUserId(int i, boolean z) {
        int myUserId = UserHandle.myUserId();
        if (myUserId == i) {
            return true;
        }
        if (!z) {
            return false;
        }
        Log.d("TaskStackChangeListener", "UID mismatch. Process is uid=" + myUserId + " and the current user is uid=" + i);
        return false;
    }
}
