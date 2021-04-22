package com.android.keyguard;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.CarrierTextController;
import com.android.systemui.C0001R$array;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.statusbar.policy.FiveGServiceClient;
import java.util.ArrayList;
import java.util.List;

public class CarrierTextController {
    private int mActiveMobileDataSubscription = -1;
    protected final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.android.keyguard.CarrierTextController.AnonymousClass2 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onRefreshCarrierInfo() {
            CarrierTextController.this.updateCarrierText();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onTelephonyCapable(boolean z) {
            CarrierTextController.this.mTelephonyCapable = z;
            CarrierTextController.this.updateCarrierText();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onSimStateChanged(int i, int i2, IccCardConstants.State state) {
            if (i2 < 0 || i2 >= CarrierTextController.this.mSimSlotsNumber) {
                Log.d("CarrierTextController", "onSimStateChanged() - slotId invalid: " + i2 + " mTelephonyCapable: " + Boolean.toString(CarrierTextController.this.mTelephonyCapable));
            } else if (CarrierTextController.this.getStatusForIccState(state) == StatusMode.SimIoError) {
                CarrierTextController.this.mSimErrorState[i2] = true;
                CarrierTextController.this.updateCarrierText();
            } else if (CarrierTextController.this.mSimErrorState[i2]) {
                CarrierTextController.this.mSimErrorState[i2] = false;
                CarrierTextController.this.updateCarrierText();
            }
        }
    };
    private CarrierTextCallback mCarrierTextCallback;
    private Context mContext;
    protected boolean mDisplayOpportunisticSubscriptionCarrierText;
    private FiveGServiceClient mFiveGServiceClient;
    private final boolean mIsEmergencyCallCapable;
    private final boolean mIsUseDocomoText;
    protected KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /* class com.android.keyguard.CarrierTextController.AnonymousClass3 */

        public void onActiveDataSubscriptionIdChanged(int i) {
            CarrierTextController.this.mActiveMobileDataSubscription = i;
            CarrierTextController carrierTextController = CarrierTextController.this;
            if (carrierTextController.mKeyguardUpdateMonitor != null) {
                carrierTextController.updateCarrierText();
            }
        }
    };
    private CharSequence mSeparator;
    private boolean mShowAirplaneMode;
    private boolean mShowMissingSim;
    private boolean[] mSimErrorState;
    private final int mSimSlotsNumber;
    private boolean mTelephonyCapable;
    private WakefulnessLifecycle mWakefulnessLifecycle;
    private final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
        /* class com.android.keyguard.CarrierTextController.AnonymousClass1 */

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onFinishedWakingUp() {
            CarrierTextController.this.mCarrierTextCallback.finishedWakingUp();
        }

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onStartedGoingToSleep() {
            CarrierTextController.this.mCarrierTextCallback.startedGoingToSleep();
        }
    };
    private WifiManager mWifiManager;

    public interface CarrierTextCallback {
        default void finishedWakingUp() {
        }

        default boolean isInsideEmergencyCarrierArea() {
            return false;
        }

        default void startedGoingToSleep() {
        }

        default void updateCarrierInfo(CarrierTextCallbackInfo carrierTextCallbackInfo) {
        }
    }

    /* access modifiers changed from: private */
    public enum StatusMode {
        Normal,
        NetworkLocked,
        SimMissing,
        SimMissingLocked,
        SimPukLocked,
        SimLocked,
        SimPermDisabled,
        SimNotReady,
        SimIoError,
        SimUnknown
    }

    public CarrierTextController(Context context, CharSequence charSequence, boolean z, boolean z2) {
        this.mContext = context;
        this.mIsEmergencyCallCapable = context.getResources().getBoolean(17891589);
        this.mShowAirplaneMode = z;
        this.mShowMissingSim = z2;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mSeparator = charSequence;
        this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
        this.mSimSlotsNumber = ((TelephonyManager) context.getSystemService("phone")).getPhoneCount();
        this.mSimErrorState = new boolean[this.mSimSlotsNumber];
        updateDisplayOpportunisticSubscriptionCarrierText(SystemProperties.getBoolean("persist.radio.display_opportunistic_carrier", false));
        this.mIsUseDocomoText = context.getResources().getBoolean(R$bool.somc_keyguard_docomo_device);
    }

    private CharSequence updateCarrierTextWithSimIoError(CharSequence charSequence, CharSequence[] charSequenceArr, int[] iArr, boolean z) {
        CharSequence carrierTextForSimState = getCarrierTextForSimState(IccCardConstants.State.CARD_IO_ERROR, "");
        int i = 0;
        while (true) {
            boolean[] zArr = this.mSimErrorState;
            if (i >= zArr.length) {
                return charSequence;
            }
            if (zArr[i]) {
                if (z) {
                    return concatenate(carrierTextForSimState, getContext().getText(17039960), this.mSeparator);
                }
                if (iArr[i] != -1) {
                    int i2 = iArr[i];
                    charSequenceArr[i2] = concatenate(carrierTextForSimState, charSequenceArr[i2], this.mSeparator);
                } else {
                    charSequence = concatenate(charSequence, carrierTextForSimState, this.mSeparator);
                }
            }
            i++;
        }
    }

    public void setListening(CarrierTextCallback carrierTextCallback) {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (carrierTextCallback != null) {
            this.mCarrierTextCallback = carrierTextCallback;
            if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
                this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
                this.mKeyguardUpdateMonitor.registerCallback(this.mCallback);
                this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
                telephonyManager.listen(this.mPhoneStateListener, 4194304);
                return;
            }
            this.mKeyguardUpdateMonitor = null;
            carrierTextCallback.updateCarrierInfo(new CarrierTextCallbackInfo("", null, false, null));
            return;
        }
        this.mCarrierTextCallback = null;
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
        if (keyguardUpdateMonitor != null) {
            keyguardUpdateMonitor.removeCallback(this.mCallback);
            this.mWakefulnessLifecycle.removeObserver(this.mWakefulnessObserver);
        }
        telephonyManager.listen(this.mPhoneStateListener, 0);
    }

    private void filterMobileSubscriptionInSameGroup(List<SubscriptionInfo> list) {
        if (list.size() == 2) {
            SubscriptionInfo subscriptionInfo = list.get(0);
            SubscriptionInfo subscriptionInfo2 = list.get(1);
            if (subscriptionInfo.getGroupUuid() != null && subscriptionInfo.getGroupUuid().equals(subscriptionInfo2.getGroupUuid())) {
                if (!subscriptionInfo.isOpportunistic() && !subscriptionInfo2.isOpportunistic()) {
                    return;
                }
                if (CarrierConfigManager.getDefaultConfig().getBoolean("always_show_primary_signal_bar_in_opportunistic_network_boolean")) {
                    if (!subscriptionInfo.isOpportunistic()) {
                        subscriptionInfo = subscriptionInfo2;
                    }
                    list.remove(subscriptionInfo);
                    return;
                }
                if (subscriptionInfo.getSubscriptionId() == this.mActiveMobileDataSubscription) {
                    subscriptionInfo = subscriptionInfo2;
                }
                list.remove(subscriptionInfo);
            }
        }
    }

    public void updateDisplayOpportunisticSubscriptionCarrierText(boolean z) {
        this.mDisplayOpportunisticSubscriptionCarrierText = z;
    }

    /* access modifiers changed from: protected */
    public List<SubscriptionInfo> getSubscriptionInfo() {
        if (!this.mDisplayOpportunisticSubscriptionCarrierText) {
            return this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        }
        List activeSubscriptionInfoList = ((SubscriptionManager) this.mContext.getSystemService("telephony_subscription_service")).getActiveSubscriptionInfoList(false);
        if (activeSubscriptionInfoList == null) {
            return new ArrayList();
        }
        filterMobileSubscriptionInSameGroup(activeSubscriptionInfoList);
        return activeSubscriptionInfoList;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x01b9  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01c6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateCarrierText() {
        /*
        // Method dump skipped, instructions count: 496
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.CarrierTextController.updateCarrierText():void");
    }

    /* access modifiers changed from: protected */
    public void postToCallback(CarrierTextCallbackInfo carrierTextCallbackInfo) {
        Handler handler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
        CarrierTextCallback carrierTextCallback = this.mCarrierTextCallback;
        if (carrierTextCallback != null) {
            handler.post(new Runnable(carrierTextCallbackInfo) {
                /* class com.android.keyguard.$$Lambda$CarrierTextController$MiJe6zX1bpo5TwEBp8HSL1qzz0 */
                private final /* synthetic */ CarrierTextController.CarrierTextCallbackInfo f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    CarrierTextController.CarrierTextCallback.this.updateCarrierInfo(this.f$1);
                }
            });
        }
    }

    private Context getContext() {
        return this.mContext;
    }

    private String getMissingSimMessage() {
        return (!this.mShowMissingSim || !this.mTelephonyCapable) ? "" : getContext().getString(R$string.keyguard_missing_sim_message_short);
    }

    private String getAirplaneModeMessage() {
        return this.mShowAirplaneMode ? getContext().getString(R$string.airplane_mode) : "";
    }

    private CharSequence getCarrierTextForSimState(IccCardConstants.State state, CharSequence charSequence) {
        int i;
        int i2;
        int i3;
        switch (AnonymousClass4.$SwitchMap$com$android$keyguard$CarrierTextController$StatusMode[getStatusForIccState(state).ordinal()]) {
            case 1:
                return charSequence == null ? getContext().getText(R$string.keyguard_carrier_default) : charSequence;
            case 2:
                return charSequence == null ? getContext().getText(R$string.keyguard_carrier_default) : charSequence;
            case 3:
                CharSequence text = this.mContext.getText(R$string.keyguard_perso_locked_message);
                if (!this.mContext.getResources().getBoolean(R$bool.somc_add_emergency_call_info_to_persolocked_carrier_text)) {
                    charSequence = null;
                }
                return makeCarrierStringOnEmergencyCapable(text, charSequence);
            case 4:
            case 10:
            default:
                return null;
            case 5:
                Context context = getContext();
                if (this.mIsUseDocomoText) {
                    i = R$string.lockscreen_permanent_disabled_sim_message_short_nexti;
                } else {
                    i = R$string.keyguard_permanent_disabled_sim_message_short;
                }
                return makeCarrierStringOnEmergencyCapable(context.getText(i), charSequence);
            case 6:
                CharSequence text2 = getContext().getText(R$string.keyguard_missing_sim_message_short);
                if (!getContext().getResources().getBoolean(R$bool.somc_add_emergency_call_info_to_sim_missing_carrier_text)) {
                    charSequence = null;
                }
                return makeCarrierStringOnEmergencyCapable(text2, charSequence);
            case 7:
                if (charSequence == null) {
                    charSequence = getContext().getText(R$string.keyguard_carrier_default);
                }
                Context context2 = getContext();
                if (this.mIsUseDocomoText) {
                    i2 = R$string.lockscreen_sim_locked_message_nexti;
                } else {
                    i2 = R$string.keyguard_sim_locked_message;
                }
                return makeCarrierStringOnLocked(context2.getText(i2), charSequence);
            case 8:
                if (charSequence == null) {
                    charSequence = getContext().getText(R$string.keyguard_carrier_default);
                }
                Context context3 = getContext();
                if (this.mIsUseDocomoText) {
                    i3 = R$string.lockscreen_sim_puk_locked_message_nexti;
                } else {
                    i3 = R$string.keyguard_sim_puk_locked_message;
                }
                return makeCarrierStringOnLocked(context3.getText(i3), charSequence);
            case 9:
                return makeCarrierStringOnEmergencyCapable(getContext().getText(R$string.keyguard_sim_error_message_short), charSequence);
        }
    }

    private CharSequence makeCarrierStringOnEmergencyCapable(CharSequence charSequence, CharSequence charSequence2) {
        return this.mIsEmergencyCallCapable ? concatenate(charSequence, charSequence2, this.mSeparator) : charSequence;
    }

    private CharSequence makeCarrierStringOnLocked(CharSequence charSequence, CharSequence charSequence2) {
        boolean z = !TextUtils.isEmpty(charSequence);
        boolean z2 = !TextUtils.isEmpty(charSequence2);
        if (z && z2) {
            return this.mContext.getString(R$string.keyguard_carrier_name_with_sim_locked_template, charSequence2, charSequence);
        } else if (z) {
            return charSequence;
        } else {
            return z2 ? charSequence2 : "";
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private StatusMode getStatusForIccState(IccCardConstants.State state) {
        if (state == null) {
            return StatusMode.Normal;
        }
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceProvisioned() && (state == IccCardConstants.State.ABSENT || state == IccCardConstants.State.PERM_DISABLED)) {
            state = IccCardConstants.State.NETWORK_LOCKED;
        }
        switch (AnonymousClass4.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
            case 1:
                return StatusMode.SimMissing;
            case 2:
                return StatusMode.NetworkLocked;
            case 3:
                return StatusMode.SimNotReady;
            case 4:
                return StatusMode.SimLocked;
            case 5:
                return StatusMode.SimPukLocked;
            case 6:
                return StatusMode.Normal;
            case 7:
                return StatusMode.SimPermDisabled;
            case 8:
                return StatusMode.SimUnknown;
            case 9:
                return StatusMode.SimIoError;
            default:
                return StatusMode.SimUnknown;
        }
    }

    /* access modifiers changed from: package-private */
    /* renamed from: com.android.keyguard.CarrierTextController$4  reason: invalid class name */
    public static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];
        static final /* synthetic */ int[] $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode = new int[StatusMode.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(38:0|(2:1|2)|3|(2:5|6)|7|(2:9|10)|11|(2:13|14)|15|(2:17|18)|19|(2:21|22)|23|(2:25|26)|27|(2:29|30)|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|(3:55|56|58)) */
        /* JADX WARNING: Can't wrap try/catch for region: R(41:0|(2:1|2)|3|5|6|7|(2:9|10)|11|(2:13|14)|15|17|18|19|(2:21|22)|23|(2:25|26)|27|29|30|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|(3:55|56|58)) */
        /* JADX WARNING: Can't wrap try/catch for region: R(45:0|(2:1|2)|3|5|6|7|(2:9|10)|11|13|14|15|17|18|19|(2:21|22)|23|25|26|27|29|30|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|55|56|58) */
        /* JADX WARNING: Can't wrap try/catch for region: R(46:0|1|2|3|5|6|7|(2:9|10)|11|13|14|15|17|18|19|(2:21|22)|23|25|26|27|29|30|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|55|56|58) */
        /* JADX WARNING: Can't wrap try/catch for region: R(47:0|1|2|3|5|6|7|(2:9|10)|11|13|14|15|17|18|19|(2:21|22)|23|25|26|27|29|30|31|33|34|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|55|56|58) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:39:0x0081 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:41:0x008b */
        /* JADX WARNING: Missing exception handler attribute for start block: B:43:0x0095 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:45:0x009f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:47:0x00a9 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:49:0x00b3 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:51:0x00bd */
        /* JADX WARNING: Missing exception handler attribute for start block: B:53:0x00c7 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:55:0x00d1 */
        static {
            /*
            // Method dump skipped, instructions count: 222
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.CarrierTextController.AnonymousClass4.<clinit>():void");
        }
    }

    private static CharSequence concatenate(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        boolean z = !TextUtils.isEmpty(charSequence);
        boolean z2 = !TextUtils.isEmpty(charSequence2);
        if (z && z2) {
            StringBuilder sb = new StringBuilder();
            sb.append(charSequence);
            sb.append(charSequence3);
            sb.append(charSequence2);
            return sb.toString();
        } else if (z) {
            return charSequence;
        } else {
            return z2 ? charSequence2 : "";
        }
    }

    private static CharSequence joinNotEmpty(CharSequence charSequence, CharSequence[] charSequenceArr) {
        int length = charSequenceArr.length;
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (!TextUtils.isEmpty(charSequenceArr[i])) {
                if (!TextUtils.isEmpty(sb)) {
                    sb.append(charSequence);
                }
                sb.append(charSequenceArr[i]);
            }
        }
        return sb.toString();
    }

    public static final class CarrierTextCallbackInfo {
        public boolean airplaneMode;
        public final boolean anySimReady;
        public final CharSequence carrierText;
        public final CharSequence[] listOfCarriers;
        public final int[] subscriptionIds;

        public CarrierTextCallbackInfo(CharSequence charSequence, CharSequence[] charSequenceArr, boolean z, int[] iArr) {
            this(charSequence, charSequenceArr, z, iArr, false);
        }

        public CarrierTextCallbackInfo(CharSequence charSequence, CharSequence[] charSequenceArr, boolean z, int[] iArr, boolean z2) {
            this.carrierText = charSequence;
            this.listOfCarriers = charSequenceArr;
            this.anySimReady = z;
            this.subscriptionIds = iArr;
            this.airplaneMode = z2;
        }
    }

    private String getCustomizeCarrierName(CharSequence charSequence, SubscriptionInfo subscriptionInfo) {
        StringBuilder sb = new StringBuilder();
        String networkClassToString = networkClassToString(TelephonyManager.getNetworkClass(getNetworkType(subscriptionInfo.getSubscriptionId())));
        String str = get5GNetworkClass(subscriptionInfo);
        if (str == null) {
            str = networkClassToString;
        }
        if (!TextUtils.isEmpty(charSequence)) {
            String[] split = charSequence.toString().split(this.mSeparator.toString(), 2);
            for (int i = 0; i < split.length; i++) {
                split[i] = getLocalString(split[i], C0001R$array.origin_carrier_names, C0001R$array.locale_carrier_names);
                if (!TextUtils.isEmpty(split[i])) {
                    if (!TextUtils.isEmpty(str)) {
                        split[i] = split[i] + " " + str;
                    }
                    if (i <= 0 || !split[i].equals(split[i - 1])) {
                        if (i > 0) {
                            sb.append(this.mSeparator);
                        }
                        sb.append(split[i]);
                    }
                }
            }
        }
        return sb.toString();
    }

    private int getNetworkType(int i) {
        ServiceState serviceState = this.mKeyguardUpdateMonitor.mServiceStates.get(Integer.valueOf(i));
        if (serviceState == null || (serviceState.getDataRegState() != 0 && serviceState.getVoiceRegState() != 0)) {
            return 0;
        }
        int dataNetworkType = serviceState.getDataNetworkType();
        return dataNetworkType == 0 ? serviceState.getVoiceNetworkType() : dataNetworkType;
    }

    private String networkClassToString(int i) {
        int[] iArr = {C0014R$string.config_rat_unknown, C0014R$string.config_rat_2g, C0014R$string.config_rat_3g, C0014R$string.config_rat_4g};
        String string = i < iArr.length ? getContext().getResources().getString(iArr[i]) : null;
        return string == null ? "" : string;
    }

    private String getLocalString(String str, int i, int i2) {
        String[] stringArray = getContext().getResources().getStringArray(i);
        String[] stringArray2 = getContext().getResources().getStringArray(i2);
        for (int i3 = 0; i3 < stringArray.length; i3++) {
            if (stringArray[i3].equalsIgnoreCase(str)) {
                return stringArray2[i3];
            }
        }
        return str;
    }

    private CharSequence concatenateForECA(CharSequence charSequence, CharSequence charSequence2) {
        String property = System.getProperty("line.separator");
        boolean z = !TextUtils.isEmpty(charSequence);
        boolean z2 = !TextUtils.isEmpty(charSequence2);
        if (z && z2) {
            return charSequence + property + charSequence2;
        } else if (z) {
            return charSequence;
        } else {
            return z2 ? charSequence2 : "";
        }
    }

    private CharSequence concatenateForMultiSim(CharSequence charSequence, CharSequence charSequence2) {
        boolean z = !TextUtils.isEmpty(charSequence);
        boolean z2 = !TextUtils.isEmpty(charSequence2);
        if (!z || !z2) {
            return charSequence2;
        }
        return charSequence + ": " + charSequence2;
    }

    private String get5GNetworkClass(SubscriptionInfo subscriptionInfo) {
        int simSlotIndex = subscriptionInfo.getSimSlotIndex();
        int subscriptionId = subscriptionInfo.getSubscriptionId();
        if (this.mFiveGServiceClient == null) {
            this.mFiveGServiceClient = FiveGServiceClient.getInstance(this.mContext);
            this.mFiveGServiceClient.registerCallback(this.mCallback);
        }
        if (!this.mFiveGServiceClient.getCurrentServiceState(simSlotIndex).isNrIconTypeValid() || !isDataRegisteredOnLte(subscriptionId)) {
            return null;
        }
        return this.mContext.getResources().getString(R$string.data_connection_5g);
    }

    private boolean isDataRegisteredOnLte(int i) {
        int dataNetworkType = ((TelephonyManager) this.mContext.getSystemService("phone")).getDataNetworkType(i);
        return dataNetworkType == 13 || dataNetworkType == 19;
    }
}
