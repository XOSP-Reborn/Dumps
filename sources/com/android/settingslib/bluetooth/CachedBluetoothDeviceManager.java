package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CachedBluetoothDeviceManager {
    private final LocalBluetoothManager mBtManager;
    @VisibleForTesting
    final List<CachedBluetoothDevice> mCachedDevices = new ArrayList();
    private Context mContext;
    @VisibleForTesting
    HearingAidDeviceManager mHearingAidDeviceManager;

    CachedBluetoothDeviceManager(Context context, LocalBluetoothManager localBluetoothManager) {
        this.mContext = context;
        this.mBtManager = localBluetoothManager;
        this.mHearingAidDeviceManager = new HearingAidDeviceManager(localBluetoothManager, this.mCachedDevices);
    }

    public synchronized Collection<CachedBluetoothDevice> getCachedDevicesCopy() {
        return new ArrayList(this.mCachedDevices);
    }

    public void onDeviceNameUpdated(BluetoothDevice bluetoothDevice) {
        CachedBluetoothDevice findDevice = findDevice(bluetoothDevice);
        if (findDevice != null) {
            findDevice.refreshName();
        }
    }

    public synchronized CachedBluetoothDevice findDevice(BluetoothDevice bluetoothDevice) {
        for (CachedBluetoothDevice cachedBluetoothDevice : this.mCachedDevices) {
            if (cachedBluetoothDevice.getDevice().equals(bluetoothDevice)) {
                return cachedBluetoothDevice;
            }
            CachedBluetoothDevice subDevice = cachedBluetoothDevice.getSubDevice();
            if (subDevice != null && subDevice.getDevice().equals(bluetoothDevice)) {
                return subDevice;
            }
        }
        return null;
    }

    public CachedBluetoothDevice addDevice(BluetoothDevice bluetoothDevice) {
        CachedBluetoothDevice cachedBluetoothDevice = new CachedBluetoothDevice(this.mContext, this.mBtManager.getProfileManager(), bluetoothDevice);
        this.mHearingAidDeviceManager.initHearingAidDeviceIfNeeded(cachedBluetoothDevice);
        synchronized (this) {
            if (!this.mHearingAidDeviceManager.setSubDeviceIfNeeded(cachedBluetoothDevice)) {
                this.mCachedDevices.add(cachedBluetoothDevice);
                this.mBtManager.getEventManager().dispatchDeviceAdded(cachedBluetoothDevice);
            }
        }
        return cachedBluetoothDevice;
    }

    public synchronized boolean isSubDevice(BluetoothDevice bluetoothDevice) {
        CachedBluetoothDevice subDevice;
        for (CachedBluetoothDevice cachedBluetoothDevice : this.mCachedDevices) {
            if (!(cachedBluetoothDevice.getDevice().equals(bluetoothDevice) || (subDevice = cachedBluetoothDevice.getSubDevice()) == null || !subDevice.getDevice().equals(bluetoothDevice))) {
                return true;
            }
        }
        return false;
    }

    public synchronized void updateHearingAidsDevices() {
        this.mHearingAidDeviceManager.updateHearingAidsDevices();
    }

    public synchronized void clearNonBondedDevices() {
        clearNonBondedSubDevices();
        this.mCachedDevices.removeIf($$Lambda$CachedBluetoothDeviceManager$1n6G0RUX5KnCwfoBdpyaC68q3xA.INSTANCE);
    }

    static /* synthetic */ boolean lambda$clearNonBondedDevices$0(CachedBluetoothDevice cachedBluetoothDevice) {
        return cachedBluetoothDevice.getBondState() == 10;
    }

    private void clearNonBondedSubDevices() {
        for (int size = this.mCachedDevices.size() - 1; size >= 0; size--) {
            CachedBluetoothDevice cachedBluetoothDevice = this.mCachedDevices.get(size);
            CachedBluetoothDevice subDevice = cachedBluetoothDevice.getSubDevice();
            if (subDevice != null && subDevice.getDevice().getBondState() == 10) {
                cachedBluetoothDevice.setSubDevice(null);
            }
        }
    }

    public synchronized void onScanningStateChanged(boolean z) {
        if (z) {
            for (int size = this.mCachedDevices.size() - 1; size >= 0; size--) {
                CachedBluetoothDevice cachedBluetoothDevice = this.mCachedDevices.get(size);
                cachedBluetoothDevice.setJustDiscovered(false);
                CachedBluetoothDevice subDevice = cachedBluetoothDevice.getSubDevice();
                if (subDevice != null) {
                    subDevice.setJustDiscovered(false);
                }
            }
        }
    }

    public synchronized void onBluetoothStateChanged(int i) {
        if (i == 13) {
            for (int size = this.mCachedDevices.size() - 1; size >= 0; size--) {
                CachedBluetoothDevice cachedBluetoothDevice = this.mCachedDevices.get(size);
                CachedBluetoothDevice subDevice = cachedBluetoothDevice.getSubDevice();
                if (!(subDevice == null || subDevice.getBondState() == 12)) {
                    cachedBluetoothDevice.setSubDevice(null);
                }
                if (cachedBluetoothDevice.getBondState() != 12) {
                    cachedBluetoothDevice.setJustDiscovered(false);
                    this.mCachedDevices.remove(size);
                }
                cachedBluetoothDevice.mTwspBatteryState = -1;
                cachedBluetoothDevice.mTwspBatteryLevel = -1;
            }
        }
    }

    public synchronized void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        for (CachedBluetoothDevice cachedBluetoothDevice2 : this.mCachedDevices) {
            cachedBluetoothDevice2.onActiveDeviceChanged(Objects.equals(cachedBluetoothDevice2, cachedBluetoothDevice), i);
        }
    }

    public synchronized boolean onProfileConnectionStateChangedIfProcessed(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        return this.mHearingAidDeviceManager.onProfileConnectionStateChangedIfProcessed(cachedBluetoothDevice, i);
    }

    public synchronized void onDeviceUnpaired(CachedBluetoothDevice cachedBluetoothDevice) {
        CachedBluetoothDevice findMainDevice = this.mHearingAidDeviceManager.findMainDevice(cachedBluetoothDevice);
        CachedBluetoothDevice subDevice = cachedBluetoothDevice.getSubDevice();
        if (subDevice != null) {
            subDevice.unpair();
            cachedBluetoothDevice.setSubDevice(null);
        } else if (findMainDevice != null) {
            findMainDevice.unpair();
            findMainDevice.setSubDevice(null);
        }
    }

    public synchronized void dispatchAudioModeChanged() {
        for (CachedBluetoothDevice cachedBluetoothDevice : this.mCachedDevices) {
            cachedBluetoothDevice.onAudioModeChanged();
        }
    }
}
