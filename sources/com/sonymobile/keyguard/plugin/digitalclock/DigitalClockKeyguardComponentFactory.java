package com.sonymobile.keyguard.plugin.digitalclock;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextClock;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;
import java.util.Calendar;

public class DigitalClockKeyguardComponentFactory extends KeyguardComponentFactory {
    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public final ViewGroup createKeyguardClockView(Context context, ViewGroup viewGroup) {
        return (ViewGroup) LayoutInflater.from(context).inflate(C0010R$layout.somc_digital_clock_view, viewGroup, false);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory
    public final ViewGroup createKeyguardClockPreviewView(Context context, ViewGroup viewGroup) {
        ViewGroup createKeyguardClockView = createKeyguardClockView(context, viewGroup);
        scaleTextViewsIfNecessary(createKeyguardClockView, context);
        return createKeyguardClockView;
    }

    private void scaleTextViewsIfNecessary(ViewGroup viewGroup, Context context) {
        TextClock textClock = (TextClock) viewGroup.findViewById(C0007R$id.somc_digital_clock_view_clock);
        TextClock textClock2 = (TextClock) viewGroup.findViewById(C0007R$id.somc_digital_clock_view_am_pm);
        Paint paint = new Paint();
        Rect rect = new Rect();
        Calendar instance = Calendar.getInstance();
        instance.set(11, 23);
        instance.set(12, 47);
        String charSequence = DateFormat.format(context.getResources().getString(C0014R$string.keyguard_widget_24_hours_format), instance).toString();
        paint.setTextSize(textClock.getTextSize());
        paint.getTextBounds(charSequence, 0, charSequence.length(), rect);
        float dimension = context.getResources().getDimension(C0005R$dimen.somc_keyguard_clock_picker_clock_back_plate_width) * 0.9f;
        if (((float) rect.width()) > dimension) {
            float width = dimension / (((float) rect.width()) * 1.2f);
            textClock.setHorizontallyScrolling(true);
            viewGroup.setClipChildren(false);
            viewGroup.setScaleX(width);
            viewGroup.setScaleY(width);
        } else {
            viewGroup.setScaleX(0.8f);
            viewGroup.setScaleY(0.8f);
        }
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(Math.round(((float) rect.width()) * 1.2f), -2, 17);
        Resources resources = context.getResources();
        if (resources != null) {
            layoutParams.setMargins(0, resources.getDimensionPixelSize(C0005R$dimen.sony_digital_clock_picker_top_margin), 0, 0);
        }
        viewGroup.setLayoutParams(layoutParams);
        textClock2.setVisibility(8);
    }
}
