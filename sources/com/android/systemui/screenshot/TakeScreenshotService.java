package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;

public class TakeScreenshotService extends Service {
    private static GlobalScreenshot mScreenshot;
    private static boolean mWorking;
    private Handler mHandler = new Handler() {
        /* class com.android.systemui.screenshot.TakeScreenshotService.AnonymousClass1 */

        public void handleMessage(Message message) {
            final Messenger messenger = message.replyTo;
            final AnonymousClass1 r1 = new Runnable() {
                /* class com.android.systemui.screenshot.TakeScreenshotService.AnonymousClass1.AnonymousClass1 */

                public void run() {
                    boolean unused = TakeScreenshotService.mWorking = false;
                }
            };
            AnonymousClass2 r2 = new Runnable() {
                /* class com.android.systemui.screenshot.TakeScreenshotService.AnonymousClass1.AnonymousClass2 */

                public void run() {
                    TakeScreenshotService.this.mHandler.removeCallbacks(r1);
                    r1.run();
                    try {
                        messenger.send(Message.obtain((Handler) null, 1));
                    } catch (RemoteException unused) {
                    }
                }
            };
            if (!TakeScreenshotService.mWorking) {
                boolean z = true;
                boolean unused = TakeScreenshotService.mWorking = true;
                TakeScreenshotService.this.mHandler.postDelayed(r1, 10000);
                if (!((UserManager) TakeScreenshotService.this.getSystemService(UserManager.class)).isUserUnlocked()) {
                    Log.w("TakeScreenshotService", "Skipping screenshot because storage is locked!");
                    post(r2);
                    return;
                }
                if (TakeScreenshotService.mScreenshot == null) {
                    GlobalScreenshot unused2 = TakeScreenshotService.mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
                }
                int i = message.what;
                if (i == 1) {
                    GlobalScreenshot globalScreenshot = TakeScreenshotService.mScreenshot;
                    boolean z2 = message.arg1 > 0;
                    if (message.arg2 <= 0) {
                        z = false;
                    }
                    globalScreenshot.takeScreenshot(r2, z2, z);
                } else if (i != 2) {
                    Log.d("TakeScreenshotService", "Invalid screenshot option: " + message.what);
                } else {
                    GlobalScreenshot globalScreenshot2 = TakeScreenshotService.mScreenshot;
                    boolean z3 = message.arg1 > 0;
                    if (message.arg2 <= 0) {
                        z = false;
                    }
                    globalScreenshot2.takeScreenshotPartial(r2, z3, z);
                }
            }
        }
    };

    public IBinder onBind(Intent intent) {
        return new Messenger(this.mHandler).getBinder();
    }

    public boolean onUnbind(Intent intent) {
        GlobalScreenshot globalScreenshot = mScreenshot;
        if (globalScreenshot == null) {
            return true;
        }
        globalScreenshot.stopScreenshot();
        return true;
    }
}
