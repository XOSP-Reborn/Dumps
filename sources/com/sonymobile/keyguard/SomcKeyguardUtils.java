package com.sonymobile.keyguard;

import android.content.Context;
import android.widget.Button;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0005R$dimen;

public final class SomcKeyguardUtils {
    public static void limitButtonTextSize(Context context, Button button) {
        float dimensionPixelSize = (float) context.getResources().getDimensionPixelSize(C0005R$dimen.somc_kg_status_line_font_max_size);
        boolean z = context.getResources().getBoolean(C0003R$bool.somc_keyguard_shown_on_phone);
        if (button.getTextSize() > dimensionPixelSize && z) {
            button.setTextSize(0, dimensionPixelSize);
        }
    }
}
