package com.android.systemui.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Handler;
import android.os.Trace;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmControllerImpl;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import com.android.systemui.util.wakelock.SettableWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class KeyguardSliceProvider extends SliceProvider implements NextAlarmController.NextAlarmChangeCallback, ZenModeController.Callback, NotificationMediaManager.MediaListener, StatusBarStateController.StateListener {
    @VisibleForTesting
    static final int ALARM_VISIBILITY_HOURS = 12;
    private static final StyleSpan BOLD_STYLE = new StyleSpan(1);
    private static KeyguardSliceProvider sInstance;
    @VisibleForTesting
    protected AlarmManager mAlarmManager;
    protected final Uri mAlarmUri;
    @VisibleForTesting
    protected ContentResolver mContentResolver;
    private final Date mCurrentTime;
    private DateFormat mDateFormat;
    private String mDatePattern;
    protected final Uri mDateUri;
    protected final Uri mDndUri;
    protected boolean mDozing;
    private final Handler mHandler;
    protected final Uri mHeaderUri;
    @VisibleForTesting
    final BroadcastReceiver mIntentReceiver;
    @VisibleForTesting
    final KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback;
    private String mLastText;
    private CharSequence mMediaArtist;
    private final HashSet<Integer> mMediaInvisibleStates;
    private boolean mMediaIsVisible;
    protected NotificationMediaManager mMediaManager;
    private CharSequence mMediaTitle;
    private final Object mMediaToken;
    protected final Uri mMediaUri;
    @VisibleForTesting
    protected SettableWakeLock mMediaWakeLock;
    private String mNextAlarm;
    private NextAlarmController mNextAlarmController;
    private AlarmManager.AlarmClockInfo mNextAlarmInfo;
    private PendingIntent mPendingIntent;
    private boolean mRegistered;
    protected final Uri mSliceUri;
    private int mStatusBarState;
    private StatusBarStateController mStatusBarStateController;
    private final AlarmManager.OnAlarmListener mUpdateNextAlarm;
    @VisibleForTesting
    protected ZenModeController mZenModeController;

    public KeyguardSliceProvider() {
        this(new Handler());
    }

    public static KeyguardSliceProvider getAttachedInstance() {
        return sInstance;
    }

    @VisibleForTesting
    KeyguardSliceProvider(Handler handler) {
        this.mCurrentTime = new Date();
        this.mUpdateNextAlarm = new AlarmManager.OnAlarmListener() {
            /* class com.android.systemui.keyguard.$$Lambda$KeyguardSliceProvider$IhzByd8TsqFuOrSyuGurVskyPLo */

            public final void onAlarm() {
                KeyguardSliceProvider.this.updateNextAlarm();
            }
        };
        this.mMediaToken = new Object();
        this.mIntentReceiver = new BroadcastReceiver() {
            /* class com.android.systemui.keyguard.KeyguardSliceProvider.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.DATE_CHANGED".equals(action)) {
                    synchronized (this) {
                        KeyguardSliceProvider.this.updateClockLocked();
                    }
                } else if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
                    synchronized (this) {
                        KeyguardSliceProvider.this.cleanDateFormatLocked();
                    }
                }
            }
        };
        this.mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.systemui.keyguard.KeyguardSliceProvider.AnonymousClass2 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onTimeChanged() {
                synchronized (this) {
                    KeyguardSliceProvider.this.updateClockLocked();
                }
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onTimeZoneChanged(TimeZone timeZone) {
                synchronized (this) {
                    KeyguardSliceProvider.this.cleanDateFormatLocked();
                }
            }
        };
        this.mHandler = handler;
        this.mSliceUri = Uri.parse("content://com.android.systemui.keyguard/main");
        this.mHeaderUri = Uri.parse("content://com.android.systemui.keyguard/header");
        this.mDateUri = Uri.parse("content://com.android.systemui.keyguard/date");
        this.mAlarmUri = Uri.parse("content://com.android.systemui.keyguard/alarm");
        this.mDndUri = Uri.parse("content://com.android.systemui.keyguard/dnd");
        this.mMediaUri = Uri.parse("content://com.android.systemui.keyguard/media");
        this.mMediaInvisibleStates = new HashSet<>();
        this.mMediaInvisibleStates.add(0);
        this.mMediaInvisibleStates.add(1);
        this.mMediaInvisibleStates.add(2);
    }

    public void initDependencies(NotificationMediaManager notificationMediaManager, StatusBarStateController statusBarStateController) {
        this.mMediaManager = notificationMediaManager;
        this.mMediaManager.addCallback(this);
        this.mStatusBarStateController = statusBarStateController;
        this.mStatusBarStateController.addCallback(this);
    }

    @Override // androidx.slice.SliceProvider
    public Slice onBindSlice(Uri uri) {
        Slice build;
        Trace.beginSection("KeyguardSliceProvider#onBindSlice");
        synchronized (this) {
            ListBuilder listBuilder = new ListBuilder(getContext(), this.mSliceUri, -1);
            if (needsMediaLocked()) {
                addMediaLocked(listBuilder);
            } else {
                ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder(this.mDateUri);
                rowBuilder.setTitle(this.mLastText);
                listBuilder.addRow(rowBuilder);
            }
            addNextAlarmLocked(listBuilder);
            addZenModeLocked(listBuilder);
            addPrimaryActionLocked(listBuilder);
            build = listBuilder.build();
        }
        Trace.endSection();
        return build;
    }

    /* access modifiers changed from: protected */
    public boolean needsMediaLocked() {
        return !TextUtils.isEmpty(this.mMediaTitle) && this.mMediaIsVisible && (this.mDozing || (this.mStatusBarState == 0 && this.mMediaIsVisible));
    }

    /* access modifiers changed from: protected */
    public void addMediaLocked(ListBuilder listBuilder) {
        if (!TextUtils.isEmpty(this.mMediaTitle)) {
            ListBuilder.HeaderBuilder headerBuilder = new ListBuilder.HeaderBuilder(this.mHeaderUri);
            headerBuilder.setTitle(this.mMediaTitle);
            listBuilder.setHeader(headerBuilder);
            if (!TextUtils.isEmpty(this.mMediaArtist)) {
                ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder(this.mMediaUri);
                rowBuilder.setTitle(this.mMediaArtist);
                NotificationMediaManager notificationMediaManager = this.mMediaManager;
                IconCompat iconCompat = null;
                Icon mediaIcon = notificationMediaManager == null ? null : notificationMediaManager.getMediaIcon();
                if (mediaIcon != null) {
                    iconCompat = IconCompat.createFromIcon(getContext(), mediaIcon);
                }
                if (iconCompat != null) {
                    rowBuilder.addEndItem(iconCompat, 0);
                }
                listBuilder.addRow(rowBuilder);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void addPrimaryActionLocked(ListBuilder listBuilder) {
        SliceAction createDeeplink = SliceAction.createDeeplink(this.mPendingIntent, IconCompat.createWithResource(getContext(), C0006R$drawable.ic_access_alarms_big), 0, this.mLastText);
        ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder(Uri.parse("content://com.android.systemui.keyguard/action"));
        rowBuilder.setPrimaryAction(createDeeplink);
        listBuilder.addRow(rowBuilder);
    }

    /* access modifiers changed from: protected */
    public void addNextAlarmLocked(ListBuilder listBuilder) {
        if (!TextUtils.isEmpty(this.mNextAlarm)) {
            IconCompat createWithResource = IconCompat.createWithResource(getContext(), C0006R$drawable.ic_access_alarms_big);
            ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder(this.mAlarmUri);
            rowBuilder.setTitle(this.mNextAlarm);
            rowBuilder.addEndItem(createWithResource, 0);
            listBuilder.addRow(rowBuilder);
        }
    }

    /* access modifiers changed from: protected */
    public void addZenModeLocked(ListBuilder listBuilder) {
        if (isDndOn()) {
            ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder(this.mDndUri);
            rowBuilder.setContentDescription(getContext().getResources().getString(C0014R$string.accessibility_quick_settings_dnd));
            rowBuilder.addEndItem(IconCompat.createWithResource(getContext(), C0006R$drawable.stat_sys_dnd), 0);
            listBuilder.addRow(rowBuilder);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isDndOn() {
        return this.mZenModeController.getZen() != 0;
    }

    @Override // androidx.slice.SliceProvider
    public boolean onCreateSliceProvider() {
        synchronized (this) {
            KeyguardSliceProvider keyguardSliceProvider = sInstance;
            if (keyguardSliceProvider != null) {
                keyguardSliceProvider.onDestroy();
            }
            this.mAlarmManager = (AlarmManager) getContext().getSystemService(AlarmManager.class);
            this.mContentResolver = getContext().getContentResolver();
            this.mNextAlarmController = new NextAlarmControllerImpl(getContext());
            this.mNextAlarmController.addCallback(this);
            this.mZenModeController = new ZenModeControllerImpl(getContext(), this.mHandler);
            this.mZenModeController.addCallback(this);
            this.mDatePattern = getContext().getString(C0014R$string.system_ui_aod_date_pattern);
            this.mPendingIntent = PendingIntent.getActivity(getContext(), 0, new Intent(), 0);
            this.mMediaWakeLock = new SettableWakeLock(WakeLock.createPartial(getContext(), "media"), "media");
            sInstance = this;
            registerClockUpdate();
            updateClockLocked();
        }
        return true;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void onDestroy() {
        synchronized (this) {
            this.mNextAlarmController.removeCallback(this);
            this.mZenModeController.removeCallback(this);
            this.mMediaWakeLock.setAcquired(false);
            this.mAlarmManager.cancel(this.mUpdateNextAlarm);
            if (this.mRegistered) {
                this.mRegistered = false;
                getKeyguardUpdateMonitor().removeCallback(this.mKeyguardUpdateMonitorCallback);
                getContext().unregisterReceiver(this.mIntentReceiver);
            }
        }
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onZenChanged(int i) {
        notifyChange();
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onConfigChanged(ZenModeConfig zenModeConfig) {
        notifyChange();
    }

    /* access modifiers changed from: private */
    public void updateNextAlarm() {
        synchronized (this) {
            if (withinNHoursLocked(this.mNextAlarmInfo, 12)) {
                this.mNextAlarm = android.text.format.DateFormat.format(android.text.format.DateFormat.is24HourFormat(getContext(), ActivityManager.getCurrentUser()) ? "HH:mm" : "h:mm", this.mNextAlarmInfo.getTriggerTime()).toString();
            } else {
                this.mNextAlarm = "";
            }
        }
        notifyChange();
    }

    private boolean withinNHoursLocked(AlarmManager.AlarmClockInfo alarmClockInfo, int i) {
        if (alarmClockInfo == null) {
            return false;
        }
        if (this.mNextAlarmInfo.getTriggerTime() <= System.currentTimeMillis() + TimeUnit.HOURS.toMillis((long) i)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void registerClockUpdate() {
        synchronized (this) {
            if (!this.mRegistered) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.DATE_CHANGED");
                intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
                getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, null);
                getKeyguardUpdateMonitor().registerCallback(this.mKeyguardUpdateMonitorCallback);
                this.mRegistered = true;
            }
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean isRegistered() {
        boolean z;
        synchronized (this) {
            z = this.mRegistered;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public void updateClockLocked() {
        String formattedDateLocked = getFormattedDateLocked();
        if (!formattedDateLocked.equals(this.mLastText)) {
            this.mLastText = formattedDateLocked;
            notifyChange();
        }
    }

    /* access modifiers changed from: protected */
    public String getFormattedDateLocked() {
        if (this.mDateFormat == null) {
            DateFormat instanceForSkeleton = DateFormat.getInstanceForSkeleton(this.mDatePattern, Locale.getDefault());
            instanceForSkeleton.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
            this.mDateFormat = instanceForSkeleton;
        }
        this.mCurrentTime.setTime(System.currentTimeMillis());
        return this.mDateFormat.format(this.mCurrentTime);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void cleanDateFormatLocked() {
        this.mDateFormat = null;
    }

    @Override // com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo alarmClockInfo) {
        long triggerTime;
        synchronized (this) {
            this.mNextAlarmInfo = alarmClockInfo;
            this.mAlarmManager.cancel(this.mUpdateNextAlarm);
            if (this.mNextAlarmInfo == null) {
                triggerTime = -1;
            } else {
                triggerTime = this.mNextAlarmInfo.getTriggerTime() - TimeUnit.HOURS.toMillis(12);
            }
            if (triggerTime > 0) {
                this.mAlarmManager.setExact(1, triggerTime, "lock_screen_next_alarm", this.mUpdateNextAlarm, this.mHandler);
            }
        }
        updateNextAlarm();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public KeyguardUpdateMonitor getKeyguardUpdateMonitor() {
        return KeyguardUpdateMonitor.getInstance(getContext());
    }

    @Override // com.android.systemui.statusbar.NotificationMediaManager.MediaListener
    public void onMetadataOrStateChanged(MediaMetadata mediaMetadata, int i) {
        synchronized (this) {
            boolean z = !this.mMediaInvisibleStates.contains(Integer.valueOf(i));
            this.mHandler.removeCallbacksAndMessages(this.mMediaToken);
            if (!this.mMediaIsVisible || z || this.mStatusBarState == 0) {
                this.mMediaWakeLock.setAcquired(false);
                updateMediaStateLocked(mediaMetadata, i);
            } else {
                this.mMediaWakeLock.setAcquired(true);
                this.mHandler.postDelayed(new Runnable(mediaMetadata, i) {
                    /* class com.android.systemui.keyguard.$$Lambda$KeyguardSliceProvider$nRbfFxAPvCUbdEsypLUXXuYm6z0 */
                    private final /* synthetic */ MediaMetadata f$1;
                    private final /* synthetic */ int f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        KeyguardSliceProvider.this.lambda$onMetadataOrStateChanged$0$KeyguardSliceProvider(this.f$1, this.f$2);
                    }
                }, this.mMediaToken, 2000);
            }
        }
    }

    public /* synthetic */ void lambda$onMetadataOrStateChanged$0$KeyguardSliceProvider(MediaMetadata mediaMetadata, int i) {
        updateMediaStateLocked(mediaMetadata, i);
        this.mMediaWakeLock.setAcquired(false);
    }

    private void updateMediaStateLocked(MediaMetadata mediaMetadata, int i) {
        CharSequence charSequence;
        boolean z = !this.mMediaInvisibleStates.contains(Integer.valueOf(i));
        CharSequence charSequence2 = null;
        if (mediaMetadata != null) {
            charSequence = mediaMetadata.getText("android.media.metadata.TITLE");
            if (TextUtils.isEmpty(charSequence)) {
                charSequence = getContext().getResources().getString(C0014R$string.music_controls_no_title);
            }
        } else {
            charSequence = null;
        }
        if (mediaMetadata != null) {
            charSequence2 = mediaMetadata.getText("android.media.metadata.ARTIST");
        }
        if (z != this.mMediaIsVisible || !TextUtils.equals(charSequence, this.mMediaTitle) || !TextUtils.equals(charSequence2, this.mMediaArtist)) {
            this.mMediaTitle = charSequence;
            this.mMediaArtist = charSequence2;
            this.mMediaIsVisible = z;
            notifyChange();
        }
    }

    /* access modifiers changed from: protected */
    public void notifyChange() {
        this.mContentResolver.notifyChange(this.mSliceUri, null);
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozingChanged(boolean z) {
        boolean z2;
        synchronized (this) {
            boolean needsMediaLocked = needsMediaLocked();
            this.mDozing = z;
            z2 = needsMediaLocked != needsMediaLocked();
        }
        if (z2) {
            notifyChange();
        }
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        boolean z;
        synchronized (this) {
            boolean needsMediaLocked = needsMediaLocked();
            this.mStatusBarState = i;
            z = needsMediaLocked != needsMediaLocked();
        }
        if (z) {
            notifyChange();
        }
    }
}
