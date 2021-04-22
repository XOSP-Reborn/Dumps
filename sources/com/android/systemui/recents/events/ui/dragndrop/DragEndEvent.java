package com.android.systemui.recents.events.ui.dragndrop;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.DropTarget;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;

public class DragEndEvent extends EventBus.AnimatedEvent {
    public final DropTarget dropTarget;
    public final Task task;
    public final TaskView taskView;

    public DragEndEvent(Task task2, TaskView taskView2, DropTarget dropTarget2) {
        this.task = task2;
        this.taskView = taskView2;
        this.dropTarget = dropTarget2;
    }
}
