package com.sonymobile.keyguard.plugin.docomoclock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;

public class DocomoClockPluginFactory extends KeyguardComponentFactory {
    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        return (ViewGroup) LayoutInflater.from(context).inflate(C0010R$layout.docomo_clock_view, viewGroup, false);
    }
}
