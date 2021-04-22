package com.sonymobile.keyguard.plugin.analogclock;

import android.content.Context;
import android.util.AttributeSet;
import com.sonymobile.keyguard.SomcKeyguardClockScaleContainer;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;

public class AnalogClockContainer extends SomcKeyguardClockScaleContainer implements ClockPlugin {
    public AnalogClockContainer(Context context) {
        this(context, null, 0, 0);
    }

    public AnalogClockContainer(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public AnalogClockContainer(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AnalogClockContainer(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    private ClockPlugin getClockPluginChild() {
        if (getChildCount() > 0) {
            return (ClockPlugin) getChildAt(0);
        }
        return null;
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void setNextAlarmText(String str) {
        ClockPlugin clockPluginChild = getClockPluginChild();
        if (clockPluginChild != null) {
            clockPluginChild.setNextAlarmText(str);
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void startClockTicking() {
        ClockPlugin clockPluginChild = getClockPluginChild();
        if (clockPluginChild != null) {
            clockPluginChild.startClockTicking();
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void stopClockTicking() {
        ClockPlugin clockPluginChild = getClockPluginChild();
        if (clockPluginChild != null) {
            clockPluginChild.stopClockTicking();
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
        ClockPlugin clockPluginChild = getClockPluginChild();
        if (clockPluginChild != null && (clockPluginChild instanceof ClockPlugin)) {
            clockPluginChild.setDoze();
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
        ClockPlugin clockPluginChild = getClockPluginChild();
        if (clockPluginChild != null && (clockPluginChild instanceof ClockPlugin)) {
            clockPluginChild.dozeTimeTick();
        }
    }
}
