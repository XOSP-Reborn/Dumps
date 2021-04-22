package com.android.systemui.recents;

import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.pip.phone.ForegroundThread;
import com.android.systemui.recents.IRecentsNonSystemUserCallbacks;
import com.android.systemui.recents.IRecentsSystemUserCallbacks;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;

public class RecentsSystemUser extends IRecentsSystemUserCallbacks.Stub {
    private Context mContext;
    private RecentsImpl mImpl;
    private final SparseArray<IRecentsNonSystemUserCallbacks> mNonSystemUserRecents = new SparseArray<>();

    public RecentsSystemUser(Context context, RecentsImpl recentsImpl) {
        this.mContext = context;
        this.mImpl = recentsImpl;
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void registerNonSystemUserCallbacks(IBinder iBinder, final int i) {
        try {
            final IRecentsNonSystemUserCallbacks asInterface = IRecentsNonSystemUserCallbacks.Stub.asInterface(iBinder);
            iBinder.linkToDeath(new IBinder.DeathRecipient() {
                /* class com.android.systemui.recents.RecentsSystemUser.AnonymousClass1 */

                public void binderDied() {
                    RecentsSystemUser.this.mNonSystemUserRecents.removeAt(RecentsSystemUser.this.mNonSystemUserRecents.indexOfValue(asInterface));
                    EventLog.writeEvent(36060, 5, Integer.valueOf(i));
                }
            }, 0);
            this.mNonSystemUserRecents.put(i, asInterface);
            EventLog.writeEvent(36060, 4, Integer.valueOf(i));
        } catch (RemoteException e) {
            Log.e("RecentsSystemUser", "Failed to register NonSystemUserCallbacks", e);
        }
    }

    public IRecentsNonSystemUserCallbacks getNonSystemUserRecentsForUser(int i) {
        return this.mNonSystemUserRecents.get(i);
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void updateRecentsVisibility(boolean z) {
        ForegroundThread.getHandler().post(new Runnable(z) {
            /* class com.android.systemui.recents.$$Lambda$RecentsSystemUser$mq7gzWWErKCOgjCgOrRqm6b0eU */
            private final /* synthetic */ boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                RecentsSystemUser.this.lambda$updateRecentsVisibility$0$RecentsSystemUser(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$updateRecentsVisibility$0$RecentsSystemUser(boolean z) {
        this.mImpl.onVisibilityChanged(this.mContext, z);
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void startScreenPinning(int i) {
        ForegroundThread.getHandler().post(new Runnable(i) {
            /* class com.android.systemui.recents.$$Lambda$RecentsSystemUser$RuMGq01oJynKESbiTF6h02bxcQ4 */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                RecentsSystemUser.this.lambda$startScreenPinning$1$RecentsSystemUser(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$startScreenPinning$1$RecentsSystemUser(int i) {
        this.mImpl.onStartScreenPinning(this.mContext, i);
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendRecentsDrawnEvent() {
        EventBus.getDefault().post(new RecentsDrawnEvent());
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendDockingTopTaskEvent(Rect rect) throws RemoteException {
        EventBus.getDefault().post(new DockedTopTaskEvent(rect));
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendLaunchRecentsEvent() throws RemoteException {
        EventBus.getDefault().post(new RecentsActivityStartingEvent());
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendDockedFirstAnimationFrameEvent() throws RemoteException {
        EventBus.getDefault().post(new DockedFirstAnimationFrameEvent());
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void setWaitingForTransitionStartEvent(boolean z) {
        EventBus.getDefault().post(new SetWaitingForTransitionStartEvent(z));
    }
}
