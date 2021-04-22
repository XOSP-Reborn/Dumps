package com.sonymobile.keyguard.plugin.sonyclock;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;

public class SonyClockKeyguardComponentFactory extends KeyguardComponentFactory {
    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public final ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        return (ViewGroup) LayoutInflater.from(context).inflate(C0010R$layout.somc_sony_clock_view, viewGroup, false);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public final ViewGroup createKeyguardClockPreviewView(Context context, ViewGroup viewGroup) {
        ViewGroup createKeyguardClockView = createKeyguardClockView(context, viewGroup);
        if (createKeyguardClockView != null) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 17);
            Resources resources = context.getResources();
            if (resources != null) {
                layoutParams.setMargins(0, resources.getDimensionPixelSize(C0005R$dimen.sony_clock_clock_picker_top_margin), 0, resources.getDimensionPixelSize(C0005R$dimen.sony_clock_clock_picker_bottom_margin));
            }
            createKeyguardClockView.setLayoutParams(layoutParams);
            createKeyguardClockView.setScaleX(0.9f);
            createKeyguardClockView.setScaleY(0.9f);
        }
        return createKeyguardClockView;
    }
}
