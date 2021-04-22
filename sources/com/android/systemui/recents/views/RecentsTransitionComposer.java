package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.RecentsTransition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentsTransitionComposer {
    private Context mContext;
    private TaskViewTransform mTmpTransform = new TaskViewTransform();

    public RecentsTransitionComposer(Context context) {
        this.mContext = context;
    }

    private static AppTransitionAnimationSpecCompat composeAnimationSpec(TaskStackView taskStackView, TaskView taskView, TaskViewTransform taskViewTransform, boolean z) {
        Bitmap bitmap;
        if (z) {
            bitmap = composeHeaderBitmap(taskView, taskViewTransform);
            if (bitmap == null) {
                return null;
            }
        } else {
            bitmap = null;
        }
        Rect rect = new Rect();
        taskViewTransform.rect.round(rect);
        if (!LegacyRecentsImpl.getConfiguration().isLowRamDevice && taskView.getTask() != taskStackView.getStack().getFrontMostTask()) {
            rect.bottom = rect.top + taskStackView.getMeasuredHeight();
        }
        return new AppTransitionAnimationSpecCompat(taskView.getTask().key.id, bitmap, rect);
    }

    public List<AppTransitionAnimationSpecCompat> composeDockAnimationSpec(TaskView taskView, Rect rect) {
        this.mTmpTransform.fillIn(taskView);
        Task task = taskView.getTask();
        return Collections.singletonList(new AppTransitionAnimationSpecCompat(task.key.id, composeTaskBitmap(taskView, this.mTmpTransform), rect));
    }

    public List<AppTransitionAnimationSpecCompat> composeAnimationSpecs(Task task, TaskStackView taskStackView, int i, int i2, Rect rect) {
        TaskView childViewForTask = taskStackView.getChildViewForTask(task);
        TaskStackLayoutAlgorithm stackAlgorithm = taskStackView.getStackAlgorithm();
        Rect rect2 = new Rect();
        stackAlgorithm.getFrontOfStackTransform().rect.round(rect2);
        if (i != 1 && i != 3 && i != 4 && i2 != 4 && i != 0) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        if (childViewForTask == null) {
            arrayList.add(composeOffscreenAnimationSpec(task, rect2));
        } else {
            this.mTmpTransform.fillIn(childViewForTask);
            stackAlgorithm.transformToScreenCoordinates(this.mTmpTransform, rect);
            AppTransitionAnimationSpecCompat composeAnimationSpec = composeAnimationSpec(taskStackView, childViewForTask, this.mTmpTransform, true);
            if (composeAnimationSpec != null) {
                arrayList.add(composeAnimationSpec);
            }
        }
        return arrayList;
    }

    private static AppTransitionAnimationSpecCompat composeOffscreenAnimationSpec(Task task, Rect rect) {
        return new AppTransitionAnimationSpecCompat(task.key.id, null, rect);
    }

    public static Bitmap composeTaskBitmap(TaskView taskView, TaskViewTransform taskViewTransform) {
        float f = taskViewTransform.scale;
        int width = (int) (taskViewTransform.rect.width() * f);
        int height = (int) (taskViewTransform.rect.height() * f);
        if (width != 0 && height != 0) {
            return RecentsTransition.drawViewIntoHardwareBitmap(width, height, taskView, f, 0);
        }
        Log.e("RecentsTransitionComposer", "Could not compose thumbnail for task: " + taskView.getTask() + " at transform: " + taskViewTransform);
        return RecentsTransition.drawViewIntoHardwareBitmap(1, 1, null, 1.0f, 16777215);
    }

    private static Bitmap composeHeaderBitmap(TaskView taskView, TaskViewTransform taskViewTransform) {
        float f = taskViewTransform.scale;
        int width = (int) taskViewTransform.rect.width();
        int measuredHeight = (int) (((float) taskView.mHeaderView.getMeasuredHeight()) * f);
        if (width == 0 || measuredHeight == 0) {
            return null;
        }
        return RecentsTransition.drawViewIntoHardwareBitmap(width, measuredHeight, taskView.mHeaderView, f, 0);
    }
}
