package com.android.systemui.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.SystemUI;
import com.android.systemui.screenshot.GlobalScreenshot;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.NotificationChannels;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GlobalScreenshot {
    private ImageView mBackgroundView = ((ImageView) this.mScreenshotLayout.findViewById(C0007R$id.global_screenshot_background));
    private float mBgPadding;
    private float mBgPaddingScale;
    private MediaActionSound mCameraSound;
    private Context mContext;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private int mNotificationIconSize;
    private NotificationManager mNotificationManager;
    private final int mPreviewHeight;
    private final int mPreviewWidth;
    private AsyncTask<Void, Void, Void> mSaveInBgTask;
    private final float mScale;
    private Bitmap mScreenBitmap;
    private AnimatorSet mScreenshotAnimation;
    private ImageView mScreenshotFlash = ((ImageView) this.mScreenshotLayout.findViewById(C0007R$id.global_screenshot_flash));
    private View mScreenshotLayout;
    private ScreenshotSelectorView mScreenshotSelectorView = ((ScreenshotSelectorView) this.mScreenshotLayout.findViewById(C0007R$id.global_screenshot_selector));
    private ImageView mScreenshotView = ((ImageView) this.mScreenshotLayout.findViewById(C0007R$id.global_screenshot));
    private ShutterSoundController mShutterSoundController;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private WindowManager mWindowManager;

    public class ShutterSoundController extends CameraManager.AvailabilityCallback {
        private Map<String, Boolean> mCameraAvailabilityMap = new HashMap();
        private CameraManager mCm;

        public ShutterSoundController() {
            GlobalScreenshot.this = r2;
            this.mCm = (CameraManager) r2.mContext.getSystemService("camera");
        }

        public void playIfCameraIsInUse() {
            this.mCm.unregisterAvailabilityCallback(this);
            if (!((DevicePolicyManager) GlobalScreenshot.this.mContext.getSystemService("device_policy")).getCameraDisabled(null)) {
                refreshCameraIdList();
                if (hasCameraDevices()) {
                    this.mCm.registerAvailabilityCallback(this, (Handler) null);
                }
            }
        }

        private void refreshCameraIdList() {
            this.mCameraAvailabilityMap.clear();
            try {
                for (String str : this.mCm.getCameraIdList()) {
                    this.mCameraAvailabilityMap.put(str, false);
                }
            } catch (CameraAccessException unused) {
            }
        }

        private boolean hasCameraDevices() {
            return !this.mCameraAvailabilityMap.isEmpty();
        }

        public void onCameraUnavailable(String str) {
            GlobalScreenshot.this.mCameraSound.play(0);
            this.mCm.unregisterAvailabilityCallback(this);
        }

        public void onCameraAvailable(String str) {
            this.mCameraAvailabilityMap.put(str, true);
            if (areAllCamerasAvailable()) {
                this.mCm.unregisterAvailabilityCallback(this);
            }
        }

        private boolean areAllCamerasAvailable() {
            for (Boolean bool : this.mCameraAvailabilityMap.values()) {
                if (!bool.booleanValue()) {
                    return false;
                }
            }
            return true;
        }
    }

    public GlobalScreenshot(Context context) {
        int i;
        Resources resources = context.getResources();
        this.mContext = context;
        this.mScreenshotLayout = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(C0010R$layout.global_screenshot, (ViewGroup) null);
        this.mScreenshotLayout.setFocusable(true);
        this.mScreenshotSelectorView.setFocusable(true);
        this.mScreenshotSelectorView.setFocusableInTouchMode(true);
        this.mScreenshotLayout.setOnTouchListener(new View.OnTouchListener() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass1 */

            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        this.mWindowLayoutParams = new WindowManager.LayoutParams(-1, -1, 0, 0, 2036, 525568, -3);
        this.mWindowLayoutParams.setTitle("ScreenshotAnimation");
        this.mWindowLayoutParams.layoutInDisplayCutoutMode = 1;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        this.mNotificationIconSize = resources.getDimensionPixelSize(17104902);
        this.mBgPadding = (float) resources.getDimensionPixelSize(C0005R$dimen.global_screenshot_bg_padding);
        this.mBgPaddingScale = this.mBgPadding / ((float) this.mDisplayMetrics.widthPixels);
        try {
            i = resources.getDimensionPixelSize(C0005R$dimen.notification_panel_width);
        } catch (Resources.NotFoundException unused) {
            i = 0;
        }
        this.mPreviewWidth = i <= 0 ? this.mDisplayMetrics.widthPixels : i;
        this.mPreviewHeight = resources.getDimensionPixelSize(C0005R$dimen.notification_max_height);
        int i2 = this.mDisplay.getRotation() % 2 == 0 ? this.mDisplayMetrics.widthPixels : this.mDisplayMetrics.heightPixels;
        Display.Mode[] supportedModes = this.mDisplay.getSupportedModes();
        int i3 = i2;
        for (Display.Mode mode : supportedModes) {
            if (i2 < mode.getPhysicalWidth()) {
                i3 = mode.getPhysicalWidth();
            }
        }
        this.mScale = ((float) i2) / ((float) i3);
        this.mCameraSound = new MediaActionSound();
        this.mCameraSound.load(0);
        if (context.getResources().getBoolean(17891388)) {
            this.mShutterSoundController = new ShutterSoundController();
        }
    }

    private void saveScreenshotInWorkerThread(Runnable runnable) {
        SaveImageInBackgroundData saveImageInBackgroundData = new SaveImageInBackgroundData();
        saveImageInBackgroundData.context = this.mContext;
        saveImageInBackgroundData.image = this.mScreenBitmap;
        saveImageInBackgroundData.iconSize = this.mNotificationIconSize;
        saveImageInBackgroundData.finisher = runnable;
        saveImageInBackgroundData.previewWidth = this.mPreviewWidth;
        saveImageInBackgroundData.previewheight = this.mPreviewHeight;
        saveImageInBackgroundData.previewScale = this.mScale;
        AsyncTask<Void, Void, Void> asyncTask = this.mSaveInBgTask;
        if (asyncTask != null) {
            asyncTask.cancel(false);
        }
        this.mSaveInBgTask = new SaveImageInBackgroundTask(this.mContext, saveImageInBackgroundData, this.mNotificationManager).execute(new Void[0]);
    }

    private void takeScreenshot(Runnable runnable, boolean z, boolean z2, Rect rect) {
        int rotation = this.mDisplay.getRotation();
        int width = rect.width();
        int height = rect.height();
        float f = this.mScale;
        this.mScreenBitmap = SurfaceControl.screenshot(rect, (int) (((float) width) / f), (int) (((float) height) / f), rotation);
        Bitmap bitmap = this.mScreenBitmap;
        if (bitmap == null) {
            notifyScreenshotError(this.mContext, this.mNotificationManager, C0014R$string.screenshot_failed_to_capture_text);
            runnable.run();
            return;
        }
        bitmap.setHasAlpha(false);
        this.mScreenBitmap.prepareToDraw();
        DisplayMetrics displayMetrics = this.mDisplayMetrics;
        startAnimation(runnable, displayMetrics.widthPixels, displayMetrics.heightPixels, z, z2);
    }

    public void takeScreenshot(Runnable runnable, boolean z, boolean z2) {
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        DisplayMetrics displayMetrics = this.mDisplayMetrics;
        takeScreenshot(runnable, z, z2, new Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels));
    }

    public void takeScreenshotPartial(final Runnable runnable, final boolean z, final boolean z2) {
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        this.mScreenshotSelectorView.setOnTouchListener(new View.OnTouchListener() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass2 */

            public boolean onTouch(View view, MotionEvent motionEvent) {
                ScreenshotSelectorView screenshotSelectorView = (ScreenshotSelectorView) view;
                int action = motionEvent.getAction();
                if (action == 0) {
                    screenshotSelectorView.startSelection((int) motionEvent.getX(), (int) motionEvent.getY());
                    return true;
                } else if (action == 1) {
                    screenshotSelectorView.setVisibility(8);
                    GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                    final Rect selectionRect = screenshotSelectorView.getSelectionRect();
                    if (!(selectionRect == null || selectionRect.width() == 0 || selectionRect.height() == 0)) {
                        GlobalScreenshot.this.mScreenshotLayout.post(new Runnable() {
                            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass2.AnonymousClass1 */

                            public void run() {
                                AnonymousClass2 r0 = AnonymousClass2.this;
                                GlobalScreenshot.this.takeScreenshot(runnable, z, z2, selectionRect);
                            }
                        });
                    }
                    screenshotSelectorView.stopSelection();
                    return true;
                } else if (action != 2) {
                    return false;
                } else {
                    screenshotSelectorView.updateSelection((int) motionEvent.getX(), (int) motionEvent.getY());
                    return true;
                }
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass3 */

            public void run() {
                GlobalScreenshot.this.mScreenshotSelectorView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotSelectorView.requestFocus();
            }
        });
    }

    public void stopScreenshot() {
        if (this.mScreenshotSelectorView.getSelectionRect() != null) {
            this.mWindowManager.removeView(this.mScreenshotLayout);
            this.mScreenshotSelectorView.stopSelection();
        }
    }

    private void startAnimation(final Runnable runnable, int i, int i2, boolean z, boolean z2) {
        if (((PowerManager) this.mContext.getSystemService("power")).isPowerSaveMode()) {
            Toast.makeText(this.mContext, C0014R$string.screenshot_saved_title, 0).show();
        }
        this.mScreenshotView.setImageBitmap(this.mScreenBitmap);
        this.mScreenshotLayout.requestFocus();
        AnimatorSet animatorSet = this.mScreenshotAnimation;
        if (animatorSet != null) {
            if (animatorSet.isStarted()) {
                this.mScreenshotAnimation.end();
            }
            this.mScreenshotAnimation.removeAllListeners();
        }
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        ValueAnimator createScreenshotDropInAnimation = createScreenshotDropInAnimation();
        ValueAnimator createScreenshotDropOutAnimation = createScreenshotDropOutAnimation(i, i2, z, z2);
        this.mScreenshotAnimation = new AnimatorSet();
        this.mScreenshotAnimation.playSequentially(createScreenshotDropInAnimation, createScreenshotDropOutAnimation);
        this.mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass4 */

            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.saveScreenshotInWorkerThread(runnable);
                GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                GlobalScreenshot.this.mScreenBitmap = null;
                GlobalScreenshot.this.mScreenshotView.setImageBitmap(null);
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass5 */

            public void run() {
                if (GlobalScreenshot.this.mShutterSoundController != null) {
                    GlobalScreenshot.this.mShutterSoundController.playIfCameraIsInUse();
                } else {
                    GlobalScreenshot.this.mCameraSound.play(0);
                }
                GlobalScreenshot.this.mScreenshotView.setLayerType(2, null);
                GlobalScreenshot.this.mScreenshotView.buildLayer();
                GlobalScreenshot.this.mScreenshotAnimation.start();
            }
        });
    }

    private ValueAnimator createScreenshotDropInAnimation() {
        final AnonymousClass6 r0 = new Interpolator() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass6 */

            public float getInterpolation(float f) {
                if (f <= 0.60465115f) {
                    return (float) Math.sin(((double) (f / 0.60465115f)) * 3.141592653589793d);
                }
                return 0.0f;
            }
        };
        final AnonymousClass7 r1 = new Interpolator() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass7 */

            public float getInterpolation(float f) {
                if (f < 0.30232558f) {
                    return 0.0f;
                }
                return (f - 0.60465115f) / 0.39534885f;
            }
        };
        if ((this.mContext.getResources().getConfiguration().uiMode & 48) == 32) {
            this.mScreenshotView.getBackground().setTint(-16777216);
        } else {
            this.mScreenshotView.getBackground().setTintList(null);
        }
        ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        ofFloat.setDuration(430L);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass8 */

            public void onAnimationStart(Animator animator) {
                GlobalScreenshot.this.mBackgroundView.setAlpha(0.0f);
                GlobalScreenshot.this.mBackgroundView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotView.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationX(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationY(0.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleX(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleY(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(0);
            }

            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(8);
            }
        });
        ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass9 */

            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float interpolation = (GlobalScreenshot.this.mBgPaddingScale + 1.0f) - (r1.getInterpolation(floatValue) * 0.27499998f);
                GlobalScreenshot.this.mBackgroundView.setAlpha(r1.getInterpolation(floatValue) * 0.5f);
                GlobalScreenshot.this.mScreenshotView.setAlpha(floatValue);
                GlobalScreenshot.this.mScreenshotView.setScaleX(interpolation);
                GlobalScreenshot.this.mScreenshotView.setScaleY(interpolation);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(r0.getInterpolation(floatValue));
            }
        });
        return ofFloat;
    }

    private ValueAnimator createScreenshotDropOutAnimation(int i, int i2, boolean z, boolean z2) {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        ofFloat.setStartDelay(500);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass10 */

            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.mBackgroundView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setLayerType(0, null);
            }
        });
        if (!z || !z2) {
            ofFloat.setDuration(320L);
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass11 */

                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    float f = (GlobalScreenshot.this.mBgPaddingScale + 0.725f) - (0.125f * floatValue);
                    float f2 = 1.0f - floatValue;
                    GlobalScreenshot.this.mBackgroundView.setAlpha(0.5f * f2);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(f2);
                    GlobalScreenshot.this.mScreenshotView.setScaleX(f);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(f);
                }
            });
        } else {
            final AnonymousClass12 r6 = new Interpolator() {
                /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass12 */

                public float getInterpolation(float f) {
                    if (f < 0.8604651f) {
                        return (float) (1.0d - Math.pow((double) (1.0f - (f / 0.8604651f)), 2.0d));
                    }
                    return 1.0f;
                }
            };
            float f = this.mBgPadding;
            float f2 = (((float) i) - (f * 2.0f)) / 2.0f;
            float f3 = (((float) i2) - (f * 2.0f)) / 2.0f;
            final PointF pointF = new PointF((-f2) + (f2 * 0.45f), (-f3) + (f3 * 0.45f));
            ofFloat.setDuration(430L);
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.systemui.screenshot.GlobalScreenshot.AnonymousClass13 */

                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    float interpolation = (GlobalScreenshot.this.mBgPaddingScale + 0.725f) - (r6.getInterpolation(floatValue) * 0.27500004f);
                    GlobalScreenshot.this.mBackgroundView.setAlpha((1.0f - floatValue) * 0.5f);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(1.0f - r6.getInterpolation(floatValue));
                    GlobalScreenshot.this.mScreenshotView.setScaleX(interpolation);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(interpolation);
                    GlobalScreenshot.this.mScreenshotView.setTranslationX(pointF.x * floatValue);
                    GlobalScreenshot.this.mScreenshotView.setTranslationY(floatValue * pointF.y);
                }
            });
        }
        return ofFloat;
    }

    static void notifyScreenshotError(Context context, NotificationManager notificationManager, int i) {
        Resources resources = context.getResources();
        String string = resources.getString(i);
        Notification.Builder color = new Notification.Builder(context, NotificationChannels.ALERTS).setTicker(resources.getString(C0014R$string.screenshot_failed_title)).setContentTitle(resources.getString(C0014R$string.screenshot_failed_title)).setContentText(string).setSmallIcon(C0006R$drawable.stat_notify_image_error).setWhen(System.currentTimeMillis()).setVisibility(1).setCategory("err").setAutoCancel(true).setColor(context.getColor(17170460));
        Intent createAdminSupportIntent = ((DevicePolicyManager) context.getSystemService("device_policy")).createAdminSupportIntent("policy_disable_screen_capture");
        if (createAdminSupportIntent != null) {
            color.setContentIntent(PendingIntent.getActivityAsUser(context, 0, createAdminSupportIntent, 0, null, UserHandle.CURRENT));
        }
        SystemUI.overrideNotificationAppName(context, color, true);
        notificationManager.notify(1, new Notification.BigTextStyle(color).bigText(string).build());
    }

    public static class ActionProxyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            ((StatusBar) SysUiServiceProvider.getComponent(context, StatusBar.class)).executeRunnableDismissingKeyguard(new Runnable(intent, context) {
                /* class com.android.systemui.screenshot.$$Lambda$GlobalScreenshot$ActionProxyReceiver$tBhjeKzNYNKU1TanWTPaMXUfmOc */
                private final /* synthetic */ Intent f$0;
                private final /* synthetic */ Context f$1;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                }

                public final void run() {
                    GlobalScreenshot.ActionProxyReceiver.lambda$onReceive$0(this.f$0, this.f$1);
                }
            }, null, true, true, true);
        }

        static /* synthetic */ void lambda$onReceive$0(Intent intent, Context context) {
            try {
                ActivityManagerWrapper.getInstance().closeSystemWindows("screenshot").get(3000, TimeUnit.MILLISECONDS);
                Intent intent2 = (Intent) intent.getParcelableExtra("android:screenshot_action_intent");
                if (intent.getBooleanExtra("android:screenshot_cancel_notification", false)) {
                    GlobalScreenshot.cancelScreenshotNotification(context);
                }
                ActivityOptions makeBasic = ActivityOptions.makeBasic();
                makeBasic.setDisallowEnterPictureInPictureWhileLaunching(intent.getBooleanExtra("android:screenshot_disallow_enter_pip", false));
                context.startActivityAsUser(intent2, makeBasic.toBundle(), UserHandle.CURRENT);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Slog.e("GlobalScreenshot", "Unable to share screenshot", e);
            }
        }
    }

    public static class TargetChosenReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            GlobalScreenshot.cancelScreenshotNotification(context);
        }
    }

    public static class DeleteScreenshotReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("android:screenshot_uri_id")) {
                GlobalScreenshot.cancelScreenshotNotification(context);
                Uri parse = Uri.parse(intent.getStringExtra("android:screenshot_uri_id"));
                new DeleteImageInBackgroundTask(context).execute(parse);
            }
        }
    }

    public static void cancelScreenshotNotification(Context context) {
        ((NotificationManager) context.getSystemService("notification")).cancel(1);
    }
}
