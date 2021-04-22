package com.sonymobile.systemui.lockscreen;

public interface LockscreenLoopsControllerCallback {
    void hide(boolean z);

    void restartClockForDozing();

    void show();

    void stopClockForDozing();
}
