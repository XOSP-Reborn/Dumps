package com.android.systemui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.settings.MaxBrightnessHeadsUp;
import com.android.systemui.statusbar.phone.SystemUIDialog;

public class MaxBrightnessHeadsUpImpl implements MaxBrightnessHeadsUp {
    private final Context mContext;
    private MaxBrightnessHeadsUp.Listener mListener;

    public MaxBrightnessHeadsUpImpl(Context context) {
        this.mContext = context;
    }

    @Override // com.android.systemui.settings.MaxBrightnessHeadsUp
    public void setOnUserRespondedListener(MaxBrightnessHeadsUp.Listener listener) {
        this.mListener = listener;
    }

    @Override // com.android.systemui.settings.MaxBrightnessHeadsUp
    public void show() {
        SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
        systemUIDialog.setTitle(C0014R$string.oled_max_brightness_dialog_title_txt);
        systemUIDialog.setMessage(C0014R$string.oled_max_brightness_dialog_body_txt);
        systemUIDialog.setPositiveButton(C0014R$string.oled_max_brightness_dialog_yes_button_txt, new DialogInterface.OnClickListener() {
            /* class com.android.systemui.settings.$$Lambda$MaxBrightnessHeadsUpImpl$dIfhRidfEkl16ELGsy7_hXrmM */

            public final void onClick(DialogInterface dialogInterface, int i) {
                MaxBrightnessHeadsUpImpl.this.lambda$show$0$MaxBrightnessHeadsUpImpl(dialogInterface, i);
            }
        });
        systemUIDialog.setNegativeButton(C0014R$string.oled_max_brightness_dialog_no_button_txt, new DialogInterface.OnClickListener() {
            /* class com.android.systemui.settings.$$Lambda$MaxBrightnessHeadsUpImpl$JVKNg00U0S32RAJAFOEYf7qlbyw */

            public final void onClick(DialogInterface dialogInterface, int i) {
                MaxBrightnessHeadsUpImpl.this.lambda$show$1$MaxBrightnessHeadsUpImpl(dialogInterface, i);
            }
        });
        systemUIDialog.show();
    }

    public /* synthetic */ void lambda$show$0$MaxBrightnessHeadsUpImpl(DialogInterface dialogInterface, int i) {
        dispatchUserResponded();
        startSettingsActivity();
    }

    public /* synthetic */ void lambda$show$1$MaxBrightnessHeadsUpImpl(DialogInterface dialogInterface, int i) {
        dispatchUserResponded();
    }

    @Override // com.android.systemui.settings.MaxBrightnessHeadsUp
    public void showWithGameEnhancer() {
        SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
        systemUIDialog.setTitle(C0014R$string.oled_max_brightness_game_dialog_title_txt);
        systemUIDialog.setMessage(C0014R$string.oled_max_brightness_game_dialog_body_txt);
        systemUIDialog.setPositiveButton(C0014R$string.oled_max_brightness_game_dialog_got_it_button_txt, new DialogInterface.OnClickListener() {
            /* class com.android.systemui.settings.$$Lambda$MaxBrightnessHeadsUpImpl$FUXAW48HufRLQqgmYVeUXITKC0 */

            public final void onClick(DialogInterface dialogInterface, int i) {
                MaxBrightnessHeadsUpImpl.this.lambda$showWithGameEnhancer$2$MaxBrightnessHeadsUpImpl(dialogInterface, i);
            }
        });
        systemUIDialog.show();
    }

    public /* synthetic */ void lambda$showWithGameEnhancer$2$MaxBrightnessHeadsUpImpl(DialogInterface dialogInterface, int i) {
        dispatchUserResponded();
    }

    private void dispatchUserResponded() {
        MaxBrightnessHeadsUp.Listener listener = this.mListener;
        if (listener != null) {
            listener.onUserResponded();
        }
    }

    private void startSettingsActivity() {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(new Intent("com.sonymobile.intent.action.ADAPTIVE_BRIGHTNESS_SETTINGS"), 0);
    }
}
