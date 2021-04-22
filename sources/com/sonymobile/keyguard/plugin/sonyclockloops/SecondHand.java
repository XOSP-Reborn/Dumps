package com.sonymobile.keyguard.plugin.sonyclockloops;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.Utils;
import com.android.systemui.C0000R$anim;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0004R$color;
import com.android.systemui.Dependency;
import com.sonymobile.systemui.lockscreen.LockscreenAssistIconController;
import com.sonymobile.systemui.lockscreen.LockscreenLoopsController;
import com.sonymobile.systemui.lockscreen.LockscreenLoopsControllerCallback;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Calendar;
import java.util.TimeZone;

public class SecondHand extends View {
    private LockscreenLoopsControllerCallback mCallback;
    private Runnable mClearRunnable;
    private boolean mDozing;
    private Handler mHandler;
    private Animation mHideAnimation;
    private Runnable mHideRunnable;
    private Runnable mHideWoAnimRunnable;
    private boolean mIsPicker;
    private boolean mIsTicking;
    private LockscreenLoopsController mLockscreenLoopsController;
    private KeyguardUpdateMonitor mMonitor;
    private Paint mPaintDot;
    private Paint mPaintLoop;
    private final Runnable mSecondRunnable;
    private Animation mShowAnimation;
    private Runnable mShowRunnable;
    private SonyClockLoops mSonyClockLoops;
    private final Calendar mTime;
    private final BroadcastReceiver mTimeEventReceiver;

    public SecondHand(Context context) {
        this(context, null, 0);
    }

    public SecondHand(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SecondHand(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mTime = Calendar.getInstance();
        this.mHandler = new Handler();
        this.mPaintLoop = new Paint();
        this.mPaintDot = new Paint();
        this.mIsTicking = false;
        this.mDozing = false;
        this.mShowRunnable = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass1 */

            public void run() {
                if (SecondHand.this.getVisibility() == 4) {
                    SecondHand secondHand = SecondHand.this;
                    secondHand.startAnimation(secondHand.mShowAnimation);
                }
            }
        };
        this.mHideRunnable = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass2 */

            public void run() {
                if (SecondHand.this.getVisibility() == 0) {
                    SecondHand secondHand = SecondHand.this;
                    secondHand.startAnimation(secondHand.mHideAnimation);
                }
            }
        };
        this.mHideWoAnimRunnable = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass3 */

