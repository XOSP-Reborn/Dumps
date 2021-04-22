package com.sonymobile.runtimeskinning;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.Map;

public interface ISkinningBridgeEndpoint extends IInterface {
    void registerEndpoint(ISkinningBridgeEndpoint iSkinningBridgeEndpoint) throws RemoteException;

    void transfer(Map map) throws RemoteException;

    boolean useVersion(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements ISkinningBridgeEndpoint {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
        }

        public static ISkinningBridgeEndpoint asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof ISkinningBridgeEndpoint)) {
                return new Proxy(iBinder);
            }
            return (ISkinningBridgeEndpoint) queryLocalInterface;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1) {
                parcel.enforceInterface("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
                boolean useVersion = useVersion(parcel.readString());
                parcel2.writeNoException();
                parcel2.writeInt(useVersion ? 1 : 0);
                return true;
            } else if (i == 2) {
                parcel.enforceInterface("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
                registerEndpoint(asInterface(parcel.readStrongBinder()));
                parcel2.writeNoException();
                return true;
            } else if (i == 3) {
                parcel.enforceInterface("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
                transfer(parcel.readHashMap(getClass().getClassLoader()));
                return true;
            } else if (i != 1598968902) {
                return super.onTransact(i, parcel, parcel2, i2);
            } else {
                parcel2.writeString("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
                return true;
            }
        }

        /* access modifiers changed from: private */
        public static class Proxy implements ISkinningBridgeEndpoint {
            public static ISkinningBridgeEndpoint sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint
            public boolean useVersion(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
                    obtain.writeString(str);
                    boolean z = false;
                    if (!this.mRemote.transact(1, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().useVersion(str);
                    }
                    obtain2.readException();
                    if (obtain2.readInt() != 0) {
                        z = true;
                    }
                    obtain2.recycle();
                    obtain.recycle();
                    return z;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint
            public void registerEndpoint(ISkinningBridgeEndpoint iSkinningBridgeEndpoint) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
                    obtain.writeStrongBinder(iSkinningBridgeEndpoint != null ? iSkinningBridgeEndpoint.asBinder() : null);
                    if (this.mRemote.transact(2, obtain, obtain2, 0) || Stub.getDefaultImpl() == null) {
                        obtain2.readException();
                        obtain2.recycle();
                        obtain.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().registerEndpoint(iSkinningBridgeEndpoint);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint
            public void transfer(Map map) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.sonymobile.runtimeskinning.ISkinningBridgeEndpoint");
                    obtain.writeMap(map);
                    if (this.mRemote.transact(3, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().transfer(map);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static ISkinningBridgeEndpoint getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
