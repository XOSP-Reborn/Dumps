package com.android.systemui.recents.events.ui.dragndrop;

import android.graphics.Point;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;

public class DragStartEvent extends EventBus.Event {
    public final boolean isUserTouchInitiated;
    public final Task task;
    public final TaskView taskView;
    public final Point tlOffset;

    public DragStartEvent(Task task2, TaskView taskView2, Point point) {
        this(task2, taskView2, point, true);
    }

    public DragStartEvent(Task task2, TaskView taskView2, Point point, boolean z) {
        this.task = task2;
        this.taskView = taskView2;
        this.tlOffset = point;
        this.isUserTouchInitiated = z;
    }
}
