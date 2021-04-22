package com.android.settingslib.wifi;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkKey;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.net.ScoredNetwork;
import android.net.wifi.IWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkScoreCache;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.ProvisioningCallback;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.CollectionUtils;
import com.android.settingslib.R$array;
import com.android.settingslib.R$string;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.AccessPoint;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AccessPoint implements Comparable<AccessPoint> {
    static final AtomicInteger sLastId = new AtomicInteger(0);
    private static final String sOpenSuffix = ("," + String.valueOf(0));
    private static final String sOweSuffix = ("," + String.valueOf(4));
    private static final String sOweTransSuffix = ("," + String.valueOf(8));
    private static final String sPskSaeSuffix = ("," + String.valueOf(7));
    private static final String sPskSuffix = ("," + String.valueOf(2));
    private static final String sSaeSuffix = ("," + String.valueOf(5));
    private String bssid;
    AccessPointListener mAccessPointListener;
    private int mCarrierApEapType;
    private String mCarrierName;
    private WifiConfiguration mConfig;
    private WifiManager.ActionListener mConnectListener;
    private final Context mContext;
    private int mEapType;
    private final ArraySet<ScanResult> mExtraScanResults;
    private String mFqdn;
    private WifiInfo mInfo;
    private boolean mIsCarrierAp;
    private boolean mIsRoaming;
    private boolean mIsScoredNetworkMetered;
    private String mKey;
    private final Object mLock;
    private NetworkInfo mNetworkInfo;
    private String mOsuFailure;
    private OsuProvider mOsuProvider;
    private boolean mOsuProvisioningComplete;
    private String mOsuStatus;
    private String mProviderFriendlyName;
    private int mRssi;
    private final ArraySet<ScanResult> mScanResults;
    private final Map<String, TimestampedScoredNetwork> mScoredNetworkCache;
    private int mSpeed;
    private Object mTag;
    private boolean mTwtSupport;
    private boolean mVhtMax8SpatialStreamsSupport;
    private int mWifiGeneration;
    private WifiManager mWifiManager;
    private int networkId;
    private int pskType;
    private int security;
    private String ssid;

    public interface AccessPointListener {
        void onAccessPointChanged(AccessPoint accessPoint);

        void onLevelChanged(AccessPoint accessPoint);
    }

    private static int roundToClosestSpeedEnum(int i) {
        if (i < 5) {
            return 0;
        }
        if (i < 7) {
            return 5;
        }
        if (i < 15) {
            return 10;
        }
        return i < 25 ? 20 : 30;
    }

    public static String securityToString(int i, int i2) {
        return i == 1 ? "WEP" : i == 2 ? i2 == 1 ? "WPA" : i2 == 2 ? "WPA2" : i2 == 3 ? "WPA_WPA2" : "PSK" : i == 3 ? "EAP" : i == 5 ? "SAE" : i == 6 ? "SUITE_B" : i == 4 ? "OWE" : i == 7 ? "PSK+SAE" : i == 8 ? "OWE_TRANSITION" : i == 9 ? "WAPI PSK" : i == 10 ? "WAPI CERT" : "NONE";
    }

    public AccessPoint(Context context, Bundle bundle) {
        this.mLock = new Object();
        this.mScanResults = new ArraySet<>();
        this.mExtraScanResults = new ArraySet<>();
        this.mScoredNetworkCache = new HashMap();
        this.networkId = -1;
        this.pskType = 0;
        this.mEapType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWifiGeneration = 0;
        this.mTwtSupport = false;
        this.mVhtMax8SpatialStreamsSupport = false;
        this.mSpeed = 0;
        this.mIsScoredNetworkMetered = false;
        this.mIsRoaming = false;
        this.mIsCarrierAp = false;
        this.mOsuProvisioningComplete = false;
        this.mCarrierApEapType = -1;
        this.mCarrierName = null;
        this.mContext = context;
        if (bundle.containsKey("key_config")) {
            this.mConfig = (WifiConfiguration) bundle.getParcelable("key_config");
        }
        WifiConfiguration wifiConfiguration = this.mConfig;
        if (wifiConfiguration != null) {
            loadConfig(wifiConfiguration);
        }
        if (bundle.containsKey("key_ssid")) {
            this.ssid = bundle.getString("key_ssid");
        }
        if (bundle.containsKey("key_security")) {
            this.security = bundle.getInt("key_security");
        }
        if (bundle.containsKey("key_speed")) {
            this.mSpeed = bundle.getInt("key_speed");
        }
        if (bundle.containsKey("key_psktype")) {
            this.pskType = bundle.getInt("key_psktype");
        }
        if (bundle.containsKey("eap_psktype")) {
            this.mEapType = bundle.getInt("eap_psktype");
        }
        this.mInfo = (WifiInfo) bundle.getParcelable("key_wifiinfo");
        if (bundle.containsKey("key_networkinfo")) {
            this.mNetworkInfo = (NetworkInfo) bundle.getParcelable("key_networkinfo");
        }
        if (bundle.containsKey("key_scanresults")) {
            Parcelable[] parcelableArray = bundle.getParcelableArray("key_scanresults");
            this.mScanResults.clear();
            for (Parcelable parcelable : parcelableArray) {
                this.mScanResults.add((ScanResult) parcelable);
            }
        }
        if (bundle.containsKey("key_scorednetworkcache")) {
            Iterator it = bundle.getParcelableArrayList("key_scorednetworkcache").iterator();
            while (it.hasNext()) {
                TimestampedScoredNetwork timestampedScoredNetwork = (TimestampedScoredNetwork) it.next();
                this.mScoredNetworkCache.put(timestampedScoredNetwork.getScore().networkKey.wifiKey.bssid, timestampedScoredNetwork);
            }
        }
        if (bundle.containsKey("key_fqdn")) {
            this.mFqdn = bundle.getString("key_fqdn");
        }
        if (bundle.containsKey("key_provider_friendly_name")) {
            this.mProviderFriendlyName = bundle.getString("key_provider_friendly_name");
        }
        if (bundle.containsKey("key_is_carrier_ap")) {
            this.mIsCarrierAp = bundle.getBoolean("key_is_carrier_ap");
        }
        if (bundle.containsKey("key_carrier_ap_eap_type")) {
            this.mCarrierApEapType = bundle.getInt("key_carrier_ap_eap_type");
        }
        if (bundle.containsKey("key_carrier_name")) {
            this.mCarrierName = bundle.getString("key_carrier_name");
        }
        update(this.mConfig, this.mInfo, this.mNetworkInfo);
        updateKey();
        updateBestRssiInfo();
        updateWifiGeneration();
    }

    public AccessPoint(Context context, WifiConfiguration wifiConfiguration) {
        this.mLock = new Object();
        this.mScanResults = new ArraySet<>();
        this.mExtraScanResults = new ArraySet<>();
        this.mScoredNetworkCache = new HashMap();
        this.networkId = -1;
        this.pskType = 0;
        this.mEapType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWifiGeneration = 0;
        this.mTwtSupport = false;
        this.mVhtMax8SpatialStreamsSupport = false;
        this.mSpeed = 0;
        this.mIsScoredNetworkMetered = false;
        this.mIsRoaming = false;
        this.mIsCarrierAp = false;
        this.mOsuProvisioningComplete = false;
        this.mCarrierApEapType = -1;
        this.mCarrierName = null;
        this.mContext = context;
        loadConfig(wifiConfiguration);
        updateKey();
    }

    public AccessPoint(Context context, WifiConfiguration wifiConfiguration, Collection<ScanResult> collection, Collection<ScanResult> collection2) {
        this.mLock = new Object();
        this.mScanResults = new ArraySet<>();
        this.mExtraScanResults = new ArraySet<>();
        this.mScoredNetworkCache = new HashMap();
        this.networkId = -1;
        this.pskType = 0;
        this.mEapType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWifiGeneration = 0;
        this.mTwtSupport = false;
        this.mVhtMax8SpatialStreamsSupport = false;
        this.mSpeed = 0;
        this.mIsScoredNetworkMetered = false;
        this.mIsRoaming = false;
        this.mIsCarrierAp = false;
        this.mOsuProvisioningComplete = false;
        this.mCarrierApEapType = -1;
        this.mCarrierName = null;
        this.mContext = context;
        this.networkId = wifiConfiguration.networkId;
        this.mConfig = wifiConfiguration;
        this.mFqdn = wifiConfiguration.FQDN;
        setScanResultsPasspoint(collection, collection2);
        updateKey();
    }

    public AccessPoint(Context context, OsuProvider osuProvider, Collection<ScanResult> collection) {
        this.mLock = new Object();
        this.mScanResults = new ArraySet<>();
        this.mExtraScanResults = new ArraySet<>();
        this.mScoredNetworkCache = new HashMap();
        this.networkId = -1;
        this.pskType = 0;
        this.mEapType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWifiGeneration = 0;
        this.mTwtSupport = false;
        this.mVhtMax8SpatialStreamsSupport = false;
        this.mSpeed = 0;
        this.mIsScoredNetworkMetered = false;
        this.mIsRoaming = false;
        this.mIsCarrierAp = false;
        this.mOsuProvisioningComplete = false;
        this.mCarrierApEapType = -1;
        this.mCarrierName = null;
        this.mContext = context;
        this.mOsuProvider = osuProvider;
        setScanResults(collection);
        updateKey();
    }

    AccessPoint(Context context, Collection<ScanResult> collection) {
        this.mLock = new Object();
        this.mScanResults = new ArraySet<>();
        this.mExtraScanResults = new ArraySet<>();
        this.mScoredNetworkCache = new HashMap();
        this.networkId = -1;
        this.pskType = 0;
        this.mEapType = 0;
        this.mRssi = Integer.MIN_VALUE;
        this.mWifiGeneration = 0;
        this.mTwtSupport = false;
        this.mVhtMax8SpatialStreamsSupport = false;
        this.mSpeed = 0;
        this.mIsScoredNetworkMetered = false;
        this.mIsRoaming = false;
        this.mIsCarrierAp = false;
        this.mOsuProvisioningComplete = false;
        this.mCarrierApEapType = -1;
        this.mCarrierName = null;
        this.mContext = context;
        setScanResults(collection);
        updateKey();
    }

    public void loadConfig(WifiConfiguration wifiConfiguration) {
        String str = wifiConfiguration.SSID;
        this.ssid = str == null ? "" : removeDoubleQuotes(str);
        this.bssid = wifiConfiguration.BSSID;
        this.security = getSecurity(wifiConfiguration);
        this.networkId = wifiConfiguration.networkId;
        this.mConfig = wifiConfiguration;
    }

    private void updateKey() {
        if (isPasspoint()) {
            this.mKey = getKey(this.mConfig);
        } else if (isPasspointConfig()) {
            this.mKey = getKey(this.mFqdn);
        } else if (isOsuProvider()) {
            this.mKey = getKey(this.mOsuProvider);
        } else {
            this.mKey = getKey(getSsidStr(), getBssid(), getSecurity());
        }
    }

    public int compareTo(AccessPoint accessPoint) {
        if (isActive() && !accessPoint.isActive()) {
            return -1;
        }
        if (!isActive() && accessPoint.isActive()) {
            return 1;
        }
        if (isReachable() && !accessPoint.isReachable()) {
            return -1;
        }
        if (!isReachable() && accessPoint.isReachable()) {
            return 1;
        }
        if (isSaved() && !accessPoint.isSaved()) {
            return -1;
        }
        if (!isSaved() && accessPoint.isSaved()) {
            return 1;
        }
        if (getSpeed() != accessPoint.getSpeed()) {
            return accessPoint.getSpeed() - getSpeed();
        }
        int calculateSignalLevel = WifiManager.calculateSignalLevel(accessPoint.mRssi, 5) - WifiManager.calculateSignalLevel(this.mRssi, 5);
        if (calculateSignalLevel != 0) {
            return calculateSignalLevel;
        }
        int compareToIgnoreCase = getTitle().compareToIgnoreCase(accessPoint.getTitle());
        if (compareToIgnoreCase != 0) {
            return compareToIgnoreCase;
        }
        return getSsidStr().compareTo(accessPoint.getSsidStr());
    }

    public boolean equals(Object obj) {
        if ((obj instanceof AccessPoint) && compareTo((AccessPoint) obj) == 0) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        WifiInfo wifiInfo = this.mInfo;
        int i = 0;
        if (wifiInfo != null) {
            i = 0 + (wifiInfo.hashCode() * 13);
        }
        return i + (this.mRssi * 19) + (this.networkId * 23) + (this.ssid.hashCode() * 29);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AccessPoint(");
        sb.append(this.ssid);
        if (this.bssid != null) {
            sb.append(":");
            sb.append(this.bssid);
        }
        if (isSaved()) {
            sb.append(',');
            sb.append("saved");
        }
        if (isActive()) {
            sb.append(',');
            sb.append("active");
        }
        if (isEphemeral()) {
            sb.append(',');
            sb.append("ephemeral");
        }
        if (isConnectable()) {
            sb.append(',');
            sb.append("connectable");
        }
        int i = this.security;
        if (!(i == 0 || i == 4)) {
            sb.append(',');
            sb.append(securityToString(this.security, this.pskType));
        }
        sb.append(",level=");
        sb.append(getLevel());
        if (this.mSpeed != 0) {
            sb.append(",speed=");
            sb.append(this.mSpeed);
        }
        sb.append(",metered=");
        sb.append(isMetered());
        if (isVerboseLoggingEnabled()) {
            sb.append(",rssi=");
            sb.append(this.mRssi);
            synchronized (this.mLock) {
                sb.append(",scan cache size=");
                sb.append(this.mScanResults.size() + this.mExtraScanResults.size());
            }
        }
        sb.append(')');
        return sb.toString();
    }

    public boolean update(WifiNetworkScoreCache wifiNetworkScoreCache, boolean z, long j) {
        boolean updateScores = z ? updateScores(wifiNetworkScoreCache, j) : false;
        if (updateMetered(wifiNetworkScoreCache) || updateScores) {
            return true;
        }
        return false;
    }

    private boolean updateScores(WifiNetworkScoreCache wifiNetworkScoreCache, long j) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            Iterator<ScanResult> it = this.mScanResults.iterator();
            while (it.hasNext()) {
                ScanResult next = it.next();
                ScoredNetwork scoredNetwork = wifiNetworkScoreCache.getScoredNetwork(next);
                if (scoredNetwork != null) {
                    TimestampedScoredNetwork timestampedScoredNetwork = this.mScoredNetworkCache.get(next.BSSID);
                    if (timestampedScoredNetwork == null) {
                        this.mScoredNetworkCache.put(next.BSSID, new TimestampedScoredNetwork(scoredNetwork, elapsedRealtime));
                    } else {
                        timestampedScoredNetwork.update(scoredNetwork, elapsedRealtime);
                    }
                }
            }
        }
        Iterator<TimestampedScoredNetwork> it2 = this.mScoredNetworkCache.values().iterator();
        it2.forEachRemaining(new Consumer(elapsedRealtime - j, it2) {
            /* class com.android.settingslib.wifi.$$Lambda$AccessPoint$OIXfUc7y1PqI_zmQ3STe_086YzY */
            private final /* synthetic */ long f$0;
            private final /* synthetic */ Iterator f$1;

            {
                this.f$0 = r1;
                this.f$1 = r3;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AccessPoint.lambda$updateScores$0(this.f$0, this.f$1, (TimestampedScoredNetwork) obj);
            }
        });
        return updateSpeed();
    }

    static /* synthetic */ void lambda$updateScores$0(long j, Iterator it, TimestampedScoredNetwork timestampedScoredNetwork) {
        if (timestampedScoredNetwork.getUpdatedTimestampMillis() < j) {
            it.remove();
        }
    }

    private boolean updateSpeed() {
        int i = this.mSpeed;
        this.mSpeed = generateAverageSpeedForSsid();
        boolean z = i != this.mSpeed;
        if (isVerboseLoggingEnabled() && z) {
            Log.i("SettingsLib.AccessPoint", String.format("%s: Set speed to %d", this.ssid, Integer.valueOf(this.mSpeed)));
        }
        return z;
    }

    private int generateAverageSpeedForSsid() {
        if (this.mScoredNetworkCache.isEmpty()) {
            return 0;
        }
        if (Log.isLoggable("SettingsLib.AccessPoint", 3)) {
            Log.d("SettingsLib.AccessPoint", String.format("Generating fallbackspeed for %s using cache: %s", getSsidStr(), this.mScoredNetworkCache));
        }
        int i = 0;
        int i2 = 0;
        for (TimestampedScoredNetwork timestampedScoredNetwork : this.mScoredNetworkCache.values()) {
            int calculateBadge = timestampedScoredNetwork.getScore().calculateBadge(this.mRssi);
            if (calculateBadge != 0) {
                i++;
                i2 += calculateBadge;
            }
        }
        int i3 = i == 0 ? 0 : i2 / i;
        if (isVerboseLoggingEnabled()) {
            Log.i("SettingsLib.AccessPoint", String.format("%s generated fallback speed is: %d", getSsidStr(), Integer.valueOf(i3)));
        }
        return roundToClosestSpeedEnum(i3);
    }

    private boolean updateMetered(WifiNetworkScoreCache wifiNetworkScoreCache) {
        WifiInfo wifiInfo;
        boolean z = this.mIsScoredNetworkMetered;
        this.mIsScoredNetworkMetered = false;
        if (!isActive() || (wifiInfo = this.mInfo) == null) {
            synchronized (this.mLock) {
                Iterator<ScanResult> it = this.mScanResults.iterator();
                while (it.hasNext()) {
                    ScoredNetwork scoredNetwork = wifiNetworkScoreCache.getScoredNetwork(it.next());
                    if (scoredNetwork != null) {
                        this.mIsScoredNetworkMetered = scoredNetwork.meteredHint | this.mIsScoredNetworkMetered;
                    }
                }
            }
        } else {
            ScoredNetwork scoredNetwork2 = wifiNetworkScoreCache.getScoredNetwork(NetworkKey.createFromWifiInfo(wifiInfo));
            if (scoredNetwork2 != null) {
                this.mIsScoredNetworkMetered = scoredNetwork2.meteredHint | this.mIsScoredNetworkMetered;
            }
        }
        if (z == this.mIsScoredNetworkMetered) {
            return true;
        }
        return false;
    }

    public static String getKey(ScanResult scanResult) {
        return getKey(scanResult.SSID, scanResult.BSSID, getSecurity(scanResult));
    }

    public static String getKey(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.isPasspoint()) {
            return getKey(wifiConfiguration.FQDN);
        }
        return getKey(removeDoubleQuotes(wifiConfiguration.SSID), wifiConfiguration.BSSID, getSecurity(wifiConfiguration));
    }

    public static String getKey(String str) {
        return "FQDN:" + str;
    }

    public static String getKey(OsuProvider osuProvider) {
        return "OSU:" + osuProvider.getFriendlyName() + ',' + osuProvider.getServerUri();
    }

    private static String getKey(String str, String str2, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("AP:");
        if (TextUtils.isEmpty(str)) {
            sb.append(str2);
        } else {
            sb.append(str);
        }
        sb.append(',');
        sb.append(i);
        return sb.toString();
    }

    public String getKey() {
        return this.mKey;
    }

    public WifiConfiguration getConfig() {
        return this.mConfig;
    }

    public WifiInfo getInfo() {
        return this.mInfo;
    }

    public int getLevel() {
        return WifiManager.calculateSignalLevel(this.mRssi, 5);
    }

    public Set<ScanResult> getScanResults() {
        ArraySet arraySet = new ArraySet();
        synchronized (this.mLock) {
            arraySet.addAll((Collection) this.mScanResults);
            arraySet.addAll((Collection) this.mExtraScanResults);
        }
        return arraySet;
    }

    public Map<String, TimestampedScoredNetwork> getScoredNetworkCache() {
        return this.mScoredNetworkCache;
    }

    private void updateBestRssiInfo() {
        ScanResult scanResult;
        int i;
        int i2;
        if (!isActive()) {
            synchronized (this.mLock) {
                Iterator<ScanResult> it = this.mScanResults.iterator();
                scanResult = null;
                i = Integer.MIN_VALUE;
                while (it.hasNext()) {
                    ScanResult next = it.next();
                    if (next.level > i) {
                        i = next.level;
                        scanResult = next;
                    }
                }
            }
            if (i == Integer.MIN_VALUE || (i2 = this.mRssi) == Integer.MIN_VALUE) {
                this.mRssi = i;
            } else {
                this.mRssi = (i2 + i) / 2;
            }
            if (scanResult != null) {
                this.ssid = scanResult.SSID;
                this.bssid = scanResult.BSSID;
                this.security = getSecurity(scanResult);
                int i3 = this.security;
                if (i3 == 2 || i3 == 5 || i3 == 7) {
                    this.pskType = getPskType(scanResult);
                }
                if (this.security == 3) {
                    this.mEapType = getEapType(scanResult);
                }
                this.mIsCarrierAp = scanResult.isCarrierAp;
                this.mCarrierApEapType = scanResult.carrierApEapType;
                this.mCarrierName = scanResult.carrierName;
            }
            if (isPasspoint()) {
                this.mConfig.SSID = convertToQuotedString(this.ssid);
            }
        }
    }

    private int getMaxCapability(ScanResult scanResult) {
        if (isVerboseLoggingEnabled()) {
            Log.i("SettingsLib.AccessPoint", "SSID: " + scanResult.SSID + ", bssid: " + scanResult.BSSID + ", capabilities: " + scanResult.capabilities);
        }
        if (scanResult.capabilities.contains("WFA-HE")) {
            return 3;
        }
        if (scanResult.capabilities.contains("WFA-VHT")) {
            return 2;
        }
        return scanResult.capabilities.contains("WFA-HT") ? 1 : 0;
    }

    private void updateWifiGeneration() {
        if (!isActive()) {
            int i = Integer.MAX_VALUE;
            this.mTwtSupport = false;
            this.mVhtMax8SpatialStreamsSupport = false;
            Iterator<ScanResult> it = this.mScanResults.iterator();
            while (it.hasNext()) {
                int maxCapability = getMaxCapability(it.next());
                if (maxCapability < i) {
                    i = maxCapability;
                }
            }
            if (i == 1) {
                this.mWifiGeneration = 4;
            } else if (i == 2) {
                this.mWifiGeneration = 5;
            } else if (i != 3) {
                this.mWifiGeneration = 0;
            } else {
                this.mWifiGeneration = 6;
            }
        }
    }

    public boolean isMetered() {
        return this.mIsScoredNetworkMetered || WifiConfiguration.isMetered(this.mConfig, this.mInfo);
    }

    public int getSecurity() {
        return this.security;
    }

    public String getSsidStr() {
        return this.ssid;
    }

    public String getBssid() {
        return this.bssid;
    }

    public CharSequence getSsid() {
        return this.ssid;
    }

    public NetworkInfo.DetailedState getDetailedState() {
        NetworkInfo networkInfo = this.mNetworkInfo;
        if (networkInfo != null) {
            return networkInfo.getDetailedState();
        }
        Log.w("SettingsLib.AccessPoint", "NetworkInfo is null, cannot return detailed state");
        return null;
    }

    public String getTitle() {
        if (isPasspoint()) {
            return this.mConfig.providerFriendlyName;
        }
        if (isPasspointConfig()) {
            return this.mProviderFriendlyName;
        }
        if (isOsuProvider()) {
            return this.mOsuProvider.getFriendlyName();
        }
        return getSsidStr();
    }

    public String getSummary() {
        return getSettingsSummary();
    }

    public String getSettingsSummary() {
        return getSettingsSummary(false);
    }

    public String getSettingsSummary(boolean z) {
        int i;
        StringBuilder sb = new StringBuilder();
        if (isOsuProvider()) {
            if (this.mOsuProvisioningComplete) {
                sb.append(this.mContext.getString(R$string.osu_sign_up_complete));
            } else {
                String str = this.mOsuFailure;
                if (str != null) {
                    sb.append(str);
                } else {
                    String str2 = this.mOsuStatus;
                    if (str2 != null) {
                        sb.append(str2);
                    } else {
                        sb.append(this.mContext.getString(R$string.tap_to_sign_up));
                    }
                }
            }
        } else if (!isActive()) {
            WifiConfiguration wifiConfiguration = this.mConfig;
            if (wifiConfiguration == null || wifiConfiguration.autoConnect != 0) {
                WifiConfiguration wifiConfiguration2 = this.mConfig;
                if (wifiConfiguration2 == null || !wifiConfiguration2.hasNoInternetAccess()) {
                    WifiConfiguration wifiConfiguration3 = this.mConfig;
                    if (wifiConfiguration3 == null || wifiConfiguration3.getNetworkSelectionStatus().isNetworkEnabled()) {
                        WifiConfiguration wifiConfiguration4 = this.mConfig;
                        if (wifiConfiguration4 != null && wifiConfiguration4.getNetworkSelectionStatus().isNotRecommended()) {
                            sb.append(this.mContext.getString(R$string.wifi_disabled_by_recommendation_provider));
                        } else if (this.mIsCarrierAp) {
                            sb.append(String.format(this.mContext.getString(R$string.available_via_carrier), this.mCarrierName));
                        } else if (!isReachable()) {
                            sb.append(this.mContext.getString(R$string.wifi_not_in_range));
                        } else {
                            WifiConfiguration wifiConfiguration5 = this.mConfig;
                            if (wifiConfiguration5 != null) {
                                if (wifiConfiguration5.recentFailure.getAssociationStatus() == 17) {
                                    sb.append(this.mContext.getString(R$string.wifi_ap_unable_to_handle_new_sta));
                                } else if (z) {
                                    sb.append(this.mContext.getString(R$string.wifi_disconnected));
                                } else {
                                    sb.append(this.mContext.getString(R$string.wifi_remembered));
                                }
                            }
                        }
                    } else {
                        int networkSelectionDisableReason = this.mConfig.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                        if (networkSelectionDisableReason == 2) {
                            sb.append(this.mContext.getString(R$string.wifi_disabled_generic));
                        } else if (networkSelectionDisableReason == 3) {
                            sb.append(this.mContext.getString(R$string.wifi_disabled_password_failure));
                        } else if (networkSelectionDisableReason == 4 || networkSelectionDisableReason == 5) {
                            sb.append(this.mContext.getString(R$string.wifi_disabled_network_failure));
                        } else if (networkSelectionDisableReason == 13) {
                            sb.append(this.mContext.getString(R$string.wifi_check_password_try_again));
                        }
                    }
                } else {
                    if (this.mConfig.getNetworkSelectionStatus().isNetworkPermanentlyDisabled()) {
                        i = R$string.wifi_no_internet_no_reconnect;
                    } else {
                        i = R$string.wifi_no_internet;
                    }
                    sb.append(this.mContext.getString(i));
                }
            } else {
                sb.append(this.mContext.getString(R$string.wifi_auto_connect_off));
            }
        } else if (getDetailedState() != NetworkInfo.DetailedState.CONNECTED || !this.mIsCarrierAp) {
            Context context = this.mContext;
            NetworkInfo.DetailedState detailedState = getDetailedState();
            WifiInfo wifiInfo = this.mInfo;
            boolean z2 = wifiInfo != null && wifiInfo.isEphemeral();
            WifiInfo wifiInfo2 = this.mInfo;
            sb.append(getSummary(context, null, detailedState, z2, wifiInfo2 != null ? wifiInfo2.getNetworkSuggestionOrSpecifierPackageName() : null));
        } else {
            sb.append(String.format(this.mContext.getString(R$string.connected_via_carrier), this.mCarrierName));
        }
        if (isVerboseLoggingEnabled()) {
            sb.append(WifiUtils.buildLoggingSummary(this, this.mConfig));
        }
        WifiConfiguration wifiConfiguration6 = this.mConfig;
        if (wifiConfiguration6 != null && (WifiUtils.isMeteredOverridden(wifiConfiguration6) || this.mConfig.meteredHint)) {
            return this.mContext.getResources().getString(R$string.preference_summary_default_combination, WifiUtils.getMeteredLabel(this.mContext, this.mConfig), sb.toString());
        } else if (getSpeedLabel() != null && sb.length() != 0) {
            return this.mContext.getResources().getString(R$string.preference_summary_default_combination, getSpeedLabel(), sb.toString());
        } else if (getSpeedLabel() != null) {
            return getSpeedLabel();
        } else {
            return sb.toString();
        }
    }

    public boolean isActive() {
        NetworkInfo networkInfo = this.mNetworkInfo;
        return (networkInfo == null || (this.networkId == -1 && networkInfo.getState() == NetworkInfo.State.DISCONNECTED)) ? false : true;
    }

    public boolean isConnectable() {
        return getLevel() != -1 && getDetailedState() == null;
    }

    public boolean isEphemeral() {
        NetworkInfo networkInfo;
        WifiInfo wifiInfo = this.mInfo;
        return (wifiInfo == null || !wifiInfo.isEphemeral() || (networkInfo = this.mNetworkInfo) == null || networkInfo.getState() == NetworkInfo.State.DISCONNECTED) ? false : true;
    }

    public boolean isPasspoint() {
        WifiConfiguration wifiConfiguration = this.mConfig;
        return wifiConfiguration != null && wifiConfiguration.isPasspoint();
    }

    public boolean isPasspointConfig() {
        return this.mFqdn != null && this.mConfig == null;
    }

    public boolean isOsuProvider() {
        return this.mOsuProvider != null;
    }

    private boolean isInfoForThisAccessPoint(WifiConfiguration wifiConfiguration, WifiInfo wifiInfo) {
        if (wifiInfo.isOsuAp() || this.mOsuStatus != null) {
            return wifiInfo.isOsuAp() && this.mOsuStatus != null;
        }
        if (wifiInfo.isPasspointAp() || isPasspoint()) {
            return wifiInfo.isPasspointAp() && isPasspoint() && TextUtils.equals(wifiInfo.getPasspointFqdn(), this.mConfig.FQDN);
        }
        int i = this.networkId;
        if (i != -1) {
            return i == wifiInfo.getNetworkId();
        }
        if (wifiConfiguration != null) {
            return isKeyEqual(getKey(wifiConfiguration));
        }
        return TextUtils.equals(removeDoubleQuotes(wifiInfo.getSSID()), this.ssid);
    }

    public boolean isSaved() {
        return this.mConfig != null;
    }

    public void setTag(Object obj) {
        this.mTag = obj;
    }

    public void generateOpenNetworkConfig() {
        int i = this.security;
        if (i != 0 && i != 4 && i != 8) {
            throw new IllegalStateException();
        } else if (this.mConfig == null) {
            this.mConfig = new WifiConfiguration();
            this.mConfig.SSID = convertToQuotedString(this.ssid);
            if (this.security == 0 || !getWifiManager().isEasyConnectSupported()) {
                this.mConfig.allowedKeyManagement.set(0);
                return;
            }
            this.mConfig.allowedKeyManagement.set(9);
            this.mConfig.requirePMF = true;
        }
    }

    private boolean isKeyEqual(String str) {
        if (this.mKey == null) {
            return false;
        }
        if ((str.endsWith(sPskSuffix) || str.endsWith(sSaeSuffix)) && this.mKey.endsWith(sPskSaeSuffix)) {
            String str2 = this.mKey;
            return TextUtils.equals(str2.substring(0, str2.lastIndexOf(44)), str.substring(0, str.lastIndexOf(44)));
        } else if ((!str.endsWith(sOpenSuffix) && !str.endsWith(sOweSuffix)) || !this.mKey.endsWith(sOweTransSuffix)) {
            return this.mKey.equals(str);
        } else {
            String str3 = this.mKey;
            return TextUtils.equals(str3.substring(0, str3.lastIndexOf(44)), str.substring(0, str.lastIndexOf(44)));
        }
    }

    public void setScanResults(Collection<ScanResult> collection) {
        if (CollectionUtils.isEmpty(collection)) {
            Log.d("SettingsLib.AccessPoint", "Cannot set scan results to empty list");
            return;
        }
        if (this.mKey != null && !isPasspoint() && !isOsuProvider()) {
            for (ScanResult scanResult : collection) {
                String key = getKey(scanResult);
                if (!isKeyEqual(key)) {
                    Log.d("SettingsLib.AccessPoint", String.format("ScanResult %s\nkey of %s did not match current AP key %s", scanResult, key, this.mKey));
                    return;
                }
            }
        }
        int level = getLevel();
        synchronized (this.mLock) {
            this.mScanResults.clear();
            this.mScanResults.addAll(collection);
        }
        updateBestRssiInfo();
        updateWifiGeneration();
        int level2 = getLevel();
        if (level2 > 0 && level2 != level) {
            updateSpeed();
            ThreadUtils.postOnMainThread(new Runnable() {
                /* class com.android.settingslib.wifi.$$Lambda$AccessPoint$MkkIS1nUbezHicDMmYnviyiBJyo */

                public final void run() {
                    AccessPoint.this.lambda$setScanResults$1$AccessPoint();
                }
            });
        }
        ThreadUtils.postOnMainThread(new Runnable() {
            /* class com.android.settingslib.wifi.$$Lambda$AccessPoint$0Yq14aFJZLjPMzFGAvglLaxsblI */

            public final void run() {
                AccessPoint.this.lambda$setScanResults$2$AccessPoint();
            }
        });
    }

    public /* synthetic */ void lambda$setScanResults$1$AccessPoint() {
        AccessPointListener accessPointListener = this.mAccessPointListener;
        if (accessPointListener != null) {
            accessPointListener.onLevelChanged(this);
        }
    }

    public /* synthetic */ void lambda$setScanResults$2$AccessPoint() {
        AccessPointListener accessPointListener = this.mAccessPointListener;
        if (accessPointListener != null) {
            accessPointListener.onAccessPointChanged(this);
        }
    }

    public void setScanResultsPasspoint(Collection<ScanResult> collection, Collection<ScanResult> collection2) {
        synchronized (this.mLock) {
            this.mExtraScanResults.clear();
            if (!CollectionUtils.isEmpty(collection)) {
                this.mIsRoaming = false;
                if (!CollectionUtils.isEmpty(collection2)) {
                    this.mExtraScanResults.addAll(collection2);
                }
                setScanResults(collection);
            } else if (!CollectionUtils.isEmpty(collection2)) {
                this.mIsRoaming = true;
                setScanResults(collection2);
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0071, code lost:
        if (r5.getDetailedState() != r7.getDetailedState()) goto L_0x0061;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean update(android.net.wifi.WifiConfiguration r5, android.net.wifi.WifiInfo r6, android.net.NetworkInfo r7) {
        /*
        // Method dump skipped, instructions count: 163
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.wifi.AccessPoint.update(android.net.wifi.WifiConfiguration, android.net.wifi.WifiInfo, android.net.NetworkInfo):boolean");
    }

    public /* synthetic */ void lambda$update$3$AccessPoint() {
        AccessPointListener accessPointListener = this.mAccessPointListener;
        if (accessPointListener != null) {
            accessPointListener.onAccessPointChanged(this);
        }
    }

    public /* synthetic */ void lambda$update$4$AccessPoint() {
        AccessPointListener accessPointListener = this.mAccessPointListener;
        if (accessPointListener != null) {
            accessPointListener.onLevelChanged(this);
        }
    }

    public void update(WifiConfiguration wifiConfiguration) {
        this.mConfig = wifiConfiguration;
        WifiConfiguration wifiConfiguration2 = this.mConfig;
        if (wifiConfiguration2 != null) {
            this.ssid = removeDoubleQuotes(wifiConfiguration2.SSID);
        }
        this.networkId = wifiConfiguration != null ? wifiConfiguration.networkId : -1;
        ThreadUtils.postOnMainThread(new Runnable() {
            /* class com.android.settingslib.wifi.$$Lambda$AccessPoint$QyP0aXhFuWtm7lmBu1IY3qbfmBA */

            public final void run() {
                AccessPoint.this.lambda$update$5$AccessPoint();
            }
        });
    }

    public /* synthetic */ void lambda$update$5$AccessPoint() {
        AccessPointListener accessPointListener = this.mAccessPointListener;
        if (accessPointListener != null) {
            accessPointListener.onAccessPointChanged(this);
        }
    }

    public void setRssi(int i) {
        this.mRssi = i;
    }

    public int getSpeed() {
        return this.mSpeed;
    }

    public String getSpeedLabel() {
        return getSpeedLabel(this.mSpeed);
    }

    public String getSpeedLabel(int i) {
        return getSpeedLabel(this.mContext, i);
    }

    private static String getSpeedLabel(Context context, int i) {
        if (i == 5) {
            return context.getString(R$string.speed_label_slow);
        }
        if (i == 10) {
            return context.getString(R$string.speed_label_okay);
        }
        if (i == 20) {
            return context.getString(R$string.speed_label_fast);
        }
        if (i != 30) {
            return null;
        }
        return context.getString(R$string.speed_label_very_fast);
    }

    public static String getSpeedLabel(Context context, ScoredNetwork scoredNetwork, int i) {
        return getSpeedLabel(context, roundToClosestSpeedEnum(scoredNetwork.calculateBadge(i)));
    }

    public boolean isReachable() {
        return this.mRssi != Integer.MIN_VALUE;
    }

    private static CharSequence getAppLabel(String str, PackageManager packageManager) {
        try {
            ApplicationInfo applicationInfoAsUser = packageManager.getApplicationInfoAsUser(str, 0, UserHandle.getUserId(-2));
            if (applicationInfoAsUser != null) {
                return applicationInfoAsUser.loadLabel(packageManager);
            }
            return "";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SettingsLib.AccessPoint", "Failed to get app info", e);
            return "";
        }
    }

    public static String getSummary(Context context, String str, NetworkInfo.DetailedState detailedState, boolean z, String str2) {
        if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
            if (z && !TextUtils.isEmpty(str2)) {
                return context.getString(R$string.connected_via_app, getAppLabel(str2, context.getPackageManager()));
            } else if (z) {
                NetworkScorerAppData activeScorer = ((NetworkScoreManager) context.getSystemService(NetworkScoreManager.class)).getActiveScorer();
                if (activeScorer == null || activeScorer.getRecommendationServiceLabel() == null) {
                    return context.getString(R$string.connected_via_network_scorer_default);
                }
                return String.format(context.getString(R$string.connected_via_network_scorer), activeScorer.getRecommendationServiceLabel());
            }
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
            NetworkCapabilities networkCapabilities = null;
            try {
                networkCapabilities = connectivityManager.getNetworkCapabilities(IWifiManager.Stub.asInterface(ServiceManager.getService("wifi")).getCurrentNetwork());
            } catch (RemoteException unused) {
            }
            if (networkCapabilities != null) {
                if (networkCapabilities.hasCapability(17)) {
                    return context.getString(context.getResources().getIdentifier("network_available_sign_in", "string", "android"));
                }
                if (networkCapabilities.hasCapability(24)) {
                    return context.getString(R$string.wifi_limited_connection);
                }
                if (!networkCapabilities.hasCapability(16)) {
                    return context.getString(R$string.wifi_connected_no_internet);
                }
            }
        }
        if (detailedState == null) {
            Log.w("SettingsLib.AccessPoint", "state is null, returning empty summary");
            return "";
        }
        String[] stringArray = context.getResources().getStringArray(str == null ? R$array.wifi_status : R$array.wifi_status_with_ssid);
        int ordinal = detailedState.ordinal();
        if (ordinal >= stringArray.length || stringArray[ordinal].length() == 0) {
            return "";
        }
        return String.format(stringArray[ordinal], str);
    }

    public static String convertToQuotedString(String str) {
        return "\"" + str + "\"";
    }

    private static int getPskType(ScanResult scanResult) {
        boolean contains = scanResult.capabilities.contains("WPA-PSK");
        boolean contains2 = scanResult.capabilities.contains("RSN-PSK");
        boolean z = scanResult.capabilities.contains("PSK") && scanResult.capabilities.contains("SAE");
        boolean contains3 = scanResult.capabilities.contains("RSN-SAE");
        if (z) {
            return 4;
        }
        if (contains2 && contains) {
            return 3;
        }
        if (contains2) {
            return 2;
        }
        if (contains) {
            return 1;
        }
        if (!contains3) {
            Log.w("SettingsLib.AccessPoint", "Received abnormal flag string: " + scanResult.capabilities);
        }
        return 0;
    }

    private static int getEapType(ScanResult scanResult) {
        if (scanResult.capabilities.contains("RSN-EAP")) {
            return 2;
        }
        return scanResult.capabilities.contains("WPA-EAP") ? 1 : 0;
    }

    private static int getSecurity(ScanResult scanResult) {
        if (scanResult.capabilities.contains("WEP")) {
            return 1;
        }
        if (scanResult.capabilities.contains("PSK") && scanResult.capabilities.contains("SAE")) {
            return 7;
        }
        if (scanResult.capabilities.contains("SAE")) {
            return 5;
        }
        if (scanResult.capabilities.contains("PSK")) {
            return 2;
        }
        if (scanResult.capabilities.contains("EAP_SUITE_B_192")) {
            return 6;
        }
        if (scanResult.capabilities.contains("EAP")) {
            return 3;
        }
        if (scanResult.capabilities.contains("OWE_TRANSITION")) {
            return 8;
        }
        if (scanResult.capabilities.contains("OWE")) {
            return 4;
        }
        if (scanResult.capabilities.contains("WAPI")) {
            return scanResult.capabilities.contains("CERT") ? 10 : 9;
        }
        return 0;
    }

    static int getSecurity(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.allowedKeyManagement.get(8)) {
            return 5;
        }
        if (wifiConfiguration.allowedKeyManagement.get(1)) {
            return 2;
        }
        if (wifiConfiguration.allowedKeyManagement.get(10)) {
            return 6;
        }
        if (wifiConfiguration.allowedKeyManagement.get(2) || wifiConfiguration.allowedKeyManagement.get(3)) {
            return 3;
        }
        if (wifiConfiguration.allowedKeyManagement.get(9)) {
            return 4;
        }
        if (wifiConfiguration.allowedKeyManagement.get(100)) {
            return 9;
        }
        if (wifiConfiguration.allowedKeyManagement.get(101)) {
            return 10;
        }
        if (wifiConfiguration.wepKeys[0] != null) {
            return 1;
        }
        return 0;
    }

    static String removeDoubleQuotes(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        int length = str.length();
        if (length <= 1 || str.charAt(0) != '\"') {
            return str;
        }
        int i = length - 1;
        return str.charAt(i) == '\"' ? str.substring(1, i) : str;
    }

    private WifiManager getWifiManager() {
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        return this.mWifiManager;
    }

    private static boolean isVerboseLoggingEnabled() {
        return WifiTracker.sVerboseLogging || Log.isLoggable("SettingsLib.AccessPoint", 2);
    }

    public class AccessPointProvisioningCallback extends ProvisioningCallback {
        final /* synthetic */ AccessPoint this$0;

        public void onProvisioningFailure(int i) {
            if (TextUtils.equals(this.this$0.mOsuStatus, this.this$0.mContext.getString(R$string.osu_completing_sign_up))) {
                AccessPoint accessPoint = this.this$0;
                accessPoint.mOsuFailure = accessPoint.mContext.getString(R$string.osu_sign_up_failed);
            } else {
                AccessPoint accessPoint2 = this.this$0;
                accessPoint2.mOsuFailure = accessPoint2.mContext.getString(R$string.osu_connect_failed);
            }
            this.this$0.mOsuStatus = null;
            this.this$0.mOsuProvisioningComplete = false;
            ThreadUtils.postOnMainThread(new Runnable() {
                /* class com.android.settingslib.wifi.$$Lambda$AccessPoint$AccessPointProvisioningCallback$74qKnAJvzvRGvsJDwRIri14jOnQ */

                public final void run() {
                    AccessPoint.AccessPointProvisioningCallback.this.lambda$onProvisioningFailure$0$AccessPoint$AccessPointProvisioningCallback();
                }
            });
        }

        public /* synthetic */ void lambda$onProvisioningFailure$0$AccessPoint$AccessPointProvisioningCallback() {
            AccessPoint accessPoint = this.this$0;
            AccessPointListener accessPointListener = accessPoint.mAccessPointListener;
            if (accessPointListener != null) {
                accessPointListener.onAccessPointChanged(accessPoint);
            }
        }

        public void onProvisioningStatus(int i) {
            String str;
            switch (i) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    str = String.format(this.this$0.mContext.getString(R$string.osu_opening_provider), this.this$0.mOsuProvider.getFriendlyName());
                    break;
                case 8:
                case 9:
                case 10:
                case 11:
                    str = this.this$0.mContext.getString(R$string.osu_completing_sign_up);
                    break;
                default:
                    str = null;
                    break;
            }
            boolean equals = true ^ TextUtils.equals(this.this$0.mOsuStatus, str);
            this.this$0.mOsuStatus = str;
            this.this$0.mOsuFailure = null;
            this.this$0.mOsuProvisioningComplete = false;
            if (equals) {
                ThreadUtils.postOnMainThread(new Runnable() {
                    /* class com.android.settingslib.wifi.$$Lambda$AccessPoint$AccessPointProvisioningCallback$ko59tOsAuz6AC9y5NqUikXZo9s */

                    public final void run() {
                        AccessPoint.AccessPointProvisioningCallback.this.lambda$onProvisioningStatus$1$AccessPoint$AccessPointProvisioningCallback();
                    }
                });
            }
        }

        public /* synthetic */ void lambda$onProvisioningStatus$1$AccessPoint$AccessPointProvisioningCallback() {
            AccessPoint accessPoint = this.this$0;
            AccessPointListener accessPointListener = accessPoint.mAccessPointListener;
            if (accessPointListener != null) {
                accessPointListener.onAccessPointChanged(accessPoint);
            }
        }

        public void onProvisioningComplete() {
            this.this$0.mOsuProvisioningComplete = true;
            this.this$0.mOsuFailure = null;
            this.this$0.mOsuStatus = null;
            ThreadUtils.postOnMainThread(new Runnable() {
                /* class com.android.settingslib.wifi.$$Lambda$AccessPoint$AccessPointProvisioningCallback$8NkGPNV0jfGEnIZHmtcNMYE5Q7Q */

                public final void run() {
                    AccessPoint.AccessPointProvisioningCallback.this.lambda$onProvisioningComplete$2$AccessPoint$AccessPointProvisioningCallback();
                }
            });
            WifiManager wifiManager = this.this$0.getWifiManager();
            PasspointConfiguration passpointConfiguration = (PasspointConfiguration) wifiManager.getMatchingPasspointConfigsForOsuProviders(Collections.singleton(this.this$0.mOsuProvider)).get(this.this$0.mOsuProvider);
            if (passpointConfiguration == null) {
                Log.e("SettingsLib.AccessPoint", "Missing PasspointConfiguration for newly provisioned network!");
                if (this.this$0.mConnectListener != null) {
                    this.this$0.mConnectListener.onFailure(0);
                    return;
                }
                return;
            }
            String fqdn = passpointConfiguration.getHomeSp().getFqdn();
            for (Pair pair : wifiManager.getAllMatchingWifiConfigs(wifiManager.getScanResults())) {
                WifiConfiguration wifiConfiguration = (WifiConfiguration) pair.first;
                if (TextUtils.equals(wifiConfiguration.FQDN, fqdn)) {
                    wifiManager.connect(new AccessPoint(this.this$0.mContext, wifiConfiguration, (List) ((Map) pair.second).get(0), (List) ((Map) pair.second).get(1)).getConfig(), this.this$0.mConnectListener);
                    return;
                }
            }
            if (this.this$0.mConnectListener != null) {
                this.this$0.mConnectListener.onFailure(0);
            }
        }

        public /* synthetic */ void lambda$onProvisioningComplete$2$AccessPoint$AccessPointProvisioningCallback() {
            AccessPoint accessPoint = this.this$0;
            AccessPointListener accessPointListener = accessPoint.mAccessPointListener;
            if (accessPointListener != null) {
                accessPointListener.onAccessPointChanged(accessPoint);
            }
        }
    }
}
