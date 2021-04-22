package com.sonymobile.keyguard.plugininfrastructure;

import android.content.ContentResolver;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public final class KeyguardStatusViewHelper {
    public static ViewGroup loadCurrentClock(Context context, KeyguardPluginFactoryLoader keyguardPluginFactoryLoader, ViewGroup viewGroup, LinearLayout linearLayout, ViewGroup viewGroup2) {
        removeClockPluginView(linearLayout, viewGroup);
        ViewGroup createKeyguardClockView = keyguardPluginFactoryLoader.createKeyguardClockView(linearLayout);
        showCurrentClock(createKeyguardClockView, linearLayout, viewGroup2);
        return createKeyguardClockView;
    }

    public static void startClockTicking(ViewGroup viewGroup) {
        if (viewGroup != null && (viewGroup instanceof ClockPlugin)) {
            ((ClockPlugin) viewGroup).startClockTicking();
        }
    }

    public static void stopClockTicking(ViewGroup viewGroup) {
        if (viewGroup != null && (viewGroup instanceof ClockPlugin)) {
            ((ClockPlugin) viewGroup).stopClockTicking();
        }
    }

    public static KeyguardPluginFactoryLoader createKeyguardPluginFactoryForUser(int i, Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        RealDefaultKeyguardFactoryProvider realDefaultKeyguardFactoryProvider = new RealDefaultKeyguardFactoryProvider(new KeyguardPluginMetaDataLoader(context), new RealCustomizationResourceLoader(context));
        return new KeyguardPluginFactoryLoader(context, realDefaultKeyguardFactoryProvider, new RealClockPluginUserSelectionHandler(new KeyguardPluginMetaDataLoader(context), new RealKeyguardPluginSecureSettingsAbstraction(contentResolver, i), realDefaultKeyguardFactoryProvider));
    }

    public static void setNextAlarm(ViewGroup viewGroup, String str) {
        if (viewGroup != null && (viewGroup instanceof ClockPlugin)) {
            ((ClockPlugin) viewGroup).setNextAlarmText(str);
        }
    }

    private static void showCurrentClock(ViewGroup viewGroup, LinearLayout linearLayout, ViewGroup viewGroup2) {
        if (viewGroup != null) {
            addClockPluginView(linearLayout, viewGroup);
            setVisibilityOnKeyguardStatusViews(viewGroup2, 8);
            return;
        }
        setVisibilityOnKeyguardStatusViews(viewGroup2, 0);
    }

    private static void removeClockPluginView(LinearLayout linearLayout, ViewGroup viewGroup) {
        if (viewGroup != null) {
            stopClockTicking(viewGroup);
            linearLayout.removeView(viewGroup);
        }
    }

    private static void setVisibilityOnKeyguardStatusViews(ViewGroup viewGroup, int i) {
        if (viewGroup != null) {
            viewGroup.setVisibility(i);
        }
    }

    private static void addClockPluginView(LinearLayout linearLayout, ViewGroup viewGroup) {
        if (viewGroup != null) {
            linearLayout.addView(viewGroup, 0);
        }
    }
}
