package com.sonymobile.runtimeskinning;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ISkinManager extends IInterface {
    String getSkin() throws RemoteException;

    Bundle getSkinState() throws RemoteException;

    public static abstract class Stub extends Binder implements ISkinManager {
        public static ISkinManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.sonymobile.runtimeskinning.ISkinManager");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof ISkinManager)) {
                return new Proxy(iBinder);
            }
            return (ISkinManager) queryLocalInterface;
        }

        private static class Proxy implements ISkinManager {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.sonymobile.runtimeskinning.ISkinManager
            public String getSkin() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.runtimeskinning.ISkinManager");
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readString();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.runtimeskinning.ISkinManager
            public Bundle getSkinState() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.runtimeskinning.ISkinManager");
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(obtain2) : null;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}
