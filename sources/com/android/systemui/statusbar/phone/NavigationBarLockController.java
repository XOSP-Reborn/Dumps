package com.android.systemui.statusbar.phone;

import android.view.MotionEvent;

public interface NavigationBarLockController {
    void destroy();

    void onAccessibilityButtonState(boolean z);

    void onTouchEvent(MotionEvent motionEvent);

    void setEnabled(boolean z);

    void setNavigationBarView(NavigationBarView navigationBarView);

    void setReLockDelay(long j);

    void unlock();
}
