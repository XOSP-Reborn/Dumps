package com.sonymobile.keyguard.aod;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.display.AmbientDisplayConfiguration;
import android.media.MediaMetadata;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.phone.DoubleTapHelper;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.AlarmTimeout;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginFactoryLoader;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardStatusViewHelper;
import java.util.Locale;

public class AodView extends LinearLayout {
    private static int LAYOUT_MOVE_POSITIONS = 3;
    private static long LAYOUT_MOVE_TIMEOUT = 3600000;
    private AlarmManager mAlarmManager;
    private int mAmbientTopBottomMergin;
    private ViewGroup mClockPluginView;
    private LinearLayout mClockView;
    private Context mContext;
    private DoubleTapHelper mDoubleTapHelper;
    private FingerPrintFeedBackView mFingerFBView;
    private boolean mIsAlwaysOnAvailable;
    private boolean mIsDozing;
    private KeyguardPluginFactoryLoader mKeyguardPluginFactoryLoader;
    private int mLayoutMoveCount = 0;
    private onDoubleTapAmbientListener mListener = null;
    private NotificationMediaManager mMediaManager;
    private MusicInfoView mMusicInfoView;
    private NotificationsBatteryView mNotificationsBatteryView;
    private PhotoPlaybackView mPhotoPlaybackView;
    private final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() {
        /* class com.sonymobile.keyguard.aod.AodView.AnonymousClass1 */

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOn() {
            AodView.this.dozeTimeTick();
        }
    };
    private StatusBar mStatusBar;
    private StickerView mStickerView;
    private AlarmTimeout mTimeTicker;
    private KeyguardUpdateMonitorCallback mUpdateMonitor;
    private LinearLayout mViewItems;

    public interface onDoubleTapAmbientListener {
        void onDoubleTap();
    }

    static /* synthetic */ void lambda$new$0(boolean z) {
    }

