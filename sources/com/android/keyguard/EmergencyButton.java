package com.android.keyguard;

import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.telephony.ServiceState;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.widget.LockPatternUtils;
import com.sonymobile.keyguard.SomcKeyguardUtils;

public class EmergencyButton extends Button {
    private static final Intent INTENT_EMERGENCY_DIAL = new Intent().setAction("com.android.phone.EmergencyDialer.DIAL").setPackage("com.android.phone").setFlags(343932928).putExtra("com.android.phone.EmergencyDialer.extra.ENTRY_TYPE", 1);
    private int mDownX;
    private int mDownY;
    private final EmergencyAffordanceManager mEmergencyAffordanceManager;
    private EmergencyButtonCallback mEmergencyButtonCallback;
    private final boolean mEnableEmergencyCallWhileSimLocked;
    KeyguardUpdateMonitorCallback mInfoCallback;
    private final boolean mIsVoiceCapable;
    private LockPatternUtils mLockPatternUtils;
    private boolean mLongPressWasDragged;
    private PowerManager mPowerManager;

    public interface EmergencyButtonCallback {
        void onEmergencyButtonClickedWhenInCall();
    }

    public EmergencyButton(Context context) {
        this(context, null);
    }

    public EmergencyButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.EmergencyButton.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onSimStateChanged(int i, int i2, IccCardConstants.State state) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onPhoneStateChanged(int i) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onServiceStateChanged(int i, ServiceState serviceState) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onKeyguardVisibilityChanged(boolean z) {
                if (z) {
                    EmergencyButton.this.updateEmergencyCallButton();
                }
            }
        };
        this.mIsVoiceCapable = context.getResources().getBoolean(17891589);
        this.mEnableEmergencyCallWhileSimLocked = ((Button) this).mContext.getResources().getBoolean(17891459);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(((Button) this).mContext).registerCallback(this.mInfoCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(((Button) this).mContext).removeCallback(this.mInfoCallback);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(((Button) this).mContext);
        this.mPowerManager = (PowerManager) ((Button) this).mContext.getSystemService("power");
        setOnClickListener(new View.OnClickListener() {
            /* class com.android.keyguard.EmergencyButton.AnonymousClass2 */

            public void onClick(View view) {
                EmergencyButton.this.takeEmergencyCallAction();
            }
        });
        setOnLongClickListener(new View.OnLongClickListener() {
            /* class com.android.keyguard.EmergencyButton.AnonymousClass3 */

            public boolean onLongClick(View view) {
                if (EmergencyButton.this.isInCall() || EmergencyButton.this.mLongPressWasDragged || !EmergencyButton.this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
                    return false;
                }
                EmergencyButton.this.mEmergencyAffordanceManager.performEmergencyCall();
                return true;
            }
        });
        updateEmergencyCallButton();
        SomcKeyguardUtils.limitButtonTextSize(((Button) this).mContext, this);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        if (motionEvent.getActionMasked() == 0) {
            this.mDownX = x;
            this.mDownY = y;
            this.mLongPressWasDragged = false;
        } else {
            int abs = Math.abs(x - this.mDownX);
            int abs2 = Math.abs(y - this.mDownY);
            int scaledTouchSlop = ViewConfiguration.get(((Button) this).mContext).getScaledTouchSlop();
            if (Math.abs(abs2) > scaledTouchSlop || Math.abs(abs) > scaledTouchSlop) {
                this.mLongPressWasDragged = true;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    public boolean performLongClick() {
        return super.performLongClick();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateEmergencyCallButton();
    }

    public void takeEmergencyCallAction() {
        MetricsLogger.action(((Button) this).mContext, 200);
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
        try {
            ActivityTaskManager.getService().stopSystemLockTaskMode();
        } catch (RemoteException unused) {
            Slog.w("EmergencyButton", "Failed to stop app pinning");
        }
        if (isInCall()) {
            resumeCall();
            EmergencyButtonCallback emergencyButtonCallback = this.mEmergencyButtonCallback;
            if (emergencyButtonCallback != null) {
                emergencyButtonCallback.onEmergencyButtonClickedWhenInCall();
                return;
            }
            return;
        }
        KeyguardUpdateMonitor.getInstance(((Button) this).mContext).reportEmergencyCallAction(true);
        getContext().startActivityAsUser(INTENT_EMERGENCY_DIAL, ActivityOptions.makeCustomAnimation(getContext(), 0, 0).toBundle(), new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0054, code lost:
        if (r3.isOOS() == false) goto L_0x005a;
     */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x005c  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0070  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateEmergencyCallButton() {
        /*
        // Method dump skipped, instructions count: 118
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.EmergencyButton.updateEmergencyCallButton():void");
    }

    public void setCallback(EmergencyButtonCallback emergencyButtonCallback) {
        this.mEmergencyButtonCallback = emergencyButtonCallback;
    }

    private void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isInCall() {
        return getTelecommManager().isInCall();
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) ((Button) this).mContext.getSystemService("telecom");
    }
}
