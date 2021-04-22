package com.android.systemui.recents.views;

import android.app.ActivityTaskManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.ui.HideIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartInitializeDropTargetsEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shared.recents.model.Task;
import java.util.ArrayList;

public class RecentsViewTouchHandler {
    private int mDeviceId = -1;
    private DividerSnapAlgorithm mDividerSnapAlgorithm;
    @ViewDebug.ExportedProperty(category = "recents")
    private Point mDownPos = new Point();
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mDragRequested;
    private float mDragSlop;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "drag_task")
    private Task mDragTask;
    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mIsDragging;
    private DropTarget mLastDropTarget;
    private RecentsView mRv;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "drag_task_view_")
    private TaskView mTaskView;
    @ViewDebug.ExportedProperty(category = "recents")
    private Point mTaskViewOffset = new Point();
    private ArrayList<DockState> mVisibleDockStates = new ArrayList<>();

    public RecentsViewTouchHandler(RecentsView recentsView) {
        this.mRv = recentsView;
        this.mDragSlop = (float) ViewConfiguration.get(recentsView.getContext()).getScaledTouchSlop();
        updateSnapAlgorithm();
    }

    private void updateSnapAlgorithm() {
        Rect rect = new Rect();
        SystemServicesProxy.getInstance(this.mRv.getContext()).getStableInsets(rect);
        this.mDividerSnapAlgorithm = DividerSnapAlgorithm.create(this.mRv.getContext(), rect);
    }

    public void registerDropTargetForCurrentDrag(DropTarget dropTarget) {
        this.mDropTargets.add(dropTarget);
    }

    public ArrayList<DockState> getVisibleDockStates() {
        return this.mVisibleDockStates;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return handleTouchEvent(motionEvent) || this.mDragRequested;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        handleTouchEvent(motionEvent);
        if (motionEvent.getAction() == 1 && this.mRv.getStack().getTaskCount() == 0) {
            EventBus.getDefault().send(new HideRecentsEvent(false, true));
        }
        return true;
    }

    public final void onBusEvent(DragStartEvent dragStartEvent) {
        InputDevice device;
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        this.mRv.getParent().requestDisallowInterceptTouchEvent(true);
        this.mDragRequested = true;
        this.mIsDragging = false;
        this.mDragTask = dragStartEvent.task;
        this.mTaskView = dragStartEvent.taskView;
        this.mDropTargets.clear();
        int[] iArr = new int[2];
        this.mRv.getLocationInWindow(iArr);
        this.mTaskViewOffset.set((this.mTaskView.getLeft() - iArr[0]) + dragStartEvent.tlOffset.x, (this.mTaskView.getTop() - iArr[1]) + dragStartEvent.tlOffset.y);
        if (dragStartEvent.isUserTouchInitiated) {
            Point point = this.mDownPos;
            int i = point.x;
            Point point2 = this.mTaskViewOffset;
            this.mTaskView.setTranslationX((float) (i - point2.x));
            this.mTaskView.setTranslationY((float) (point.y - point2.y));
        }
        this.mVisibleDockStates.clear();
        if (ActivityTaskManager.supportsMultiWindow(this.mRv.getContext()) && !systemServices.hasDockedTask() && this.mDividerSnapAlgorithm.isSplitScreenFeasible()) {
            LegacyRecentsImpl.logDockAttempt(this.mRv.getContext(), dragStartEvent.task.getTopComponent(), dragStartEvent.task.resizeMode);
            if (!dragStartEvent.task.isDockable) {
                EventBus.getDefault().send(new ShowIncompatibleAppOverlayEvent());
            } else {
                DockState[] dockStatesForCurrentOrientation = LegacyRecentsImpl.getConfiguration().getDockStatesForCurrentOrientation();
                for (DockState dockState : dockStatesForCurrentOrientation) {
                    registerDropTargetForCurrentDrag(dockState);
                    dockState.update(this.mRv.getContext());
                    this.mVisibleDockStates.add(dockState);
                }
            }
        }
        EventBus.getDefault().send(new DragStartInitializeDropTargetsEvent(dragStartEvent.task, dragStartEvent.taskView, this));
        int i2 = this.mDeviceId;
        if (!(i2 == -1 || (device = InputDevice.getDevice(i2)) == null)) {
            device.setPointerType(1021);
        }
    }

    public final void onBusEvent(DragEndEvent dragEndEvent) {
        if (!this.mDragTask.isDockable) {
            EventBus.getDefault().send(new HideIncompatibleAppOverlayEvent());
        }
        this.mDragRequested = false;
        this.mDragTask = null;
        this.mTaskView = null;
        this.mLastDropTarget = null;
    }

    public final void onBusEvent(ConfigurationChangedEvent configurationChangedEvent) {
        if (configurationChangedEvent.fromDisplayDensityChange || configurationChangedEvent.fromDeviceOrientationChange) {
            updateSnapAlgorithm();
        }
    }

    /* access modifiers changed from: package-private */
    public void cancelStackActionButtonClick() {
        this.mRv.getStackActionButton().setPressed(false);
    }

    private boolean isWithinStackActionButton(float f, float f2) {
        Rect stackActionButtonBoundsFromStackLayout = this.mRv.getStackActionButtonBoundsFromStackLayout();
        return this.mRv.getStackActionButton().getVisibility() == 0 && this.mRv.getStackActionButton().pointInView(f - ((float) stackActionButtonBoundsFromStackLayout.left), f2 - ((float) stackActionButtonBoundsFromStackLayout.top), 0.0f);
    }

    private void changeStackActionButtonDrawableHotspot(float f, float f2) {
        Rect stackActionButtonBoundsFromStackLayout = this.mRv.getStackActionButtonBoundsFromStackLayout();
        this.mRv.getStackActionButton().drawableHotspotChanged(f - ((float) stackActionButtonBoundsFromStackLayout.left), f2 - ((float) stackActionButtonBoundsFromStackLayout.top));
    }

    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0019, code lost:
        if (r1 != 3) goto L_0x0144;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean handleTouchEvent(android.view.MotionEvent r19) {
        /*
        // Method dump skipped, instructions count: 326
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.views.RecentsViewTouchHandler.handleTouchEvent(android.view.MotionEvent):boolean");
    }
}
