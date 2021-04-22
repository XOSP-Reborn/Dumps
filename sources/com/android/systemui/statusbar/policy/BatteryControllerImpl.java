package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.fuelgauge.BatterySaverUtils;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.utils.PowerUtil;
import com.android.systemui.Dependency;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.statusbar.policy.BatteryController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BatteryControllerImpl extends BroadcastReceiver implements BatteryController {
    private static final boolean DEBUG = Log.isLoggable("BatteryController", 3);
    protected boolean mAodPowerSave;
    private final List<BatteryController.BatteryStateChangeCallback> mChangeCallbacks;
    protected boolean mCharged;
    protected boolean mCharging;
    private final Context mContext;
    private boolean mDemoMode;
    private Estimate mEstimate;
    private final EnhancedEstimates mEstimates;
    private final ArrayList<BatteryController.EstimateFetchCompletion> mFetchCallbacks;
    private boolean mFetchingEstimate;
    private final Handler mHandler;
    private boolean mHasReceivedBattery;
    protected int mLevel;
    protected boolean mPluggedIn;
    private final PowerManager mPowerManager;
    protected boolean mPowerSave;
    private boolean mTestmode;

    public BatteryControllerImpl(Context context, EnhancedEstimates enhancedEstimates) {
        this(context, enhancedEstimates, (PowerManager) context.getSystemService(PowerManager.class));
    }

    @VisibleForTesting
    BatteryControllerImpl(Context context, EnhancedEstimates enhancedEstimates, PowerManager powerManager) {
        this.mFetchCallbacks = new ArrayList<>();
        this.mChangeCallbacks = new CopyOnWriteArrayList();
        this.mTestmode = false;
        this.mHasReceivedBattery = false;
        this.mFetchingEstimate = false;
        this.mContext = context;
        this.mHandler = new Handler();
        this.mPowerManager = powerManager;
        this.mEstimates = enhancedEstimates;
        registerReceiver();
        updatePowerSave();
        updateEstimate();
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGING");
        intentFilter.addAction("com.android.systemui.BATTERY_LEVEL_TEST");
        this.mContext.registerReceiver(this, intentFilter);
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("BatteryController state:");
        printWriter.print("  mLevel=");
        printWriter.println(this.mLevel);
        printWriter.print("  mPluggedIn=");
        printWriter.println(this.mPluggedIn);
        printWriter.print("  mCharging=");
        printWriter.println(this.mCharging);
        printWriter.print("  mCharged=");
        printWriter.println(this.mCharged);
        printWriter.print("  mPowerSave=");
        printWriter.println(this.mPowerSave);
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController
    public void setPowerSaveMode(boolean z) {
        BatterySaverUtils.setPowerSaveMode(this.mContext, z, true);
    }

    public void addCallback(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        this.mChangeCallbacks.add(batteryStateChangeCallback);
        if (this.mHasReceivedBattery) {
            batteryStateChangeCallback.onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
            batteryStateChangeCallback.onPowerSaveChanged(this.mPowerSave);
        }
    }

    public void removeCallback(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        this.mChangeCallbacks.remove(batteryStateChangeCallback);
    }

    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        boolean z = true;
        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            if (!this.mTestmode || intent.getBooleanExtra("testmode", false)) {
                this.mHasReceivedBattery = true;
                this.mLevel = (int) ((((float) intent.getIntExtra("level", 0)) * 100.0f) / ((float) intent.getIntExtra("scale", 100)));
                this.mPluggedIn = intent.getIntExtra("plugged", 0) != 0;
                int intExtra = intent.getIntExtra("status", 1);
                this.mCharged = intExtra == 5;
                if (!this.mCharged && intExtra != 2) {
                    z = false;
                }
                this.mCharging = z;
                fireBatteryLevelChanged();
            }
        } else if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
            updatePowerSave();
        } else if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGING")) {
            setPowerSave(intent.getBooleanExtra("mode", false));
        } else if (action.equals("com.android.systemui.BATTERY_LEVEL_TEST")) {
            this.mTestmode = true;
            this.mHandler.post(new Runnable() {
                /* class com.android.systemui.statusbar.policy.BatteryControllerImpl.AnonymousClass1 */
                int curLevel = 0;
                Intent dummy;
                int incr = 1;
                int saveLevel;
                boolean savePlugged;

                {
                    BatteryControllerImpl batteryControllerImpl = BatteryControllerImpl.this;
                    this.saveLevel = batteryControllerImpl.mLevel;
                    this.savePlugged = batteryControllerImpl.mPluggedIn;
                    this.dummy = new Intent("android.intent.action.BATTERY_CHANGED");
                }

                public void run() {
                    int i = this.curLevel;
                    int i2 = 0;
                    if (i < 0) {
                        BatteryControllerImpl.this.mTestmode = false;
                        this.dummy.putExtra("level", this.saveLevel);
                        this.dummy.putExtra("plugged", this.savePlugged);
                        this.dummy.putExtra("testmode", false);
                    } else {
                        this.dummy.putExtra("level", i);
                        Intent intent = this.dummy;
                        if (this.incr > 0) {
                            i2 = 1;
                        }
                        intent.putExtra("plugged", i2);
                        this.dummy.putExtra("testmode", true);
                    }
                    context.sendBroadcast(this.dummy);
                    if (BatteryControllerImpl.this.mTestmode) {
                        int i3 = this.curLevel;
                        int i4 = this.incr;
                        this.curLevel = i3 + i4;
                        if (this.curLevel == 100) {
                            this.incr = i4 * -1;
                        }
                        BatteryControllerImpl.this.mHandler.postDelayed(this, 200);
                    }
                }
            });
        }
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController
    public boolean isPowerSave() {
        return this.mPowerSave;
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController
    public boolean isAodPowerSave() {
        return this.mAodPowerSave;
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController
    public void getEstimatedTimeRemainingString(BatteryController.EstimateFetchCompletion estimateFetchCompletion) {
        synchronized (this.mFetchCallbacks) {
            this.mFetchCallbacks.add(estimateFetchCompletion);
        }
        updateEstimateInBackground();
    }

    private String generateTimeRemainingString() {
        synchronized (this.mFetchCallbacks) {
            if (this.mEstimate == null) {
                return null;
            }
            NumberFormat.getPercentInstance().format(((double) this.mLevel) / 100.0d);
            return PowerUtil.getBatteryRemainingShortStringFormatted(this.mContext, this.mEstimate.getEstimateMillis());
        }
    }

    private void updateEstimateInBackground() {
        if (!this.mFetchingEstimate) {
            this.mFetchingEstimate = true;
            ((Handler) Dependency.get(Dependency.BG_HANDLER)).post(new Runnable() {
                /* class com.android.systemui.statusbar.policy.$$Lambda$BatteryControllerImpl$Q2m5_jQFbUIrN5x5MkihyCoos8 */

                public final void run() {
                    BatteryControllerImpl.this.lambda$updateEstimateInBackground$0$BatteryControllerImpl();
                }
            });
        }
    }

    public /* synthetic */ void lambda$updateEstimateInBackground$0$BatteryControllerImpl() {
        synchronized (this.mFetchCallbacks) {
            this.mEstimate = null;
            if (this.mEstimates.isHybridNotificationEnabled()) {
                updateEstimate();
            }
        }
        this.mFetchingEstimate = false;
        ((Handler) Dependency.get(Dependency.MAIN_HANDLER)).post(new Runnable() {
            /* class com.android.systemui.statusbar.policy.$$Lambda$BatteryControllerImpl$xVvPxv9usTpbGvWx3jH4_VH1nvI */

            public final void run() {
                BatteryControllerImpl.this.notifyEstimateFetchCallbacks();
            }
        });
    }

    /* access modifiers changed from: private */
    public void notifyEstimateFetchCallbacks() {
        synchronized (this.mFetchCallbacks) {
            String generateTimeRemainingString = generateTimeRemainingString();
            Iterator<BatteryController.EstimateFetchCompletion> it = this.mFetchCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onBatteryRemainingEstimateRetrieved(generateTimeRemainingString);
            }
            this.mFetchCallbacks.clear();
        }
    }

    private void updateEstimate() {
        this.mEstimate = Estimate.getCachedEstimateIfAvailable(this.mContext);
        if (this.mEstimate == null) {
            this.mEstimate = this.mEstimates.getEstimate();
            Estimate estimate = this.mEstimate;
            if (estimate != null) {
                Estimate.storeCachedEstimate(this.mContext, estimate);
            }
        }
    }

    private void updatePowerSave() {
        setPowerSave(this.mPowerManager.isPowerSaveMode());
    }

    private void setPowerSave(boolean z) {
        if (z != this.mPowerSave) {
            this.mPowerSave = z;
            this.mAodPowerSave = this.mPowerManager.getPowerSaveState(14).batterySaverEnabled;
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Power save is ");
                sb.append(this.mPowerSave ? "on" : "off");
                Log.d("BatteryController", sb.toString());
            }
            firePowerSaveChanged();
        }
    }

    /* access modifiers changed from: protected */
    public void fireBatteryLevelChanged() {
        this.mChangeCallbacks.forEach(new Consumer() {
            /* class com.android.systemui.statusbar.policy.$$Lambda$BatteryControllerImpl$ITMQYFRjQy6O8MQ8BATrOTWYQiY */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BatteryControllerImpl.this.lambda$fireBatteryLevelChanged$1$BatteryControllerImpl((BatteryController.BatteryStateChangeCallback) obj);
            }
        });
    }

    public /* synthetic */ void lambda$fireBatteryLevelChanged$1$BatteryControllerImpl(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        batteryStateChangeCallback.onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
    }

    private void firePowerSaveChanged() {
        this.mChangeCallbacks.forEach(new Consumer() {
            /* class com.android.systemui.statusbar.policy.$$Lambda$BatteryControllerImpl$dmMSualt6qNbla6XCGADzO_iWk */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BatteryControllerImpl.this.lambda$firePowerSaveChanged$2$BatteryControllerImpl((BatteryController.BatteryStateChangeCallback) obj);
            }
        });
    }

    public /* synthetic */ void lambda$firePowerSaveChanged$2$BatteryControllerImpl(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        batteryStateChangeCallback.onPowerSaveChanged(this.mPowerSave);
    }

    @Override // com.android.systemui.DemoMode
    public void dispatchDemoCommand(String str, Bundle bundle) {
        if (!this.mDemoMode && str.equals("enter")) {
            this.mDemoMode = true;
            this.mContext.unregisterReceiver(this);
        } else if (this.mDemoMode && str.equals("exit")) {
            this.mDemoMode = false;
            registerReceiver();
            updatePowerSave();
        } else if (this.mDemoMode && str.equals("battery")) {
            String string = bundle.getString("level");
            String string2 = bundle.getString("plugged");
            String string3 = bundle.getString("powersave");
            if (string != null) {
                this.mLevel = Math.min(Math.max(Integer.parseInt(string), 0), 100);
            }
            if (string2 != null) {
                this.mPluggedIn = Boolean.parseBoolean(string2);
            }
            if (string3 != null) {
                this.mPowerSave = string3.equals("true");
                firePowerSaveChanged();
            }
            fireBatteryLevelChanged();
        }
    }
}
