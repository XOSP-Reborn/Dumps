package com.sonymobile.keyguard.clock.picker;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactoryEntry;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;

public class ClockItem extends LinearLayout {
    private KeyguardComponentFactoryEntry mClock;

    public ClockItem(Context context) {
        super(context, null);
    }

    public ClockItem(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ClockItem(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public final void initWithClock(KeyguardComponentFactoryEntry keyguardComponentFactoryEntry, ClockPickerController clockPickerController) {
        this.mClock = keyguardComponentFactoryEntry;
        initView(clockPickerController);
    }

    private void initView(ClockPickerController clockPickerController) {
        FrameLayout frameLayout = (FrameLayout) findViewById(C0007R$id.somc_keyguard_clock_back_plate);
        TextView textView = (TextView) findViewById(C0007R$id.somc_keyguard_clock_name);
        View createClockView = clockPickerController.createClockView(this.mClock);
        if (createClockView != null && frameLayout != null && textView != null) {
            frameLayout.setClipChildren(false);
            frameLayout.addView(createClockView);
            textView.setText(this.mClock.getName());
            initThemeColors(frameLayout, textView);
        }
    }

    public final KeyguardComponentFactoryEntry getClock() {
        return this.mClock;
    }

    public final int getPageWidthInPixels() {
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        return getBackPlateSize() + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin;
    }

    private int getBackPlateSize() {
        return ((ViewGroup.MarginLayoutParams) findViewById(C0007R$id.somc_keyguard_clock_back_plate).getLayoutParams()).width;
    }

    private void initThemeColors(FrameLayout frameLayout, TextView textView) {
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            frameLayout.setBackgroundColor(resources.getColor(C0004R$color.somc_keyguard_theme_color_clock_picker_back_plate, null));
            textView.setTextColor(resources.getColor(C0004R$color.somc_keyguard_theme_color_primary_text, null));
        }
    }
}
