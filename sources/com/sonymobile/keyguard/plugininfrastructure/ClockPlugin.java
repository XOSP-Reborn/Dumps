package com.sonymobile.keyguard.plugininfrastructure;

public interface ClockPlugin {
    void dozeTimeTick();

    void setDoze();

    void setNextAlarmText(String str);

    void startClockTicking();

    void stopClockTicking();
}
