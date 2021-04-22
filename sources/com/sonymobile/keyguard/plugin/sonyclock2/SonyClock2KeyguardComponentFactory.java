package com.sonymobile.keyguard.plugin.sonyclock2;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;

public class SonyClock2KeyguardComponentFactory extends KeyguardComponentFactory {
    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public final ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        return (ViewGroup) LayoutInflater.from(context).inflate(C0010R$layout.somc_sony_clock_2_view, viewGroup, false);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public final ViewGroup createKeyguardClockPreviewView(Context context, ViewGroup viewGroup) {
        ViewGroup createKeyguardClockView = createKeyguardClockView(context, viewGroup);
        if (createKeyguardClockView != null) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 17);
            float f = 0.9f;
            Resources resources = context.getResources();
            if (resources != null) {
                layoutParams.setMargins(resources.getDimensionPixelSize(C0005R$dimen.sony_clock_2_clock_picker_left_margin), 0, resources.getDimensionPixelSize(C0005R$dimen.sony_clock_2_clock_picker_right_margin), 0);
                TypedValue typedValue = new TypedValue();
                resources.getValue(C0005R$dimen.sony_clock_2_clock_picker_scale, typedValue, true);
                f = typedValue.getFloat();
            }
            createKeyguardClockView.setLayoutParams(layoutParams);
            createKeyguardClockView.setScaleX(f);
            createKeyguardClockView.setScaleY(f);
        }
        return createKeyguardClockView;
    }
}
