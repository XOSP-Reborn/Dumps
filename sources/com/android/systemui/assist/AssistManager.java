package com.android.systemui.assist;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.metrics.LogMaker;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionListener;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.applications.InterestingConfigChanges;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0010R$layout;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.Dependency;
import com.android.systemui.assist.ui.DefaultUiController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.sonymobile.notifyassist.NotifyAssistUtils;

public class AssistManager implements ConfigurationChangedReceiver {
    private final AssistDisclosure mAssistDisclosure;
    protected final AssistUtils mAssistUtils;
    protected final Context mContext;
    private final DeviceProvisionedController mDeviceProvisionedController;
    private final AssistHandleBehaviorController mHandleController;
    private Runnable mHideRunnable = new Runnable() {
        /* class com.android.systemui.assist.AssistManager.AnonymousClass2 */

        public void run() {
            AssistManager.this.mView.removeCallbacks(this);
            AssistManager.this.mView.show(false, true);
        }
    };
    private final InterestingConfigChanges mInterestingConfigChanges;
    private final PhoneStateMonitor mPhoneStateMonitor;
    private final boolean mShouldEnableOrb;
    private IVoiceInteractionSessionShowCallback mShowCallback = new IVoiceInteractionSessionShowCallback.Stub() {
        /* class com.android.systemui.assist.AssistManager.AnonymousClass1 */

        public void onFailed() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }

