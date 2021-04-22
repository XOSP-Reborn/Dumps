package com.sonymobile.keyguard;

import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.keyguard.KeyguardSecurityView;
import com.android.keyguard.KeyguardSecurityViewFlipper;
import com.android.keyguard.KeyguardSimPinView;
import com.android.keyguard.KeyguardSimPukView;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;

public class SomcKeyguardRuntimeResources {
    public static void reload(View view, Resources resources) {
        if (view != null) {
            View findViewById = view.findViewById(C0007R$id.keyguard_host_view);
            updateLayoutBottomMargin(findViewById, resources, C0005R$dimen.somc_keyguard_bouncer_security_bottom_margin);
            View view2 = null;
            if (findViewById != null) {
                View findViewById2 = findViewById.findViewById(C0007R$id.view_flipper);
                if (findViewById2 instanceof KeyguardSecurityViewFlipper) {
                    view2 = getSecurityView((KeyguardSecurityViewFlipper) findViewById2);
                }
            }
            if (view2 != null) {
                View findViewById3 = view2.findViewById(C0007R$id.keyguard_selector_fade_container);
                boolean z = view2 instanceof KeyguardSimPinView;
                if (!z && !(view2 instanceof KeyguardSimPukView)) {
                    updateLayoutBottomMargin(findViewById3, resources, C0005R$dimen.somc_keyguard_emergency_carrier_area_layout_bottom_margin);
                }
                updateVisibility(view2.findViewById(C0007R$id.keyguard_sim), resources, C0014R$string.somc_keyguard_visible_tablet_or_portrait);
                updateVisibility(view2.findViewById(C0007R$id.somc_keyguard_sim_small_icon), resources, C0014R$string.somc_keyguard_visible_phone_landscape);
                if (z || (view2 instanceof KeyguardSimPukView)) {
                    updateMessageArea(view2, resources);
                    if (TelephonyManager.getDefault().getPhoneCount() > 1) {
                        updateVisibility(view2.findViewById(C0007R$id.sim_info_message), resources, C0014R$string.somc_keyguard_visible_tablet_or_portrait);
                    }
                }
            }
        }
    }

    private static View getSecurityView(KeyguardSecurityViewFlipper keyguardSecurityViewFlipper) {
        View childAt = keyguardSecurityViewFlipper.getChildAt(keyguardSecurityViewFlipper.getDisplayedChild());
        if (!(childAt instanceof KeyguardSecurityView)) {
            return null;
        }
        return childAt;
    }

    public static void updateVisibility(View view, Resources resources, int i) {
        int i2;
        if (view != null && resources != null) {
            int i3 = 0;
            try {
                int parseInt = Integer.parseInt(resources.getString(i));
                if (parseInt != 0) {
                    if (parseInt == 1) {
                        i2 = 4;
                    } else if (parseInt != 2) {
                        Log.w("SomcStatusBarKeyguardLoadResources", "Invalid visibility value");
                    } else {
                        i2 = 8;
                    }
                    i3 = i2;
                }
            } catch (NumberFormatException e) {
                Log.w("SomcStatusBarKeyguardLoadResources", "Invalid visibility format", e);
            }
            view.setVisibility(i3);
        }
    }

    private static void updateLayoutBottomMargin(View view, Resources resources, int i) {
        if (view != null && resources != null) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            marginLayoutParams.bottomMargin = resources.getDimensionPixelSize(i);
            view.setLayoutParams(marginLayoutParams);
        }
    }

    private static void updateLayoutTopPadding(View view, Resources resources, int i) {
        if (view != null && resources != null) {
            view.setPadding(view.getPaddingLeft(), resources.getDimensionPixelSize(i), view.getPaddingRight(), view.getPaddingBottom());
        }
    }

    private static void updateMessageArea(View view, Resources resources) {
        TextView textView;
        if (view != null && resources != null && (textView = (TextView) view.findViewById(C0007R$id.keyguard_message_area)) != null) {
            boolean z = resources.getBoolean(C0003R$bool.somc_keyguard_message_area_single_line);
            int integer = resources.getInteger(C0008R$integer.somc_keyguard_message_area_max_lines);
            updateLayoutTopPadding(textView, resources, C0005R$dimen.somc_keyguard_message_area_padding_top);
            textView.setSingleLine(z);
            textView.setLines(integer);
            textView.setMaxLines(integer);
            if (z) {
                textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            } else {
                textView.setEllipsize(null);
            }
        }
    }
}
