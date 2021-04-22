package com.sonymobile.keyguard.plugin.sonyclockloops;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;

public class SonyClockLoopsKeyguardComponentFactory extends KeyguardComponentFactory {
    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        return (SonyClockLoops) LayoutInflater.from(context).inflate(C0010R$layout.somc_sony_clock_loops_view, viewGroup, false);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public ViewGroup createKeyguardClockPreviewView(Context context, ViewGroup viewGroup) {
        SonyClockLoops sonyClockLoops = (SonyClockLoops) createKeyguardClockView(context, viewGroup);
        if (sonyClockLoops != null) {
            sonyClockLoops.setPicker(true);
            LinearLayout linearLayout = (LinearLayout) sonyClockLoops.findViewById(C0007R$id.somc_sony_clock_loops_clock_view);
            if (linearLayout != null) {
                float f = context.getResources().getFloat(C0008R$integer.sony_clock_loops_picker_character_scale);
                linearLayout.setScaleX(f);
                linearLayout.setScaleY(f);
            }
        }
        return sonyClockLoops;
    }
}
