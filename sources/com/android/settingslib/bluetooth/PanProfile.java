package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.HashMap;
import java.util.List;

public class PanProfile implements LocalBluetoothProfile {
    private final HashMap<BluetoothDevice, Integer> mDeviceRoleMap = new HashMap<>();
    private boolean mIsProfileReady;
    private BluetoothPan mService;

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean accessProfileEnabled() {
        return true;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getProfileId() {
        return 5;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isAutoConnectable() {
        return false;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return true;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
    }

    public String toString() {
        return "PAN";
    }

    private final class PanServiceListener implements BluetoothProfile.ServiceListener {
        private PanServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            PanProfile.this.mService = (BluetoothPan) bluetoothProfile;
            PanProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            PanProfile.this.mIsProfileReady = false;
        }
    }

    PanProfile(Context context) {
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new PanServiceListener(), 5);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothPan bluetoothPan = this.mService;
        if (bluetoothPan == null) {
            return false;
        }
        List<BluetoothDevice> connectedDevices = bluetoothPan.getConnectedDevices();
        if (connectedDevices != null) {
            for (BluetoothDevice bluetoothDevice2 : connectedDevices) {
                this.mService.disconnect(bluetoothDevice2);
            }
        }
        return this.mService.connect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothPan bluetoothPan = this.mService;
        if (bluetoothPan == null) {
            return false;
        }
        return bluetoothPan.disconnect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothPan bluetoothPan = this.mService;
        if (bluetoothPan == null) {
            return 0;
        }
        return bluetoothPan.getConnectionState(bluetoothDevice);
    }

    /* access modifiers changed from: package-private */
    public void setLocalRole(BluetoothDevice bluetoothDevice, int i) {
        this.mDeviceRoleMap.put(bluetoothDevice, Integer.valueOf(i));
    }

    /* access modifiers changed from: package-private */
    public boolean isLocalRoleNap(BluetoothDevice bluetoothDevice) {
        if (!this.mDeviceRoleMap.containsKey(bluetoothDevice)) {
            return false;
        }
        if (this.mDeviceRoleMap.get(bluetoothDevice).intValue() == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        Log.d("PanProfile", "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(5, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("PanProfile", "Error cleaning up PAN proxy", th);
            }
        }
    }
}
