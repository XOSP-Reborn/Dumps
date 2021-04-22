package com.android.systemui.recents.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.ViewDebug;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.FreePathInterpolator;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm;
import com.android.systemui.recents.views.lowram.TaskStackLowRamLayoutAlgorithm;
import com.android.systemui.shared.recents.model.Task;
import java.io.PrintWriter;
import java.util.ArrayList;

public class TaskStackLayoutAlgorithm {
    TaskViewTransform mBackOfStackTransform = new TaskViewTransform();
    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseBottomMargin;
    private int mBaseInitialBottomOffset;
    private int mBaseInitialTopOffset;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseSideMargin;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseTopMargin;
    private TaskStackLayoutAlgorithmCallbacks mCb;
    Context mContext;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusState;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusedBottomPeekHeight;
    private Path mFocusedCurve;
    private FreePathInterpolator mFocusedCurveInterpolator;
    private Path mFocusedDimCurve;
    private FreePathInterpolator mFocusedDimCurveInterpolator;
    private Range mFocusedRange;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusedTopPeekHeight;
    @ViewDebug.ExportedProperty(category = "recents")
    float mFrontMostTaskP;
    TaskViewTransform mFrontOfStackTransform = new TaskViewTransform();
    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialBottomOffset;
    @ViewDebug.ExportedProperty(category = "recents")
    float mInitialScrollP;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialTopOffset;
    @ViewDebug.ExportedProperty(category = "recents")
    float mMaxScrollP;
    @ViewDebug.ExportedProperty(category = "recents")
    public int mMaxTranslationZ;
    private int mMinMargin;
    @ViewDebug.ExportedProperty(category = "recents")
    float mMinScrollP;
    @ViewDebug.ExportedProperty(category = "recents")
    int mMinTranslationZ;
    @ViewDebug.ExportedProperty(category = "recents")
    int mNumStackTasks;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStackActionButtonRect = new Rect();
    @ViewDebug.ExportedProperty(category = "recents")
    private int mStackBottomOffset;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mStackRect = new Rect();
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mSystemInsets = new Rect();
    TaskGridLayoutAlgorithm mTaskGridLayoutAlgorithm;
    private SparseIntArray mTaskIndexMap = new SparseIntArray();
    private SparseArray<Float> mTaskIndexOverrideMap = new SparseArray<>();
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mTaskRect = new Rect();
    TaskStackLowRamLayoutAlgorithm mTaskStackLowRamLayoutAlgorithm;
    private int mTitleBarHeight;
    private Path mUnfocusedCurve;
    private FreePathInterpolator mUnfocusedCurveInterpolator;
    private Path mUnfocusedDimCurve;
    private FreePathInterpolator mUnfocusedDimCurveInterpolator;
    private Range mUnfocusedRange;

    public interface TaskStackLayoutAlgorithmCallbacks {
        void onFocusStateChanged(int i, int i2);
    }

    /* access modifiers changed from: package-private */
    public boolean useGridLayout() {
        return LegacyRecentsImpl.getConfiguration().isGridEnabled;
    }

    public static class VisibilityReport {
        public int numVisibleTasks;
        public int numVisibleThumbnails;

        public VisibilityReport(int i, int i2) {
            this.numVisibleTasks = i;
            this.numVisibleThumbnails = i2;
        }
    }

    public TaskStackLayoutAlgorithm(Context context, TaskStackLayoutAlgorithmCallbacks taskStackLayoutAlgorithmCallbacks) {
        this.mContext = context;
        this.mCb = taskStackLayoutAlgorithmCallbacks;
        this.mTaskGridLayoutAlgorithm = new TaskGridLayoutAlgorithm(context);
        this.mTaskStackLowRamLayoutAlgorithm = new TaskStackLowRamLayoutAlgorithm(context);
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        Resources resources = context.getResources();
        this.mFocusedRange = new Range(resources.getFloat(2131427444), resources.getFloat(2131427443));
        this.mUnfocusedRange = new Range(resources.getFloat(2131427446), resources.getFloat(2131427445));
        this.mFocusState = getInitialFocusState();
        this.mFocusedTopPeekHeight = resources.getDimensionPixelSize(2131166243);
        this.mFocusedBottomPeekHeight = resources.getDimensionPixelSize(2131166227);
        this.mMinTranslationZ = resources.getDimensionPixelSize(2131166245);
        this.mMaxTranslationZ = resources.getDimensionPixelSize(2131166244);
        this.mBaseInitialTopOffset = getDimensionForDevice(context, 2131166232, 2131166231, 2131166233, 2131166233, 2131166233, 2131166233, 2131166233);
        this.mBaseInitialBottomOffset = getDimensionForDevice(context, 2131166229, 2131166228, 2131166230, 2131166230, 2131166230, 2131166230, 2131166230);
        this.mTaskGridLayoutAlgorithm.reloadOnConfigurationChange(context);
        this.mTaskStackLowRamLayoutAlgorithm.reloadOnConfigurationChange(context);
        this.mMinMargin = resources.getDimensionPixelSize(2131166234);
        this.mBaseTopMargin = getDimensionForDevice(context, 2131166240, 2131166241, 2131166242, 2131166241);
        this.mBaseSideMargin = getDimensionForDevice(context, 2131166235, 2131166236, 2131166238, 2131166236);
        this.mBaseBottomMargin = resources.getDimensionPixelSize(2131166226);
        this.mTitleBarHeight = getDimensionForDevice(this.mContext, 2131166256, 2131166256, 2131166256, 2131166257, 2131166256, 2131166257, 2131166224);
    }

