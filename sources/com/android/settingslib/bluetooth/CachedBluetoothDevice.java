package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private final Collection<Callback> mCallbacks = new ArrayList();
    private long mConnectAttempted;
    private final Context mContext;
    BluetoothDevice mDevice;
    private long mHiSyncId;
    private boolean mIsActiveDeviceA2dp = false;
    private boolean mIsActiveDeviceHeadset = false;
    private boolean mIsActiveDeviceHearingAid = false;
    boolean mJustDiscovered;
    private final BluetoothAdapter mLocalAdapter;
    private boolean mLocalNapRoleConnected;
    private final Object mProfileLock = new Object();
    private final LocalBluetoothProfileManager mProfileManager;
    private final List<LocalBluetoothProfile> mProfiles = new ArrayList();
    private final List<LocalBluetoothProfile> mRemovedProfiles = new ArrayList();
    short mRssi;
    private CachedBluetoothDevice mSubDevice;
    public int mTwspBatteryLevel;
    public int mTwspBatteryState;

    public interface Callback {
        void onDeviceAttributesChanged();
    }

    CachedBluetoothDevice(Context context, LocalBluetoothProfileManager localBluetoothProfileManager, BluetoothDevice bluetoothDevice) {
        this.mContext = context;
        this.mLocalAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mProfileManager = localBluetoothProfileManager;
        this.mDevice = bluetoothDevice;
        fillData();
        this.mHiSyncId = 0;
        this.mTwspBatteryState = -1;
        this.mTwspBatteryLevel = -1;
    }

    private BluetoothDevice getTwsPeerDevice() {
        if (this.mDevice.isTwsPlusDevice()) {
            return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.mDevice.getTwsPlusPeerAddress());
        }
        return null;
    }

    private String describe(LocalBluetoothProfile localBluetoothProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:");
        sb.append(this.mDevice);
        if (localBluetoothProfile != null) {
            sb.append(" Profile:");
            sb.append(localBluetoothProfile);
        }
        return sb.toString();
    }

    /* access modifiers changed from: package-private */
    public void onProfileStateChanged(LocalBluetoothProfile localBluetoothProfile, int i) {
        Log.d("CachedBluetoothDevice", "onProfileStateChanged: profile " + localBluetoothProfile + ", device=" + this.mDevice + ", newProfileState " + i);
        if (this.mLocalAdapter.getState() == 13) {
            Log.d("CachedBluetoothDevice", " BT Turninig Off...Profile conn state change ignored...");
            return;
        }
        synchronized (this.mProfileLock) {
            if (i == 2) {
                if (localBluetoothProfile instanceof MapProfile) {
                    localBluetoothProfile.setPreferred(this.mDevice, true);
                }
                if (!this.mProfiles.contains(localBluetoothProfile)) {
                    this.mRemovedProfiles.remove(localBluetoothProfile);
                    this.mProfiles.add(localBluetoothProfile);
                    if ((localBluetoothProfile instanceof PanProfile) && ((PanProfile) localBluetoothProfile).isLocalRoleNap(this.mDevice)) {
                        this.mLocalNapRoleConnected = true;
                    }
                }
            } else if ((localBluetoothProfile instanceof MapProfile) && i == 0) {
                localBluetoothProfile.setPreferred(this.mDevice, false);
            } else if (this.mLocalNapRoleConnected && (localBluetoothProfile instanceof PanProfile) && ((PanProfile) localBluetoothProfile).isLocalRoleNap(this.mDevice) && i == 0) {
                Log.d("CachedBluetoothDevice", "Removing PanProfile from device after NAP disconnect");
                this.mProfiles.remove(localBluetoothProfile);
                this.mRemovedProfiles.add(localBluetoothProfile);
                this.mLocalNapRoleConnected = false;
            } else if ((localBluetoothProfile instanceof HeadsetProfile) && i == 0) {
                this.mTwspBatteryState = -1;
                this.mTwspBatteryLevel = -1;
            }
        }
        fetchActiveDevices();
    }

    public void disconnect() {
        synchronized (this.mProfileLock) {
            for (LocalBluetoothProfile localBluetoothProfile : this.mProfiles) {
                disconnect(localBluetoothProfile);
            }
        }
        PbapServerProfile pbapProfile = this.mProfileManager.getPbapProfile();
        if (pbapProfile != null && isConnectedProfile(pbapProfile)) {
            pbapProfile.disconnect(this.mDevice);
        }
    }

    public void disconnect(LocalBluetoothProfile localBluetoothProfile) {
        if (localBluetoothProfile.disconnect(this.mDevice)) {
            Log.d("CachedBluetoothDevice", "Command sent successfully:DISCONNECT " + describe(localBluetoothProfile));
        }
    }

    public void connect(boolean z) {
        if (ensurePaired()) {
            this.mConnectAttempted = SystemClock.elapsedRealtime();
            connectWithoutResettingTimer(z);
        }
    }

    public long getHiSyncId() {
        return this.mHiSyncId;
    }

    public void setHiSyncId(long j) {
        Log.d("CachedBluetoothDevice", "setHiSyncId: mDevice " + this.mDevice + ", id " + j);
        this.mHiSyncId = j;
    }

    public boolean isHearingAidDevice() {
        return this.mHiSyncId != 0;
    }

    /* access modifiers changed from: package-private */
    public void onBondingDockConnect() {
        connect(false);
    }

    private void connectWithoutResettingTimer(boolean z) {
        synchronized (this.mProfileLock) {
            if (this.mProfiles.isEmpty()) {
                Log.d("CachedBluetoothDevice", "No profiles. Maybe we will connect later for device " + this.mDevice);
                return;
            }
            int i = 0;
            for (LocalBluetoothProfile localBluetoothProfile : this.mProfiles) {
                if (z) {
                    if (localBluetoothProfile.accessProfileEnabled()) {
                    }
                } else if (!localBluetoothProfile.isAutoConnectable()) {
                }
                if (localBluetoothProfile.isPreferred(this.mDevice)) {
                    i++;
                    connectInt(localBluetoothProfile);
                }
            }
            Log.d("CachedBluetoothDevice", "Preferred profiles = " + i);
            if (i == 0) {
                connectAutoConnectableProfiles();
            }
        }
    }

    private void connectAutoConnectableProfiles() {
        if (ensurePaired()) {
            synchronized (this.mProfileLock) {
                for (LocalBluetoothProfile localBluetoothProfile : this.mProfiles) {
                    if (localBluetoothProfile.isAutoConnectable()) {
                        localBluetoothProfile.setPreferred(this.mDevice, true);
                        connectInt(localBluetoothProfile);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public synchronized void connectInt(LocalBluetoothProfile localBluetoothProfile) {
        if (ensurePaired()) {
            if (localBluetoothProfile.connect(this.mDevice)) {
                Log.d("CachedBluetoothDevice", "Command sent successfully:CONNECT " + describe(localBluetoothProfile));
                return;
            }
            Log.i("CachedBluetoothDevice", "Failed to connect " + localBluetoothProfile.toString() + " to " + getName());
        }
    }

    private boolean ensurePaired() {
        if (getBondState() != 10) {
            return true;
        }
        startPairing();
        return false;
    }

    public boolean startPairing() {
        if (this.mLocalAdapter.isDiscovering()) {
            this.mLocalAdapter.cancelDiscovery();
        }
        return this.mDevice.createBond();
    }

    public void unpair() {
        BluetoothDevice twsPeerDevice;
        int bondState = getBondState();
        if (bondState == 11) {
            this.mDevice.cancelBondProcess();
        }
        if (bondState != 10) {
            BluetoothDevice bluetoothDevice = this.mDevice;
            if (bluetoothDevice.isTwsPlusDevice() && (twsPeerDevice = getTwsPeerDevice()) != null && twsPeerDevice.removeBond()) {
                Log.d("CachedBluetoothDevice", "Command sent successfully:REMOVE_BOND " + twsPeerDevice.getName());
            }
            if (bluetoothDevice != null && bluetoothDevice.removeBond()) {
                Log.d("CachedBluetoothDevice", "Command sent successfully:REMOVE_BOND " + describe(null));
            }
        }
    }

    public int getProfileConnectionState(LocalBluetoothProfile localBluetoothProfile) {
        if (localBluetoothProfile != null) {
            return localBluetoothProfile.getConnectionStatus(this.mDevice);
        }
        return 0;
    }

    private void fillData() {
        updateProfiles();
        fetchActiveDevices();
        migratePhonebookPermissionChoice();
        migrateMessagePermissionChoice();
        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public String getAddress() {
        return this.mDevice.getAddress();
    }

    public String getName() {
        String aliasName = this.mDevice.getAliasName();
        return TextUtils.isEmpty(aliasName) ? getAddress() : aliasName;
    }

    /* access modifiers changed from: package-private */
    public void refreshName() {
        Log.d("CachedBluetoothDevice", "Device name: " + getName());
        dispatchAttributesChanged();
    }

    public int getBatteryLevel() {
        return this.mDevice.getBatteryLevel();
    }

    /* access modifiers changed from: package-private */
    public void refresh() {
        dispatchAttributesChanged();
    }

    public void setJustDiscovered(boolean z) {
        if (this.mJustDiscovered != z) {
            this.mJustDiscovered = z;
            dispatchAttributesChanged();
        }
    }

    public int getBondState() {
        return this.mDevice.getBondState();
    }

    public void onActiveDeviceChanged(boolean z, int i) {
        boolean z2 = false;
        if (i == 1) {
            if (this.mIsActiveDeviceHeadset != z) {
                z2 = true;
            }
            this.mIsActiveDeviceHeadset = z;
        } else if (i == 2) {
            if (this.mIsActiveDeviceA2dp != z) {
                z2 = true;
            }
            this.mIsActiveDeviceA2dp = z;
        } else if (i != 21) {
            Log.w("CachedBluetoothDevice", "onActiveDeviceChanged: unknown profile " + i + " isActive " + z);
        } else {
            if (this.mIsActiveDeviceHearingAid != z) {
                z2 = true;
            }
            this.mIsActiveDeviceHearingAid = z;
        }
        if (z2) {
            dispatchAttributesChanged();
        }
    }

    /* access modifiers changed from: package-private */
    public void onAudioModeChanged() {
        dispatchAttributesChanged();
    }

    public boolean isActiveDevice(int i) {
        if (i == 1) {
            return this.mIsActiveDeviceHeadset;
        }
        if (i == 2) {
            return this.mIsActiveDeviceA2dp;
        }
        if (i == 21) {
            return this.mIsActiveDeviceHearingAid;
        }
        Log.w("CachedBluetoothDevice", "getActiveDevice: unknown profile " + i);
        return false;
    }

    /* access modifiers changed from: package-private */
    public void setRssi(short s) {
        if (this.mRssi != s) {
            this.mRssi = s;
            dispatchAttributesChanged();
        }
    }

    public boolean isConnected() {
        synchronized (this.mProfileLock) {
            for (LocalBluetoothProfile localBluetoothProfile : this.mProfiles) {
                if (getProfileConnectionState(localBluetoothProfile) == 2) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isConnectedProfile(LocalBluetoothProfile localBluetoothProfile) {
        return getProfileConnectionState(localBluetoothProfile) == 2;
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids;
        ParcelUuid[] uuids2 = this.mDevice.getUuids();
        if (uuids2 == null || (uuids = this.mLocalAdapter.getUuids()) == null) {
            return false;
        }
        processPhonebookAccess();
        synchronized (this.mProfileLock) {
            this.mProfileManager.updateProfiles(uuids2, uuids, this.mProfiles, this.mRemovedProfiles, this.mLocalNapRoleConnected, this.mDevice);
        }
        Log.e("CachedBluetoothDevice", "updating profiles for " + this.mDevice.getAliasName() + ", " + this.mDevice);
        BluetoothClass bluetoothClass = this.mDevice.getBluetoothClass();
        if (bluetoothClass != null) {
            Log.v("CachedBluetoothDevice", "Class: " + bluetoothClass.toString());
        }
        Log.v("CachedBluetoothDevice", "UUID:");
        for (ParcelUuid parcelUuid : uuids2) {
            Log.v("CachedBluetoothDevice", "  " + parcelUuid);
        }
        return true;
    }

    private void fetchActiveDevices() {
        A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
        if (a2dpProfile != null) {
            this.mIsActiveDeviceA2dp = this.mDevice.equals(a2dpProfile.getActiveDevice());
        }
        HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
        if (headsetProfile != null) {
            this.mIsActiveDeviceHeadset = this.mDevice.equals(headsetProfile.getActiveDevice());
        }
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        if (hearingAidProfile != null) {
            this.mIsActiveDeviceHearingAid = hearingAidProfile.getActiveDevices().contains(this.mDevice);
        }
    }

    /* access modifiers changed from: package-private */
    public void onUuidChanged() {
        long j;
        updateProfiles();
        ParcelUuid[] uuids = this.mDevice.getUuids();
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hogp)) {
            j = 30000;
        } else {
            j = BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HearingAid) ? 15000 : 5000;
        }
        Log.d("CachedBluetoothDevice", "onUuidChanged: Time since last connect=" + (SystemClock.elapsedRealtime() - this.mConnectAttempted));
        if (!this.mProfiles.isEmpty() && this.mConnectAttempted + j > SystemClock.elapsedRealtime()) {
            connectWithoutResettingTimer(false);
        }
        dispatchAttributesChanged();
    }

    /* access modifiers changed from: package-private */
    public void onBondingStateChanged(int i) {
        if (i == 10) {
            synchronized (this.mProfileLock) {
                this.mProfiles.clear();
            }
            this.mDevice.setPhonebookAccessPermission(0);
            this.mDevice.setMessageAccessPermission(0);
            this.mDevice.setSimAccessPermission(0);
        }
        refresh();
        if (i == 12) {
            boolean isBondingInitiatedLocally = this.mDevice.isBondingInitiatedLocally();
            Log.w("CachedBluetoothDevice", "mIsBondingInitiatedLocally" + isBondingInitiatedLocally);
            if (isBondingInitiatedLocally) {
                this.mDevice.setBondingInitiatedLocally(false);
            }
            if (this.mDevice.isBluetoothDock()) {
                onBondingDockConnect();
            } else if (isBondingInitiatedLocally) {
                connect(false);
            }
        }
    }

    public BluetoothClass getBtClass() {
        return this.mDevice.getBluetoothClass();
    }

    public List<LocalBluetoothProfile> getProfiles() {
        return Collections.unmodifiableList(this.mProfiles);
    }

    public void registerCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(callback);
        }
    }

    /* access modifiers changed from: package-private */
    public void dispatchAttributesChanged() {
        synchronized (this.mCallbacks) {
            for (Callback callback : this.mCallbacks) {
                callback.onDeviceAttributesChanged();
            }
        }
    }

    public String toString() {
        return this.mDevice.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CachedBluetoothDevice)) {
            return false;
        }
        return this.mDevice.equals(((CachedBluetoothDevice) obj).mDevice);
    }

    public int hashCode() {
        return this.mDevice.getAddress().hashCode();
    }

    public int compareTo(CachedBluetoothDevice cachedBluetoothDevice) {
        int i = (cachedBluetoothDevice.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (i != 0) {
            return i;
        }
        int i2 = 1;
        int i3 = cachedBluetoothDevice.getBondState() == 12 ? 1 : 0;
        if (getBondState() != 12) {
            i2 = 0;
        }
        int i4 = i3 - i2;
        if (i4 != 0) {
            return i4;
        }
        int i5 = (cachedBluetoothDevice.mJustDiscovered ? 1 : 0) - (this.mJustDiscovered ? 1 : 0);
        if (i5 != 0) {
            return i5;
        }
        int i6 = cachedBluetoothDevice.mRssi - this.mRssi;
        if (i6 != 0) {
            return i6;
        }
        return getName().compareTo(cachedBluetoothDevice.getName());
    }

    private void migratePhonebookPermissionChoice() {
        SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("bluetooth_phonebook_permission", 0);
        if (sharedPreferences.contains(this.mDevice.getAddress())) {
            if (this.mDevice.getPhonebookAccessPermission() == 0) {
                int i = sharedPreferences.getInt(this.mDevice.getAddress(), 0);
                if (i == 1) {
                    this.mDevice.setPhonebookAccessPermission(1);
                } else if (i == 2) {
                    this.mDevice.setPhonebookAccessPermission(2);
                }
            }
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.remove(this.mDevice.getAddress());
            edit.commit();
        }
    }

    private void migrateMessagePermissionChoice() {
        SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("bluetooth_message_permission", 0);
        if (sharedPreferences.contains(this.mDevice.getAddress())) {
            if (this.mDevice.getMessageAccessPermission() == 0) {
                int i = sharedPreferences.getInt(this.mDevice.getAddress(), 0);
                if (i == 1) {
                    this.mDevice.setMessageAccessPermission(1);
                } else if (i == 2) {
                    this.mDevice.setMessageAccessPermission(2);
                }
            }
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.remove(this.mDevice.getAddress());
            edit.commit();
        }
    }

    private void processPhonebookAccess() {
        if (this.mDevice.getBondState() == 12 && BluetoothUuid.containsAnyUuid(this.mDevice.getUuids(), PbapServerProfile.PBAB_CLIENT_UUIDS) && this.mDevice.getPhonebookAccessPermission() == 0) {
            if (this.mDevice.getBluetoothClass() != null && (this.mDevice.getBluetoothClass().getDeviceClass() == 1032 || this.mDevice.getBluetoothClass().getDeviceClass() == 1028)) {
                EventLog.writeEvent(1397638484, "138529441", -1, "");
            }
            this.mDevice.setPhonebookAccessPermission(2);
        }
    }

    public int getMaxConnectionState() {
        int i;
        synchronized (this.mProfileLock) {
            i = 0;
            for (LocalBluetoothProfile localBluetoothProfile : getProfiles()) {
                int profileConnectionState = getProfileConnectionState(localBluetoothProfile);
                if (profileConnectionState > i) {
                    i = profileConnectionState;
                }
            }
        }
        return i;
    }

    public CachedBluetoothDevice getSubDevice() {
        return this.mSubDevice;
    }

    public void setSubDevice(CachedBluetoothDevice cachedBluetoothDevice) {
        this.mSubDevice = cachedBluetoothDevice;
    }

    public void switchSubDeviceContent() {
        BluetoothDevice bluetoothDevice = this.mDevice;
        short s = this.mRssi;
        boolean z = this.mJustDiscovered;
        CachedBluetoothDevice cachedBluetoothDevice = this.mSubDevice;
        this.mDevice = cachedBluetoothDevice.mDevice;
        this.mRssi = cachedBluetoothDevice.mRssi;
        this.mJustDiscovered = cachedBluetoothDevice.mJustDiscovered;
        cachedBluetoothDevice.mDevice = bluetoothDevice;
        cachedBluetoothDevice.mRssi = s;
        cachedBluetoothDevice.mJustDiscovered = z;
        fetchActiveDevices();
    }
}
