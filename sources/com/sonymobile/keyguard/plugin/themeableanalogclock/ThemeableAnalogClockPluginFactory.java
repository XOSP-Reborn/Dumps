package com.sonymobile.keyguard.plugin.themeableanalogclock;

import android.content.Context;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;
import com.sonymobile.runtimeskinning.SkinningBridge;

public class ThemeableAnalogClockPluginFactory extends KeyguardComponentFactory {
    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        ViewGroup clockView = getClockView(context, viewGroup);
        SkinningBridgeClockLayout skinningBridgeClockLayout = getSkinningBridgeClockLayout(clockView);
        skinningBridgeClockLayout.setPlace("lockscreenClock");
        Size lockScreenClockDimens = SkinningBridge.getLockScreenClockDimens();
        skinningBridgeClockLayout.getLayoutParams().width = lockScreenClockDimens.getWidth();
        skinningBridgeClockLayout.getLayoutParams().height = lockScreenClockDimens.getHeight();
        return clockView;
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public ViewGroup createKeyguardClockPreviewView(Context context, ViewGroup viewGroup) {
        ViewGroup clockView = getClockView(context, viewGroup);
        getSkinningBridgeClockLayout(clockView).setPlace("lockscreenClockPicker");
        return clockView;
    }

    private ViewGroup getClockView(Context context, ViewGroup viewGroup) {
        return (ViewGroup) LayoutInflater.from(context).inflate(C0010R$layout.somc_themeable_analog_clock_view, viewGroup, false);
    }

    private SkinningBridgeClockLayout getSkinningBridgeClockLayout(ViewGroup viewGroup) {
        return (SkinningBridgeClockLayout) viewGroup.findViewById(C0007R$id.somc_themeable_analog_clock);
    }
}