    public void reset() {
        this.mTaskIndexOverrideMap.clear();
        setFocusState(getInitialFocusState());
    }

    public boolean setSystemInsets(Rect rect) {
        boolean z = !this.mSystemInsets.equals(rect);
        this.mSystemInsets.set(rect);
        this.mTaskGridLayoutAlgorithm.setSystemInsets(rect);
        this.mTaskStackLowRamLayoutAlgorithm.setSystemInsets(rect);
        return z;
    }

    public void setFocusState(int i) {
        int i2 = this.mFocusState;
        this.mFocusState = i;
        updateFrontBackTransforms();
        TaskStackLayoutAlgorithmCallbacks taskStackLayoutAlgorithmCallbacks = this.mCb;
        if (taskStackLayoutAlgorithmCallbacks != null) {
            taskStackLayoutAlgorithmCallbacks.onFocusStateChanged(i2, i);
        }
    }

    public int getFocusState() {
        return this.mFocusState;
    }

    public void initialize(Rect rect, Rect rect2, Rect rect3) {
        Rect rect4 = new Rect(this.mStackRect);
        int scaleForExtent = getScaleForExtent(rect2, rect, this.mBaseTopMargin, this.mMinMargin, 1);
        int scaleForExtent2 = getScaleForExtent(rect2, rect, this.mBaseBottomMargin, this.mMinMargin, 1);
        this.mInitialTopOffset = getScaleForExtent(rect2, rect, this.mBaseInitialTopOffset, this.mMinMargin, 1);
        this.mInitialBottomOffset = this.mBaseInitialBottomOffset;
        this.mStackBottomOffset = this.mSystemInsets.bottom + scaleForExtent2;
        this.mStackRect.set(rect3);
        Rect rect5 = this.mStackRect;
        rect5.top += scaleForExtent;
        Rect rect6 = this.mStackActionButtonRect;
        int i = rect5.left;
        int i2 = rect5.top;
        rect6.set(i, i2 - scaleForExtent, rect5.right, i2 + this.mFocusedTopPeekHeight);
        int height = (this.mStackRect.height() - this.mInitialTopOffset) - this.mStackBottomOffset;
        Rect rect7 = this.mTaskRect;
        Rect rect8 = this.mStackRect;
        int i3 = rect8.left;
        int i4 = rect8.top;
        rect7.set(i3, i4, rect8.right, height + i4);
        if (this.mTaskRect.width() <= 0 || this.mTaskRect.height() <= 0) {
            Log.e("TaskStackLayoutAlgorithm", "Invalid task rect: taskRect=" + this.mTaskRect + " stackRect=" + this.mStackRect + " displayRect=" + rect + " windowRect=" + rect2 + " taskStackBounds=" + rect3);
        }
        if (!rect4.equals(this.mStackRect)) {
            this.mUnfocusedCurve = constructUnfocusedCurve();
            this.mUnfocusedCurveInterpolator = new FreePathInterpolator(this.mUnfocusedCurve);
            this.mFocusedCurve = constructFocusedCurve();
            this.mFocusedCurveInterpolator = new FreePathInterpolator(this.mFocusedCurve);
            this.mUnfocusedDimCurve = constructUnfocusedDimCurve();
            this.mUnfocusedDimCurveInterpolator = new FreePathInterpolator(this.mUnfocusedDimCurve);
            this.mFocusedDimCurve = constructFocusedDimCurve();
            this.mFocusedDimCurveInterpolator = new FreePathInterpolator(this.mFocusedDimCurve);
            updateFrontBackTransforms();
        }
        this.mTaskGridLayoutAlgorithm.initialize(rect2);
        this.mTaskStackLowRamLayoutAlgorithm.initialize(rect2);
    }

