package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.List;

public class A2dpProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] SINK_UUIDS = {BluetoothUuid.AudioSink, BluetoothUuid.AdvAudioDist};
    private Context mContext;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothA2dp mService;

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean accessProfileEnabled() {
        return true;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getProfileId() {
        return 2;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "A2DP";
    }

    private final class A2dpServiceListener implements BluetoothProfile.ServiceListener {
        private A2dpServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            A2dpProfile.this.mService = (BluetoothA2dp) bluetoothProfile;
            List<BluetoothDevice> connectedDevices = A2dpProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice remove = connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = A2dpProfile.this.mDeviceManager.findDevice(remove);
                if (findDevice == null) {
                    Log.w("A2dpProfile", "A2dpProfile found new device: " + remove);
                    findDevice = A2dpProfile.this.mDeviceManager.addDevice(remove);
                }
                findDevice.onProfileStateChanged(A2dpProfile.this, 2);
                findDevice.refresh();
            }
            A2dpProfile.this.mIsProfileReady = true;
            A2dpProfile.this.mProfileManager.callServiceConnectedListeners();
        }

        public void onServiceDisconnected(int i) {
            A2dpProfile.this.mIsProfileReady = false;
        }
    }

    A2dpProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mContext = context;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new A2dpServiceListener(), 2);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothA2dp bluetoothA2dp = this.mService;
        if (bluetoothA2dp == null) {
            return false;
        }
        return bluetoothA2dp.connect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothA2dp bluetoothA2dp = this.mService;
        if (bluetoothA2dp == null) {
            return false;
        }
        if (bluetoothA2dp.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothA2dp bluetoothA2dp = this.mService;
        if (bluetoothA2dp == null) {
            return 0;
        }
        return bluetoothA2dp.getConnectionState(bluetoothDevice);
    }

    public BluetoothDevice getActiveDevice() {
        BluetoothA2dp bluetoothA2dp = this.mService;
        if (bluetoothA2dp == null) {
            return null;
        }
        return bluetoothA2dp.getActiveDevice();
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothA2dp bluetoothA2dp = this.mService;
        if (bluetoothA2dp != null && bluetoothA2dp.getPriority(bluetoothDevice) > 0) {
            return true;
        }
        return false;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothA2dp bluetoothA2dp = this.mService;
        if (bluetoothA2dp != null) {
            if (!z) {
                bluetoothA2dp.setPriority(bluetoothDevice, 0);
            } else if (bluetoothA2dp.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        Log.d("A2dpProfile", "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(2, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("A2dpProfile", "Error cleaning up A2DP proxy", th);
            }
        }
    }
}
