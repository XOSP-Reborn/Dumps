package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.CellSignalStrengthNr;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.feature.MmTelFeature;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.Utils;
import com.android.settingslib.graph.SignalDrawable;
import com.android.settingslib.net.SignalStrengthUtil;
import com.android.systemui.C0014R$string;
import com.android.systemui.statusbar.policy.FiveGServiceClient;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SignalController;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class MobileSignalController extends SignalController<MobileState, MobileIconGroup> {
    private int mCallState = 0;
    private CellSignalStrengthNr mCellSignalStrengthNr;
    private FiveGServiceClient mClient;
    private NetworkControllerImpl.Config mConfig;
    private Map<Pair<Integer, Integer>, String> mDLThroughputIntervalMap = null;
    private int mDataNetType = 0;
    private int mDataState = 0;
    private MobileIconGroup mDefaultIcons;
    private final NetworkControllerImpl.SubscriptionDefaults mDefaults;
    @VisibleForTesting
    FiveGServiceClient.FiveGServiceState mFiveGState;
    @VisibleForTesting
    FiveGStateListener mFiveGStateListener;
    @VisibleForTesting
    final ImsManager.Connector.Listener mImsConnectionListener = new ImsConnectionListener();
    private final ImsManager.Connector mImsManagerConnector;
    @VisibleForTesting
    final ImsMmTelManager.CapabilityCallback mImsMmTelCapabilityListener = new ImsMmTelCapabilityListener();
    @VisibleForTesting
    ImsMmTelManager mImsMmTelMgr;
    @VisibleForTesting
    final ImsMmTelManager.RegistrationCallback mImsMmTelRegistrationListener = new ImsMmTelRegistrationListener();
    @VisibleForTesting
    boolean mInflateSignalStrengths = false;
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    final SparseArray<MobileIconGroup> mNetworkToIconLookup = new SparseArray<>();
    private final ContentObserver mObserver;
    private final TelephonyManager mPhone;
    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.policy.MobileSignalController.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "mReceiver: action " + intent.getAction());
            }
            if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED") && intent.getExtras().getInt("phone") == SubscriptionManager.getPhoneId(MobileSignalController.this.mSubscriptionInfo.getSubscriptionId())) {
                MobileSignalController.this.parseLTEDLThroughputThresholds();
            }
        }
    };
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    final SubscriptionInfo mSubscriptionInfo;
    final ArrayMap<String, MobileIconGroup> mThroughputToIconLookup = new ArrayMap<>();

    public MobileSignalController(Context context, NetworkControllerImpl.Config config, boolean z, TelephonyManager telephonyManager, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl, SubscriptionInfo subscriptionInfo, NetworkControllerImpl.SubscriptionDefaults subscriptionDefaults, Looper looper) {
        super("MobileSignalController(" + subscriptionInfo.getSubscriptionId() + ")", context, 0, callbackHandler, networkControllerImpl);
        String str;
        this.mConfig = config;
        this.mPhone = telephonyManager;
        this.mDefaults = subscriptionDefaults;
        this.mSubscriptionInfo = subscriptionInfo;
        this.mFiveGStateListener = new FiveGStateListener();
        this.mFiveGState = new FiveGServiceClient.FiveGServiceState();
        this.mPhoneStateListener = new MobilePhoneStateListener(looper);
        this.mNetworkNameSeparator = getStringIfExists(C0014R$string.status_bar_network_name_separator);
        this.mNetworkNameDefault = getStringIfExists(17040364);
        mapIconSets();
        if (subscriptionInfo.getCarrierName() != null) {
            str = subscriptionInfo.getCarrierName().toString();
        } else {
            str = this.mNetworkNameDefault;
        }
        T t = this.mLastState;
        T t2 = this.mCurrentState;
        ((MobileState) t2).networkName = str;
        ((MobileState) t).networkName = str;
        ((MobileState) t2).networkNameData = str;
        ((MobileState) t).networkNameData = str;
        ((MobileState) t2).enabled = z;
        ((MobileState) t).enabled = z;
        MobileIconGroup mobileIconGroup = this.mDefaultIcons;
        ((MobileState) t2).iconGroup = mobileIconGroup;
        ((MobileState) t).iconGroup = mobileIconGroup;
        updateDataSim();
        this.mObserver = new ContentObserver(new Handler(looper)) {
            /* class com.android.systemui.statusbar.policy.MobileSignalController.AnonymousClass1 */

            public void onChange(boolean z) {
                MobileSignalController.this.updateTelephony();
            }
        };
        try {
            this.mImsMmTelMgr = ImsMmTelManager.createForSubscriptionId(subscriptionInfo.getSubscriptionId());
        } catch (IllegalArgumentException e) {
            String str2 = this.mTag;
            Log.w(str2, "MobileSignalController e:" + e);
        }
        this.mImsManagerConnector = new ImsManager.Connector(context, subscriptionInfo.getSimSlotIndex(), this.mImsConnectionListener);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void parseLTEDLThroughputThresholds() {
        String[] stringArray = getCarrierConfig().getStringArray("s_lte_rat_icon_based_on_throughput_string_array");
        if (stringArray != null && this.mDLThroughputIntervalMap == null) {
            this.mDLThroughputIntervalMap = new ArrayMap();
            for (int i = 0; i < stringArray.length; i++) {
                String[] split = stringArray[i].replaceAll(" ", "").split(":");
                if (split.length == 2) {
                    String[] split2 = split[0].split(",");
                    try {
                        this.mDLThroughputIntervalMap.put(new Pair<>(Integer.valueOf(Integer.parseInt(split2[0])), Integer.valueOf(split2.length == 2 ? Integer.parseInt(split2[1]) : Integer.MAX_VALUE)), split[1]);
                    } catch (NumberFormatException unused) {
                        Log.d(this.mTag, "Exception in parsing throughput string:" + stringArray[i]);
                        this.mDLThroughputIntervalMap = null;
                        return;
                    }
                }
            }
            ((MobileState) this.mCurrentState).throughputLevel = "4G";
        }
    }

    private PersistableBundle getCarrierConfig() {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null || (configForSubId = carrierConfigManager.getConfigForSubId(this.mSubscriptionInfo.getSubscriptionId())) == null) {
            return CarrierConfigManager.getDefaultConfig();
        }
        return configForSubId;
    }

    public void setConfiguration(NetworkControllerImpl.Config config) {
        this.mConfig = config;
        updateInflateSignalStrength();
        mapIconSets();
        updateTelephony();
    }

    public void setAirplaneMode(boolean z) {
        ((MobileState) this.mCurrentState).airplaneMode = z;
        notifyListenersIfNecessary();
    }

    public void setUserSetupComplete(boolean z) {
        ((MobileState) this.mCurrentState).userSetup = z;
        notifyListenersIfNecessary();
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public void updateConnectivity(BitSet bitSet, BitSet bitSet2) {
        boolean z = bitSet2.get(this.mTransportType);
        ((MobileState) this.mCurrentState).isDefault = bitSet.get(this.mTransportType);
        T t = this.mCurrentState;
        ((MobileState) t).inetCondition = (z || !((MobileState) t).isDefault) ? 1 : 0;
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean z) {
        ((MobileState) this.mCurrentState).carrierNetworkChangeMode = z;
        updateTelephony();
    }

    public void registerListener() {
        this.mPhone.listen(this.mPhoneStateListener, 268501473);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_data"), true, this.mObserver);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        contentResolver.registerContentObserver(Settings.Global.getUriFor("mobile_data" + this.mSubscriptionInfo.getSubscriptionId()), true, this.mObserver);
        this.mImsManagerConnector.connect();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
    }

    public void unregisterListener() {
        this.mPhone.listen(this.mPhoneStateListener, 0);
        this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
        this.mImsManagerConnector.disconnect();
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private void mapIconSets() {
        MobileIconGroup mobileIconGroup;
        this.mNetworkToIconLookup.clear();
        this.mThroughputToIconLookup.clear();
        this.mNetworkToIconLookup.put(5, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(6, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(12, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(14, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(17, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(20, TelephonyIcons.FIVE_G_SA);
        if (!this.mConfig.showAtLeast3G) {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.UNKNOWN);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.E);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.ONE_X);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.ONE_X);
            this.mDefaultIcons = TelephonyIcons.G;
        } else {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.THREE_G);
            this.mDefaultIcons = TelephonyIcons.THREE_G;
        }
        MobileIconGroup mobileIconGroup2 = TelephonyIcons.THREE_G;
        if (this.mConfig.hspaDataDistinguishable) {
            mobileIconGroup2 = TelephonyIcons.H;
            mobileIconGroup = TelephonyIcons.H_PLUS;
        } else {
            mobileIconGroup = mobileIconGroup2;
        }
        this.mNetworkToIconLookup.put(8, mobileIconGroup2);
        this.mNetworkToIconLookup.put(9, mobileIconGroup2);
        this.mNetworkToIconLookup.put(10, mobileIconGroup2);
        this.mNetworkToIconLookup.put(15, mobileIconGroup);
        if (this.mConfig.show4gForLte) {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
            if (this.mConfig.hideLtePlus) {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G);
            } else {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G_PLUS);
            }
        } else {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
            if (this.mConfig.hideLtePlus) {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE);
            } else {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE_PLUS);
            }
        }
        this.mNetworkToIconLookup.put(21, TelephonyIcons.LTE_CA_5G_E);
        this.mNetworkToIconLookup.put(18, TelephonyIcons.WFC);
        this.mThroughputToIconLookup.put("4G", TelephonyIcons.FOUR_G);
        this.mThroughputToIconLookup.put("4.5G", TelephonyIcons.FOUR_FIVE_G);
        this.mThroughputToIconLookup.put("4.5G+", TelephonyIcons.FOUR_FIVE_G_PLUS);
    }

    private void updateInflateSignalStrength() {
        this.mInflateSignalStrengths = SignalStrengthUtil.shouldInflateSignalStrength(this.mContext, this.mSubscriptionInfo.getSubscriptionId());
    }

    private int getNumLevels() {
        return this.mInflateSignalStrengths ? 6 : 5;
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public int getCurrentIconId() {
        T t = this.mCurrentState;
        if (((MobileState) t).iconGroup == TelephonyIcons.CARRIER_NETWORK_CHANGE) {
            return SignalDrawable.getCarrierChangeState(getNumLevels());
        }
        boolean z = false;
        if (((MobileState) t).connected) {
            int i = ((MobileState) t).level;
            if (this.mInflateSignalStrengths) {
                i++;
            }
            T t2 = this.mCurrentState;
            boolean z2 = true;
            boolean z3 = ((MobileState) t2).userSetup && (((MobileState) t2).iconGroup == TelephonyIcons.DATA_DISABLED || ((MobileState) t2).iconGroup == TelephonyIcons.NOT_DEFAULT_DATA);
            boolean z4 = ((MobileState) this.mCurrentState).inetCondition == 0;
            if (!z3 && !z4) {
                z2 = false;
            }
            if (!this.mConfig.hideNoInternetState) {
                z = z2;
            }
            return SignalDrawable.getState(i, getNumLevels(), z);
        } else if (((MobileState) t).enabled) {
            return SignalDrawable.getEmptyState(getNumLevels());
        } else {
            return 0;
        }
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public int getQsCurrentIconId() {
        return getCurrentIconId();
    }

    /* JADX WARNING: Removed duplicated region for block: B:71:0x0117  */
    @Override // com.android.systemui.statusbar.policy.SignalController
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyListeners(com.android.systemui.statusbar.policy.NetworkController.SignalCallback r18) {
        /*
        // Method dump skipped, instructions count: 468
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.MobileSignalController.notifyListeners(com.android.systemui.statusbar.policy.NetworkController$SignalCallback):void");
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.policy.SignalController
    public MobileState cleanState() {
        return new MobileState();
    }

    private boolean isCdma() {
        SignalStrength signalStrength = this.mSignalStrength;
        return signalStrength != null && !signalStrength.isGsm();
    }

    public boolean isEmergencyOnly() {
        ServiceState serviceState = this.mServiceState;
        return serviceState != null && serviceState.isEmergencyOnly();
    }

    private boolean isRoaming() {
        ServiceState serviceState;
        if (isCarrierNetworkChangeActive()) {
            return false;
        }
        if (!isCdma() || (serviceState = this.mServiceState) == null) {
            ServiceState serviceState2 = this.mServiceState;
            if (serviceState2 == null || !serviceState2.getRoaming()) {
                return false;
            }
            return true;
        }
        int cdmaEriIconMode = serviceState.getCdmaEriIconMode();
        if (this.mServiceState.getCdmaEriIconIndex() == 1) {
            return false;
        }
        if (cdmaEriIconMode == 0 || cdmaEriIconMode == 1) {
            return true;
        }
        return false;
    }

    private boolean isCarrierNetworkChangeActive() {
        return ((MobileState) this.mCurrentState).carrierNetworkChangeMode;
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")) {
            updateNetworkName(intent.getBooleanExtra("showSpn", false), intent.getStringExtra("spn"), intent.getStringExtra("spnData"), intent.getBooleanExtra("showPlmn", false), intent.getStringExtra("plmn"));
            notifyListenersIfNecessary();
        } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
            updateDataSim();
            notifyListenersIfNecessary();
        }
    }

    private void updateDataSim() {
        int defaultDataSubId = this.mDefaults.getDefaultDataSubId();
        boolean z = true;
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) {
            MobileState mobileState = (MobileState) this.mCurrentState;
            if (defaultDataSubId != this.mSubscriptionInfo.getSubscriptionId()) {
                z = false;
            }
            mobileState.dataSim = z;
            return;
        }
        ((MobileState) this.mCurrentState).dataSim = true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isCarrierSpecificDataIcon() {
        String str = this.mConfig.patternOfCarrierSpecificDataIcon;
        if (!(str == null || str.length() == 0)) {
            Pattern compile = Pattern.compile(this.mConfig.patternOfCarrierSpecificDataIcon);
            String[] strArr = {this.mServiceState.getOperatorAlphaLongRaw(), this.mServiceState.getOperatorAlphaShortRaw()};
            for (String str2 : strArr) {
                if (!TextUtils.isEmpty(str2) && compile.matcher(str2).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void updateNetworkName(boolean z, String str, String str2, boolean z2, String str3) {
        if (SignalController.CHATTY) {
            Log.d("CarrierLabel", "updateNetworkName showSpn=" + z + " spn=" + str + " dataSpn=" + str2 + " showPlmn=" + z2 + " plmn=" + str3);
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        if (z2 && str3 != null) {
            sb.append(str3);
            sb2.append(str3);
        }
        if (z && str != null) {
            if (sb.length() != 0) {
                sb.append(this.mNetworkNameSeparator);
            }
            sb.append(str);
        }
        if (sb.length() != 0) {
            ((MobileState) this.mCurrentState).networkName = sb.toString();
        } else {
            ((MobileState) this.mCurrentState).networkName = this.mNetworkNameDefault;
        }
        if (z && str2 != null) {
            if (sb2.length() != 0) {
                sb2.append(this.mNetworkNameSeparator);
            }
            sb2.append(str2);
        }
        if (sb2.length() != 0) {
            ((MobileState) this.mCurrentState).networkNameData = sb2.toString();
            return;
        }
        ((MobileState) this.mCurrentState).networkNameData = this.mNetworkNameDefault;
    }

    /* access modifiers changed from: protected */
    public final void updateLTEDLThrouputLevel(int i) {
        int i2;
        if (SignalController.DEBUG) {
            String str = this.mTag;
            Log.d(str, "updateLTEDLThrouputLevel,throughput=" + i);
        }
        if (this.mDLThroughputIntervalMap == null) {
            parseLTEDLThroughputThresholds();
        }
        if (this.mDLThroughputIntervalMap != null) {
            if (i < 0) {
                i2 = 0;
            } else {
                i2 = i / 1024;
            }
            for (Map.Entry<Pair<Integer, Integer>, String> entry : this.mDLThroughputIntervalMap.entrySet()) {
                Pair<Integer, Integer> key = entry.getKey();
                int min = Math.min(((Integer) key.first).intValue(), ((Integer) key.second).intValue());
                int max = Math.max(((Integer) key.first).intValue(), ((Integer) key.second).intValue());
                if (i2 >= min && i2 < max) {
                    if (!((MobileState) this.mCurrentState).throughputLevel.equalsIgnoreCase(entry.getValue())) {
                        if (SignalController.DEBUG) {
                            String str2 = this.mTag;
                            Log.d(str2, "updateLTEDLThroughputLevel: " + entry.getValue());
                        }
                        ((MobileState) this.mCurrentState).throughputLevel = entry.getValue();
                        updateTelephony();
                        return;
                    }
                    return;
                }
            }
        } else if (SignalController.DEBUG) {
            Log.d(this.mTag, "Disabled Feature:RAT icon changes with LTE DL throughput updates");
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private final void updateTelephony() {
        ServiceState serviceState;
        ServiceState serviceState2;
        int voiceNetworkType;
        if (SignalController.DEBUG) {
            Log.d(this.mTag, "updateTelephonySignalStrength: hasService=" + Utils.isInService(this.mServiceState) + " ss=" + this.mSignalStrength);
        }
        boolean z = true;
        int i = 0;
        ((MobileState) this.mCurrentState).connected = Utils.isInService(this.mServiceState) && this.mSignalStrength != null;
        if (((MobileState) this.mCurrentState).connected) {
            if (this.mSignalStrength.isGsm() || !this.mConfig.alwaysShowCdmaRssi) {
                ((MobileState) this.mCurrentState).level = this.mSignalStrength.getLevel();
                if (this.mConfig.showRsrpSignalLevelforLTE) {
                    if (SignalController.DEBUG) {
                        Log.d(this.mTag, "updateTelephony CS:" + this.mServiceState.getVoiceNetworkType() + "/" + TelephonyManager.getNetworkTypeName(this.mServiceState.getVoiceNetworkType()) + ", PS:" + this.mServiceState.getDataNetworkType() + "/" + TelephonyManager.getNetworkTypeName(this.mServiceState.getDataNetworkType()));
                    }
                    int dataNetworkType = this.mServiceState.getDataNetworkType();
                    if (dataNetworkType == 13 || dataNetworkType == 19) {
                        ((MobileState) this.mCurrentState).level = getAlternateLteLevel(this.mSignalStrength);
                    } else if (dataNetworkType == 0 && ((voiceNetworkType = this.mServiceState.getVoiceNetworkType()) == 13 || voiceNetworkType == 19)) {
                        ((MobileState) this.mCurrentState).level = getAlternateLteLevel(this.mSignalStrength);
                    }
                }
            } else {
                ((MobileState) this.mCurrentState).level = this.mSignalStrength.getCdmaLevel();
            }
        }
        MobileIconGroup nr5GIconGroup = getNr5GIconGroup();
        if (nr5GIconGroup != null) {
            ((MobileState) this.mCurrentState).iconGroup = nr5GIconGroup;
        } else if (this.mNetworkToIconLookup.indexOfKey(this.mDataNetType) >= 0) {
            ((MobileState) this.mCurrentState).iconGroup = this.mNetworkToIconLookup.get(this.mDataNetType);
            int i2 = this.mDataNetType;
            if ((i2 == 13 || i2 == 19) && this.mThroughputToIconLookup.indexOfKey(((MobileState) this.mCurrentState).throughputLevel) >= 0) {
                T t = this.mCurrentState;
                ((MobileState) t).iconGroup = this.mThroughputToIconLookup.get(((MobileState) t).throughputLevel);
            }
        } else {
            ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
        }
        if (this.mDataNetType == 20) {
            if (this.mFiveGState.isNrIconTypeValid()) {
                ((MobileState) this.mCurrentState).iconGroup = this.mFiveGState.getIconGroup();
                if (SignalController.DEBUG) {
                    Log.d(this.mTag, "get 5G SA icon from side-car");
                }
            }
            int nrLevel = getNrLevel();
            if (nrLevel > this.mFiveGState.getSignalLevel()) {
                ((MobileState) this.mCurrentState).level = nrLevel;
                if (SignalController.DEBUG) {
                    Log.d(this.mTag, "get 5G SA sinal strength from AOSP");
                }
            } else {
                ((MobileState) this.mCurrentState).level = this.mFiveGState.getSignalLevel();
                if (SignalController.DEBUG) {
                    Log.d(this.mTag, "get 5G SA sinal strength from side-car");
                }
            }
        } else if (nr5GIconGroup == null && isSideCarNsaValid()) {
            ((MobileState) this.mCurrentState).iconGroup = this.mFiveGState.getIconGroup();
            if (SignalController.DEBUG) {
                Log.d(this.mTag, "get 5G NSA icon from side-car");
            }
        }
        T t2 = this.mCurrentState;
        MobileState mobileState = (MobileState) t2;
        if (!((MobileState) t2).connected || this.mDataState != 2) {
            z = false;
        }
        mobileState.dataConnected = z;
        ((MobileState) this.mCurrentState).roaming = isRoaming();
        if (isCarrierNetworkChangeActive()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isDataDisabled() && !this.mConfig.alwaysShowDataRatIcon) {
            if (this.mSubscriptionInfo.getSubscriptionId() != this.mDefaults.getDefaultDataSubId()) {
                ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.NOT_DEFAULT_DATA;
            } else {
                ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.DATA_DISABLED;
            }
        }
        boolean isEmergencyOnly = isEmergencyOnly();
        T t3 = this.mCurrentState;
        if (isEmergencyOnly != ((MobileState) t3).isEmergency) {
            ((MobileState) t3).isEmergency = isEmergencyOnly();
            this.mNetworkController.recalculateEmergency();
        }
        if (((MobileState) this.mCurrentState).networkName.equals(this.mNetworkNameDefault) && (serviceState2 = this.mServiceState) != null && !TextUtils.isEmpty(serviceState2.getOperatorAlphaShort())) {
            ((MobileState) this.mCurrentState).networkName = this.mServiceState.getOperatorAlphaShort();
        }
        if (((MobileState) this.mCurrentState).networkNameData.equals(this.mNetworkNameDefault) && (serviceState = this.mServiceState) != null && ((MobileState) this.mCurrentState).dataSim && !TextUtils.isEmpty(serviceState.getDataOperatorAlphaShort())) {
            ((MobileState) this.mCurrentState).networkNameData = this.mServiceState.getDataOperatorAlphaShort();
        }
        if (this.mConfig.alwaysShowNetworkTypeIcon && nr5GIconGroup == null) {
            if (((MobileState) this.mCurrentState).connected) {
                if (isDataNetworkTypeAvailable()) {
                    i = this.mDataNetType;
                } else {
                    i = getVoiceNetworkType();
                }
            }
            if (this.mNetworkToIconLookup.indexOfKey(i) >= 0) {
                ((MobileState) this.mCurrentState).iconGroup = this.mNetworkToIconLookup.get(i);
            } else {
                ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
            }
        }
        ((MobileState) this.mCurrentState).wifiCallingAvailable = isWifiCallingAvailable();
        ((MobileState) this.mCurrentState).volteAvailable = isVolteAvailable();
        notifyListenersIfNecessary();
    }

    private MobileIconGroup getNr5GIconGroup() {
        ServiceState serviceState = this.mServiceState;
        if (serviceState == null) {
            return null;
        }
        int nrState = serviceState.getNrState();
        if (nrState == 3) {
            if (this.mServiceState.getNrFrequencyRange() == 4 && this.mConfig.nr5GIconMap.containsKey(1)) {
                return this.mConfig.nr5GIconMap.get(1);
            }
            if (this.mConfig.nr5GIconMap.containsKey(2)) {
                return this.mConfig.nr5GIconMap.get(2);
            }
        } else if (nrState == 2) {
            if (this.mConfig.nr5GIconMap.containsKey(3)) {
                return this.mConfig.nr5GIconMap.get(3);
            }
        } else if (nrState == 1 && this.mConfig.nr5GIconMap.containsKey(4)) {
            return this.mConfig.nr5GIconMap.get(4);
        }
        return null;
    }

    private boolean isDataDisabled() {
        return !this.mPhone.isDataCapable();
    }

    private boolean isWifiCallingAvailable() {
        ImsMmTelManager imsMmTelManager = this.mImsMmTelMgr;
        if (imsMmTelManager == null) {
            return false;
        }
        if (imsMmTelManager.isAvailable(1, 1) || this.mImsMmTelMgr.isAvailable(2, 1)) {
            return true;
        }
        return false;
    }

    private boolean isVolteAvailable() {
        ImsMmTelManager imsMmTelManager;
        ServiceState serviceState = this.mServiceState;
        if (serviceState == null) {
            return false;
        }
        if (!(serviceState.getRilDataRadioTechnology() == 14 || this.mServiceState.getRilVoiceRadioTechnology() == 14) || (imsMmTelManager = this.mImsMmTelMgr) == null || !imsMmTelManager.isAvailable(1, 0)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startListeningForImsCapabilities() {
        ImsMmTelManager imsMmTelManager = this.mImsMmTelMgr;
        if (imsMmTelManager != null) {
            try {
                imsMmTelManager.registerImsRegistrationCallback(this.mContext.getMainExecutor(), this.mImsMmTelRegistrationListener);
                this.mImsMmTelMgr.registerMmTelCapabilityCallback(this.mContext.getMainExecutor(), this.mImsMmTelCapabilityListener);
            } catch (ImsException | IllegalArgumentException e) {
                String str = this.mTag;
                Log.w(str, "startListeningForImsCapabilities e:" + e);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void stopListeningForImsCapabilities() {
        ImsMmTelManager imsMmTelManager = this.mImsMmTelMgr;
        if (imsMmTelManager != null) {
            try {
                imsMmTelManager.unregisterImsRegistrationCallback(this.mImsMmTelRegistrationListener);
                this.mImsMmTelMgr.unregisterMmTelCapabilityCallback(this.mImsMmTelCapabilityListener);
            } catch (IllegalArgumentException e) {
                String str = this.mTag;
                Log.w(str, "stopListeningForImsCapabilities e:" + e);
            }
        }
    }

    private boolean isDataNetworkTypeAvailable() {
        if (this.mDataNetType == 0) {
            return false;
        }
        int dataNetworkType = getDataNetworkType();
        int voiceNetworkType = getVoiceNetworkType();
        if ((dataNetworkType == 6 || dataNetworkType == 12 || dataNetworkType == 14 || dataNetworkType == 13 || dataNetworkType == 19) && ((voiceNetworkType == 16 || voiceNetworkType == 7 || voiceNetworkType == 4) && !isCallIdle())) {
            return false;
        }
        return true;
    }

    private boolean isCallIdle() {
        return this.mCallState == 0;
    }

    private int getVoiceNetworkType() {
        ServiceState serviceState = this.mServiceState;
        if (serviceState != null) {
            return serviceState.getVoiceNetworkType();
        }
        return 0;
    }

    private int getDataNetworkType() {
        ServiceState serviceState = this.mServiceState;
        if (serviceState != null) {
            return serviceState.getDataNetworkType();
        }
        return 0;
    }

    private int getAlternateLteLevel(SignalStrength signalStrength) {
        int lteDbm = signalStrength.getLteDbm();
        if (lteDbm == Integer.MAX_VALUE) {
            int level = signalStrength.getLevel();
            if (SignalController.DEBUG) {
                String str = this.mTag;
                Log.d(str, "getAlternateLteLevel lteRsrp:INVALID  signalStrengthLevel = " + level);
            }
            return level;
        }
        int i = 0;
        if (lteDbm <= -44) {
            if (lteDbm >= -97) {
                i = 4;
            } else if (lteDbm >= -105) {
                i = 3;
            } else if (lteDbm >= -113) {
                i = 2;
            } else if (lteDbm >= -120) {
                i = 1;
            }
        }
        if (SignalController.DEBUG) {
            String str2 = this.mTag;
            Log.d(str2, "getAlternateLteLevel lteRsrp:" + lteDbm + " rsrpLevel = " + i);
        }
        return i;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setActivity(int i) {
        boolean z = false;
        ((MobileState) this.mCurrentState).activityIn = i == 3 || i == 1;
        MobileState mobileState = (MobileState) this.mCurrentState;
        if (i == 3 || i == 2) {
            z = true;
        }
        mobileState.activityOut = z;
        notifyListenersIfNecessary();
    }

    public void registerFiveGStateListener(FiveGServiceClient fiveGServiceClient) {
        fiveGServiceClient.registerListener(this.mSubscriptionInfo.getSimSlotIndex(), this.mFiveGStateListener);
        this.mClient = fiveGServiceClient;
    }

    public void unregisterFiveGStateListener(FiveGServiceClient fiveGServiceClient) {
        fiveGServiceClient.unregisterListener(this.mSubscriptionInfo.getSimSlotIndex());
    }

    private boolean isDataRegisteredOnLte() {
        int dataNetworkType = getDataNetworkType();
        return dataNetworkType == 13 || dataNetworkType == 19;
    }

    private boolean isSideCarNsaValid() {
        return this.mFiveGState.isNrIconTypeValid() && isDataRegisteredOnLte();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isCellSignalStrengthNrValid() {
        CellSignalStrengthNr cellSignalStrengthNr = this.mCellSignalStrengthNr;
        return cellSignalStrengthNr != null && cellSignalStrengthNr.isValid();
    }

    private int getNrLevel() {
        CellSignalStrengthNr cellSignalStrengthNr = this.mCellSignalStrengthNr;
        if (cellSignalStrengthNr != null) {
            return cellSignalStrengthNr.getLevel();
        }
        return 0;
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public void dump(PrintWriter printWriter) {
        super.dump(printWriter);
        printWriter.println("  mSubscription=" + this.mSubscriptionInfo + ",");
        printWriter.println("  mServiceState=" + this.mServiceState + ",");
        printWriter.println("  mSignalStrength=" + this.mSignalStrength + ",");
        printWriter.println("  mDataState=" + this.mDataState + ",");
        printWriter.println("  mDataNetType=" + this.mDataNetType + ",");
        printWriter.println("  mInflateSignalStrengths=" + this.mInflateSignalStrengths + ",");
        printWriter.println("  isDataDisabled=" + isDataDisabled() + ",");
        printWriter.println("  mFiveGState=" + this.mFiveGState + ",");
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(Looper looper) {
            super(looper);
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            String str;
            if (SignalController.DEBUG) {
                String str2 = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onSignalStrengthsChanged signalStrength=");
                sb.append(signalStrength);
                if (signalStrength == null) {
                    str = "";
                } else {
                    str = " level=" + signalStrength.getLevel();
                }
                sb.append(str);
                Log.d(str2, sb.toString());
            }
            MobileSignalController.this.mSignalStrength = signalStrength;
            updateCellSignalStrengthNr(signalStrength);
            MobileSignalController.this.updateTelephony();
        }

        private void updateCellSignalStrengthNr(SignalStrength signalStrength) {
            if (signalStrength != null) {
                List cellSignalStrengths = MobileSignalController.this.mSignalStrength.getCellSignalStrengths(CellSignalStrengthNr.class);
                if (cellSignalStrengths == null || cellSignalStrengths.size() <= 0) {
                    MobileSignalController.this.mCellSignalStrengthNr = null;
                } else {
                    MobileSignalController.this.mCellSignalStrengthNr = (CellSignalStrengthNr) cellSignalStrengths.get(0);
                }
            } else {
                MobileSignalController.this.mCellSignalStrengthNr = null;
            }
            if (MobileSignalController.this.mDataNetType == 20 && !MobileSignalController.this.isCellSignalStrengthNrValid() && MobileSignalController.this.mClient != null) {
                MobileSignalController.this.mClient.queryNrSignalStrength(MobileSignalController.this.mSubscriptionInfo.getSimSlotIndex());
            }
        }

        public void onServiceStateChanged(ServiceState serviceState) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onServiceStateChanged voiceState=" + serviceState.getVoiceRegState() + " dataState=" + serviceState.getDataRegState());
            }
            MobileSignalController.this.mServiceState = serviceState;
            if (MobileSignalController.this.mServiceState != null) {
                updateDataNetType(MobileSignalController.this.mServiceState.getDataNetworkType());
                if (!(MobileSignalController.this.mDLThroughputIntervalMap == null || MobileSignalController.this.mDataNetType == 13 || MobileSignalController.this.mDataNetType == 19)) {
                    ((MobileState) MobileSignalController.this.mCurrentState).throughputLevel = "4G";
                }
            }
            MobileSignalController.this.updateTelephony();
        }

        public void onDataConnectionStateChanged(int i, int i2) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onDataConnectionStateChanged: state=" + i + " type=" + i2);
            }
            MobileSignalController.this.mDataState = i;
            updateDataNetType(i2);
            MobileSignalController.this.updateTelephony();
        }

        private void updateDataNetType(int i) {
            MobileSignalController.this.mDataNetType = i;
            if (MobileSignalController.this.mDataNetType != 13) {
                return;
            }
            if (MobileSignalController.this.isCarrierSpecificDataIcon()) {
                MobileSignalController.this.mDataNetType = 21;
            } else if (MobileSignalController.this.mServiceState != null && MobileSignalController.this.mServiceState.isUsingCarrierAggregation()) {
                MobileSignalController.this.mDataNetType = 19;
            }
        }

        public void onDataActivity(int i) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onDataActivity: direction=" + i);
            }
            MobileSignalController.this.setActivity(i);
        }

        public void onCarrierNetworkChange(boolean z) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onCarrierNetworkChange: active=" + z);
            }
            MobileSignalController mobileSignalController = MobileSignalController.this;
            ((MobileState) mobileSignalController.mCurrentState).carrierNetworkChangeMode = z;
            mobileSignalController.updateTelephony();
        }

        public void onCallStateChanged(int i, String str) {
            if (SignalController.DEBUG) {
                String str2 = MobileSignalController.this.mTag;
                Log.d(str2, "onCarrierNetworkChange: state=" + i);
            }
            MobileSignalController.this.mCallState = i;
            MobileSignalController.this.updateTelephony();
        }

        public void onSomcHookRawEvent(byte[] bArr) {
            if (bArr == null) {
                Log.d(MobileSignalController.this.mTag, "OnSomcHookRawEvent: empty data");
                return;
            }
            ByteBuffer wrap = ByteBuffer.wrap(bArr);
            wrap.order(ByteOrder.nativeOrder());
            if (wrap.capacity() < 8) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "SOMC_HOOK_RAW: Data size is " + wrap.capacity());
                return;
            }
            int i = wrap.getInt();
            if (SignalController.DEBUG) {
                String str2 = MobileSignalController.this.mTag;
                Log.d(str2, "OnSomcHookRawEvent: responseId=" + i);
            }
            wrap.position(wrap.position() + 4);
            if (593923 == i) {
                MobileSignalController.this.updateLTEDLThrouputLevel(wrap.getInt());
            }
        }
    }

    /* access modifiers changed from: package-private */
    public class FiveGStateListener implements FiveGServiceClient.IFiveGStateListener {
        FiveGStateListener() {
        }

        @Override // com.android.systemui.statusbar.policy.FiveGServiceClient.IFiveGStateListener
        public void onStateChanged(FiveGServiceClient.FiveGServiceState fiveGServiceState) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onStateChanged: state=" + fiveGServiceState);
            }
            MobileSignalController mobileSignalController = MobileSignalController.this;
            mobileSignalController.mFiveGState = fiveGServiceState;
            mobileSignalController.updateTelephony();
            MobileSignalController.this.notifyListeners();
        }
    }

    private class ImsConnectionListener implements ImsManager.Connector.Listener {
        private ImsConnectionListener() {
        }

        public void connectionReady(ImsManager imsManager) throws com.android.ims.ImsException {
            if (SignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "connectionReady");
            }
            MobileSignalController.this.startListeningForImsCapabilities();
        }

        public void connectionUnavailable() {
            if (SignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "connectionUnavailable");
            }
            MobileSignalController.this.stopListeningForImsCapabilities();
            MobileSignalController.this.updateTelephony();
        }
    }

    private class ImsMmTelRegistrationListener extends ImsMmTelManager.RegistrationCallback {
        private ImsMmTelRegistrationListener() {
        }

        public void onRegistered(int i) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onRegistered imsRadioTech=" + i);
            }
            MobileSignalController.this.updateTelephony();
        }

        public void onRegistering(int i) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onRegistering imsRadioTech=" + i);
            }
            MobileSignalController.this.updateTelephony();
        }

        public void onUnregistered(ImsReasonInfo imsReasonInfo) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onUnregistered imsReasonInfo=" + imsReasonInfo);
            }
            MobileSignalController.this.updateTelephony();
        }
    }

    private class ImsMmTelCapabilityListener extends ImsMmTelManager.CapabilityCallback {
        private ImsMmTelCapabilityListener() {
        }

        public void onCapabilitiesStatusChanged(MmTelFeature.MmTelCapabilities mmTelCapabilities) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onCapabilitiesStatusChanged capabilities=" + mmTelCapabilities);
            }
            MobileSignalController.this.updateTelephony();
        }
    }

    /* access modifiers changed from: package-private */
    public static class MobileIconGroup extends SignalController.IconGroup {
        final int mDataContentDescription;
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;

        public MobileIconGroup(String str, int[][] iArr, int[][] iArr2, int[] iArr3, int i, int i2, int i3, int i4, int i5, int i6, int i7, boolean z) {
            super(str, iArr, iArr2, iArr3, i, i2, i3, i4, i5);
            this.mDataContentDescription = i6;
            this.mDataType = i7;
            this.mIsWide = z;
            this.mQsDataType = i7;
        }
    }

    /* access modifiers changed from: package-private */
    public static class MobileState extends SignalController.State {
        boolean airplaneMode;
        boolean carrierNetworkChangeMode;
        boolean dataConnected;
        boolean dataSim;
        boolean isDefault;
        boolean isEmergency;
        String networkName;
        String networkNameData;
        boolean roaming;
        String throughputLevel = "unknonw";
        boolean userSetup;
        boolean volteAvailable;
        boolean wifiCallingAvailable;

        MobileState() {
        }

        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public void copyFrom(SignalController.State state) {
            super.copyFrom(state);
            MobileState mobileState = (MobileState) state;
            this.dataSim = mobileState.dataSim;
            this.networkName = mobileState.networkName;
            this.networkNameData = mobileState.networkNameData;
            this.dataConnected = mobileState.dataConnected;
            this.isDefault = mobileState.isDefault;
            this.isEmergency = mobileState.isEmergency;
            this.airplaneMode = mobileState.airplaneMode;
            this.carrierNetworkChangeMode = mobileState.carrierNetworkChangeMode;
            this.userSetup = mobileState.userSetup;
            this.roaming = mobileState.roaming;
            this.wifiCallingAvailable = mobileState.wifiCallingAvailable;
            this.volteAvailable = mobileState.volteAvailable;
            this.throughputLevel = mobileState.throughputLevel;
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public void toString(StringBuilder sb) {
            super.toString(sb);
            sb.append(',');
            sb.append("dataSim=");
            sb.append(this.dataSim);
            sb.append(',');
            sb.append("networkName=");
            sb.append(this.networkName);
            sb.append(',');
            sb.append("networkNameData=");
            sb.append(this.networkNameData);
            sb.append(',');
            sb.append("dataConnected=");
            sb.append(this.dataConnected);
            sb.append(',');
            sb.append("roaming=");
            sb.append(this.roaming);
            sb.append(',');
            sb.append("isDefault=");
            sb.append(this.isDefault);
            sb.append(',');
            sb.append("isEmergency=");
            sb.append(this.isEmergency);
            sb.append(',');
            sb.append("airplaneMode=");
            sb.append(this.airplaneMode);
            sb.append(',');
            sb.append("carrierNetworkChangeMode=");
            sb.append(this.carrierNetworkChangeMode);
            sb.append(',');
            sb.append("userSetup=");
            sb.append(this.userSetup);
            sb.append(',');
            sb.append("wifiCallingAvailable=");
            sb.append(this.wifiCallingAvailable);
            sb.append(',');
            sb.append("volteAvailable=");
            sb.append(this.volteAvailable);
            sb.append(',');
            sb.append("throughputLevel=");
            sb.append(this.throughputLevel);
        }

        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public boolean equals(Object obj) {
            if (super.equals(obj)) {
                MobileState mobileState = (MobileState) obj;
                return Objects.equals(mobileState.networkName, this.networkName) && Objects.equals(mobileState.networkNameData, this.networkNameData) && mobileState.dataSim == this.dataSim && mobileState.dataConnected == this.dataConnected && mobileState.isEmergency == this.isEmergency && mobileState.airplaneMode == this.airplaneMode && mobileState.carrierNetworkChangeMode == this.carrierNetworkChangeMode && mobileState.userSetup == this.userSetup && mobileState.isDefault == this.isDefault && mobileState.roaming == this.roaming && mobileState.wifiCallingAvailable == this.wifiCallingAvailable && mobileState.volteAvailable == this.volteAvailable && Objects.equals(mobileState.throughputLevel, this.throughputLevel);
            }
        }
    }
}
