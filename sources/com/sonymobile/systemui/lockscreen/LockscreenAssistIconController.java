package com.sonymobile.systemui.lockscreen;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Parcelable;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.C0000R$anim;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.sonymobile.keyguard.SomcUsmHelper;
import com.sonymobile.keyguard.statistics.LockscreenStatisticsAssistIconReporter;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;

public class LockscreenAssistIconController {
    private static final String TAG = "LockscreenAssistIconController";
    private static boolean sIsReadyForShowAssistIcon;
    private AnimatedVectorDrawable mAssistIconDrawable;
    private ImageView mAssistIconView;
    private PendingIntent mAssistLaunchIntent;
    private String mAssistVersionName = "";
    private Context mContext;
    private Handler mHandler = new Handler();
    private Animation mHideAnimation;
    private boolean mIsAlreadyTap = false;
    private boolean mIsAssistShowing = false;
    private boolean mIsDoze = false;
    private KeyguardIndicationController mKeyguardIndicationController;
    private LockscreenLoopsController mLockscreenLoopsController;
    private BroadcastReceiver mReceiver;
    private int mResId = 0;
    private Animation mShowAnimation;
    private Runnable mShowAssistIconRunnable = new Runnable() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenAssistIconController.AnonymousClass2 */

        public void run() {
            LockscreenAssistIconController.this.showAssistIcon();
        }
    };
    private long mShowCount = 0;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenAssistIconController.AnonymousClass1 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardVisibilityChanged(boolean z) {
            if (SomcUsmHelper.isUsmEnabled(LockscreenAssistIconController.this.mContext)) {
                LockscreenAssistIconController.this.hideAssistIcon(false);
                LockscreenAssistIconController.this.mAssistIconDrawable = null;
                LockscreenAssistIconController.this.mAssistLaunchIntent = null;
            }
            if (z) {
                LockscreenAssistIconController.this.readyForShowAssistIcon();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onScreenTurnedOn() {
            LockscreenAssistIconController.this.readyForShowAssistIcon();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStartedWakingUp() {
            LockscreenAssistIconController.this.readyForShowAssistIcon();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onFinishedGoingToSleep(int i) {
            LockscreenAssistIconController.this.hideAssistIcon(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onScreenTurnedOff() {
            LockscreenAssistIconController.this.hideAssistIcon(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitchComplete(int i) {
            LockscreenAssistIconController.this.hideAssistIcon(false);
            LockscreenAssistIconController.this.mAssistIconDrawable = null;
            LockscreenAssistIconController.this.mAssistLaunchIntent = null;
            LockscreenAssistIconController.this.initReceiverForCurrentUser();
        }
    };

    public LockscreenAssistIconController(Context context, LockscreenLoopsController lockscreenLoopsController) {
        this.mContext = context;
        this.mLockscreenLoopsController = lockscreenLoopsController;
        this.mShowAnimation = AnimationUtils.loadAnimation(context, C0000R$anim.somc_keyguard_assist_icon_show);
        this.mHideAnimation = AnimationUtils.loadAnimation(context, C0000R$anim.somc_keyguard_assist_icon_hide);
        initReceiverForCurrentUser();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void initReceiverForCurrentUser() {
        if (this.mContext.getResources().getBoolean(C0003R$bool.config_somcAssistIconSupport)) {
            BroadcastReceiver broadcastReceiver = this.mReceiver;
            if (broadcastReceiver != null) {
                this.mContext.unregisterReceiver(broadcastReceiver);
                this.mReceiver = null;
            }
            this.mReceiver = new AssistIconReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.sonymobile.keyguard.assist.action.SHOW_ICON");
            intentFilter.addAction("com.sonymobile.keyguard.assist.action.HIDE_ICON");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.CURRENT, intentFilter, "com.sonymobile.keyguard.assist.permission.CONTROL_ICON", this.mHandler);
        }
    }

    public static boolean isReadyForShowAssistIcon() {
        return sIsReadyForShowAssistIcon;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void readyForShowAssistIcon() {
        if (!this.mIsAssistShowing && this.mAssistIconView != null && this.mAssistIconDrawable != null && isLockscreenVisible()) {
            this.mAssistIconView.setVisibility(4);
            this.mAssistIconView.setImageDrawable(null);
            this.mHandler.removeCallbacks(this.mShowAssistIconRunnable);
            this.mHandler.postDelayed(this.mShowAssistIconRunnable, 1000);
            sIsReadyForShowAssistIcon = true;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isLockscreenVisible() {
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        return instance.isScreenOn() && instance.isKeyguardVisible();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showAssistIcon() {
        if (!this.mIsDoze) {
            if (this.mAssistIconView == null || this.mAssistIconDrawable == null || !isLockscreenVisible()) {
                sIsReadyForShowAssistIcon = false;
                return;
            }
            this.mIsAssistShowing = true;
            this.mHandler.removeCallbacks(this.mShowAssistIconRunnable);
            updateThemeColors();
            this.mAssistIconView.setImageDrawable(this.mAssistIconDrawable);
            this.mAssistIconDrawable.clearAnimationCallbacks();
            this.mAssistIconDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
                /* class com.sonymobile.systemui.lockscreen.LockscreenAssistIconController.AnonymousClass3 */

                public void onAnimationEnd(Drawable drawable) {
                    LockscreenAssistIconController.this.requestAssistEmphasis(true);
                }
            });
            this.mShowCount++;
            this.mAssistIconDrawable.start();
            this.mAssistIconView.setVisibility(0);
            this.mAssistIconView.startAnimation(this.mShowAnimation);
            sIsReadyForShowAssistIcon = false;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void hideAssistIcon(boolean z) {
        if (this.mAssistIconView != null) {
            this.mIsAssistShowing = false;
            this.mHideAnimation.setAnimationListener(new Animation.AnimationListener() {
                /* class com.sonymobile.systemui.lockscreen.LockscreenAssistIconController.AnonymousClass4 */

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    LockscreenAssistIconController.this.mAssistIconView.setVisibility(8);
                    LockscreenAssistIconController.this.mAssistIconView.setImageDrawable(null);
                }
            });
            if (z) {
                this.mAssistIconView.startAnimation(this.mHideAnimation);
            } else {
                this.mAssistIconView.setVisibility(8);
                this.mAssistIconView.setImageDrawable(null);
            }
            requestAssistEmphasis(false);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void requestAssistEmphasis(boolean z) {
        int[] iArr = new int[2];
        this.mAssistIconView.getLocationInWindow(iArr);
        float width = (float) this.mAssistIconView.getWidth();
        float height = (float) this.mAssistIconView.getHeight();
        this.mLockscreenLoopsController.requestAssistEmphasis(z, ((float) iArr[0]) + (width / 2.0f), ((float) iArr[1]) + (height / 2.0f), width, height);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private AnimatedVectorDrawable getIcon(int i) {
        PackageManager packageManager = this.mContext.getPackageManager();
        AnimatedVectorDrawable animatedVectorDrawable = null;
        try {
            try {
                animatedVectorDrawable = (AnimatedVectorDrawable) packageManager.getResourcesForApplicationAsUser("com.sonymobile.assist", KeyguardUpdateMonitor.getCurrentUser()).getDrawable(i, null);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            this.mAssistVersionName = packageManager.getPackageInfoAsUser("com.sonymobile.assist", 0, KeyguardUpdateMonitor.getCurrentUser()).versionName;
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e(TAG, e2.toString());
        }
        return animatedVectorDrawable;
    }

    private void updateThemeColors() {
        int i;
        if (!this.mIsDoze) {
            Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
            if (resources != null) {
                i = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
            } else {
                i = Utils.getColorAttrDefaultColor(this.mContext, C0002R$attr.wallpaperTextColor);
            }
            this.mAssistIconDrawable.setTint(i);
            return;
        }
        this.mAssistIconDrawable.setTint(-1);
    }

    public void setDoze(boolean z) {
        this.mIsDoze = z;
        if (this.mAssistIconDrawable != null) {
            updateThemeColors();
        }
    }

    /* access modifiers changed from: private */
    public class AssistIconReceiver extends BroadcastReceiver {
        private AssistIconReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            PendingIntent pendingIntent = null;
            if (intent != null && intent.getAction().equals("com.sonymobile.keyguard.assist.action.SHOW_ICON")) {
                LockscreenAssistIconController.this.mResId = intent.getIntExtra("com.sonymobile.keyguard.assist.extra.RES_ID", 0);
                LockscreenAssistIconController lockscreenAssistIconController = LockscreenAssistIconController.this;
                lockscreenAssistIconController.mAssistIconDrawable = lockscreenAssistIconController.getIcon(lockscreenAssistIconController.mResId);
                Parcelable parcelableExtra = intent.getParcelableExtra("com.sonymobile.keyguard.assist.extra.LAUNCH_INTENT");
                LockscreenAssistIconController lockscreenAssistIconController2 = LockscreenAssistIconController.this;
                if (parcelableExtra instanceof PendingIntent) {
                    pendingIntent = (PendingIntent) parcelableExtra;
                }
                lockscreenAssistIconController2.mAssistLaunchIntent = pendingIntent;
                LockscreenAssistIconController.this.showAssistIcon();
                LockscreenStatisticsAssistIconReporter.sendReceiveEvent(context, LockscreenAssistIconController.this.mAssistVersionName, LockscreenAssistIconController.this.mResId);
            } else if (intent != null && intent.getAction().equals("com.sonymobile.keyguard.assist.action.HIDE_ICON")) {
                LockscreenAssistIconController lockscreenAssistIconController3 = LockscreenAssistIconController.this;
                lockscreenAssistIconController3.hideAssistIcon(lockscreenAssistIconController3.isLockscreenVisible());
                LockscreenAssistIconController.this.mAssistIconDrawable = null;
                LockscreenAssistIconController.this.mAssistLaunchIntent = null;
                LockscreenAssistIconController.this.mResId = 0;
                LockscreenAssistIconController.this.mAssistVersionName = "";
                LockscreenAssistIconController.this.mShowCount = 0;
            }
        }
    }

    public void init(ViewGroup viewGroup, KeyguardIndicationController keyguardIndicationController) {
        this.mAssistIconView = (ImageView) viewGroup.findViewById(C0007R$id.assist_icon);
        this.mKeyguardIndicationController = keyguardIndicationController;
        this.mAssistIconView.setOnClickListener(new AssistIconClickListener());
    }

    public void setAssistIconView(ImageView imageView) {
        this.mAssistIconView = imageView;
    }

    public void onThemeChanged(ViewGroup viewGroup) {
        ImageView imageView = this.mAssistIconView;
        this.mAssistIconView = (ImageView) viewGroup.findViewById(C0007R$id.assist_icon);
        this.mAssistIconView.setOnClickListener(new AssistIconClickListener());
        AnimatedVectorDrawable animatedVectorDrawable = this.mAssistIconDrawable;
        if (animatedVectorDrawable != null) {
            this.mAssistIconView.setImageDrawable(animatedVectorDrawable);
        }
        if (imageView != null) {
            this.mAssistIconView.setVisibility(imageView.getVisibility());
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startConversationalUi() {
        PendingIntent pendingIntent = this.mAssistLaunchIntent;
        if (pendingIntent != null) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private class AssistIconClickListener implements View.OnClickListener {
        private AssistIconClickListener() {
        }

        public void onClick(final View view) {
            if (!LockscreenAssistIconController.this.mIsAlreadyTap) {
                Animation loadAnimation = AnimationUtils.loadAnimation(LockscreenAssistIconController.this.mContext, C0000R$anim.somc_keyguard_assist_icon_tap);
                loadAnimation.setAnimationListener(new Animation.AnimationListener() {
                    /* class com.sonymobile.systemui.lockscreen.LockscreenAssistIconController.AssistIconClickListener.AnonymousClass1 */

                    public void onAnimationRepeat(Animation animation) {
                    }

                    public void onAnimationStart(Animation animation) {
                    }

                    public void onAnimationEnd(Animation animation) {
                        LockscreenAssistIconController.this.mIsAlreadyTap = false;
                        view.setBackgroundResource(0);
                        if (LockscreenAssistIconController.this.mKeyguardIndicationController != null) {
                            LockscreenAssistIconController.this.mKeyguardIndicationController.hideTransientIndication();
                        }
                    }
                });
                view.startAnimation(loadAnimation);
                view.setBackgroundResource(C0006R$drawable.somc_assist_icon_tapped);
                view.setElevation(12.0f);
                LockscreenAssistIconController.this.mIsAlreadyTap = true;
                if (LockscreenAssistIconController.this.mKeyguardIndicationController != null) {
                    LockscreenAssistIconController.this.mKeyguardIndicationController.showTransientIndication(C0014R$string.notification_tap_again);
                    return;
                }
                return;
            }
            view.clearAnimation();
            LockscreenAssistIconController.this.mIsAlreadyTap = false;
            if (LockscreenAssistIconController.this.mKeyguardIndicationController != null) {
                LockscreenAssistIconController.this.mKeyguardIndicationController.hideTransientIndication();
            }
            LockscreenAssistIconController.this.startConversationalUi();
            LockscreenStatisticsAssistIconReporter.sendTapEvent(LockscreenAssistIconController.this.mContext, LockscreenAssistIconController.this.mAssistVersionName, LockscreenAssistIconController.this.mResId, LockscreenAssistIconController.this.mShowCount);
        }
    }
}
