package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.hardware.biometrics.BiometricSourceType;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.LatencyTracker;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.C0003R$bool;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.tuner.TunerService;
import java.io.PrintWriter;

public class BiometricUnlockController extends KeyguardUpdateMonitorCallback {
    private final Context mContext;
    private DozeScrimController mDozeScrimController;
    private final TunerService.Tunable mFaceDismissedKeyguardTunable;
    @VisibleForTesting
    protected boolean mFaceDismissesKeyguard;
    private final boolean mFaceDismissesKeyguardByDefault;
    private boolean mFadedAwayAfterWakeAndUnlock;
    private final Handler mHandler;
    private boolean mHasScreenTurnedOnSinceAuthenticating;
    private KeyguardViewMediator mKeyguardViewMediator;
    private final NotificationMediaManager mMediaManager;
    private final MetricsLogger mMetricsLogger;
    private int mMode;
    private BiometricSourceType mPendingAuthenticatedBioSourceType;
    private int mPendingAuthenticatedUserId;
    private boolean mPendingShowBouncer;
    private final PowerManager mPowerManager;
    private final Runnable mReleaseBiometricWakeLockRunnable;
    private final ScreenLifecycle.Observer mScreenObserver;
    private ScrimController mScrimController;
    private StatusBar mStatusBar;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private final StatusBarWindowController mStatusBarWindowController;
    private final UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitor mUpdateMonitor;
    private PowerManager.WakeLock mWakeLock;
    private final int mWakeUpDelay;
    private final WakefulnessLifecycle.Observer mWakefulnessObserver;

