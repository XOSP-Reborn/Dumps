package com.sonymobile.keyguard.plugininfrastructure;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import com.android.keyguard.KeyguardUpdateMonitor;
import java.lang.ref.WeakReference;

public class KeyguardPluginChangedObserver extends ContentObserver {
    private final WeakReference<KeyguardUpdateMonitor> mWeakKeyguardUpdateMonitor;

    public KeyguardPluginChangedObserver(Handler handler, KeyguardUpdateMonitor keyguardUpdateMonitor) {
        super(handler);
        this.mWeakKeyguardUpdateMonitor = new WeakReference<>(keyguardUpdateMonitor);
    }

    public final void registerForUser(Context context, int i) {
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null) {
            RealKeyguardPluginSecureSettingsAbstraction realKeyguardPluginSecureSettingsAbstraction = new RealKeyguardPluginSecureSettingsAbstraction(contentResolver, i);
            contentResolver.unregisterContentObserver(this);
            contentResolver.registerContentObserver(realKeyguardPluginSecureSettingsAbstraction.getExplicitlySelectedKeyguardPluginValueUri(), false, this, i);
        }
    }

    public final void onChange(boolean z) {
        KeyguardUpdateMonitor keyguardUpdateMonitor;
        if (!z && (keyguardUpdateMonitor = this.mWeakKeyguardUpdateMonitor.get()) != null) {
            keyguardUpdateMonitor.onUserClockChanged();
        }
    }

    public final void onChange(boolean z, Uri uri) {
        onChange(z);
    }
}
