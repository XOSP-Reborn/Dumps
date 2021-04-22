package com.sonymobile.systemui.gameservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGameServiceOnGestureListener extends IInterface {
    void onGameButtonDoubleTap() throws RemoteException;

    void onGameButtonLongPress() throws RemoteException;

    void onGameButtonSingleTap() throws RemoteException;

    public static abstract class Stub extends Binder implements IGameServiceOnGestureListener {
        public static IGameServiceOnGestureListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IGameServiceOnGestureListener)) {
                return new Proxy(iBinder);
            }
            return (IGameServiceOnGestureListener) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IGameServiceOnGestureListener {
            public static IGameServiceOnGestureListener sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener
            public void onGameButtonSingleTap() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener");
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onGameButtonSingleTap();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener
            public void onGameButtonDoubleTap() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener");
                    if (this.mRemote.transact(2, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onGameButtonDoubleTap();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener
            public void onGameButtonLongPress() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener");
                    if (this.mRemote.transact(3, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onGameButtonLongPress();
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static IGameServiceOnGestureListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