    public AodView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mKeyguardPluginFactoryLoader = KeyguardStatusViewHelper.createKeyguardPluginFactoryForUser(KeyguardUpdateMonitor.getCurrentUser(), context.getApplicationContext());
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mIsAlwaysOnAvailable = new AmbientDisplayConfiguration(this.mContext).alwaysOnAvailable();
        if (this.mIsAlwaysOnAvailable) {
            this.mTimeTicker = new AlarmTimeout(this.mAlarmManager, new AlarmManager.OnAlarmListener() {
                /* class com.sonymobile.keyguard.aod.$$Lambda$AodView$49Q9MWfXDo0DoKPEl777Y7_BRJg */

                public final void onAlarm() {
                    AodView.this.onLayoutMoveTimeout();
                }
            }, "aod_layout_move", new Handler());
        }
        registerCallbacks(KeyguardUpdateMonitor.getInstance(context));
        ((ScreenLifecycle) Dependency.get(ScreenLifecycle.class)).addObserver(this.mScreenObserver);
        this.mAmbientTopBottomMergin = getResources().getDimensionPixelSize(C0005R$dimen.ambient_top_bottom_margin);
        this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        this.mDoubleTapHelper = new DoubleTapHelper(this, $$Lambda$AodView$f9WBICLDouFUfhQMTK67e_JxNk.INSTANCE, new DoubleTapHelper.DoubleTapListener() {
            /* class com.sonymobile.keyguard.aod.$$Lambda$AodView$vR4FR8KIvj6j8PdUQDkVyrPMktk */

            @Override // com.android.systemui.statusbar.phone.DoubleTapHelper.DoubleTapListener
            public final boolean onDoubleTap() {
                return AodView.this.lambda$new$1$AodView();
            }
        }, null, null);
    }

    public /* synthetic */ boolean lambda$new$1$AodView() {
        this.mListener.onDoubleTap();
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        this.mDoubleTapHelper.onTouchEvent(motionEvent);
        return true;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mViewItems = (LinearLayout) findViewById(C0007R$id.aod_view_items);
        this.mClockView = (LinearLayout) findViewById(C0007R$id.aod_clock_view);
        this.mNotificationsBatteryView = (NotificationsBatteryView) findViewById(C0007R$id.aod_notifications_battery_view);
        this.mMusicInfoView = (MusicInfoView) findViewById(C0007R$id.aod_music_info_view);
        this.mStickerView = (StickerView) findViewById(C0007R$id.aod_sticker_view);
        if (this.mIsAlwaysOnAvailable) {
            this.mTimeTicker.schedule(LAYOUT_MOVE_TIMEOUT, 1);
        }
        this.mFingerFBView = (FingerPrintFeedBackView) findViewById(C0007R$id.aod_fingerprint_feedback_text);
        this.mPhotoPlaybackView = (PhotoPlaybackView) findViewById(C0007R$id.aod_photo_playback_view);
        this.mPhotoPlaybackView.setFFView(this.mFingerFBView);
        this.mPhotoPlaybackView.setMusicInfoAndStickerView(this.mMusicInfoView, this.mStickerView);
    }

    public void setDozing(boolean z) {
        if (this.mIsDozing != z) {
            this.mIsDozing = z;
            if (z) {
                this.mMusicInfoView.setVisibility(8);
                this.mStickerView.setVisibility(4);
                this.mKeyguardPluginFactoryLoader.refreshLoader();
                ViewGroup createKeyguardClockView = this.mKeyguardPluginFactoryLoader.createKeyguardClockView(this.mClockView);
                setGravity();
                setVisibility(0);
                if (createKeyguardClockView != null) {
                    this.mClockView.addView(createKeyguardClockView, 0);
                    this.mClockPluginView = createKeyguardClockView;
                    if (createKeyguardClockView instanceof ClockPlugin) {
                        ClockPlugin clockPlugin = (ClockPlugin) createKeyguardClockView;
                        clockPlugin.setDoze();
                        clockPlugin.startClockTicking();
                        setAlarm();
                    }
                }
                updateMediaMetaData(this.mMediaManager.getMediaMetadata());
                setOnDoubleTapListener(new onDoubleTapAmbientListener() {
                    /* class com.sonymobile.keyguard.aod.$$Lambda$AodView$f3d1WCC5Mo2R1N6BFwlFKrqfi8I */

                    @Override // com.sonymobile.keyguard.aod.AodView.onDoubleTapAmbientListener
                    public final void onDoubleTap() {
                        AodView.this.lambda$setDozing$2$AodView();
                    }
                });
            } else {
                ViewGroup viewGroup = this.mClockPluginView;
                if (viewGroup != null && (viewGroup instanceof ClockPlugin)) {
                    ((ClockPlugin) viewGroup).stopClockTicking();
                    this.mClockView.removeView(this.mClockPluginView);
                    this.mClockPluginView = null;
                }
                setVisibility(4);
            }
            this.mNotificationsBatteryView.setDozing(z);
            this.mStickerView.setDozing(z);
            this.mPhotoPlaybackView.setDozing(z);
        }
    }

    public /* synthetic */ void lambda$setDozing$2$AodView() {
        this.mStatusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), this, "DOUBLE_TAP_TO_AMBIENT");
        onParentDoubleTap();
    }

    public void setNotificationData(NotificationData notificationData) {
        this.mNotificationsBatteryView.setNotificationData(notificationData);
    }

    public void updateMediaMetaData(MediaMetadata mediaMetadata) {
        if (!this.mMusicInfoView.updateMediaMetaData(mediaMetadata)) {
            if (this.mStickerView == null) {
                return;
            }
            if (PhotoPlaybackProviderUtils.isPhotoPlaybackEnabled(this.mContext)) {
                this.mPhotoPlaybackView.setShouldShowMusicInfoOrSticker(false, true);
            } else if (this.mStickerView.getVisibility() != 0) {
                this.mMusicInfoView.setVisibility(8);
                this.mStickerView.setVisibility(0);
            }
        } else if (PhotoPlaybackProviderUtils.isPhotoPlaybackEnabled(this.mContext)) {
            this.mPhotoPlaybackView.setShouldShowMusicInfoOrSticker(true, false);
        } else if (this.mMusicInfoView.getVisibility() != 0) {
            this.mStickerView.setVisibility(8);
            this.mMusicInfoView.setVisibility(0);
        }
    }

    private void setGravity() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2);
        if (this.mIsAlwaysOnAvailable) {
            int i = this.mLayoutMoveCount % LAYOUT_MOVE_POSITIONS;
            if (i == 0) {
                layoutParams.gravity = 17;
            } else if (i != 1) {
                layoutParams.gravity = 80;
                layoutParams.bottomMargin = this.mAmbientTopBottomMergin;
            } else {
                layoutParams.gravity = 48;
                layoutParams.topMargin = this.mAmbientTopBottomMergin;
            }
        } else {
            layoutParams.gravity = 17;
        }
        this.mViewItems.setLayoutParams(layoutParams);
    }

    public Rect getClockViewPosition() {
        int i;
        int i2;
        int i3;
        int i4;
        int[] iArr = new int[2];
        LinearLayout linearLayout = this.mClockView;
        if (linearLayout != null) {
            View findViewById = linearLayout.findViewById(C0007R$id.somc_keyguard_clockplugin_scale_container);
            if (findViewById != null) {
                findViewById.getLocationInWindow(iArr);
                i3 = findViewById.getWidth();
                i4 = findViewById.getHeight();
            } else {
                i4 = 0;
                i3 = 0;
            }
            View findViewWithTag = this.mClockView.findViewWithTag("main_clock");
            if (findViewWithTag != null) {
                findViewWithTag.getLocationInWindow(iArr);
                i2 = findViewWithTag.getWidth();
                i = findViewWithTag.getHeight();
            } else {
                i = i4;
                i2 = i3;
            }
        } else {
            i = 0;
            i2 = 0;
        }
        return new Rect(iArr[0], iArr[1], iArr[0] + i2, iArr[1] + i);
    }

    private void setAlarm() {
        AlarmManager.AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(-2);
        if (nextAlarmClock != null) {
            KeyguardStatusViewHelper.setNextAlarm(this.mClockPluginView, formatNextAlarm(this.mContext, nextAlarmClock));
            return;
        }
        KeyguardStatusViewHelper.setNextAlarm(this.mClockPluginView, "");
    }

    private String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo alarmClockInfo) {
        if (alarmClockInfo == null) {
            return "";
        }
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString();
    }

    public void dozeTimeTick() {
        ViewGroup viewGroup = this.mClockPluginView;
        if (viewGroup != null && this.mIsDozing) {
            ((ClockPlugin) viewGroup).dozeTimeTick();
        }
    }

    /* access modifiers changed from: private */
    public void onLayoutMoveTimeout() {
        this.mLayoutMoveCount++;
        if (this.mLayoutMoveCount >= LAYOUT_MOVE_POSITIONS) {
            this.mLayoutMoveCount = 0;
        }
        setGravity();
        this.mTimeTicker.schedule(LAYOUT_MOVE_TIMEOUT, 2);
    }

    private void onParentDoubleTap() {
        this.mFingerFBView.onParentDoubleTap();
    }

    public void onUpdateNotifications() {
        this.mNotificationsBatteryView.onUpdateNotifications();
    }

    private void registerCallbacks(KeyguardUpdateMonitor keyguardUpdateMonitor) {
        keyguardUpdateMonitor.registerCallback(getUpdateMonitorCallback());
    }

    /* access modifiers changed from: protected */
    public KeyguardUpdateMonitorCallback getUpdateMonitorCallback() {
        if (this.mUpdateMonitor == null) {
            this.mUpdateMonitor = new UserChangeCallBack();
        }
        return this.mUpdateMonitor;
    }

    /* access modifiers changed from: protected */
    public class UserChangeCallBack extends KeyguardUpdateMonitorCallback {
        protected UserChangeCallBack() {
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitchComplete(int i) {
            AodView aodView = AodView.this;
            aodView.mKeyguardPluginFactoryLoader = KeyguardStatusViewHelper.createKeyguardPluginFactoryForUser(i, aodView.mContext.getApplicationContext());
        }
    }

    public void setOnDoubleTapListener(onDoubleTapAmbientListener ondoubletapambientlistener) {
        this.mListener = ondoubletapambientlistener;
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }
}
