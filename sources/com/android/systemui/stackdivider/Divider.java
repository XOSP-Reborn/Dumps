package com.android.systemui.stackdivider;

import android.content.res.Configuration;
import android.os.RemoteException;
import android.util.Log;
import android.view.IDockedStackListener;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import com.android.systemui.C0010R$layout;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.DividerView;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class Divider extends SystemUI implements DividerView.DividerCallbacks {
    private boolean mAdjustedForIme = false;
    private final DividerState mDividerState = new DividerState();
    private DockDividerVisibilityListener mDockDividerVisibilityListener;
    private ForcedResizableInfoActivityController mForcedResizableController;
    private boolean mHomeStackResizable = false;
    private boolean mMinimized = false;
    private DividerView mView;
    private boolean mVisible = false;
    private DividerWindowManager mWindowManager;

    @Override // com.android.systemui.SystemUI
    public void start() {
        this.mWindowManager = new DividerWindowManager(this.mContext);
        update(this.mContext.getResources().getConfiguration());
        putComponent(Divider.class, this);
        this.mDockDividerVisibilityListener = new DockDividerVisibilityListener();
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(this.mDockDividerVisibilityListener);
        } catch (Exception e) {
            Log.e("Divider", "Failed to register docked stack listener", e);
        }
        this.mForcedResizableController = new ForcedResizableInfoActivityController(this.mContext);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.SystemUI
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        update(configuration);
    }

    public DividerView getView() {
        return this.mView;
    }

    public boolean isMinimized() {
        return this.mMinimized;
    }

    public boolean isHomeStackResizable() {
        return this.mHomeStackResizable;
    }

    private void addDivider(Configuration configuration) {
        this.mView = (DividerView) LayoutInflater.from(this.mContext).inflate(C0010R$layout.docked_stack_divider, (ViewGroup) null);
        this.mView.injectDependencies(this.mWindowManager, this.mDividerState, this);
        boolean z = false;
        this.mView.setVisibility(this.mVisible ? 0 : 4);
        this.mView.setMinimizedDockStack(this.mMinimized, this.mHomeStackResizable);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(17105147);
        if (configuration.orientation == 2) {
            z = true;
        }
        int i = -1;
        int i2 = z ? dimensionPixelSize : -1;
        if (!z) {
            i = dimensionPixelSize;
        }
        this.mWindowManager.add(this.mView, i2, i);
    }

    private void removeDivider() {
        DividerView dividerView = this.mView;
        if (dividerView != null) {
            dividerView.onDividerRemoved();
        }
        this.mWindowManager.remove();
    }

    private void update(Configuration configuration) {
        removeDivider();
        addDivider(configuration);
        if (this.mMinimized) {
            this.mView.setMinimizedDockStack(true, this.mHomeStackResizable);
            updateTouchable();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateVisibility(final boolean z) {
        this.mView.post(new Runnable() {
            /* class com.android.systemui.stackdivider.Divider.AnonymousClass1 */

            public void run() {
                boolean z = Divider.this.mVisible;
                boolean z2 = z;
                if (z != z2) {
                    Divider.this.mVisible = z2;
                    Divider.this.mView.setVisibility(z ? 0 : 4);
                    Divider.this.mView.setMinimizedDockStack(Divider.this.mMinimized, Divider.this.mHomeStackResizable);
                }
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateMinimizedDockedStack(final boolean z, final long j, final boolean z2) {
        this.mView.post(new Runnable() {
            /* class com.android.systemui.stackdivider.Divider.AnonymousClass2 */

            public void run() {
                Divider.this.mHomeStackResizable = z2;
                boolean z = Divider.this.mMinimized;
                boolean z2 = z;
                if (z != z2) {
                    Divider.this.mMinimized = z2;
                    Divider.this.updateTouchable();
                    if (j > 0) {
                        Divider.this.mView.setMinimizedDockStack(z, j, z2);
                    } else {
                        Divider.this.mView.setMinimizedDockStack(z, z2);
                    }
                }
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyDockedStackExistsChanged(final boolean z) {
        this.mView.post(new Runnable() {
            /* class com.android.systemui.stackdivider.Divider.AnonymousClass3 */

            public void run() {
                Divider.this.mForcedResizableController.notifyDockedStackExistsChanged(z);
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTouchable() {
        this.mWindowManager.setTouchable((this.mHomeStackResizable || !this.mMinimized) && !this.mAdjustedForIme);
    }

    public void onRecentsActivityStarting() {
        DividerView dividerView = this.mView;
        if (dividerView != null) {
            dividerView.onRecentsActivityStarting();
        }
    }

    public void onRecentsDrawn() {
        DividerView dividerView = this.mView;
        if (dividerView != null) {
            dividerView.onRecentsDrawn();
        }
    }

    public void onUndockingTask() {
        DividerView dividerView = this.mView;
        if (dividerView != null) {
            dividerView.onUndockingTask();
        }
    }

    public void onDockedFirstAnimationFrame() {
        DividerView dividerView = this.mView;
        if (dividerView != null) {
            dividerView.onDockedFirstAnimationFrame();
        }
    }

    public void onDockedTopTask() {
        DividerView dividerView = this.mView;
        if (dividerView != null) {
            dividerView.onDockedTopTask();
        }
    }

    public void onAppTransitionFinished() {
        this.mForcedResizableController.onAppTransitionFinished();
    }

    @Override // com.android.systemui.stackdivider.DividerView.DividerCallbacks
    public void onDraggingStart() {
        this.mForcedResizableController.onDraggingStart();
    }

    @Override // com.android.systemui.stackdivider.DividerView.DividerCallbacks
    public void onDraggingEnd() {
        this.mForcedResizableController.onDraggingEnd();
    }

    @Override // com.android.systemui.stackdivider.DividerView.DividerCallbacks
    public void growRecents() {
        Recents recents = (Recents) getComponent(Recents.class);
        if (recents != null) {
            recents.growRecents();
        }
    }

    @Override // com.android.systemui.SystemUI
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mVisible=");
        printWriter.println(this.mVisible);
        printWriter.print("  mMinimized=");
        printWriter.println(this.mMinimized);
        printWriter.print("  mAdjustedForIme=");
        printWriter.println(this.mAdjustedForIme);
    }

    class DockDividerVisibilityListener extends IDockedStackListener.Stub {
        DockDividerVisibilityListener() {
        }

        public void onDividerVisibilityChanged(boolean z) throws RemoteException {
            Divider.this.updateVisibility(z);
        }

        public void onDockedStackExistsChanged(boolean z) throws RemoteException {
            Divider.this.notifyDockedStackExistsChanged(z);
        }

        public void onDockedStackMinimizedChanged(boolean z, long j, boolean z2) throws RemoteException {
            Divider.this.mHomeStackResizable = z2;
            Divider.this.updateMinimizedDockedStack(z, j, z2);
        }

        public void onAdjustedForImeChanged(boolean z, long j) throws RemoteException {
            Divider.this.mView.post(new Runnable(z, j) {
                /* class com.android.systemui.stackdivider.$$Lambda$Divider$DockDividerVisibilityListener$fZDE4rhC5s3QEgR7YXeKi_feiY */
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ long f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    Divider.DockDividerVisibilityListener.this.lambda$onAdjustedForImeChanged$0$Divider$DockDividerVisibilityListener(this.f$1, this.f$2);
                }
            });
        }

        public /* synthetic */ void lambda$onAdjustedForImeChanged$0$Divider$DockDividerVisibilityListener(boolean z, long j) {
            if (Divider.this.mAdjustedForIme != z) {
                Divider.this.mAdjustedForIme = z;
                Divider.this.updateTouchable();
                if (Divider.this.mMinimized) {
                    return;
                }
                if (j > 0) {
                    Divider.this.mView.setAdjustedForIme(z, j);
                } else {
                    Divider.this.mView.setAdjustedForIme(z);
                }
            }
        }

        public /* synthetic */ void lambda$onDockSideChanged$1$Divider$DockDividerVisibilityListener(int i) {
            Divider.this.mView.notifyDockSideChanged(i);
        }

        public void onDockSideChanged(int i) throws RemoteException {
            Divider.this.mView.post(new Runnable(i) {
                /* class com.android.systemui.stackdivider.$$Lambda$Divider$DockDividerVisibilityListener$cPiHgQdgCDQeKAQTEdGGnGaaM_c */
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    Divider.DockDividerVisibilityListener.this.lambda$onDockSideChanged$1$Divider$DockDividerVisibilityListener(this.f$1);
                }
            });
        }
    }
}
