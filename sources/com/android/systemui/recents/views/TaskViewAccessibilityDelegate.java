package com.android.systemui.recents.views;

import android.app.ActivityTaskManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;

public class TaskViewAccessibilityDelegate extends View.AccessibilityDelegate {
    protected final SparseArray<AccessibilityNodeInfo.AccessibilityAction> mActions = new SparseArray<>();
    private final TaskView mTaskView;

    public TaskViewAccessibilityDelegate(TaskView taskView) {
        this.mTaskView = taskView;
        Context context = taskView.getContext();
        this.mActions.put(2131361878, new AccessibilityNodeInfo.AccessibilityAction(2131361878, context.getString(2131821940)));
        this.mActions.put(2131361876, new AccessibilityNodeInfo.AccessibilityAction(2131361876, context.getString(2131821938)));
        this.mActions.put(2131361877, new AccessibilityNodeInfo.AccessibilityAction(2131361877, context.getString(2131821939)));
    }

    public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
        if (ActivityTaskManager.supportsSplitScreenMultiWindow(this.mTaskView.getContext()) && !LegacyRecentsImpl.getSystemServices().hasDockedTask()) {
            DockState[] dockStatesForCurrentOrientation = LegacyRecentsImpl.getConfiguration().getDockStatesForCurrentOrientation();
            for (DockState dockState : dockStatesForCurrentOrientation) {
                if (dockState == DockState.TOP) {
                    accessibilityNodeInfo.addAction(this.mActions.get(2131361878));
                } else if (dockState == DockState.LEFT) {
                    accessibilityNodeInfo.addAction(this.mActions.get(2131361876));
                } else if (dockState == DockState.RIGHT) {
                    accessibilityNodeInfo.addAction(this.mActions.get(2131361877));
                }
            }
        }
    }

    public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
        if (i == 2131361878) {
            simulateDragIntoMultiwindow(DockState.TOP);
            return true;
        } else if (i == 2131361876) {
            simulateDragIntoMultiwindow(DockState.LEFT);
            return true;
        } else if (i != 2131361877) {
            return super.performAccessibilityAction(view, i, bundle);
        } else {
            simulateDragIntoMultiwindow(DockState.RIGHT);
            return true;
        }
    }

    private void simulateDragIntoMultiwindow(DockState dockState) {
        EventBus.getDefault().send(new DragStartEvent(this.mTaskView.getTask(), this.mTaskView, new Point(0, 0), false));
        EventBus.getDefault().send(new DragEndEvent(this.mTaskView.getTask(), this.mTaskView, dockState));
    }
}
