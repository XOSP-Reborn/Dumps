package com.android.systemui.recents.events.ui.dragndrop;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;

public class DragEndCancelledEvent extends EventBus.AnimatedEvent {
    public final TaskStack stack;
    public final Task task;
    public final TaskView taskView;

    public DragEndCancelledEvent(TaskStack taskStack, Task task2, TaskView taskView2) {
        this.stack = taskStack;
        this.task = task2;
        this.taskView = taskView2;
    }
}
