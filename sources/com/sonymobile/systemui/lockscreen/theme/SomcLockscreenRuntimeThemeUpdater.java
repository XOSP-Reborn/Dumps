package com.sonymobile.systemui.lockscreen.theme;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.keyguard.KeyguardSecurityContainer;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0007R$id;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;

public class SomcLockscreenRuntimeThemeUpdater {
    public static void newThemeConfiguration(ViewGroup viewGroup, KeyguardIndicationController keyguardIndicationController, Resources resources) {
        updateOwnerInfoTextColor(viewGroup, resources);
        updateKeyguardBottomAreaResources(viewGroup, keyguardIndicationController, resources);
        updateKeyguardBouncerResources(viewGroup, resources);
        updateKeyguardStatusResources(viewGroup, resources);
        updateKeyguardBarIconsResources(viewGroup, resources);
    }

    private static void updateOwnerInfoTextColor(ViewGroup viewGroup, Resources resources) {
        TextView textView = (TextView) viewGroup.findViewById(C0007R$id.owner_info);
        if (textView != null) {
            textView.setTextColor(resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground));
        }
    }

    private static void updateKeyguardBottomAreaResources(ViewGroup viewGroup, KeyguardIndicationController keyguardIndicationController, Resources resources) {
        View findViewById = viewGroup.findViewById(C0007R$id.keyguard_bottom_area);
        if (findViewById != null) {
            ((KeyguardBottomAreaView) findViewById).updateThemeResources(resources);
        }
        if (keyguardIndicationController != null) {
            keyguardIndicationController.updateThemeColors(resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground));
        }
    }

    private static void updateKeyguardBouncerResources(ViewGroup viewGroup, Resources resources) {
        View findViewById = viewGroup.findViewById(C0007R$id.keyguard_security_container);
        if (findViewById != null && (findViewById instanceof KeyguardSecurityContainer)) {
            ((KeyguardSecurityContainer) findViewById).updateThemeResources(resources);
        }
    }

    private static void updateKeyguardStatusResources(ViewGroup viewGroup, Resources resources) {
        View findViewById = viewGroup.findViewById(C0007R$id.keyguard_status_view);
        if (findViewById != null && (findViewById instanceof KeyguardStatusView)) {
            ((KeyguardStatusView) findViewById).updateSkinnedResources(resources);
        }
    }

    private static void updateKeyguardBarIconsResources(ViewGroup viewGroup, Resources resources) {
        View findViewById = viewGroup.findViewById(C0007R$id.keyguard_header);
        if (findViewById != null && (findViewById instanceof KeyguardStatusBarView)) {
            ((KeyguardStatusBarView) findViewById).updateSkinnedResources(resources);
        }
    }
}
