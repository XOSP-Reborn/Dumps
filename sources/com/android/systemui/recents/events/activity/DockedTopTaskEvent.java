package com.android.systemui.recents.events.activity;

import android.graphics.Rect;
import com.android.systemui.recents.events.EventBus;

public class DockedTopTaskEvent extends EventBus.Event {
    public Rect initialRect;

    public DockedTopTaskEvent(Rect rect) {
        this.initialRect = rect;
    }
}
