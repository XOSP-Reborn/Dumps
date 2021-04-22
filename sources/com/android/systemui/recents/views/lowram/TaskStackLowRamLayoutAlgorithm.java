package com.android.systemui.recents.views.lowram;

import android.content.Context;
import android.graphics.Rect;
import android.view.ViewConfiguration;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.shared.recents.model.Task;
import java.util.ArrayList;

public class TaskStackLowRamLayoutAlgorithm {
    private int mFlingThreshold;
    private int mPadding;
    private int mPaddingEndTopBottom;
    private int mPaddingLeftRight;
    private Rect mSystemInsets = new Rect();
    private Rect mTaskRect = new Rect();
    private int mTopOffset;
    private Rect mWindowRect;

    public float getMaxOverscroll() {
        return 0.6666666f;
    }

    public TaskStackLowRamLayoutAlgorithm(Context context) {
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        this.mPadding = context.getResources().getDimensionPixelSize(2131166235);
        this.mFlingThreshold = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    }

    public void initialize(Rect rect) {
        this.mWindowRect = rect;
        if (this.mWindowRect.height() > 0) {
            int height = this.mWindowRect.height() - this.mSystemInsets.bottom;
            int width = this.mWindowRect.width();
            Rect rect2 = this.mSystemInsets;
            int i = (width - rect2.right) - rect2.left;
            int min = Math.min(i, height) - (this.mPadding * 2);
            this.mTaskRect.set(0, 0, min, i > height ? (min * 2) / 3 : min);
            this.mPaddingLeftRight = (i - this.mTaskRect.width()) / 2;
            this.mPaddingEndTopBottom = (height - this.mTaskRect.height()) / 2;
            this.mTopOffset = (getTotalHeightOfTasks(9) - height) / 2;
        }
    }

    public void setSystemInsets(Rect rect) {
        this.mSystemInsets = rect;
    }

    public TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport(ArrayList<Task> arrayList) {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        int min = Math.min((launchState.launchedFromHome || launchState.launchedFromPipApp || launchState.launchedWithNextPipApp) ? 2 : 3, arrayList.size());
        return new TaskStackLayoutAlgorithm.VisibilityReport(min, min);
    }

    public void getFrontOfStackTransform(TaskViewTransform taskViewTransform, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        Rect rect = this.mWindowRect;
        if (rect == null) {
            taskViewTransform.reset();
        } else {
            fillStackTransform(taskViewTransform, (((rect.height() - this.mSystemInsets.bottom) + this.mTaskRect.height()) / 2) + this.mTaskRect.height() + (this.mPadding * 2), taskStackLayoutAlgorithm.mMaxTranslationZ, true);
        }
    }

    public void getBackOfStackTransform(TaskViewTransform taskViewTransform, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        Rect rect = this.mWindowRect;
        if (rect == null) {
            taskViewTransform.reset();
        } else {
            fillStackTransform(taskViewTransform, (((rect.height() - this.mSystemInsets.bottom) - this.mTaskRect.height()) / 2) - ((this.mTaskRect.height() + this.mPadding) * 2), taskStackLayoutAlgorithm.mMaxTranslationZ, true);
        }
    }

    public TaskViewTransform getTransform(int i, float f, TaskViewTransform taskViewTransform, int i2, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        int i3;
        if (i2 == 0) {
            taskViewTransform.reset();
            return taskViewTransform;
        }
        boolean z = true;
        if (i2 > 1) {
            i3 = getTaskTopFromIndex(i) - percentageToScroll(f);
            if (this.mPadding + i3 + getTaskRect().height() <= 0) {
                z = false;
            }
        } else {
            i3 = (((this.mWindowRect.height() - this.mSystemInsets.bottom) - this.mTaskRect.height()) / 2) - percentageToScroll(f);
        }
        fillStackTransform(taskViewTransform, i3, taskStackLayoutAlgorithm.mMaxTranslationZ, z);
        return taskViewTransform;
    }

    public float getClosestTaskP(float f, int i, int i2) {
        int percentageToScroll = percentageToScroll(f);
        int taskTopFromIndex = getTaskTopFromIndex(0) - this.mPaddingEndTopBottom;
        int i3 = 1;
        while (i3 < i) {
            int taskTopFromIndex2 = getTaskTopFromIndex(i3) - this.mPaddingEndTopBottom;
            int i4 = taskTopFromIndex2 - percentageToScroll;
            if (i4 > 0) {
                boolean z = i4 > Math.abs(percentageToScroll - taskTopFromIndex);
                if (Math.abs(i2) > this.mFlingThreshold) {
                    z = i2 > 0;
                }
                return z ? scrollToPercentage(taskTopFromIndex) : scrollToPercentage(taskTopFromIndex2);
            }
            i3++;
            taskTopFromIndex = taskTopFromIndex2;
        }
        return scrollToPercentage(taskTopFromIndex);
    }

    public float scrollToPercentage(int i) {
        return ((float) i) / ((float) (this.mTaskRect.height() + this.mPadding));
    }

    public int percentageToScroll(float f) {
        return (int) (f * ((float) (this.mTaskRect.height() + this.mPadding)));
    }

    public float getMinScrollP() {
        return getScrollPForTask(0);
    }

    public float getMaxScrollP(int i) {
        return getScrollPForTask(i - 1);
    }

    public float getInitialScrollP(int i, boolean z) {
        if (z) {
            return getMaxScrollP(i);
        }
        if (i < 2) {
            return 0.0f;
        }
        return getScrollPForTask(i - 2);
    }

    public float getScrollPForTask(int i) {
        return scrollToPercentage(getTaskTopFromIndex(i) - this.mPaddingEndTopBottom);
    }

    public Rect getTaskRect() {
        return this.mTaskRect;
    }

    private int getTaskTopFromIndex(int i) {
        return getTotalHeightOfTasks(i) - this.mTopOffset;
    }

    private int getTotalHeightOfTasks(int i) {
        return (this.mTaskRect.height() * i) + ((i + 1) * this.mPadding);
    }

    private void fillStackTransform(TaskViewTransform taskViewTransform, int i, int i2, boolean z) {
        taskViewTransform.scale = 1.0f;
        taskViewTransform.alpha = 1.0f;
        taskViewTransform.translationZ = (float) i2;
        taskViewTransform.dimAlpha = 0.0f;
        taskViewTransform.viewOutlineAlpha = 1.0f;
        taskViewTransform.rect.set(getTaskRect());
        taskViewTransform.rect.offset((float) (this.mPaddingLeftRight + this.mSystemInsets.left), (float) i);
        Utilities.scaleRectAboutCenter(taskViewTransform.rect, taskViewTransform.scale);
        taskViewTransform.visible = z;
    }
}
