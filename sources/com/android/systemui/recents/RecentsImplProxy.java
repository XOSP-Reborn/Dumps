package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import com.android.internal.os.SomeArgs;
import com.android.systemui.recents.IRecentsNonSystemUserCallbacks;

public class RecentsImplProxy extends IRecentsNonSystemUserCallbacks.Stub {
    private final Handler mHandler = new Handler() {
        /* class com.android.systemui.recents.RecentsImplProxy.AnonymousClass1 */

        public void handleMessage(Message message) {
            boolean z = true;
            switch (message.what) {
                case 1:
                    RecentsImplProxy.this.mImpl.preloadRecents();
                    break;
                case 2:
                    RecentsImplProxy.this.mImpl.cancelPreloadingRecents();
                    break;
                case 3:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    RecentsImpl recentsImpl = RecentsImplProxy.this.mImpl;
                    boolean z2 = someArgs.argi1 != 0;
                    boolean z3 = someArgs.argi2 != 0;
                    if (someArgs.argi3 == 0) {
                        z = false;
                    }
                    recentsImpl.showRecents(z2, z3, z, someArgs.argi4);
                    break;
                case 4:
                    RecentsImpl recentsImpl2 = RecentsImplProxy.this.mImpl;
                    boolean z4 = message.arg1 != 0;
                    if (message.arg2 == 0) {
                        z = false;
                    }
                    recentsImpl2.hideRecents(z4, z);
                    break;
                case 5:
                    RecentsImplProxy.this.mImpl.toggleRecents(((SomeArgs) message.obj).argi1);
                    break;
                case 6:
                    RecentsImplProxy.this.mImpl.onConfigurationChanged();
                    break;
                case 7:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    RecentsImpl recentsImpl3 = RecentsImplProxy.this.mImpl;
                    int i = someArgs2.argi1;
                    someArgs2.argi2 = 0;
                    recentsImpl3.splitPrimaryTask(i, 0, (Rect) someArgs2.arg1);
                    break;
                case 8:
                    RecentsImplProxy.this.mImpl.onDraggingInRecents(((Float) message.obj).floatValue());
                    break;
                case 9:
                    RecentsImplProxy.this.mImpl.onDraggingInRecentsEnded(((Float) message.obj).floatValue());
                    break;
                case 10:
                    RecentsImplProxy.this.mImpl.onShowCurrentUserToast(message.arg1, message.arg2);
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
            super.handleMessage(message);
        }
    };
    private RecentsImpl mImpl;

    public RecentsImplProxy(RecentsImpl recentsImpl) {
        this.mImpl = recentsImpl;
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void preloadRecents() throws RemoteException {
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void cancelPreloadingRecents() throws RemoteException {
        this.mHandler.sendEmptyMessage(2);
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void showRecents(boolean z, boolean z2, boolean z3, int i) throws RemoteException {
        SomeArgs obtain = SomeArgs.obtain();
        obtain.argi1 = z ? 1 : 0;
        obtain.argi2 = z2 ? 1 : 0;
        obtain.argi3 = z3 ? 1 : 0;
        obtain.argi4 = i;
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(3, obtain));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void hideRecents(boolean z, boolean z2) throws RemoteException {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(4, z ? 1 : 0, z2 ? 1 : 0));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void toggleRecents(int i) throws RemoteException {
        SomeArgs obtain = SomeArgs.obtain();
        obtain.argi1 = i;
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(5, obtain));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void onConfigurationChanged() throws RemoteException {
        this.mHandler.sendEmptyMessage(6);
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void splitPrimaryTask(int i, int i2, Rect rect) throws RemoteException {
        SomeArgs obtain = SomeArgs.obtain();
        obtain.argi1 = i;
        obtain.argi2 = i2;
        obtain.arg1 = rect;
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(7, obtain));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void onDraggingInRecents(float f) throws RemoteException {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(8, Float.valueOf(f)));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void onDraggingInRecentsEnded(float f) throws RemoteException {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(9, Float.valueOf(f)));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void showCurrentUserToast(int i, int i2) {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(10, i, i2));
    }
}
