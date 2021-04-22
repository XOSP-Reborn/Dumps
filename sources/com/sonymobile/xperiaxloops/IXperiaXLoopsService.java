package com.sonymobile.xperiaxloops;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IXperiaXLoopsService extends IInterface {
    void registerCallback(int i, IXperiaXLoopsServiceCallback iXperiaXLoopsServiceCallback) throws RemoteException;

    boolean requestAssistEmphasis(Bundle bundle) throws RemoteException;

    void sendFPAResult(int i, Bundle bundle) throws RemoteException;

    void sendKeyguardStatus(int i) throws RemoteException;

    void sendScreenStatus(int i) throws RemoteException;

    void setLoopsColorOnAmbient(int i) throws RemoteException;

    void setLoopsColorOnLockscreen(int i) throws RemoteException;

    void unregisterCallback(int i, IXperiaXLoopsServiceCallback iXperiaXLoopsServiceCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IXperiaXLoopsService {
        public static IXperiaXLoopsService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IXperiaXLoopsService)) {
                return new Proxy(iBinder);
            }
            return (IXperiaXLoopsService) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IXperiaXLoopsService {
            public static IXperiaXLoopsService sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public void registerCallback(int i, IXperiaXLoopsServiceCallback iXperiaXLoopsServiceCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    obtain.writeInt(i);
                    obtain.writeStrongBinder(iXperiaXLoopsServiceCallback != null ? iXperiaXLoopsServiceCallback.asBinder() : null);
                    if (this.mRemote.transact(1, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().registerCallback(i, iXperiaXLoopsServiceCallback);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public void unregisterCallback(int i, IXperiaXLoopsServiceCallback iXperiaXLoopsServiceCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    obtain.writeInt(i);
                    obtain.writeStrongBinder(iXperiaXLoopsServiceCallback != null ? iXperiaXLoopsServiceCallback.asBinder() : null);
                    if (this.mRemote.transact(2, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().unregisterCallback(i, iXperiaXLoopsServiceCallback);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public void setLoopsColorOnLockscreen(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    obtain.writeInt(i);
                    if (this.mRemote.transact(3, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().setLoopsColorOnLockscreen(i);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public boolean requestAssistEmphasis(Bundle bundle) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    boolean z = true;
                    if (bundle != null) {
                        obtain.writeInt(1);
                        bundle.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (!this.mRemote.transact(4, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().requestAssistEmphasis(bundle);
                    }
                    obtain2.readException();
                    if (obtain2.readInt() == 0) {
                        z = false;
                    }
                    obtain2.recycle();
                    obtain.recycle();
                    return z;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public void sendKeyguardStatus(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    obtain.writeInt(i);
                    if (this.mRemote.transact(5, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().sendKeyguardStatus(i);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public void sendFPAResult(int i, Bundle bundle) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    obtain.writeInt(i);
                    if (bundle != null) {
                        obtain.writeInt(1);
                        bundle.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(6, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().sendFPAResult(i, bundle);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public void sendScreenStatus(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    obtain.writeInt(i);
                    if (this.mRemote.transact(7, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().sendScreenStatus(i);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.xperiaxloops.IXperiaXLoopsService
            public void setLoopsColorOnAmbient(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.xperiaxloops.IXperiaXLoopsService");
                    obtain.writeInt(i);
                    if (this.mRemote.transact(9, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().setLoopsColorOnAmbient(i);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }

        public static IXperiaXLoopsService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
