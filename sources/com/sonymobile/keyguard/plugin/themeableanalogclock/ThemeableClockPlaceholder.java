package com.sonymobile.keyguard.plugin.themeableanalogclock;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.C0007R$id;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;

public class ThemeableClockPlaceholder extends ImageView implements ClockPlugin {
    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setNextAlarmText(String str) {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void startClockTicking() {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void stopClockTicking() {
    }

    public ThemeableClockPlaceholder(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewGroup viewGroup = (ViewGroup) getParent();
        while ((viewGroup.getParent() instanceof ViewGroup) && (viewGroup = (ViewGroup) viewGroup.getParent()) != null && viewGroup.getId() != C0007R$id.keyguard_status_view) {
            if (viewGroup.getId() == C0007R$id.somc_keyguard_clock_picker_view) {
                findViewById(C0007R$id.somc_themeable_analog_clock).setVisibility(0);
                return;
            }
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
        setColorFilter(-1, PorterDuff.Mode.SRC_IN);
        invalidate();
    }
}
