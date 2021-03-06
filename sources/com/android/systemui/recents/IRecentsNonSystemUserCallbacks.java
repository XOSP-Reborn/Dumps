package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRecentsNonSystemUserCallbacks extends IInterface {
    void cancelPreloadingRecents() throws RemoteException;

    void hideRecents(boolean z, boolean z2) throws RemoteException;

    void onConfigurationChanged() throws RemoteException;

    void onDraggingInRecents(float f) throws RemoteException;

    void onDraggingInRecentsEnded(float f) throws RemoteException;

    void preloadRecents() throws RemoteException;

    void showCurrentUserToast(int i, int i2) throws RemoteException;

    void showRecents(boolean z, boolean z2, boolean z3, int i) throws RemoteException;

    void splitPrimaryTask(int i, int i2, Rect rect) throws RemoteException;

    void toggleRecents(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IRecentsNonSystemUserCallbacks {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
        }

        public static IRecentsNonSystemUserCallbacks asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IRecentsNonSystemUserCallbacks)) {
                return new Proxy(iBinder);
            }
            return (IRecentsNonSystemUserCallbacks) queryLocalInterface;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i != 1598968902) {
                boolean z = false;
                switch (i) {
                    case 1:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        preloadRecents();
                        return true;
                    case 2:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        cancelPreloadingRecents();
                        return true;
                    case 3:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        boolean z2 = parcel.readInt() != 0;
                        boolean z3 = parcel.readInt() != 0;
                        if (parcel.readInt() != 0) {
                            z = true;
                        }
                        showRecents(z2, z3, z, parcel.readInt());
                        return true;
                    case 4:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        boolean z4 = parcel.readInt() != 0;
                        if (parcel.readInt() != 0) {
                            z = true;
                        }
                        hideRecents(z4, z);
                        return true;
                    case 5:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        toggleRecents(parcel.readInt());
                        return true;
                    case 6:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        onConfigurationChanged();
                        return true;
                    case 7:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        splitPrimaryTask(parcel.readInt(), parcel.readInt(), parcel.readInt() != 0 ? (Rect) Rect.CREATOR.createFromParcel(parcel) : null);
                        return true;
                    case 8:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        onDraggingInRecents(parcel.readFloat());
                        return true;
                    case 9:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        onDraggingInRecentsEnded(parcel.readFloat());
                        return true;
                    case 10:
                        parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        showCurrentUserToast(parcel.readInt(), parcel.readInt());
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            } else {
                parcel2.writeString("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                return true;
            }
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IRecentsNonSystemUserCallbacks {
            public static IRecentsNonSystemUserCallbacks sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void preloadRecents() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().preloadRecents();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void cancelPreloadingRecents() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    if (this.mRemote.transact(2, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().cancelPreloadingRecents();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void showRecents(boolean z, boolean z2, boolean z3, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    int i2 = 0;
                    obtain.writeInt(z ? 1 : 0);
                    obtain.writeInt(z2 ? 1 : 0);
                    if (z3) {
                        i2 = 1;
                    }
                    obtain.writeInt(i2);
                    obtain.writeInt(i);
                    if (this.mRemote.transact(3, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().showRecents(z, z2, z3, i);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void hideRecents(boolean z, boolean z2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    int i = 0;
                    obtain.writeInt(z ? 1 : 0);
                    if (z2) {
                        i = 1;
                    }
                    obtain.writeInt(i);
                    if (this.mRemote.transact(4, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().hideRecents(z, z2);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void toggleRecents(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    obtain.writeInt(i);
                    if (this.mRemote.transact(5, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().toggleRecents(i);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void onConfigurationChanged() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    if (this.mRemote.transact(6, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onConfigurationChanged();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void splitPrimaryTask(int i, int i2, Rect rect) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    if (rect != null) {
                        obtain.writeInt(1);
                        rect.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(7, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().splitPrimaryTask(i, i2, rect);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
            public void showCurrentUserToast(int i, int i2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    if (this.mRemote.transact(10, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().showCurrentUserToast(i, i2);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static IRecentsNonSystemUserCallbacks getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
