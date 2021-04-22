package com.android.systemui.recents.views.grid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.WindowManager;
import com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.shared.recents.model.Task;
import java.util.ArrayList;

public class TaskGridLayoutAlgorithm {
    private final String TAG = "TaskGridLayoutAlgorithm";
    private float mAppAspectRatio;
    private int mFocusedFrameThickness;
    private int mPaddingLeftRight;
    private int mPaddingTaskView;
    private int mPaddingTopBottom;
    private Point mScreenSize = new Point();
    private Rect mSystemInsets = new Rect();
    private Rect mTaskGridRect;
    private TaskGridRectInfo[] mTaskGridRectInfoList;
    private int mTitleBarHeight;
    private Rect mWindowRect;

    /* access modifiers changed from: package-private */
    public class TaskGridRectInfo {
        int lines;
        Rect size = new Rect();
        int tasksPerLine;
        int[] xOffsets;
        int[] yOffsets;

        TaskGridRectInfo(int i) {
            this.xOffsets = new int[i];
            this.yOffsets = new int[i];
            int min = Math.min(8, i);
            this.tasksPerLine = getTasksPerLine(min);
            int i2 = 4;
            this.lines = min < 4 ? 1 : 2;
            boolean z = TaskGridLayoutAlgorithm.this.mWindowRect.width() > TaskGridLayoutAlgorithm.this.mWindowRect.height();
            boolean z2 = TaskGridLayoutAlgorithm.this.mAppAspectRatio > 1.0f;
            if (!z && z2) {
                this.tasksPerLine = min < 2 ? 1 : 2;
                if (min < 3) {
                    i2 = 1;
                } else if (min < 5) {
                    i2 = 2;
                } else if (min < 7) {
                    i2 = 3;
                }
                this.lines = i2;
            }
            if (z && !z2) {
                this.tasksPerLine = min < 7 ? min : 6;
                this.lines = min < 7 ? 1 : 2;
            }
            int width = ((TaskGridLayoutAlgorithm.this.mWindowRect.width() - (TaskGridLayoutAlgorithm.this.mPaddingLeftRight * 2)) - ((this.tasksPerLine - 1) * TaskGridLayoutAlgorithm.this.mPaddingTaskView)) / this.tasksPerLine;
            int height = ((TaskGridLayoutAlgorithm.this.mWindowRect.height() - (TaskGridLayoutAlgorithm.this.mPaddingTopBottom * 2)) - ((this.lines - 1) * TaskGridLayoutAlgorithm.this.mPaddingTaskView)) / this.lines;
            float f = (float) width;
            if (((float) height) >= (f / TaskGridLayoutAlgorithm.this.mAppAspectRatio) + ((float) TaskGridLayoutAlgorithm.this.mTitleBarHeight)) {
                height = (int) (((double) ((f / TaskGridLayoutAlgorithm.this.mAppAspectRatio) + ((float) TaskGridLayoutAlgorithm.this.mTitleBarHeight))) + 0.5d);
            } else {
                width = (int) (((double) (((float) (height - TaskGridLayoutAlgorithm.this.mTitleBarHeight)) * TaskGridLayoutAlgorithm.this.mAppAspectRatio)) + 0.5d);
            }
            this.size.set(0, 0, width, height);
            int width2 = TaskGridLayoutAlgorithm.this.mWindowRect.width() - (TaskGridLayoutAlgorithm.this.mPaddingLeftRight * 2);
            int i3 = this.tasksPerLine;
            int i4 = (width2 - (i3 * width)) - ((i3 - 1) * TaskGridLayoutAlgorithm.this.mPaddingTaskView);
            int height2 = TaskGridLayoutAlgorithm.this.mWindowRect.height() - (TaskGridLayoutAlgorithm.this.mPaddingTopBottom * 2);
            int i5 = this.lines;
            int i6 = (height2 - (i5 * height)) - ((i5 - 1) * TaskGridLayoutAlgorithm.this.mPaddingTaskView);
            for (int i7 = 0; i7 < i; i7++) {
                int i8 = (i - i7) - 1;
                int i9 = this.tasksPerLine;
                this.xOffsets[i7] = TaskGridLayoutAlgorithm.this.mWindowRect.left + (i4 / 2) + TaskGridLayoutAlgorithm.this.mPaddingLeftRight + ((TaskGridLayoutAlgorithm.this.mPaddingTaskView + width) * (i8 % i9));
                this.yOffsets[i7] = TaskGridLayoutAlgorithm.this.mWindowRect.top + (i6 / 2) + TaskGridLayoutAlgorithm.this.mPaddingTopBottom + ((TaskGridLayoutAlgorithm.this.mPaddingTaskView + height) * (i8 / i9));
            }
        }

