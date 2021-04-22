package com.sonymobile.keyguard.plugin.analogclock;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;

public class AnalogClockPluginFactory extends KeyguardComponentFactory {
    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        return (ViewGroup) LayoutInflater.from(context).inflate(C0010R$layout.somc_analog_clock_view, viewGroup, false);
    }

    /* access modifiers changed from: protected */
    public View getClockView(ViewGroup viewGroup) {
        return viewGroup.findViewById(C0007R$id.somc_analog_clock);
    }

    /* access modifiers changed from: protected */
    public float getClockSize(Resources resources) {
        return resources.getDimension(C0005R$dimen.somc_analog_clock_size);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public ViewGroup createKeyguardClockPreviewView(Context context, ViewGroup viewGroup) {
        ViewGroup createKeyguardClockView = createKeyguardClockView(context, viewGroup);
        Resources resources = context.getResources();
        if (!(createKeyguardClockView == null || resources == null)) {
            View clockView = getClockView(createKeyguardClockView);
            float clockSize = getClockSize(resources);
            float dimension = resources.getDimension(C0005R$dimen.somc_keyguard_clock_picker_clock_back_plate_width) * 0.9f;
            float f = dimension / clockSize;
            if (f < 1.0f) {
                clockView.setScaleX(f);
                clockView.setScaleY(f);
                clockSize = dimension;
            }
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(Math.round(clockSize), Math.round(clockSize), 17);
            layoutParams.setMargins(0, 0, 0, 0);
            createKeyguardClockView.setLayoutParams(layoutParams);
        }
        return createKeyguardClockView;
    }
}