            public void run() {
                SecondHand.this.setVisibility(4);
            }
        };
        this.mClearRunnable = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass4 */

            public void run() {
                SecondHand.this.clearAnimation();
            }
        };
        this.mTimeEventReceiver = new BroadcastReceiver() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass5 */

            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                    SecondHand.this.setTimeZone(intent.getStringExtra("time-zone"));
                }
                SecondHand.this.updateTime();
            }
        };
        this.mIsPicker = false;
        this.mCallback = new LockscreenLoopsControllerCallback() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass6 */

            @Override // com.sonymobile.systemui.lockscreen.LockscreenLoopsControllerCallback
            public void show() {
                SecondHand.this.doShow();
            }

            @Override // com.sonymobile.systemui.lockscreen.LockscreenLoopsControllerCallback
            public void hide(boolean z) {
                SecondHand.this.doHide(z);
            }

            @Override // com.sonymobile.systemui.lockscreen.LockscreenLoopsControllerCallback
            public void stopClockForDozing() {
                SecondHand.this.stopClockTicking();
            }

            @Override // com.sonymobile.systemui.lockscreen.LockscreenLoopsControllerCallback
            public void restartClockForDozing() {
                SecondHand.this.stopClockTicking();
                SecondHand.this.startClockTicking();
            }
        };
        this.mSecondRunnable = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass7 */

            public void run() {
                SecondHand.this.mHandler.removeCallbacks(SecondHand.this.mSecondRunnable);
                if (SecondHand.this.mMonitor.isScreenOn()) {
                    SecondHand.this.updateTime();
                }
                long uptimeMillis = SystemClock.uptimeMillis();
                SecondHand.this.mHandler.postAtTime(this, uptimeMillis + (1000 - (uptimeMillis % 1000)));
            }
        };
        setupPaint();
        updateThemeColors();
        this.mShowAnimation = AnimationUtils.loadAnimation(context, C0000R$anim.somc_sony_clock_loops_show);
        this.mHideAnimation = AnimationUtils.loadAnimation(context, C0000R$anim.somc_sony_clock_loops_hide);
        this.mShowAnimation.setAnimationListener(new Animation.AnimationListener() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass8 */

            public void onAnimationEnd(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
                SecondHand.this.setVisibility(0);
            }
        });
        this.mHideAnimation.setAnimationListener(new Animation.AnimationListener() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SecondHand.AnonymousClass9 */

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                SecondHand.this.setVisibility(4);
            }
        });
        setVisibility(4);
        this.mLockscreenLoopsController = (LockscreenLoopsController) Dependency.get(LockscreenLoopsController.class);
        this.mMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    public void setPicker(boolean z) {
        this.mIsPicker = z;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startClockTicking() {
        SonyClockLoops sonyClockLoops = this.mSonyClockLoops;
        if (sonyClockLoops != null) {
            startClockTicking(sonyClockLoops);
        }
    }

    public void startClockTicking(SonyClockLoops sonyClockLoops) {
        this.mSonyClockLoops = sonyClockLoops;
        if (!this.mIsTicking) {
            this.mIsTicking = true;
            registerTimeEventReceiver();
            if (this.mIsPicker || !this.mLockscreenLoopsController.isConnected()) {
                setVisibility(0);
            } else {
                this.mLockscreenLoopsController.registerCallback(0, this.mCallback, this.mDozing);
            }
            setTimeZone(null);
            if (!this.mDozing) {
                this.mSecondRunnable.run();
            }
        }
        updateTime();
    }

    public void stopClockTicking() {
        if (this.mIsTicking) {
            if (!this.mIsPicker) {
                this.mLockscreenLoopsController.unregisterCallback(0, this.mDozing);
            }
            this.mIsTicking = false;
            unregisterTimeEventReceiver();
            if (!this.mDozing) {
                this.mHandler.removeCallbacks(this.mSecondRunnable);
            }
            doHide(false);
        }
    }

    private int dpToPx(int i) {
        return (int) (((float) i) * Resources.getSystem().getDisplayMetrics().density);
    }

    private void setupPaint() {
        this.mPaintLoop.setStyle(Paint.Style.STROKE);
        this.mPaintLoop.setAntiAlias(true);
        this.mPaintLoop.setStrokeWidth((float) dpToPx(1));
        this.mPaintDot.setStyle(Paint.Style.FILL);
        this.mPaintDot.setAntiAlias(true);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = canvas.getWidth() / 2;
        int height = canvas.getHeight() / 2;
        int width2 = ((canvas.getWidth() / 2) - dpToPx(1)) - dpToPx(5);
        canvas.drawCircle((float) width, (float) height, (float) width2, this.mPaintLoop);
        if (!this.mDozing) {
            updateSecondsHand(canvas, width, height, width2);
        }
    }

    private void updateSecondsHand(Canvas canvas, int i, int i2, int i3) {
        double d = (double) i3;
        double d2 = (double) ((this.mTime.get(13) * 6) - 90);
        canvas.drawCircle((float) (((double) i) + (Math.cos(Math.toRadians(d2)) * d)), (float) (((double) i2) + (d * Math.sin(Math.toRadians(d2)))), (float) dpToPx(5), this.mPaintDot);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTime() {
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        this.mSonyClockLoops.refresh(this.mTime);
        invalidate();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setTimeZone(String str) {
        if (str == null) {
            this.mTime.setTimeZone(TimeZone.getDefault());
        } else {
            this.mTime.setTimeZone(TimeZone.getTimeZone(str));
        }
    }

    public void setDoze() {
        this.mDozing = true;
        this.mPaintLoop.setColor(-1);
        this.mPaintDot.setColor(-1);
        invalidate();
    }

    private void updateThemeColors() {
        int i;
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            i = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
        } else {
            i = Utils.getColorAttrDefaultColor(getContext(), C0002R$attr.wallpaperTextColor);
        }
        this.mPaintLoop.setColor(i);
        this.mPaintDot.setColor(i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void doShow() {
        if (!LockscreenAssistIconController.isReadyForShowAssistIcon()) {
            this.mHandler.post(this.mClearRunnable);
            this.mHandler.post(this.mShowRunnable);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void doHide(boolean z) {
        if (z) {
            this.mHandler.post(this.mClearRunnable);
            this.mHandler.post(this.mHideRunnable);
            return;
        }
        this.mHandler.post(this.mClearRunnable);
        this.mHandler.post(this.mHideWoAnimRunnable);
    }

    private void registerTimeEventReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getContext().registerReceiver(this.mTimeEventReceiver, intentFilter, null, getHandler());
    }

    private void unregisterTimeEventReceiver() {
        getContext().unregisterReceiver(this.mTimeEventReceiver);
    }
}