        private int getTasksPerLine(int i) {
            switch (i) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                case 4:
                    return 2;
                case 3:
                case 5:
                case 6:
                    return 3;
                case 7:
                case 8:
                    return 4;
                default:
                    throw new IllegalArgumentException("Unsupported task count " + i);
            }
        }
    }

    public TaskGridLayoutAlgorithm(Context context) {
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        Resources resources = context.getResources();
        this.mPaddingTaskView = resources.getDimensionPixelSize(2131166219);
        this.mFocusedFrameThickness = resources.getDimensionPixelSize(2131166222);
        this.mTaskGridRect = new Rect();
        this.mTitleBarHeight = resources.getDimensionPixelSize(2131166224);
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getRealSize(this.mScreenSize);
        updateAppAspectRatio();
    }

    public TaskViewTransform getTransform(int i, int i2, TaskViewTransform taskViewTransform, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        if (i2 == 0) {
            taskViewTransform.reset();
            return taskViewTransform;
        }
        TaskGridRectInfo taskGridRectInfo = this.mTaskGridRectInfoList[i2 - 1];
        this.mTaskGridRect.set(taskGridRectInfo.size);
        int i3 = taskGridRectInfo.xOffsets[i];
        int i4 = taskGridRectInfo.yOffsets[i];
        float f = (float) taskStackLayoutAlgorithm.mMaxTranslationZ;
        int i5 = i2 - i;
        boolean z = true;
        if (i5 - 1 >= 8) {
            z = false;
        }
        taskViewTransform.scale = 1.0f;
        taskViewTransform.alpha = z ? 1.0f : 0.0f;
        taskViewTransform.translationZ = f;
        taskViewTransform.dimAlpha = 0.0f;
        taskViewTransform.viewOutlineAlpha = 1.0f;
        taskViewTransform.rect.set(this.mTaskGridRect);
        taskViewTransform.rect.offset((float) i3, (float) i4);
        Utilities.scaleRectAboutCenter(taskViewTransform.rect, taskViewTransform.scale);
        taskViewTransform.visible = z;
        return taskViewTransform;
    }

    public int navigateFocus(int i, int i2, NavigateTaskViewEvent.Direction direction) {
        int i3;
        int i4;
        if (i < 1 || i > 8) {
            return -1;
        }
        if (i2 == -1) {
            return 0;
        }
        int i5 = i - 1;
        TaskGridRectInfo taskGridRectInfo = this.mTaskGridRectInfoList[i5];
        int i6 = (i5 - i2) / taskGridRectInfo.tasksPerLine;
        int i7 = AnonymousClass1.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction[direction.ordinal()];
        if (i7 == 1) {
            i3 = taskGridRectInfo.tasksPerLine + i2;
            if (i3 >= i) {
                return i2;
            }
        } else if (i7 != 2) {
            if (i7 == 3) {
                i4 = i2 + 1;
                if (i4 > i5 - (i6 * taskGridRectInfo.tasksPerLine)) {
                    return i2;
                }
            } else if (i7 != 4) {
                return i2;
            } else {
                i4 = i2 - 1;
                int i8 = (i5 - ((i6 + 1) * taskGridRectInfo.tasksPerLine)) + 1;
                if (i8 < 0) {
                    i8 = 0;
                }
                if (i4 < i8) {
                    return i2;
                }
            }
            return i4;
        } else {
            i3 = i2 - taskGridRectInfo.tasksPerLine;
            if (i3 < 0) {
                return i2;
            }
        }
        return i3;
    }

    /* access modifiers changed from: package-private */
    /* renamed from: com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm$1  reason: invalid class name */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction = new int[NavigateTaskViewEvent.Direction.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(10:0|1|2|3|4|5|6|7|8|10) */
        /* JADX WARNING: Can't wrap try/catch for region: R(8:0|1|2|3|4|5|6|(3:7|8|10)) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        static {
            /*
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction[] r0 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm.AnonymousClass1.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction = r0
                int[] r0 = com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm.AnonymousClass1.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction r1 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.UP     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm.AnonymousClass1.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction r1 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.DOWN     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm.AnonymousClass1.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction r1 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.LEFT     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                int[] r0 = com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm.AnonymousClass1.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction r1 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.RIGHT     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r2 = 4
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm.AnonymousClass1.<clinit>():void");
        }
    }

    public void initialize(Rect rect) {
        this.mWindowRect = rect;
        this.mPaddingLeftRight = (int) (((float) Math.min(this.mWindowRect.width(), this.mWindowRect.height())) * 0.025f);
        this.mPaddingTopBottom = (int) (((double) this.mWindowRect.height()) * 0.1d);
        this.mTaskGridRectInfoList = new TaskGridRectInfo[8];
        int i = 0;
        while (i < 8) {
            int i2 = i + 1;
            this.mTaskGridRectInfoList[i] = new TaskGridRectInfo(i2);
            i = i2;
        }
    }

    public void setSystemInsets(Rect rect) {
        this.mSystemInsets = rect;
        updateAppAspectRatio();
    }

    private void updateAppAspectRatio() {
        Point point = this.mScreenSize;
        int i = point.x;
        Rect rect = this.mSystemInsets;
        this.mAppAspectRatio = ((float) ((i - rect.left) - rect.right)) / ((float) ((point.y - rect.top) - rect.bottom));
    }

    public Rect getStackActionButtonRect() {
        Rect rect = new Rect(this.mWindowRect);
        int i = rect.right;
        int i2 = this.mPaddingLeftRight;
        rect.right = i - i2;
        rect.left += i2;
        rect.bottom = rect.top + this.mPaddingTopBottom;
        return rect;
    }

    public void updateTaskGridRect(int i) {
        if (i > 0) {
            this.mTaskGridRect.set(this.mTaskGridRectInfoList[i - 1].size);
        }
    }

    public Rect getTaskGridRect() {
        return this.mTaskGridRect;
    }

    public int getFocusFrameThickness() {
        return this.mFocusedFrameThickness;
    }

    public TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport(ArrayList<Task> arrayList) {
        int min = Math.min(8, arrayList.size());
        return new TaskStackLayoutAlgorithm.VisibilityReport(min, min);
    }
}
