package com.sonymobile.systemui.globalactions;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.SysUIToast;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class EnableAccessibilityController {
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(11).build();
    private final AccessibilityManager mAccessibilityManager;
    private boolean mCanceled;
    private final Context mContext;
    private boolean mDestroyed;
    private float mFirstPointerDownX;
    private float mFirstPointerDownY;
    private final Handler mHandler = new Handler() {
        /* class com.sonymobile.systemui.globalactions.EnableAccessibilityController.AnonymousClass1 */

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                EnableAccessibilityController.this.mTts.speak(EnableAccessibilityController.this.mContext.getString(C0014R$string.continue_to_enable_accessibility), 0, null);
            } else if (i == 2) {
                EnableAccessibilityController.this.mTts.speak(EnableAccessibilityController.this.mContext.getString(C0014R$string.enable_accessibility_canceled), 0, null);
            } else if (i == 3) {
                EnableAccessibilityController.this.performAccessibilityShortcut();
                EnableAccessibilityController.this.mTone.play();
                EnableAccessibilityController.this.vibrate();
                EnableAccessibilityController.this.mTts.speak(EnableAccessibilityController.this.mContext.getString(C0014R$string.accessibility_enabled), 0, null);
            }
        }
    };
    private final Runnable mOnAccessibilityShortcutCallback;
    private float mSecondPointerDownX;
    private float mSecondPointerDownY;
    private final Ringtone mTone;
    private final float mTouchSlop;
    private final TextToSpeech mTts;

    public EnableAccessibilityController(Context context, Runnable runnable) {
        this.mContext = context;
        this.mAccessibilityManager = AccessibilityManager.getInstance(context);
        this.mOnAccessibilityShortcutCallback = runnable;
        this.mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            /* class com.sonymobile.systemui.globalactions.EnableAccessibilityController.AnonymousClass2 */

            public void onInit(int i) {
                if (EnableAccessibilityController.this.mDestroyed) {
                    EnableAccessibilityController.this.mTts.shutdown();
                }
            }
        });
        this.mTone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_NOTIFICATION_URI);
        this.mTone.setAudioAttributes(new AudioAttributes.Builder().setUsage(10).build());
        this.mTouchSlop = (float) context.getResources().getDimensionPixelSize(17104905);
    }

    public static boolean canEnableAccessibilityViaGesture(Context context) {
        if (!AccessibilityUtils.isAccessibilityShortcutGestureEnabled(context)) {
            return false;
        }
        AccessibilityManager instance = AccessibilityManager.getInstance(context);
        if (instance.isEnabled() && !instance.getEnabledAccessibilityServiceList(-1).isEmpty()) {
            return false;
        }
        int currentUser = ActivityManager.getCurrentUser();
        KeyguardMonitor keyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        boolean z = Settings.Secure.getIntForUser(context.getContentResolver(), "accessibility_shortcut_on_lock_screen", 0, currentUser) == 1;
        if (!AccessibilityUtils.isShortcutEnabled(context, currentUser) || getInfoForTargetService(context) == null) {
            return false;
        }
        if (!keyguardMonitor.isShowing() || z) {
            return true;
        }
        return false;
    }

    public void onDestroy() {
        this.mDestroyed = true;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() != 5 || motionEvent.getPointerCount() != 2) {
            return false;
        }
        this.mFirstPointerDownX = motionEvent.getX(0);
        this.mFirstPointerDownY = motionEvent.getY(0);
        this.mSecondPointerDownX = motionEvent.getX(1);
        this.mSecondPointerDownY = motionEvent.getY(1);
        this.mHandler.sendEmptyMessageDelayed(1, 2000);
        this.mHandler.sendEmptyMessageDelayed(3, 6000);
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x001d, code lost:
        if (r1 != 6) goto L_0x0064;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(android.view.MotionEvent r6) {
        /*
        // Method dump skipped, instructions count: 101
        */
        throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.systemui.globalactions.EnableAccessibilityController.onTouchEvent(android.view.MotionEvent):boolean");
    }

    private void cancel() {
        this.mCanceled = true;
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
        } else if (this.mHandler.hasMessages(3)) {
            this.mHandler.sendEmptyMessage(2);
        }
        this.mHandler.removeMessages(3);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void vibrate() {
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(getLongIntArray(this.mContext.getResources(), 17236044), -1, VIBRATION_ATTRIBUTES);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void performAccessibilityShortcut() {
        AccessibilityServiceInfo infoForTargetService = getInfoForTargetService(this.mContext);
        if (infoForTargetService == null) {
            Log.e("EnableAccessibilityController", "Accessibility shortcut set to invalid service");
            return;
        }
        SysUIToast.makeText(this.mContext, String.format(this.mContext.getString(isServiceEnabled(infoForTargetService) ? 17039447 : 17039448), infoForTargetService.getResolveInfo().loadLabel(this.mContext.getPackageManager()).toString()), 1).show();
        this.mAccessibilityManager.performAccessibilityShortcut();
        this.mOnAccessibilityShortcutCallback.run();
    }

    private boolean isServiceEnabled(AccessibilityServiceInfo accessibilityServiceInfo) {
        return this.mAccessibilityManager.getEnabledAccessibilityServiceList(-1).contains(accessibilityServiceInfo);
    }

    private static AccessibilityServiceInfo getInfoForTargetService(Context context) {
        AccessibilityManager instance = AccessibilityManager.getInstance(context);
        String shortcutTargetServiceComponentNameString = AccessibilityUtils.getShortcutTargetServiceComponentNameString(context, ActivityManager.getCurrentUser());
        if (shortcutTargetServiceComponentNameString == null) {
            return null;
        }
        return instance.getInstalledServiceInfoWithComponentName(ComponentName.unflattenFromString(shortcutTargetServiceComponentNameString));
    }

    private static long[] getLongIntArray(Resources resources, int i) {
        int[] intArray = resources.getIntArray(i);
        if (intArray == null) {
            return null;
        }
        long[] jArr = new long[intArray.length];
        for (int i2 = 0; i2 < intArray.length; i2++) {
            jArr[i2] = (long) intArray[i2];
        }
        return jArr;
    }
}
