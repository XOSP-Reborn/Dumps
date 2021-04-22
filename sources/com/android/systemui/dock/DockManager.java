package com.android.systemui.dock;

public interface DockManager {

    public interface DockEventListener {
    }

    void addListener(DockEventListener dockEventListener);

    boolean isDocked();

    void removeListener(DockEventListener dockEventListener);
}
