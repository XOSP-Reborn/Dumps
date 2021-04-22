package com.android.systemui.recents.model;

import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;

public interface TaskFilter {
    boolean acceptTask(SparseArray<Task> sparseArray, Task task, int i);
}