        public void onShown() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }
    };
    private final UiController mUiController;
    private AssistOrbContainer mView;
    private final WindowManager mWindowManager;

    public interface UiController {
        void onGestureCompletion(float f);

        void onInvocationProgress(int i, float f);
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowOrb() {
        return false;
    }

    public AssistManager(DeviceProvisionedController deviceProvisionedController, Context context) {
        this.mContext = context;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAssistUtils = new AssistUtils(context);
        this.mAssistDisclosure = new AssistDisclosure(context, new Handler());
        this.mPhoneStateMonitor = new PhoneStateMonitor(context);
        this.mHandleController = new AssistHandleBehaviorController(context, this.mAssistUtils, new Handler());
        registerVoiceInteractionSessionListener();
        this.mInterestingConfigChanges = new InterestingConfigChanges(-2147482748);
        onConfigurationChanged(context.getResources().getConfiguration());
        this.mShouldEnableOrb = !ActivityManager.isLowRamDeviceStatic();
        this.mUiController = new DefaultUiController(this.mContext);
        ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).addCallback((OverviewProxyService.OverviewProxyListener) new OverviewProxyService.OverviewProxyListener() {
            /* class com.android.systemui.assist.AssistManager.AnonymousClass3 */

            @Override // com.android.systemui.recents.OverviewProxyService.OverviewProxyListener
            public void onAssistantProgress(float f) {
                AssistManager.this.onInvocationProgress(1, f);
            }

            @Override // com.android.systemui.recents.OverviewProxyService.OverviewProxyListener
            public void onAssistantGestureCompletion(float f) {
                AssistManager.this.onGestureCompletion(f);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void registerVoiceInteractionSessionListener() {
        this.mAssistUtils.registerVoiceInteractionSessionListener(new IVoiceInteractionSessionListener.Stub() {
            /* class com.android.systemui.assist.AssistManager.AnonymousClass4 */

            public void onSetUiHints(Bundle bundle) {
            }

            public void onVoiceSessionHidden() throws RemoteException {
            }

            public void onVoiceSessionShown() throws RemoteException {
            }
        });
    }

    @Override // com.android.systemui.ConfigurationChangedReceiver
    public void onConfigurationChanged(Configuration configuration) {
        boolean z;
        if (this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
            AssistOrbContainer assistOrbContainer = this.mView;
            if (assistOrbContainer != null) {
                z = assistOrbContainer.isShowing();
                this.mWindowManager.removeView(this.mView);
            } else {
                z = false;
            }
            this.mView = (AssistOrbContainer) LayoutInflater.from(this.mContext).inflate(C0010R$layout.assist_orb, (ViewGroup) null);
            this.mView.setVisibility(8);
            this.mView.setSystemUiVisibility(1792);
            this.mWindowManager.addView(this.mView, getLayoutParams());
            if (z) {
                this.mView.show(true, false);
            }
        }
    }

    public void startAssist(Bundle bundle) {
        ComponentName assistInfo = getAssistInfo();
        if (assistInfo != null) {
            boolean equals = assistInfo.equals(getVoiceInteractorComponentName());
            if (!equals || (!isVoiceSessionRunning() && shouldShowOrb())) {
                showOrb(assistInfo, equals);
                this.mView.postDelayed(this.mHideRunnable, equals ? 2500 : 1000);
            }
            if (bundle == null) {
                bundle = new Bundle();
            }
            int i = bundle.getInt("invocation_type", 0);
            if (i == 1) {
                this.mHandleController.onAssistantGesturePerformed();
            }
            int phoneState = this.mPhoneStateMonitor.getPhoneState();
            bundle.putInt("invocation_phone_state", phoneState);
            bundle.putLong("invocation_time_ms", SystemClock.uptimeMillis());
            MetricsLogger.action(new LogMaker(1716).setType(1).setSubtype(toLoggingSubType(i, phoneState)));
            startAssistInternal(bundle, assistInfo, equals);
        }
    }

    public void onInvocationProgress(int i, float f) {
        this.mUiController.onInvocationProgress(i, f);
    }

    public void startAssistByDoubleTap(Bundle bundle) {
        ComponentName assistInfo;
        if (!NotifyAssistUtils.isLaunchedGoogleAssistOnceByDoubleTap(this.mContext) && (assistInfo = getAssistInfo()) != null && "com.google.android.googlequicksearchbox".equals(assistInfo.getPackageName())) {
            Log.d("AssistManager", "Set is launched Google Assist once by double-tap");
            NotifyAssistUtils.setLaunchedGoogleAssistOnceByDoubleTap(this.mContext);
        }
        startAssist(bundle);
    }

    public void onGestureCompletion(float f) {
        this.mUiController.onGestureCompletion(f);
    }

    public void hideAssist() {
        this.mAssistUtils.hideCurrentSession();
    }

    private WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.assist_orb_scrim_height), 2033, 280, -3);
        layoutParams.token = new Binder();
        layoutParams.gravity = 8388691;
        layoutParams.setTitle("AssistPreviewPanel");
        layoutParams.softInputMode = 49;
        return layoutParams;
    }

    private void showOrb(ComponentName componentName, boolean z) {
        maybeSwapSearchIcon(componentName, z);
        if (this.mShouldEnableOrb) {
            this.mView.show(true, true);
        }
    }

    private void startAssistInternal(Bundle bundle, ComponentName componentName, boolean z) {
        if (z) {
            startVoiceInteractor(bundle);
        } else {
            startAssistActivity(bundle, componentName);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0036, code lost:
        r0 = r0.getAssistIntent(r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void startAssistActivity(android.os.Bundle r6, android.content.ComponentName r7) {
        /*
        // Method dump skipped, instructions count: 123
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.assist.AssistManager.startAssistActivity(android.os.Bundle, android.content.ComponentName):void");
    }

    private void startVoiceInteractor(Bundle bundle) {
        this.mAssistUtils.showSessionForActiveService(bundle, 4, this.mShowCallback, (IBinder) null);
    }

    public void launchVoiceAssistFromKeyguard() {
        this.mAssistUtils.launchVoiceAssistFromKeyguard();
    }

    public boolean canVoiceAssistBeLaunchedFromKeyguard() {
        return this.mAssistUtils.activeServiceSupportsLaunchFromKeyguard();
    }

    public ComponentName getVoiceInteractorComponentName() {
        return this.mAssistUtils.getActiveServiceComponentName();
    }

    private boolean isVoiceSessionRunning() {
        return this.mAssistUtils.isSessionRunning();
    }

    private void maybeSwapSearchIcon(ComponentName componentName, boolean z) {
        replaceDrawable(this.mView.getOrb().getLogo(), componentName, "com.android.systemui.action_assist_icon", z);
    }

    public void replaceDrawable(ImageView imageView, ComponentName componentName, String str, boolean z) {
        Bundle bundle;
        int i;
        if (componentName != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                if (z) {
                    bundle = packageManager.getServiceInfo(componentName, 128).metaData;
                } else {
                    bundle = packageManager.getActivityInfo(componentName, 128).metaData;
                }
                if (!(bundle == null || (i = bundle.getInt(str)) == 0)) {
                    imageView.setImageDrawable(packageManager.getResourcesForApplication(componentName.getPackageName()).getDrawable(i));
                    return;
                }
            } catch (PackageManager.NameNotFoundException unused) {
            } catch (Resources.NotFoundException e) {
                Log.w("AssistManager", "Failed to swap drawable from " + componentName.flattenToShortString(), e);
            }
        }
        imageView.setImageDrawable(null);
    }

    public ComponentName getAssistInfoForUser(int i) {
        return this.mAssistUtils.getAssistComponentForUser(i);
    }

    private ComponentName getAssistInfo() {
        return getAssistInfoForUser(KeyguardUpdateMonitor.getCurrentUser());
    }

    public void showDisclosure() {
        this.mAssistDisclosure.postShow();
    }

    public void onLockscreenShown() {
        this.mAssistUtils.onLockscreenShown();
    }

    public int toLoggingSubType(int i) {
        return toLoggingSubType(i, this.mPhoneStateMonitor.getPhoneState());
    }

    private int toLoggingSubType(int i, int i2) {
        return (!this.mHandleController.areHandlesShowing()) | (i << 1) | (i2 << 4);
    }
}
