package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.shared.recents.model.Task;

public class DeleteTaskDataEvent extends EventBus.Event {
    public final Task task;

    public DeleteTaskDataEvent(Task task2) {
        this.task = task2;
    }
}
