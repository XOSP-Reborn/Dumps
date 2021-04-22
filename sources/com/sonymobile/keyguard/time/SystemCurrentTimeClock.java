package com.sonymobile.keyguard.time;

public class SystemCurrentTimeClock implements Clock {
    @Override // com.sonymobile.keyguard.time.Clock
    public long getTimeInMillis() {
        return System.currentTimeMillis();
    }
}
