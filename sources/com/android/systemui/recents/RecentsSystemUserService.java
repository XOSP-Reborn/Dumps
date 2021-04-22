package com.android.systemui.recents;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.android.systemui.SysUiServiceProvider;

public class RecentsSystemUserService extends Service {
    public void onCreate() {
        super.onCreate();
    }

    public IBinder onBind(Intent intent) {
        LegacyRecentsImpl legacyRecentsImpl = (LegacyRecentsImpl) SysUiServiceProvider.getComponent(this, LegacyRecentsImpl.class);
        if (legacyRecentsImpl != null) {
            return legacyRecentsImpl.getSystemUserCallbacks();
        }
        return null;
    }
}
