package com.sonymobile.xperiaxloops;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IXperiaXLoopsServiceCallback extends IInterface {
    void hide(boolean z) throws RemoteException;

    void show() throws RemoteException;

    public static abstract class Stub extends Binder implements IXperiaXLoopsServiceCallback {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "com.sonymobile.xperiaxloops.IXperiaXLoopsServiceCallback");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1) {
                parcel.enforceInterface("com.sonymobile.xperiaxloops.IXperiaXLoopsServiceCallback");
                show();
                parcel2.writeNoException();
                return true;
            } else if (i == 2) {
                parcel.enforceInterface("com.sonymobile.xperiaxloops.IXperiaXLoopsServiceCallback");
                hide(parcel.readInt() != 0);
                parcel2.writeNoException();
                return true;
            } else if (i != 1598968902) {
                return super.onTransact(i, parcel, parcel2, i2);
            } else {
                parcel2.writeString("com.sonymobile.xperiaxloops.IXperiaXLoopsServiceCallback");
                return true;
            }
        }
    }
}
