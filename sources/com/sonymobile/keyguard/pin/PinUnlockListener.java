package com.sonymobile.keyguard.pin;

public interface PinUnlockListener {
    void onUnlockFailed();

    void onUnlockSucceded(int i);
}
