package com.google.android.systemui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

public class AssistManagerGoogle extends AssistManager {
    private final ContentObserver mContentObserver = new AssistantSettingsObserver();
    private final ContentResolver mContentResolver;
    private final AssistantStateReceiver mEnableReceiver = new AssistantStateReceiver();
    private final OpaEnableDispatcher mOpaEnableDispatcher;
    private final KeyguardUpdateMonitorCallback mUserSwitchCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.google.android.systemui.AssistManagerGoogle.AnonymousClass1 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitching(int i) {
            AssistManagerGoogle.this.updateAssistantEnabledState();
            AssistManagerGoogle.this.unregisterSettingsObserver();
            AssistManagerGoogle.this.registerSettingsObserver();
            AssistManagerGoogle.this.unregisterEnableReceiver();
            AssistManagerGoogle.this.registerEnableReceiver(i);
        }
    };

    @Override // com.android.systemui.assist.AssistManager
    public boolean shouldShowOrb() {
        return false;
    }

    public AssistManagerGoogle(DeviceProvisionedController deviceProvisionedController, Context context) {
        super(deviceProvisionedController, context);
        this.mContentResolver = context.getContentResolver();
        this.mOpaEnableDispatcher = new OpaEnableDispatcher(context, this.mAssistUtils);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUserSwitchCallback);
        registerSettingsObserver();
        registerEnableReceiver(-2);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void registerEnableReceiver(int i) {
        this.mContext.registerReceiverAsUser(this.mEnableReceiver, new UserHandle(i), new IntentFilter("com.google.android.systemui.OPA_ENABLED"), null, null);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void unregisterEnableReceiver() {
        this.mContext.unregisterReceiver(this.mEnableReceiver);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateAssistantEnabledState() {
        this.mOpaEnableDispatcher.dispatchOpaEnabled(UserSettingsUtils.load(this.mContentResolver));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void registerSettingsObserver() {
        this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("assistant"), false, this.mContentObserver, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void unregisterSettingsObserver() {
        this.mContentResolver.unregisterContentObserver(this.mContentObserver);
    }

    private class AssistantSettingsObserver extends ContentObserver {
        public AssistantSettingsObserver() {
            super(new Handler());
        }

        public void onChange(boolean z, Uri uri) {
            AssistManagerGoogle.this.updateAssistantEnabledState();
        }
    }
}
