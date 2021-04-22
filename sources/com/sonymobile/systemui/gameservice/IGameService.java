package com.sonymobile.systemui.gameservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.sonymobile.systemui.gameservice.IGameServiceOnGestureListener;

public interface IGameService extends IInterface {
    void gameSetColoredIcon(IBinder iBinder, int i, int i2, int i3, int i4) throws RemoteException;

    void gameSetIcon(IBinder iBinder, int i, int i2) throws RemoteException;

    void gameSetOnGestureListener(IBinder iBinder, int i, IGameServiceOnGestureListener iGameServiceOnGestureListener) throws RemoteException;

    void gameUnsetIcon(IBinder iBinder, int i) throws RemoteException;

    void gameUnsetOnGestureListener(IBinder iBinder, int i) throws RemoteException;

    void lockSetEnabled(IBinder iBinder, int i, boolean z) throws RemoteException;

    void lockSetReLockDelay(IBinder iBinder, int i, long j) throws RemoteException;

    public static abstract class Stub extends Binder implements IGameService {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "com.sonymobile.systemui.gameservice.IGameService");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i != 1598968902) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface("com.sonymobile.systemui.gameservice.IGameService");
                        lockSetReLockDelay(parcel.readStrongBinder(), parcel.readInt(), parcel.readLong());
                        return true;
                    case 2:
                        parcel.enforceInterface("com.sonymobile.systemui.gameservice.IGameService");
                        lockSetEnabled(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt() != 0);
                        return true;
                    case 3:
                        parcel.enforceInterface("com.sonymobile.systemui.gameservice.IGameService");
                        gameSetIcon(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt());
                        return true;
                    case 4:
                        parcel.enforceInterface("com.sonymobile.systemui.gameservice.IGameService");
                        gameSetColoredIcon(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                        return true;
                    case 5:
                        parcel.enforceInterface("com.sonymobile.systemui.gameservice.IGameService");
                        gameUnsetIcon(parcel.readStrongBinder(), parcel.readInt());
                        return true;
                    case 6:
                        parcel.enforceInterface("com.sonymobile.systemui.gameservice.IGameService");
                        gameSetOnGestureListener(parcel.readStrongBinder(), parcel.readInt(), IGameServiceOnGestureListener.Stub.asInterface(parcel.readStrongBinder()));
                        return true;
                    case 7:
                        parcel.enforceInterface("com.sonymobile.systemui.gameservice.IGameService");
                        gameUnsetOnGestureListener(parcel.readStrongBinder(), parcel.readInt());
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            } else {
                parcel2.writeString("com.sonymobile.systemui.gameservice.IGameService");
                return true;
            }
        }
    }
}
