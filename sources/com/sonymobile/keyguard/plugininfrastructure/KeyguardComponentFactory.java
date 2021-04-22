package com.sonymobile.keyguard.plugininfrastructure;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class KeyguardComponentFactory {
    public ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        return null;
    }

    public ViewGroup createKeyguardClockPreviewView(Context context, ViewGroup viewGroup) {
        ViewGroup createKeyguardClockView = createKeyguardClockView(context, viewGroup);
        if (createKeyguardClockView != null) {
            createKeyguardClockView.setScaleX(0.9f);
            createKeyguardClockView.setScaleY(0.9f);
            createKeyguardClockView.setLayoutParams(new FrameLayout.LayoutParams(-2, -2, 17));
        }
        return createKeyguardClockView;
    }
}