    public void update(TaskStack taskStack, ArraySet<Task.TaskKey> arraySet, RecentsActivityLaunchState recentsActivityLaunchState, float f) {
        int i;
        float f2;
        LegacyRecentsImpl.getSystemServices();
        this.mTaskIndexMap.clear();
        ArrayList<Task> tasks = taskStack.getTasks();
        if (tasks.isEmpty()) {
            this.mFrontMostTaskP = 0.0f;
            this.mInitialScrollP = 0.0f;
            this.mMaxScrollP = 0.0f;
            this.mMinScrollP = 0.0f;
            this.mNumStackTasks = 0;
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < tasks.size(); i2++) {
            Task task = tasks.get(i2);
            if (!arraySet.contains(task.key)) {
                arrayList.add(task);
            }
        }
        this.mNumStackTasks = arrayList.size();
        int size = arrayList.size();
        for (int i3 = 0; i3 < size; i3++) {
            this.mTaskIndexMap.put(((Task) arrayList.get(i3)).key.id, i3);
        }
        Task launchTarget = taskStack.getLaunchTarget();
        boolean z = true;
        if (launchTarget != null) {
            i = taskStack.indexOfTask(launchTarget);
        } else {
            i = this.mNumStackTasks - 1;
        }
        if (getInitialFocusState() == 1) {
            float normalizedXFromFocusedY = getNormalizedXFromFocusedY((float) (this.mStackBottomOffset + this.mTaskRect.height()), 1);
            this.mFocusedRange.offset(0.0f);
            this.mMinScrollP = 0.0f;
            this.mMaxScrollP = Math.max(this.mMinScrollP, ((float) (this.mNumStackTasks - 1)) - Math.max(0.0f, this.mFocusedRange.getAbsoluteX(normalizedXFromFocusedY)));
            if (recentsActivityLaunchState.launchedFromHome || recentsActivityLaunchState.launchedFromPipApp || recentsActivityLaunchState.launchedWithNextPipApp) {
                this.mInitialScrollP = Utilities.clamp((float) i, this.mMinScrollP, this.mMaxScrollP);
            } else {
                this.mInitialScrollP = Utilities.clamp((float) (i - 1), this.mMinScrollP, this.mMaxScrollP);
            }
        } else if (this.mNumStackTasks == 1) {
            this.mMinScrollP = 0.0f;
            this.mMaxScrollP = 0.0f;
            this.mInitialScrollP = 0.0f;
        } else {
            float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY((float) (this.mStackBottomOffset + this.mTaskRect.height()), 1);
            this.mUnfocusedRange.offset(0.0f);
            this.mMinScrollP = LegacyRecentsImpl.getConfiguration().isLowRamDevice ? this.mTaskStackLowRamLayoutAlgorithm.getMinScrollP() : 0.0f;
            if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                f2 = this.mTaskStackLowRamLayoutAlgorithm.getMaxScrollP(size);
            } else {
                f2 = Math.max(this.mMinScrollP, ((float) (this.mNumStackTasks - 1)) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(normalizedXFromUnfocusedY)));
            }
            this.mMaxScrollP = f2;
            if (!recentsActivityLaunchState.launchedFromHome && !recentsActivityLaunchState.launchedFromPipApp && !recentsActivityLaunchState.launchedWithNextPipApp && !recentsActivityLaunchState.launchedViaDockGesture) {
                z = false;
            }
            if (recentsActivityLaunchState.launchedWithAltTab) {
                this.mInitialScrollP = Utilities.clamp((float) i, this.mMinScrollP, this.mMaxScrollP);
            } else if (0.0f <= f && f <= 1.0f) {
                this.mInitialScrollP = Utilities.mapRange(f, this.mMinScrollP, this.mMaxScrollP);
            } else if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                this.mInitialScrollP = this.mTaskStackLowRamLayoutAlgorithm.getInitialScrollP(this.mNumStackTasks, z);
            } else if (z) {
                this.mInitialScrollP = Utilities.clamp((float) i, this.mMinScrollP, this.mMaxScrollP);
            } else {
                this.mInitialScrollP = Math.max(this.mMinScrollP, Math.min(this.mMaxScrollP, (float) (this.mNumStackTasks - 2)) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(getNormalizedXFromUnfocusedY((float) this.mInitialTopOffset, 0))));
            }
        }
    }

    public void setTaskOverridesForInitialState(TaskStack taskStack, boolean z) {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        this.mTaskIndexOverrideMap.clear();
        boolean z2 = launchState.launchedFromHome || launchState.launchedFromPipApp || launchState.launchedWithNextPipApp || launchState.launchedViaDockGesture;
        if (getInitialFocusState() == 0 && this.mNumStackTasks > 1) {
            if (z || (!launchState.launchedWithAltTab && !z2)) {
                float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY((float) (this.mSystemInsets.bottom + this.mInitialBottomOffset), 1);
                float[] fArr = this.mNumStackTasks <= 2 ? new float[]{Math.min(getNormalizedXFromUnfocusedY((float) ((this.mFocusedTopPeekHeight + this.mTaskRect.height()) - this.mMinMargin), 0), normalizedXFromUnfocusedY), getNormalizedXFromUnfocusedY((float) this.mFocusedTopPeekHeight, 0)} : new float[]{normalizedXFromUnfocusedY, getNormalizedXFromUnfocusedY((float) this.mInitialTopOffset, 0)};
                this.mUnfocusedRange.offset(0.0f);
                ArrayList<Task> tasks = taskStack.getTasks();
                int size = tasks.size();
                for (int i = size - 1; i >= 0; i--) {
                    int i2 = (size - i) - 1;
                    if (i2 < fArr.length) {
                        this.mTaskIndexOverrideMap.put(tasks.get(i).key.id, Float.valueOf(this.mInitialScrollP + this.mUnfocusedRange.getAbsoluteX(fArr[i2])));
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public void addUnfocusedTaskOverride(Task task, float f) {
        if (this.mFocusState != 0) {
            this.mFocusedRange.offset(f);
            this.mUnfocusedRange.offset(f);
            float normalizedX = this.mFocusedRange.getNormalizedX((float) this.mTaskIndexMap.get(task.key.id));
            float x = this.mUnfocusedCurveInterpolator.getX(this.mFocusedCurveInterpolator.getInterpolation(normalizedX));
            float absoluteX = f + this.mUnfocusedRange.getAbsoluteX(x);
            if (Float.compare(normalizedX, x) != 0) {
                this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(absoluteX));
            }
        }
    }

    public void addUnfocusedTaskOverride(TaskView taskView, float f) {
        this.mFocusedRange.offset(f);
        this.mUnfocusedRange.offset(f);
        Task task = taskView.getTask();
        float top = (float) (taskView.getTop() - this.mTaskRect.top);
        float normalizedXFromFocusedY = getNormalizedXFromFocusedY(top, 0);
        float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY(top, 0);
        float absoluteX = f + this.mUnfocusedRange.getAbsoluteX(normalizedXFromUnfocusedY);
        if (Float.compare(normalizedXFromFocusedY, normalizedXFromUnfocusedY) != 0) {
            this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(absoluteX));
        }
    }

    public void clearUnfocusedTaskOverrides() {
        this.mTaskIndexOverrideMap.clear();
    }

    public float updateFocusStateOnScroll(float f, float f2, float f3) {
        if (f2 != f3 && !LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            float f4 = f2 - f3;
            float f5 = f2 - f;
            this.mUnfocusedRange.offset(f2);
            for (int size = this.mTaskIndexOverrideMap.size() - 1; size >= 0; size--) {
                int keyAt = this.mTaskIndexOverrideMap.keyAt(size);
                float f6 = (float) this.mTaskIndexMap.get(keyAt);
                float floatValue = this.mTaskIndexOverrideMap.get(keyAt, Float.valueOf(0.0f)).floatValue();
                float f7 = floatValue + f4;
                if (isInvalidOverrideX(f6, floatValue, f7)) {
                    this.mTaskIndexOverrideMap.removeAt(size);
                } else if ((floatValue < f6 || f4 > 0.0f) && (floatValue > f6 || f4 < 0.0f)) {
                    float f8 = floatValue - f5;
                    if (isInvalidOverrideX(f6, floatValue, f8)) {
                        this.mTaskIndexOverrideMap.removeAt(size);
                    } else {
                        this.mTaskIndexOverrideMap.put(keyAt, Float.valueOf(f8));
                    }
                    f2 = f3;
                } else {
                    this.mTaskIndexOverrideMap.put(keyAt, Float.valueOf(f7));
                }
            }
        }
        return f2;
    }

    private boolean isInvalidOverrideX(float f, float f2, float f3) {
        return ((this.mUnfocusedRange.getNormalizedX(f3) > 0.0f ? 1 : (this.mUnfocusedRange.getNormalizedX(f3) == 0.0f ? 0 : -1)) < 0 || (this.mUnfocusedRange.getNormalizedX(f3) > 1.0f ? 1 : (this.mUnfocusedRange.getNormalizedX(f3) == 1.0f ? 0 : -1)) > 0) || (f2 >= f && f >= f3) || (f2 <= f && f <= f3);
    }

    public int getInitialFocusState() {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        LegacyRecentsImpl.getDebugFlags();
        return launchState.launchedWithAltTab ? 1 : 0;
    }

    public Rect getStackActionButtonRect() {
        return useGridLayout() ? this.mTaskGridLayoutAlgorithm.getStackActionButtonRect() : this.mStackActionButtonRect;
    }

    public TaskViewTransform getBackOfStackTransform() {
        return this.mBackOfStackTransform;
    }

    public TaskViewTransform getFrontOfStackTransform() {
        return this.mFrontOfStackTransform;
    }

    public boolean isInitialized() {
        return !this.mStackRect.isEmpty();
    }

    public VisibilityReport computeStackVisibilityReport(ArrayList<Task> arrayList) {
        int i;
        int i2;
        if (useGridLayout()) {
            return this.mTaskGridLayoutAlgorithm.computeStackVisibilityReport(arrayList);
        }
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.computeStackVisibilityReport(arrayList);
        }
        if (arrayList.size() <= 1) {
            return new VisibilityReport(1, 1);
        }
        TaskViewTransform taskViewTransform = new TaskViewTransform();
        Range range = ((float) getInitialFocusState()) > 0.0f ? this.mFocusedRange : this.mUnfocusedRange;
        range.offset(this.mInitialScrollP);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(2131166256);
        float f = 2.14748365E9f;
        int size = arrayList.size() - 1;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            if (size < 0) {
                i = i3;
                break;
            }
            float stackScrollForTask = getStackScrollForTask(arrayList.get(size));
            if (!range.isInRange(stackScrollForTask)) {
                i2 = size;
            } else {
                i = i3;
                i2 = size;
                getStackTransform(stackScrollForTask, stackScrollForTask, this.mInitialScrollP, this.mFocusState, taskViewTransform, null, false, false);
                float f2 = taskViewTransform.rect.top;
                if (f - f2 > ((float) dimensionPixelSize)) {
                    i3 = i + 1;
                    i4++;
                    f = f2;
                } else {
                    int i5 = i2;
                    while (i5 >= 0 && range.isInRange(getStackScrollForTask(arrayList.get(i5)))) {
                        i4++;
                        i5--;
                    }
                }
            }
            size = i2 - 1;
        }
        return new VisibilityReport(i4, i);
    }

    public TaskViewTransform getStackTransform(Task task, float f, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2) {
        getStackTransform(task, f, this.mFocusState, taskViewTransform, taskViewTransform2, false, false);
        return taskViewTransform;
    }

    public TaskViewTransform getStackTransform(Task task, float f, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, boolean z) {
        getStackTransform(task, f, this.mFocusState, taskViewTransform, taskViewTransform2, false, z);
        return taskViewTransform;
    }

    public TaskViewTransform getStackTransform(Task task, float f, int i, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, boolean z, boolean z2) {
        float f2;
        if (useGridLayout()) {
            this.mTaskGridLayoutAlgorithm.getTransform(this.mTaskIndexMap.get(task.key.id), this.mTaskIndexMap.size(), taskViewTransform, this);
            return taskViewTransform;
        } else if (!LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            int i2 = this.mTaskIndexMap.get(task.key.id, -1);
            if (task == null || i2 == -1) {
                taskViewTransform.reset();
                return taskViewTransform;
            }
            if (z2) {
                f2 = (float) i2;
            } else {
                f2 = getStackScrollForTask(task);
            }
            getStackTransform(f2, (float) i2, f, i, taskViewTransform, taskViewTransform2, false, z);
            return taskViewTransform;
        } else if (task == null) {
            taskViewTransform.reset();
            return taskViewTransform;
        } else {
            this.mTaskStackLowRamLayoutAlgorithm.getTransform(this.mTaskIndexMap.get(task.key.id), f, taskViewTransform, this.mNumStackTasks, this);
            return taskViewTransform;
        }
    }

    public TaskViewTransform getStackTransformScreenCoordinates(Task task, float f, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, Rect rect) {
        getStackTransform(task, f, this.mFocusState, taskViewTransform, taskViewTransform2, true, false);
        transformToScreenCoordinates(taskViewTransform, rect);
        return taskViewTransform;
    }

    /* access modifiers changed from: package-private */
    public TaskViewTransform transformToScreenCoordinates(TaskViewTransform taskViewTransform, Rect rect) {
        if (rect == null) {
            rect = LegacyRecentsImpl.getSystemServices().getWindowRect();
        }
        taskViewTransform.rect.offset((float) rect.left, (float) rect.top);
        if (useGridLayout()) {
            taskViewTransform.rect.offset(0.0f, (float) this.mTitleBarHeight);
        }
        return taskViewTransform;
    }

    public void getStackTransform(float f, float f2, float f3, int i, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, boolean z, boolean z2) {
        float f4;
        int i2;
        float f5;
        float f6;
        LegacyRecentsImpl.getSystemServices();
        this.mUnfocusedRange.offset(f3);
        this.mFocusedRange.offset(f3);
        boolean isInRange = this.mUnfocusedRange.isInRange(f);
        boolean isInRange2 = this.mFocusedRange.isInRange(f);
        if (z2 || isInRange || isInRange2) {
            this.mUnfocusedRange.offset(f3);
            this.mFocusedRange.offset(f3);
            float normalizedX = this.mUnfocusedRange.getNormalizedX(f);
            float normalizedX2 = this.mFocusedRange.getNormalizedX(f);
            float clamp = Utilities.clamp(f3, this.mMinScrollP, this.mMaxScrollP);
            this.mUnfocusedRange.offset(clamp);
            this.mFocusedRange.offset(clamp);
            float normalizedX3 = this.mUnfocusedRange.getNormalizedX(f);
            float normalizedX4 = this.mUnfocusedRange.getNormalizedX(f2);
            float clamp2 = Utilities.clamp(f3, -3.4028235E38f, this.mMaxScrollP);
            this.mUnfocusedRange.offset(clamp2);
            this.mFocusedRange.offset(clamp2);
            float normalizedX5 = this.mUnfocusedRange.getNormalizedX(f);
            float normalizedX6 = this.mFocusedRange.getNormalizedX(f);
            int width = (this.mStackRect.width() - this.mTaskRect.width()) / 2;
            int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(2131166259);
            int i3 = this.mNumStackTasks;
            boolean z3 = true;
            if (i3 != 1 || z) {
                int interpolation = (int) ((1.0f - this.mUnfocusedCurveInterpolator.getInterpolation(normalizedX)) * ((float) this.mStackRect.height()));
                int interpolation2 = (int) ((1.0f - this.mFocusedCurveInterpolator.getInterpolation(normalizedX2)) * ((float) this.mStackRect.height()));
                float interpolation3 = this.mUnfocusedDimCurveInterpolator.getInterpolation(normalizedX5);
                float interpolation4 = this.mFocusedDimCurveInterpolator.getInterpolation(normalizedX6);
                if (this.mNumStackTasks <= 2 && f2 == 0.0f) {
                    if (normalizedX3 >= 0.5f) {
                        interpolation3 = 0.0f;
                    } else {
                        float interpolation5 = this.mUnfocusedDimCurveInterpolator.getInterpolation(0.5f);
                        interpolation3 = (interpolation3 - interpolation5) * (0.25f / (0.25f - interpolation5));
                    }
                }
                float f7 = (float) i;
                i2 = ((int) Utilities.mapRange(f7, (float) interpolation, (float) interpolation2)) + (this.mStackRect.top - this.mTaskRect.top) + dimensionPixelSize;
                float mapRange = Utilities.mapRange(Utilities.clamp01(normalizedX4), (float) this.mMinTranslationZ, (float) this.mMaxTranslationZ);
                float mapRange2 = Utilities.mapRange(f7, interpolation3, interpolation4);
                f4 = Utilities.mapRange(Utilities.clamp01(normalizedX3), 0.0f, 2.0f);
                f5 = mapRange2;
                f6 = mapRange;
            } else {
                float f8 = (this.mMinScrollP - f3) / ((float) i3);
                Rect rect = this.mStackRect;
                i2 = (rect.top - this.mTaskRect.top) + dimensionPixelSize + ((((rect.height() - this.mSystemInsets.bottom) - this.mTaskRect.height()) + dimensionPixelSize) / 2) + getYForDeltaP(f8, 0.0f);
                f6 = (float) this.mMaxTranslationZ;
                f4 = 1.0f;
                f5 = 0.0f;
            }
            taskViewTransform.scale = 1.0f;
            taskViewTransform.alpha = 1.0f;
            taskViewTransform.translationZ = f6;
            taskViewTransform.dimAlpha = f5;
            taskViewTransform.viewOutlineAlpha = f4;
            taskViewTransform.rect.set(this.mTaskRect);
            taskViewTransform.rect.offset((float) width, (float) i2);
            Utilities.scaleRectAboutCenter(taskViewTransform.rect, taskViewTransform.scale);
            float f9 = taskViewTransform.rect.top;
            if (f9 >= ((float) this.mStackRect.bottom) || (taskViewTransform2 != null && f9 == taskViewTransform2.rect.top)) {
                z3 = false;
            }
            taskViewTransform.visible = z3;
            return;
        }
        taskViewTransform.reset();
    }

    public Rect getUntransformedTaskViewBounds() {
        return new Rect(this.mTaskRect);
    }

    /* access modifiers changed from: package-private */
    public float getStackScrollForTask(Task task) {
        Float f = this.mTaskIndexOverrideMap.get(task.key.id, null);
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice || f == null) {
            return (float) this.mTaskIndexMap.get(task.key.id, 0);
        }
        return f.floatValue();
    }

    /* access modifiers changed from: package-private */
    public float getStackScrollForTaskIgnoreOverrides(Task task) {
        return (float) this.mTaskIndexMap.get(task.key.id, 0);
    }

    /* access modifiers changed from: package-private */
    public float getStackScrollForTaskAtInitialOffset(Task task) {
        boolean z = false;
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
            TaskStackLowRamLayoutAlgorithm taskStackLowRamLayoutAlgorithm = this.mTaskStackLowRamLayoutAlgorithm;
            int i = this.mNumStackTasks;
            if (launchState.launchedFromHome || launchState.launchedFromPipApp || launchState.launchedWithNextPipApp) {
                z = true;
            }
            return taskStackLowRamLayoutAlgorithm.getInitialScrollP(i, z);
        }
        float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY((float) this.mInitialTopOffset, 0);
        this.mUnfocusedRange.offset(0.0f);
        return Utilities.clamp(((float) this.mTaskIndexMap.get(task.key.id, 0)) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(normalizedXFromUnfocusedY)), this.mMinScrollP, this.mMaxScrollP);
    }

    public float getDeltaPForY(int i, int i2) {
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.scrollToPercentage(i - i2);
        }
        return -((((float) (i2 - i)) / ((float) this.mStackRect.height())) * this.mUnfocusedCurveInterpolator.getArcLength());
    }

    public int getYForDeltaP(float f, float f2) {
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.percentageToScroll(f - f2);
        }
        return -((int) ((f2 - f) * ((float) this.mStackRect.height()) * (1.0f / this.mUnfocusedCurveInterpolator.getArcLength())));
    }

    public void getTaskStackBounds(Rect rect, Rect rect2, int i, int i2, int i3, Rect rect3) {
        rect3.set(rect2.left + i2, rect2.top + i, rect2.right - i3, rect2.bottom);
        int width = rect3.width() - (getScaleForExtent(rect2, rect, this.mBaseSideMargin, this.mMinMargin, 0) * 2);
        if (Utilities.getAppConfiguration(this.mContext).orientation == 2) {
            Rect rect4 = new Rect(0, 0, Math.min(rect.width(), rect.height()), Math.max(rect.width(), rect.height()));
            width = Math.min(width, rect4.width() - (getScaleForExtent(rect4, rect4, this.mBaseSideMargin, this.mMinMargin, 0) * 2));
        }
        rect3.inset((rect3.width() - width) / 2, 0);
    }

    public static int getDimensionForDevice(Context context, int i, int i2, int i3, int i4) {
        return getDimensionForDevice(context, i, i, i2, i2, i3, i3, i4);
    }

    public static int getDimensionForDevice(Context context, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        RecentsConfiguration configuration = LegacyRecentsImpl.getConfiguration();
        Resources resources = context.getResources();
        boolean z = Utilities.getAppConfiguration(context).orientation == 2;
        if (configuration.isGridEnabled) {
            return resources.getDimensionPixelSize(i7);
        }
        if (configuration.isXLargeScreen) {
            if (z) {
                i5 = i6;
            }
            return resources.getDimensionPixelSize(i5);
        } else if (configuration.isLargeScreen) {
            if (z) {
                i3 = i4;
            }
            return resources.getDimensionPixelSize(i3);
        } else {
            if (z) {
                i = i2;
            }
            return resources.getDimensionPixelSize(i);
        }
    }

    private float getNormalizedXFromUnfocusedY(float f, int i) {
        if (i == 0) {
            f = ((float) this.mStackRect.height()) - f;
        }
        return this.mUnfocusedCurveInterpolator.getX(f / ((float) this.mStackRect.height()));
    }

    private float getNormalizedXFromFocusedY(float f, int i) {
        if (i == 0) {
            f = ((float) this.mStackRect.height()) - f;
        }
        return this.mFocusedCurveInterpolator.getX(f / ((float) this.mStackRect.height()));
    }

    private Path constructFocusedCurve() {
        float height = ((float) this.mFocusedTopPeekHeight) / ((float) this.mStackRect.height());
        float height2 = ((float) (this.mStackBottomOffset + this.mFocusedBottomPeekHeight)) / ((float) this.mStackRect.height());
        float height3 = ((float) ((this.mFocusedTopPeekHeight + this.mTaskRect.height()) - this.mMinMargin)) / ((float) this.mStackRect.height());
        Path path = new Path();
        path.moveTo(0.0f, 1.0f);
        path.lineTo(0.5f, 1.0f - height);
        path.lineTo(1.0f - (0.5f / this.mFocusedRange.relativeMax), Math.max(1.0f - height3, height2));
        path.lineTo(1.0f, 0.0f);
        return path;
    }

    private Path constructUnfocusedCurve() {
        float height = 1.0f - (((float) this.mFocusedTopPeekHeight) / ((float) this.mStackRect.height()));
        float f = (height - 0.975f) / 0.099999994f;
        Path path = new Path();
        path.moveTo(0.0f, 1.0f);
        path.cubicTo(0.0f, 1.0f, 0.4f, 0.975f, 0.5f, height);
        path.cubicTo(0.5f, height, 0.65f, (f * 0.65f) + (1.0f - (0.4f * f)), 1.0f, 0.0f);
        return path;
    }

    private Path constructFocusedDimCurve() {
        Path path = new Path();
        path.moveTo(0.0f, 0.25f);
        path.lineTo(0.5f, 0.0f);
        path.lineTo((0.5f / this.mFocusedRange.relativeMax) + 0.5f, 0.25f);
        path.lineTo(1.0f, 0.25f);
        return path;
    }

    private Path constructUnfocusedDimCurve() {
        float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY((float) this.mInitialTopOffset, 0);
        float f = ((1.0f - normalizedXFromUnfocusedY) / 2.0f) + normalizedXFromUnfocusedY;
        Path path = new Path();
        path.moveTo(0.0f, 0.25f);
        path.cubicTo(normalizedXFromUnfocusedY * 0.5f, 0.25f, normalizedXFromUnfocusedY * 0.75f, 0.1875f, normalizedXFromUnfocusedY, 0.0f);
        path.cubicTo(f, 0.0f, f, 0.15f, 1.0f, 0.15f);
        return path;
    }

    private int getScaleForExtent(Rect rect, Rect rect2, int i, int i2, int i3) {
        if (i3 == 0) {
            return Math.max(i2, (int) (Utilities.clamp01(((float) rect.width()) / ((float) rect2.width())) * ((float) i)));
        }
        return i3 == 1 ? Math.max(i2, (int) (Utilities.clamp01(((float) rect.height()) / ((float) rect2.height())) * ((float) i))) : i;
    }

    private void updateFrontBackTransforms() {
        if (!this.mStackRect.isEmpty()) {
            if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                this.mTaskStackLowRamLayoutAlgorithm.getBackOfStackTransform(this.mBackOfStackTransform, this);
                this.mTaskStackLowRamLayoutAlgorithm.getFrontOfStackTransform(this.mFrontOfStackTransform, this);
                return;
            }
            float mapRange = Utilities.mapRange((float) this.mFocusState, this.mUnfocusedRange.relativeMin, this.mFocusedRange.relativeMin);
            float mapRange2 = Utilities.mapRange((float) this.mFocusState, this.mUnfocusedRange.relativeMax, this.mFocusedRange.relativeMax);
            getStackTransform(mapRange, mapRange, 0.0f, this.mFocusState, this.mBackOfStackTransform, null, true, true);
            getStackTransform(mapRange2, mapRange2, 0.0f, this.mFocusState, this.mFrontOfStackTransform, null, true, true);
            this.mBackOfStackTransform.visible = true;
            this.mFrontOfStackTransform.visible = true;
        }
    }

    public Rect getTaskRect() {
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.getTaskRect();
        }
        return useGridLayout() ? this.mTaskGridLayoutAlgorithm.getTaskGridRect() : this.mTaskRect;
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.print("TaskStackLayoutAlgorithm");
        printWriter.write(" numStackTasks=");
        printWriter.print(this.mNumStackTasks);
        printWriter.println();
        printWriter.print(str2);
        printWriter.print("insets=");
        printWriter.print(Utilities.dumpRect(this.mSystemInsets));
        printWriter.print(" stack=");
        printWriter.print(Utilities.dumpRect(this.mStackRect));
        printWriter.print(" task=");
        printWriter.print(Utilities.dumpRect(this.mTaskRect));
        printWriter.print(" actionButton=");
        printWriter.print(Utilities.dumpRect(this.mStackActionButtonRect));
        printWriter.println();
        printWriter.print(str2);
        printWriter.print("minScroll=");
        printWriter.print(this.mMinScrollP);
        printWriter.print(" maxScroll=");
        printWriter.print(this.mMaxScrollP);
        printWriter.print(" initialScroll=");
        printWriter.print(this.mInitialScrollP);
        printWriter.println();
        printWriter.print(str2);
        printWriter.print("focusState=");
        printWriter.print(this.mFocusState);
        printWriter.println();
        if (this.mTaskIndexOverrideMap.size() > 0) {
            for (int size = this.mTaskIndexOverrideMap.size() - 1; size >= 0; size--) {
                int keyAt = this.mTaskIndexOverrideMap.keyAt(size);
                float floatValue = this.mTaskIndexOverrideMap.get(keyAt, Float.valueOf(0.0f)).floatValue();
                printWriter.print(str2);
                printWriter.print("taskId= ");
                printWriter.print(keyAt);
                printWriter.print(" x= ");
                printWriter.print((float) this.mTaskIndexMap.get(keyAt));
                printWriter.print(" overrideX= ");
                printWriter.print(floatValue);
                printWriter.println();
            }
        }
    }
}
