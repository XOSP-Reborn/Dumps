package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.shared.recents.model.Task;

public class ShowApplicationInfoEvent extends EventBus.Event {
    public final Task task;

    public ShowApplicationInfoEvent(Task task2) {
        this.task = task2;
    }
}
