package com.sonymobile.keyguard;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.widget.LockPatternView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSimPinView;
import com.android.keyguard.KeyguardSimPukView;
import com.android.keyguard.PasswordTextView;
import com.android.keyguard.SecurityMessageDisplay;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;

public class SomcBouncerRuntimeThemeUpdater {
    private static final int[] MSIM_TEXT_IDS = {C0007R$id.slot_id_name, C0007R$id.sub_display_name};
    private static final int[] NUM_PAD_KEY_IDS = {C0007R$id.key1, C0007R$id.key2, C0007R$id.key3, C0007R$id.key4, C0007R$id.key5, C0007R$id.key6, C0007R$id.key7, C0007R$id.key8, C0007R$id.key9, C0007R$id.key0};
    private static final int[] OPERATION_KEY_IDS = {C0007R$id.delete_button};
    private static final int[] VIEW_HAVING_RIPPLE_IDS = {C0007R$id.key1, C0007R$id.key2, C0007R$id.key3, C0007R$id.key4, C0007R$id.key5, C0007R$id.key6, C0007R$id.key7, C0007R$id.key8, C0007R$id.key9, C0007R$id.key0, C0007R$id.key_enter, C0007R$id.delete_button};

    public static void updateSecurityViewResources(View view, Resources resources) {
        KeyguardSecurityModel.SecurityMode securityModeForView;
        if (resources == null) {
            resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        }
        if (resources != null && (securityModeForView = getSecurityModeForView(view)) != KeyguardSecurityModel.SecurityMode.Invalid) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
            int color2 = resources.getColor(C0004R$color.somc_keyguard_theme_color_secondary_text, null);
            updatePinEntryViews(view, securityModeForView, resources, color);
            updatePatternView(view, securityModeForView, resources);
            updatePasswordView(view, securityModeForView, resources, color);
            updateMessageAreaColor(view, color2);
            updateEmergencyButton(view, resources, color2, color);
        }
    }

    private static KeyguardSecurityModel.SecurityMode getSecurityModeForView(View view) {
        KeyguardSecurityModel.SecurityMode securityMode = KeyguardSecurityModel.SecurityMode.Invalid;
        if (view == null) {
            return securityMode;
        }
        int id = view.getId();
        if (id == C0007R$id.keyguard_pattern_view) {
            return KeyguardSecurityModel.SecurityMode.Pattern;
        }
        if (id == C0007R$id.keyguard_pin_view) {
            return KeyguardSecurityModel.SecurityMode.PIN;
        }
        if (id == C0007R$id.keyguard_password_view) {
            return KeyguardSecurityModel.SecurityMode.Password;
        }
        if (id == C0007R$id.keyguard_sim_pin_view) {
            return KeyguardSecurityModel.SecurityMode.SimPin;
        }
        if (id == C0007R$id.keyguard_sim_puk_view) {
            return KeyguardSecurityModel.SecurityMode.SimPuk;
        }
        return KeyguardSecurityModel.SecurityMode.Invalid;
    }

    private static void updatePinEntryViews(View view, KeyguardSecurityModel.SecurityMode securityMode, Resources resources, int i) {
        if (isPinBasedInputView(securityMode)) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_translucent_foreground, null);
            int color2 = resources.getColor(C0004R$color.somc_keyguard_theme_color_ripple_color, null);
            int color3 = resources.getColor(C0004R$color.somc_keyguard_theme_color_faint_foreground, null);
            updateNumPadKeys(view, i, color);
            updateRipples(view, color2);
            updatePasswordTextColor(view, securityMode, i);
            updateDeleteAndEnterKeyColor(view, i);
            updateDividerColor(view, color3);
            if (isSimPinOrPukView(securityMode)) {
                int color4 = resources.getColor(C0004R$color.somc_keyguard_theme_color_primary_text, null);
                int color5 = resources.getColor(C0004R$color.somc_keyguard_theme_color_secondary_text, null);
                updateSimIconsColor(view, i);
                updateCarrierTextColor(view, color5);
                updateMSimTextColor(view, color4);
            }
        }
    }

    private static void updateNumPadKeys(View view, int i, int i2) {
        int i3 = 0;
        while (true) {
            int[] iArr = NUM_PAD_KEY_IDS;
            if (i3 < iArr.length) {
                View findViewById = view.findViewById(iArr[i3]);
                if (findViewById != null) {
                    TextView textView = (TextView) findViewById.findViewById(C0007R$id.digit_text);
                    if (textView != null) {
                        textView.setTextColor(i);
                    }
                    TextView textView2 = (TextView) findViewById.findViewById(C0007R$id.klondike_text);
                    if (textView2 != null) {
                        textView2.setTextColor(i2);
                    }
                }
                i3++;
            } else {
                return;
            }
        }
    }

    private static void updateRipples(View view, int i) {
        Drawable background;
        int i2 = 0;
        while (true) {
            int[] iArr = VIEW_HAVING_RIPPLE_IDS;
            if (i2 < iArr.length) {
                View findViewById = view.findViewById(iArr[i2]);
                if (!(findViewById == null || (background = findViewById.getBackground()) == null || !(background instanceof RippleDrawable))) {
                    ((RippleDrawable) background).setColor(ColorStateList.valueOf(i));
                }
                i2++;
            } else {
                return;
            }
        }
    }

    private static void updatePasswordTextColor(View view, KeyguardSecurityModel.SecurityMode securityMode, int i) {
        View findViewById = view.findViewById(getPasswordTextViewId(securityMode));
        if (findViewById != null && (findViewById instanceof PasswordTextView)) {
            ((PasswordTextView) findViewById).updateDrawColor(i);
        }
    }

    private static void updateDeleteAndEnterKeyColor(View view, int i) {
        int i2 = 0;
        while (true) {
            int[] iArr = OPERATION_KEY_IDS;
            if (i2 < iArr.length) {
                View findViewById = view.findViewById(iArr[i2]);
                if (findViewById != null && (findViewById instanceof ImageView)) {
                    updateImageTint((ImageView) findViewById, i);
                }
                i2++;
            } else {
                return;
            }
        }
    }

    private static void updateDividerColor(View view, int i) {
        View findViewById = view.findViewById(C0007R$id.divider);
        if (findViewById != null) {
            findViewById.setBackgroundColor(i);
        }
    }

    private static void updateSimIconsColor(View view, int i) {
        if (view instanceof KeyguardSimPinView) {
            ((KeyguardSimPinView) view).updateSimImageColor(i);
        } else if (view instanceof KeyguardSimPukView) {
            ((KeyguardSimPukView) view).updateSimImageColor(i);
        }
        View findViewById = view.findViewById(C0007R$id.somc_keyguard_sim_small_icon);
        if (findViewById != null && (findViewById instanceof ImageView)) {
            updateImageTint((ImageView) findViewById, i);
        }
    }

    private static void updateCarrierTextColor(View view, int i) {
        View findViewById = view.findViewById(C0007R$id.carrier_text);
        if (findViewById != null) {
            ((TextView) findViewById).setTextColor(i);
        }
    }

    private static void updateMSimTextColor(View view, int i) {
        int i2 = 0;
        while (true) {
            int[] iArr = MSIM_TEXT_IDS;
            if (i2 < iArr.length) {
                View findViewById = view.findViewById(iArr[i2]);
                if (findViewById != null && (findViewById instanceof TextView)) {
                    ((TextView) findViewById).setTextColor(i);
                }
                i2++;
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* renamed from: com.sonymobile.keyguard.SomcBouncerRuntimeThemeUpdater$1  reason: invalid class name */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode = new int[KeyguardSecurityModel.SecurityMode.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(8:0|1|2|3|4|5|6|8) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        static {
            /*
                com.android.keyguard.KeyguardSecurityModel$SecurityMode[] r0 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                com.sonymobile.keyguard.SomcBouncerRuntimeThemeUpdater.AnonymousClass1.$SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode = r0
                int[] r0 = com.sonymobile.keyguard.SomcBouncerRuntimeThemeUpdater.AnonymousClass1.$SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.PIN     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = com.sonymobile.keyguard.SomcBouncerRuntimeThemeUpdater.AnonymousClass1.$SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.SimPin     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = com.sonymobile.keyguard.SomcBouncerRuntimeThemeUpdater.AnonymousClass1.$SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.SimPuk     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.keyguard.SomcBouncerRuntimeThemeUpdater.AnonymousClass1.<clinit>():void");
        }
    }

    private static int getPasswordTextViewId(KeyguardSecurityModel.SecurityMode securityMode) {
        int i = AnonymousClass1.$SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode[securityMode.ordinal()];
        if (i == 1) {
            return C0007R$id.pinEntry;
        }
        if (i == 2) {
            return C0007R$id.simPinEntry;
        }
        if (i != 3) {
            return 0;
        }
        return C0007R$id.pukEntry;
    }

    private static boolean isPinBasedInputView(KeyguardSecurityModel.SecurityMode securityMode) {
        return securityMode == KeyguardSecurityModel.SecurityMode.PIN || isSimPinOrPukView(securityMode);
    }

    private static boolean isSimPinOrPukView(KeyguardSecurityModel.SecurityMode securityMode) {
        return securityMode == KeyguardSecurityModel.SecurityMode.SimPin || securityMode == KeyguardSecurityModel.SecurityMode.SimPuk;
    }

    private static void updateMessageAreaColor(View view, int i) {
        View findViewById = view.findViewById(C0007R$id.keyguard_message_area);
        if (findViewById != null && (findViewById instanceof SecurityMessageDisplay)) {
            ((SecurityMessageDisplay) findViewById).setDefaultMessageColor(i);
        }
    }

    private static void updateEmergencyButton(View view, Resources resources, int i, int i2) {
        View findViewById = view.findViewById(C0007R$id.emergency_call_button);
        if (findViewById != null && (findViewById instanceof Button)) {
            Button button = (Button) findViewById;
            Drawable drawable = resources.getDrawable(C0006R$drawable.somc_keyguard_secure_button, null);
            button.setTextColor(i);
            button.setBackground(drawable);
            updateCompoundDrawableTint(button, i2);
        }
    }

    private static void updatePasswordView(View view, KeyguardSecurityModel.SecurityMode securityMode, Resources resources, int i) {
        if (securityMode == KeyguardSecurityModel.SecurityMode.Password) {
            updateExitTextColors(view, resources.getColor(C0004R$color.somc_keyguard_theme_color_primary_text, null), resources.getColor(C0004R$color.somc_keyguard_theme_color_translucent_foreground, null));
            updateImeButtonColor(view, i);
        }
    }

    private static void updateExitTextColors(View view, int i, int i2) {
        View findViewById = view.findViewById(C0007R$id.passwordEntry);
        if (findViewById != null && (findViewById instanceof TextView)) {
            ((TextView) findViewById).setTextColor(i);
            findViewById.getBackground().mutate().setColorFilter(i2, PorterDuff.Mode.SRC_IN);
        }
    }

    private static void updateImeButtonColor(View view, int i) {
        View findViewById = view.findViewById(C0007R$id.switch_ime_button);
        if (findViewById != null && (findViewById instanceof ImageView)) {
            updateImageTint((ImageView) findViewById, i);
        }
    }

    private static void updateImageTint(ImageView imageView, int i) {
        imageView.setImageTintList(ColorStateList.valueOf(i));
        imageView.setImageTintMode(PorterDuff.Mode.SRC_IN);
    }

    private static void updateCompoundDrawableTint(TextView textView, int i) {
        textView.setCompoundDrawableTintList(ColorStateList.valueOf(i));
        textView.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
    }

    private static void updatePatternView(View view, KeyguardSecurityModel.SecurityMode securityMode, Resources resources) {
        LockPatternView findViewById;
        if (securityMode == KeyguardSecurityModel.SecurityMode.Pattern && (findViewById = view.findViewById(C0007R$id.lockPatternView)) != null && (findViewById instanceof LockPatternView)) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_pattern_regular, null);
            findViewById.updateColors(color, resources.getColor(C0004R$color.somc_keyguard_theme_color_pattern_error, null), color);
        }
    }
}
