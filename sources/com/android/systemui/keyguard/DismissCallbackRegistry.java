package com.android.systemui.keyguard;

import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import java.util.ArrayList;
import java.util.Objects;

public class DismissCallbackRegistry {
    private final ArrayList<DismissCallbackWrapper> mDismissCallbacks = new ArrayList<>();
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));

    public void addCallback(IKeyguardDismissCallback iKeyguardDismissCallback) {
        this.mDismissCallbacks.add(new DismissCallbackWrapper(iKeyguardDismissCallback));
    }

    public void notifyDismissCancelled() {
        for (int size = this.mDismissCallbacks.size() - 1; size >= 0; size--) {
            DismissCallbackWrapper dismissCallbackWrapper = this.mDismissCallbacks.get(size);
            UiOffloadThread uiOffloadThread = this.mUiOffloadThread;
            Objects.requireNonNull(dismissCallbackWrapper);
            uiOffloadThread.submit(new Runnable() {
                /* class com.android.systemui.keyguard.$$Lambda$zM6bayhThdtgvBghgFXo519LeO0 */

                public final void run() {
                    DismissCallbackWrapper.this.notifyDismissCancelled();
                }
            });
        }
        this.mDismissCallbacks.clear();
    }

    public void notifyDismissSucceeded() {
        for (int size = this.mDismissCallbacks.size() - 1; size >= 0; size--) {
            DismissCallbackWrapper dismissCallbackWrapper = this.mDismissCallbacks.get(size);
            UiOffloadThread uiOffloadThread = this.mUiOffloadThread;
            Objects.requireNonNull(dismissCallbackWrapper);
            uiOffloadThread.submit(new Runnable() {
                /* class com.android.systemui.keyguard.$$Lambda$2j_lq_QeR0jp4UUzPHOB_8BlctI */

                public final void run() {
                    DismissCallbackWrapper.this.notifyDismissSucceeded();
                }
            });
        }
        this.mDismissCallbacks.clear();
    }
}
