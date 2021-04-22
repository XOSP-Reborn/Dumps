package com.sonymobile.runtimeskinning;

import android.os.RemoteException;
import java.lang.Thread;

/* access modifiers changed from: package-private */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Throwable mCause;

    ExceptionHandler() {
    }

    public void uncaughtException(Thread thread, Throwable th) {
        this.mCause = th;
    }

    /* access modifiers changed from: package-private */
    public void reThrow() throws RemoteException {
        reThrow(this.mCause);
    }

    static void reThrow(Throwable th) throws RemoteException {
        if (th == null) {
            return;
        }
        if (th instanceof RemoteException) {
            throw ((RemoteException) th);
        }
        RemoteException remoteException = new RemoteException();
        remoteException.initCause(th);
        throw remoteException;
    }
}
