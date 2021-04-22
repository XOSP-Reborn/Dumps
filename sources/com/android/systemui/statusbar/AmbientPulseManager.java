package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Resources;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.C0008R$integer;
import com.android.systemui.statusbar.AlertingNotificationManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import java.util.Iterator;

public class AmbientPulseManager extends AlertingNotificationManager {
    @VisibleForTesting
    protected long mExtensionTime;
    protected final ArraySet<OnAmbientChangedListener> mListeners = new ArraySet<>();

    public interface OnAmbientChangedListener {
        void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z);
    }

    @Override // com.android.systemui.statusbar.AlertingNotificationManager
    public int getContentFlag() {
        return 8;
    }

    public AmbientPulseManager(Context context) {
        Resources resources = context.getResources();
        this.mAutoDismissNotificationDecay = resources.getInteger(C0008R$integer.ambient_notification_decay);
        this.mMinimumDisplayTime = resources.getInteger(C0008R$integer.ambient_notification_minimum_time);
        this.mExtensionTime = (long) resources.getInteger(C0008R$integer.ambient_notification_extension_time);
    }

    public void addListener(OnAmbientChangedListener onAmbientChangedListener) {
        this.mListeners.add(onAmbientChangedListener);
    }

    public void extendPulse() {
        AmbientEntry topEntry = getTopEntry();
        if (topEntry != null) {
            topEntry.extendPulse();
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.AlertingNotificationManager
    public void onAlertEntryAdded(AlertingNotificationManager.AlertEntry alertEntry) {
        NotificationEntry notificationEntry = alertEntry.mEntry;
        notificationEntry.setAmbientPulsing(true);
        Iterator<OnAmbientChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAmbientStateChanged(notificationEntry, true);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.AlertingNotificationManager
    public void onAlertEntryRemoved(AlertingNotificationManager.AlertEntry alertEntry) {
        NotificationEntry notificationEntry = alertEntry.mEntry;
        notificationEntry.setAmbientPulsing(false);
        Iterator<OnAmbientChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAmbientStateChanged(notificationEntry, false);
        }
        notificationEntry.freeContentViewWhenSafe(8);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.AlertingNotificationManager
    public AlertingNotificationManager.AlertEntry createAlertEntry() {
        return new AmbientEntry();
    }

    private AmbientEntry getTopEntry() {
        AmbientEntry ambientEntry = null;
        if (this.mAlertEntries.isEmpty()) {
            return null;
        }
        for (AlertingNotificationManager.AlertEntry alertEntry : this.mAlertEntries.values()) {
            if (ambientEntry == null || alertEntry.compareTo((AlertingNotificationManager.AlertEntry) ambientEntry) < 0) {
                ambientEntry = alertEntry;
            }
        }
        return ambientEntry;
    }

    /* access modifiers changed from: private */
    public final class AmbientEntry extends AlertingNotificationManager.AlertEntry {
        private boolean extended;

        private AmbientEntry() {
            super();
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private void extendPulse() {
            if (!this.extended) {
                this.extended = true;
                updateEntry(false);
            }
        }

        @Override // com.android.systemui.statusbar.AlertingNotificationManager.AlertEntry
        public void reset() {
            super.reset();
            this.extended = false;
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.statusbar.AlertingNotificationManager.AlertEntry
        public long calculateFinishTime() {
            return super.calculateFinishTime() + (this.extended ? AmbientPulseManager.this.mExtensionTime : 0);
        }
    }
}