    public BiometricUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache, Handler handler, KeyguardUpdateMonitor keyguardUpdateMonitor, TunerService tunerService) {
        this(context, dozeScrimController, keyguardViewMediator, scrimController, statusBar, unlockMethodCache, handler, keyguardUpdateMonitor, tunerService, context.getResources().getInteger(17694915), context.getResources().getBoolean(C0003R$bool.config_faceAuthDismissesKeyguard));
    }

    @VisibleForTesting
    protected BiometricUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache, Handler handler, KeyguardUpdateMonitor keyguardUpdateMonitor, TunerService tunerService, int i, boolean z) {
        this.mPendingAuthenticatedUserId = -1;
        this.mPendingAuthenticatedBioSourceType = null;
        this.mFaceDismissedKeyguardTunable = new TunerService.Tunable() {
            /* class com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass1 */

            @Override // com.android.systemui.tuner.TunerService.Tunable
            public void onTuningChanged(String str, String str2) {
                boolean z = BiometricUnlockController.this.mFaceDismissesKeyguardByDefault;
                BiometricUnlockController biometricUnlockController = BiometricUnlockController.this;
                biometricUnlockController.mFaceDismissesKeyguard = Settings.Secure.getIntForUser(biometricUnlockController.mContext.getContentResolver(), "face_unlock_dismisses_keyguard", z ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser()) != 0;
            }
        };
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mReleaseBiometricWakeLockRunnable = new Runnable() {
            /* class com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass2 */

            public void run() {
                Log.i("BiometricUnlockController", "biometric wakelock: TIMEOUT!!");
                BiometricUnlockController.this.releaseBiometricWakeLock();
            }
        };
        this.mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
            /* class com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass4 */

            @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
            public void onFinishedWakingUp() {
                if (BiometricUnlockController.this.mPendingShowBouncer) {
                    BiometricUnlockController.this.showBouncer();
                }
            }
        };
        this.mScreenObserver = new ScreenLifecycle.Observer() {
            /* class com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass5 */

            @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
            public void onScreenTurnedOn() {
                BiometricUnlockController.this.mHasScreenTurnedOnSinceAuthenticating = true;
            }
        };
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mUpdateMonitor = keyguardUpdateMonitor;
        this.mUpdateMonitor.registerCallback(this);
        this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        ((WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class)).addObserver(this.mWakefulnessObserver);
        ((ScreenLifecycle) Dependency.get(ScreenLifecycle.class)).addObserver(this.mScreenObserver);
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        this.mDozeScrimController = dozeScrimController;
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mScrimController = scrimController;
        this.mStatusBar = statusBar;
        this.mUnlockMethodCache = unlockMethodCache;
        this.mHandler = handler;
        this.mWakeUpDelay = i;
        this.mFaceDismissesKeyguardByDefault = z;
        tunerService.addTunable(this.mFaceDismissedKeyguardTunable, "face_unlock_dismisses_keyguard");
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void releaseBiometricWakeLock() {
        if (this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mReleaseBiometricWakeLockRunnable);
            Log.i("BiometricUnlockController", "releasing biometric wakelock");
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onBiometricAcquired(BiometricSourceType biometricSourceType) {
        Trace.beginSection("BiometricUnlockController#onBiometricAcquired");
        releaseBiometricWakeLock();
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (LatencyTracker.isEnabled(this.mContext)) {
                int i = 2;
                if (biometricSourceType == BiometricSourceType.FACE) {
                    i = 6;
                }
                LatencyTracker.getInstance(this.mContext).onActionStart(i);
            }
            this.mWakeLock = this.mPowerManager.newWakeLock(1, "wake-and-unlock wakelock");
            Trace.beginSection("acquiring wake-and-unlock");
            this.mWakeLock.acquire();
            Trace.endSection();
            Log.i("BiometricUnlockController", "biometric acquired, grabbing biometric wakelock");
            this.mHandler.postDelayed(this.mReleaseBiometricWakeLockRunnable, 15000);
        }
        Trace.endSection();
    }

    private boolean pulsingOrAod() {
        ScrimState state = this.mScrimController.getState();
        return state == ScrimState.AOD || state == ScrimState.PULSING;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    /* renamed from: onBiometricAuthenticated */
    public void lambda$onFinishedGoingToSleep$1$BiometricUnlockController(int i, BiometricSourceType biometricSourceType) {
        Trace.beginSection("BiometricUnlockController#onBiometricAuthenticated");
        if (this.mUpdateMonitor.isGoingToSleep()) {
            this.mPendingAuthenticatedUserId = i;
            this.mPendingAuthenticatedBioSourceType = biometricSourceType;
            Trace.endSection();
            return;
        }
        this.mMetricsLogger.write(new LogMaker(1697).setType(10).setSubtype(toSubtype(biometricSourceType)));
        startWakeAndUnlock(calculateMode(biometricSourceType));
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0066, code lost:
        if (r5 != 7) goto L_0x00d2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startWakeAndUnlock(int r8) {
        /*
        // Method dump skipped, instructions count: 219
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.BiometricUnlockController.startWakeAndUnlock(int):void");
    }

    public /* synthetic */ void lambda$startWakeAndUnlock$0$BiometricUnlockController(boolean z, boolean z2) {
        if (!z) {
            Log.i("BiometricUnlockController", "bio wakelock: Authenticated, waking up...");
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 4, "android.policy:BIOMETRIC");
        }
        if (z2) {
            this.mKeyguardViewMediator.onWakeAndUnlocking();
        }
        Trace.beginSection("release wake-and-unlock");
        releaseBiometricWakeLock();
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showBouncer() {
        if (this.mMode == 3) {
            this.mStatusBarKeyguardViewManager.showBouncer(false);
        }
        this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
        this.mPendingShowBouncer = false;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onStartedGoingToSleep(int i) {
        resetMode();
        this.mFadedAwayAfterWakeAndUnlock = false;
        this.mPendingAuthenticatedUserId = -1;
        this.mPendingAuthenticatedBioSourceType = null;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onFinishedGoingToSleep(int i) {
        Trace.beginSection("BiometricUnlockController#onFinishedGoingToSleep");
        BiometricSourceType biometricSourceType = this.mPendingAuthenticatedBioSourceType;
        int i2 = this.mPendingAuthenticatedUserId;
        if (!(i2 == -1 || biometricSourceType == null)) {
            this.mHandler.post(new Runnable(i2, biometricSourceType) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$BiometricUnlockController$vuxdlMXJFOLKBJ7XnmJEfPu__e4 */
                private final /* synthetic */ int f$1;
                private final /* synthetic */ BiometricSourceType f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    BiometricUnlockController.this.lambda$onFinishedGoingToSleep$1$BiometricUnlockController(this.f$1, this.f$2);
                }
            });
        }
        this.mPendingAuthenticatedUserId = -1;
        this.mPendingAuthenticatedBioSourceType = null;
        Trace.endSection();
    }

    public boolean hasPendingAuthentication() {
        return this.mPendingAuthenticatedUserId != -1 && this.mUpdateMonitor.isUnlockingWithBiometricAllowed() && this.mPendingAuthenticatedUserId == KeyguardUpdateMonitor.getCurrentUser();
    }

    public int getMode() {
        return this.mMode;
    }

    private int calculateMode(BiometricSourceType biometricSourceType) {
        boolean isUnlockingWithBiometricAllowed = this.mUpdateMonitor.isUnlockingWithBiometricAllowed();
        boolean isDreaming = this.mUpdateMonitor.isDreaming();
        boolean z = biometricSourceType == BiometricSourceType.FACE && !this.mFaceDismissesKeyguard;
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (!this.mStatusBarKeyguardViewManager.isShowing()) {
                return 4;
            }
            if (!this.mDozeScrimController.isPulsing() || !isUnlockingWithBiometricAllowed) {
                if (isUnlockingWithBiometricAllowed || !this.mUnlockMethodCache.isMethodSecure()) {
                    return 1;
                }
                return 3;
            } else if (z) {
                return 0;
            } else {
                return 2;
            }
        } else if (isUnlockingWithBiometricAllowed && isDreaming && !z) {
            return 7;
        } else {
            if (this.mStatusBarKeyguardViewManager.isShowing()) {
                if ((this.mStatusBarKeyguardViewManager.isBouncerShowing() || this.mStatusBarKeyguardViewManager.isBouncerPartiallyVisible()) && isUnlockingWithBiometricAllowed) {
                    return 6;
                }
                if (isUnlockingWithBiometricAllowed) {
                    if (z) {
                        return 4;
                    }
                    return 5;
                } else if (!this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    return 3;
                }
            }
            return 0;
        }
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onBiometricAuthFailed(BiometricSourceType biometricSourceType) {
        this.mMetricsLogger.write(new LogMaker(1697).setType(11).setSubtype(toSubtype(biometricSourceType)));
        if (this.mUpdateMonitor.isDeviceInteractive() && !this.mStatusBarKeyguardViewManager.isBouncerShowing() && !this.mUpdateMonitor.isUnlockingWithBiometricAllowed()) {
            this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
        }
        cleanup();
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onBiometricError(int i, String str, BiometricSourceType biometricSourceType) {
        this.mMetricsLogger.write(new LogMaker(1697).setType(15).setSubtype(toSubtype(biometricSourceType)).addTaggedData(1741, Integer.valueOf(i)));
        cleanup();
    }

    private void cleanup() {
        releaseBiometricWakeLock();
    }

    public void startKeyguardFadingAway() {
        this.mHandler.postDelayed(new Runnable() {
            /* class com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass3 */

            public void run() {
                BiometricUnlockController.this.mStatusBarWindowController.setForceDozeBrightness(false);
            }
        }, 96);
    }

    public void finishKeyguardFadingAway() {
        if (isWakeAndUnlock()) {
            this.mFadedAwayAfterWakeAndUnlock = true;
        }
        resetMode();
    }

    private void resetMode() {
        this.mMode = 0;
        this.mStatusBarWindowController.setForceDozeBrightness(false);
        if (this.mStatusBar.getNavigationBarView() != null) {
            this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(false);
        }
        this.mStatusBar.notifyBiometricAuthModeChanged();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println(" BiometricUnlockController:");
        printWriter.print("   mMode=");
        printWriter.println(this.mMode);
        printWriter.print("   mWakeLock=");
        printWriter.println(this.mWakeLock);
    }

    public boolean isWakeAndUnlock() {
        int i = this.mMode;
        return i == 1 || i == 2 || i == 7;
    }

    public boolean unlockedByWakeAndUnlock() {
        return isWakeAndUnlock() || this.mFadedAwayAfterWakeAndUnlock;
    }

    public boolean isBiometricUnlock() {
        return isWakeAndUnlock() || this.mMode == 5;
    }

    /* access modifiers changed from: package-private */
    /* renamed from: com.android.systemui.statusbar.phone.BiometricUnlockController$6  reason: invalid class name */
    public static /* synthetic */ class AnonymousClass6 {
        static final /* synthetic */ int[] $SwitchMap$android$hardware$biometrics$BiometricSourceType = new int[BiometricSourceType.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(8:0|1|2|3|4|5|6|8) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        static {
            /*
                android.hardware.biometrics.BiometricSourceType[] r0 = android.hardware.biometrics.BiometricSourceType.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass6.$SwitchMap$android$hardware$biometrics$BiometricSourceType = r0
                int[] r0 = com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass6.$SwitchMap$android$hardware$biometrics$BiometricSourceType     // Catch:{ NoSuchFieldError -> 0x0014 }
                android.hardware.biometrics.BiometricSourceType r1 = android.hardware.biometrics.BiometricSourceType.FINGERPRINT     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass6.$SwitchMap$android$hardware$biometrics$BiometricSourceType     // Catch:{ NoSuchFieldError -> 0x001f }
                android.hardware.biometrics.BiometricSourceType r1 = android.hardware.biometrics.BiometricSourceType.FACE     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass6.$SwitchMap$android$hardware$biometrics$BiometricSourceType     // Catch:{ NoSuchFieldError -> 0x002a }
                android.hardware.biometrics.BiometricSourceType r1 = android.hardware.biometrics.BiometricSourceType.IRIS     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.BiometricUnlockController.AnonymousClass6.<clinit>():void");
        }
    }

    private int toSubtype(BiometricSourceType biometricSourceType) {
        int i = AnonymousClass6.$SwitchMap$android$hardware$biometrics$BiometricSourceType[biometricSourceType.ordinal()];
        if (i == 1) {
            return 0;
        }
        if (i != 2) {
            return i != 3 ? 3 : 2;
        }
        return 1;
    }
}
