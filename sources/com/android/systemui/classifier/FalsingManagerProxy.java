package com.android.systemui.classifier;

import android.content.Context;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.DeviceConfig;
import android.view.MotionEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.classifier.brightline.BrightLineFalsingManager;
import com.android.systemui.classifier.brightline.FalsingDataProvider;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.FalsingPlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.util.AsyncSensorManager;
import java.io.PrintWriter;
import java.util.concurrent.Executor;

public class FalsingManagerProxy implements FalsingManager {
    private FalsingManager mInternalFalsingManager;
    private final Handler mMainHandler;

    FalsingManagerProxy(final Context context, PluginManager pluginManager, Handler handler) {
        this.mMainHandler = handler;
        DeviceConfig.addOnPropertiesChangedListener("systemui", new Executor() {
            /* class com.android.systemui.classifier.$$Lambda$FalsingManagerProxy$qZ6lxH8vX6Mj0Cv4e94eYSfUGA */

            public final void execute(Runnable runnable) {
                FalsingManagerProxy.this.lambda$new$0$FalsingManagerProxy(runnable);
            }
        }, new DeviceConfig.OnPropertiesChangedListener(context) {
            /* class com.android.systemui.classifier.$$Lambda$FalsingManagerProxy$gca_JCTVGHkvAjBNMeOIeE6opNs */
            private final /* synthetic */ Context f$1;

            {
                this.f$1 = r2;
            }

            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                FalsingManagerProxy.this.lambda$new$1$FalsingManagerProxy(this.f$1, properties);
            }
        });
        setupFalsingManager(context);
        pluginManager.addPluginListener(new PluginListener<FalsingPlugin>() {
            /* class com.android.systemui.classifier.FalsingManagerProxy.AnonymousClass1 */

            public void onPluginConnected(FalsingPlugin falsingPlugin, Context context) {
                FalsingManager falsingManager = falsingPlugin.getFalsingManager(context);
                if (falsingManager != null) {
                    FalsingManagerProxy.this.mInternalFalsingManager.cleanup();
                    FalsingManagerProxy.this.mInternalFalsingManager = falsingManager;
                }
            }

            public void onPluginDisconnected(FalsingPlugin falsingPlugin) {
                FalsingManagerProxy.this.mInternalFalsingManager = new FalsingManagerImpl(context);
            }
        }, FalsingPlugin.class);
    }

    public /* synthetic */ void lambda$new$0$FalsingManagerProxy(Runnable runnable) {
        this.mMainHandler.post(runnable);
    }

    public /* synthetic */ void lambda$new$1$FalsingManagerProxy(Context context, DeviceConfig.Properties properties) {
        onDeviceConfigPropertiesChanged(context, properties.getNamespace());
    }

    private void onDeviceConfigPropertiesChanged(Context context, String str) {
        if ("systemui".equals(str)) {
            setupFalsingManager(context);
        }
    }

    @VisibleForTesting
    public void setupFalsingManager(Context context) {
        boolean z = DeviceConfig.getBoolean("systemui", "brightline_falsing_manager_enabled", true);
        FalsingManager falsingManager = this.mInternalFalsingManager;
        if (falsingManager != null) {
            falsingManager.cleanup();
        }
        if (!z) {
            this.mInternalFalsingManager = new FalsingManagerImpl(context);
        } else {
            this.mInternalFalsingManager = new BrightLineFalsingManager(new FalsingDataProvider(context.getResources().getDisplayMetrics()), (SensorManager) Dependency.get(AsyncSensorManager.class));
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public FalsingManager getInternalFalsingManager() {
        return this.mInternalFalsingManager;
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onSucccessfulUnlock() {
        this.mInternalFalsingManager.onSucccessfulUnlock();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificationActive() {
        this.mInternalFalsingManager.onNotificationActive();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void setShowingAod(boolean z) {
        this.mInternalFalsingManager.setShowingAod(z);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStartDraggingDown() {
        this.mInternalFalsingManager.onNotificatonStartDraggingDown();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isUnlockingDisabled() {
        return this.mInternalFalsingManager.isUnlockingDisabled();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isFalseTouch() {
        return this.mInternalFalsingManager.isFalseTouch();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStopDraggingDown() {
        this.mInternalFalsingManager.onNotificatonStartDraggingDown();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void setNotificationExpanded() {
        this.mInternalFalsingManager.setNotificationExpanded();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isClassiferEnabled() {
        return this.mInternalFalsingManager.isClassiferEnabled();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onQsDown() {
        this.mInternalFalsingManager.onQsDown();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void setQsExpanded(boolean z) {
        this.mInternalFalsingManager.setQsExpanded(z);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean shouldEnforceBouncer() {
        return this.mInternalFalsingManager.shouldEnforceBouncer();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onTrackingStarted(boolean z) {
        this.mInternalFalsingManager.onTrackingStarted(z);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onTrackingStopped() {
        this.mInternalFalsingManager.onTrackingStopped();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onLeftAffordanceOn() {
        this.mInternalFalsingManager.onLeftAffordanceOn();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onCameraOn() {
        this.mInternalFalsingManager.onCameraOn();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onAffordanceSwipingStarted(boolean z) {
        this.mInternalFalsingManager.onAffordanceSwipingStarted(z);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onAffordanceSwipingAborted() {
        this.mInternalFalsingManager.onAffordanceSwipingAborted();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onStartExpandingFromPulse() {
        this.mInternalFalsingManager.onStartExpandingFromPulse();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onExpansionFromPulseStopped() {
        this.mInternalFalsingManager.onExpansionFromPulseStopped();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public Uri reportRejectedTouch() {
        return this.mInternalFalsingManager.reportRejectedTouch();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onScreenOnFromTouch() {
        this.mInternalFalsingManager.onScreenOnFromTouch();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isReportingEnabled() {
        return this.mInternalFalsingManager.isReportingEnabled();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onUnlockHintStarted() {
        this.mInternalFalsingManager.onUnlockHintStarted();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onCameraHintStarted() {
        this.mInternalFalsingManager.onCameraHintStarted();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onLeftAffordanceHintStarted() {
        this.mInternalFalsingManager.onLeftAffordanceHintStarted();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onScreenTurningOn() {
        this.mInternalFalsingManager.onScreenTurningOn();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onScreenOff() {
        this.mInternalFalsingManager.onScreenOff();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStopDismissing() {
        this.mInternalFalsingManager.onNotificatonStopDismissing();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificationDismissed() {
        this.mInternalFalsingManager.onNotificationDismissed();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStartDismissing() {
        this.mInternalFalsingManager.onNotificatonStartDismissing();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificationDoubleTap(boolean z, float f, float f2) {
        this.mInternalFalsingManager.onNotificationDoubleTap(z, f, f2);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onBouncerShown() {
        this.mInternalFalsingManager.onBouncerShown();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onBouncerHidden() {
        this.mInternalFalsingManager.onBouncerHidden();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onTouchEvent(MotionEvent motionEvent, int i, int i2) {
        this.mInternalFalsingManager.onTouchEvent(motionEvent, i, i2);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void dump(PrintWriter printWriter) {
        this.mInternalFalsingManager.dump(printWriter);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void cleanup() {
        this.mInternalFalsingManager.cleanup();
    }
}
