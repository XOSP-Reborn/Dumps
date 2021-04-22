package com.android.systemui.assist;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.DeviceConfig;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.AssistUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.DumpController;
import com.android.systemui.Dumpable;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.phone.NavigationModeController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class AssistHandleBehaviorController implements AssistHandleCallbacks, Dumpable {
    private static final AssistHandleBehavior DEFAULT_BEHAVIOR = AssistHandleBehavior.REMINDER_EXP;
    private static final long DEFAULT_SHOW_AND_GO_DURATION_MS = TimeUnit.SECONDS.toMillis(3);
    private final AssistUtils mAssistUtils;
    private final Map<AssistHandleBehavior, BehaviorController> mBehaviorMap;
    private final Context mContext;
    private AssistHandleBehavior mCurrentBehavior;
    private final Handler mHandler;
    private long mHandlesLastHiddenAt;
    private boolean mHandlesShowing;
    private final Runnable mHideHandles;
    private boolean mInGesturalMode;
    private final PhenotypeHelper mPhenotypeHelper;
    private final Supplier<ScreenDecorations> mScreenDecorationsSupplier;
    private final Runnable mShowAndGo;

    /* access modifiers changed from: package-private */
    public interface BehaviorController {
        default void dump(PrintWriter printWriter, String str) {
        }

        default void onAssistantGesturePerformed() {
        }

        void onModeActivated(Context context, AssistHandleCallbacks assistHandleCallbacks);

        default void onModeDeactivated() {
        }
    }

    AssistHandleBehaviorController(Context context, AssistUtils assistUtils, Handler handler) {
        this(context, assistUtils, handler, new Supplier(context) {
            /* class com.android.systemui.assist.$$Lambda$AssistHandleBehaviorController$YVZbhjPAFxzfykYNnZr_WVxBbM */
            private final /* synthetic */ Context f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return AssistHandleBehaviorController.lambda$new$0(this.f$0);
            }
        }, new PhenotypeHelper(), null);
    }

    static /* synthetic */ ScreenDecorations lambda$new$0(Context context) {
        return (ScreenDecorations) SysUiServiceProvider.getComponent(context, ScreenDecorations.class);
    }

    @VisibleForTesting
    AssistHandleBehaviorController(Context context, AssistUtils assistUtils, Handler handler, Supplier<ScreenDecorations> supplier, PhenotypeHelper phenotypeHelper, BehaviorController behaviorController) {
        this.mHideHandles = new Runnable() {
            /* class com.android.systemui.assist.$$Lambda$AssistHandleBehaviorController$XubZVLOT9vWCBnLQqZRgbOELVA */

            public final void run() {
                AssistHandleBehaviorController.m5lambda$XubZVLOT9vWCBnLQqZRgbOELVA(AssistHandleBehaviorController.this);
            }
        };
        this.mShowAndGo = new Runnable() {
            /* class com.android.systemui.assist.$$Lambda$AssistHandleBehaviorController$oeveMWAQo5jd5bG1H5Ci7Dy4X74 */

            public final void run() {
                AssistHandleBehaviorController.lambda$oeveMWAQo5jd5bG1H5Ci7Dy4X74(AssistHandleBehaviorController.this);
            }
        };
        this.mBehaviorMap = new EnumMap(AssistHandleBehavior.class);
        this.mHandlesShowing = false;
        AssistHandleBehavior assistHandleBehavior = AssistHandleBehavior.OFF;
        this.mCurrentBehavior = assistHandleBehavior;
        this.mContext = context;
        this.mAssistUtils = assistUtils;
        this.mHandler = handler;
        this.mScreenDecorationsSupplier = supplier;
        this.mPhenotypeHelper = phenotypeHelper;
        this.mBehaviorMap.put(assistHandleBehavior, new AssistHandleOffBehavior());
        this.mBehaviorMap.put(AssistHandleBehavior.LIKE_HOME, new AssistHandleLikeHomeBehavior());
        this.mBehaviorMap.put(AssistHandleBehavior.REMINDER_EXP, new AssistHandleReminderExpBehavior(handler, phenotypeHelper));
        if (behaviorController != null) {
            this.mBehaviorMap.put(AssistHandleBehavior.TEST, behaviorController);
        }
        this.mInGesturalMode = QuickStepContract.isGesturalMode(((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(new NavigationModeController.ModeChangedListener() {
            /* class com.android.systemui.assist.$$Lambda$AssistHandleBehaviorController$UX7PPcltnlTgxyL7MxmLbVmQRcI */

            @Override // com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener
            public final void onNavigationModeChanged(int i) {
                AssistHandleBehaviorController.lambda$UX7PPcltnlTgxyL7MxmLbVmQRcI(AssistHandleBehaviorController.this, i);
            }
        }));
        setBehavior(getBehaviorMode());
        PhenotypeHelper phenotypeHelper2 = this.mPhenotypeHelper;
        Handler handler2 = this.mHandler;
        Objects.requireNonNull(handler2);
        phenotypeHelper2.addOnPropertiesChangedListener(new Executor(handler2) {
            /* class com.android.systemui.assist.$$Lambda$LfzJt661qZfn2w6SYHFbD3aMy0 */
            private final /* synthetic */ Handler f$0;

            {
                this.f$0 = r1;
            }

            public final void execute(Runnable runnable) {
                this.f$0.post(runnable);
            }
        }, new DeviceConfig.OnPropertiesChangedListener() {
            /* class com.android.systemui.assist.$$Lambda$AssistHandleBehaviorController$lsfSpSsIpcB8nkelv4RlnknWrbw */

            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                AssistHandleBehaviorController.this.lambda$new$1$AssistHandleBehaviorController(properties);
            }
        });
        ((DumpController) Dependency.get(DumpController.class)).addListener(this);
    }

    public /* synthetic */ void lambda$new$1$AssistHandleBehaviorController(DeviceConfig.Properties properties) {
        if (properties.getKeyset().contains("assist_handles_behavior_mode")) {
            setBehavior(properties.getString("assist_handles_behavior_mode", (String) null));
        }
    }

    @Override // com.android.systemui.assist.AssistHandleCallbacks
    public void hide() {
        clearPendingCommands();
        this.mHandler.post(this.mHideHandles);
    }

    @Override // com.android.systemui.assist.AssistHandleCallbacks
    public void showAndGo() {
        clearPendingCommands();
        this.mHandler.post(this.mShowAndGo);
    }

    /* access modifiers changed from: private */
    public void showAndGoInternal() {
        maybeShowHandles(false);
        this.mHandler.postDelayed(this.mHideHandles, getShowAndGoDuration());
    }

    @Override // com.android.systemui.assist.AssistHandleCallbacks
    public void showAndGoDelayed(long j, boolean z) {
        clearPendingCommands();
        if (z) {
            this.mHandler.post(this.mHideHandles);
        }
        this.mHandler.postDelayed(this.mShowAndGo, j);
    }

    @Override // com.android.systemui.assist.AssistHandleCallbacks
    public void showAndStay() {
        clearPendingCommands();
        this.mHandler.post(new Runnable() {
            /* class com.android.systemui.assist.$$Lambda$AssistHandleBehaviorController$YuzRQKX_f6TiNUH219GL9e2kG8 */

            public final void run() {
                AssistHandleBehaviorController.this.lambda$showAndStay$2$AssistHandleBehaviorController();
            }
        });
    }

    public /* synthetic */ void lambda$showAndStay$2$AssistHandleBehaviorController() {
        maybeShowHandles(true);
    }

    /* access modifiers changed from: package-private */
    public boolean areHandlesShowing() {
        return this.mHandlesShowing;
    }

    /* access modifiers changed from: package-private */
    public void onAssistantGesturePerformed() {
        this.mBehaviorMap.get(this.mCurrentBehavior).onAssistantGesturePerformed();
    }

    /* access modifiers changed from: package-private */
    public void setBehavior(AssistHandleBehavior assistHandleBehavior) {
        if (this.mCurrentBehavior != assistHandleBehavior) {
            if (!this.mBehaviorMap.containsKey(assistHandleBehavior)) {
                Log.e("AssistHandleBehavior", "Unsupported behavior requested: " + assistHandleBehavior.toString());
                return;
            }
            if (this.mInGesturalMode) {
                this.mBehaviorMap.get(this.mCurrentBehavior).onModeDeactivated();
                this.mBehaviorMap.get(assistHandleBehavior).onModeActivated(this.mContext, this);
            }
            this.mCurrentBehavior = assistHandleBehavior;
        }
    }

    private void setBehavior(String str) {
        try {
            setBehavior(AssistHandleBehavior.valueOf(str));
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.e("AssistHandleBehavior", "Invalid behavior: " + str, e);
        }
    }

    private boolean handlesUnblocked(boolean z) {
        return (z || ((SystemClock.elapsedRealtime() - this.mHandlesLastHiddenAt) > getShownFrequencyThreshold() ? 1 : ((SystemClock.elapsedRealtime() - this.mHandlesLastHiddenAt) == getShownFrequencyThreshold() ? 0 : -1)) >= 0) && this.mAssistUtils.getAssistComponentForUser(KeyguardUpdateMonitor.getCurrentUser()) != null;
    }

    private long getShownFrequencyThreshold() {
        return this.mPhenotypeHelper.getLong("assist_handles_shown_frequency_threshold_ms", 0);
    }

    private long getShowAndGoDuration() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_duration_ms", DEFAULT_SHOW_AND_GO_DURATION_MS);
    }

    private String getBehaviorMode() {
        return this.mPhenotypeHelper.getString("assist_handles_behavior_mode", DEFAULT_BEHAVIOR.toString());
    }

    private void maybeShowHandles(boolean z) {
        if (!this.mHandlesShowing && handlesUnblocked(z)) {
            ScreenDecorations screenDecorations = this.mScreenDecorationsSupplier.get();
            if (screenDecorations == null) {
                Log.w("AssistHandleBehavior", "Couldn't show handles, ScreenDecorations unavailable");
                return;
            }
            this.mHandlesShowing = true;
            screenDecorations.lambda$setAssistHintVisible$1$ScreenDecorations(true);
        }
    }

    /* access modifiers changed from: private */
    public void hideHandles() {
        if (this.mHandlesShowing) {
            ScreenDecorations screenDecorations = this.mScreenDecorationsSupplier.get();
            if (screenDecorations == null) {
                Log.w("AssistHandleBehavior", "Couldn't hide handles, ScreenDecorations unavailable");
                return;
            }
            this.mHandlesShowing = false;
            this.mHandlesLastHiddenAt = SystemClock.elapsedRealtime();
            screenDecorations.lambda$setAssistHintVisible$1$ScreenDecorations(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleNavigationModeChange(int i) {
        boolean isGesturalMode = QuickStepContract.isGesturalMode(i);
        if (this.mInGesturalMode != isGesturalMode) {
            this.mInGesturalMode = isGesturalMode;
            if (this.mInGesturalMode) {
                this.mBehaviorMap.get(this.mCurrentBehavior).onModeActivated(this.mContext, this);
                return;
            }
            this.mBehaviorMap.get(this.mCurrentBehavior).onModeDeactivated();
            hide();
        }
    }

    private void clearPendingCommands() {
        this.mHandler.removeCallbacks(this.mHideHandles);
        this.mHandler.removeCallbacks(this.mShowAndGo);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setInGesturalModeForTest(boolean z) {
        this.mInGesturalMode = z;
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Current AssistHandleBehaviorController State:");
        printWriter.println("   mHandlesShowing=" + this.mHandlesShowing);
        printWriter.println("   mHandlesLastHiddenAt=" + this.mHandlesLastHiddenAt);
        printWriter.println("   mInGesturalMode=" + this.mInGesturalMode);
        printWriter.println("   Phenotype Flags:");
        printWriter.println("      assist_handles_show_and_go_duration_ms=" + getShowAndGoDuration());
        printWriter.println("      assist_handles_shown_frequency_threshold_ms=" + getShownFrequencyThreshold());
        printWriter.println("      assist_handles_behavior_mode=" + getBehaviorMode());
        printWriter.println("   mCurrentBehavior=" + this.mCurrentBehavior.toString());
        this.mBehaviorMap.get(this.mCurrentBehavior).dump(printWriter, "   ");
    }
}
