package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.utilities.AnimationProps;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;

public class TaskViewDismissedEvent extends EventBus.Event {
    public final AnimationProps animation;
    public final Task task;
    public final TaskView taskView;

    public TaskViewDismissedEvent(Task task2, TaskView taskView2, AnimationProps animationProps) {
        this.task = task2;
        this.taskView = taskView2;
        this.animation = animationProps;
    }
}
