package com.android.systemui.statusbar.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.statusbar.policy.MobileSignalController;
import com.google.android.collect.Lists;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import org.codeaurora.internal.Client;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.INetworkCallback;
import org.codeaurora.internal.NetworkCallbackBase;
import org.codeaurora.internal.NrConfigType;
import org.codeaurora.internal.NrIconType;
import org.codeaurora.internal.ServiceUtil;
import org.codeaurora.internal.SignalStrength;
import org.codeaurora.internal.Status;
import org.codeaurora.internal.Token;

public class FiveGServiceClient {
    private static final boolean DEBUG = true;
    private static FiveGServiceClient sInstance;
    private int mBindRetryTimes = 0;
    @VisibleForTesting
    protected INetworkCallback mCallback = new NetworkCallbackBase() {
        /* class com.android.systemui.statusbar.policy.FiveGServiceClient.AnonymousClass3 */

        public void onSignalStrength(int i, Token token, Status status, SignalStrength signalStrength) throws RemoteException {
            if (FiveGServiceClient.DEBUG) {
                Log.d("FiveGServiceClient", "onSignalStrength: slotId=" + i + " token=" + token + " status=" + status + " signalStrength=" + signalStrength);
            }
            if (status.get() == 1 && signalStrength != null) {
                FiveGServiceClient.this.updateLevel(signalStrength.getRsrp(), FiveGServiceClient.this.getCurrentServiceState(i));
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }

        public void on5gConfigInfo(int i, Token token, Status status, NrConfigType nrConfigType) throws RemoteException {
            Log.d("FiveGServiceClient", "on5gConfigInfo: slotId = " + i + " token = " + token + " status" + status + " NrConfigType = " + nrConfigType);
            if (status.get() == 1) {
                FiveGServiceClient.this.getCurrentServiceState(i).mNrConfigType = nrConfigType.get();
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }

        public void onNrIconType(int i, Token token, Status status, NrIconType nrIconType) throws RemoteException {
            Log.d("FiveGServiceClient", "onNrIconType: slotId = " + i + " token = " + token + " status" + status + " NrIconType = " + nrIconType);
            if (status.get() == 1) {
                FiveGServiceState currentServiceState = FiveGServiceClient.this.getCurrentServiceState(i);
                currentServiceState.mNrIconType = nrIconType.get();
                FiveGServiceClient.this.update5GIcon(currentServiceState, i);
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }
    };
    private Client mClient;
    private Context mContext;
    private final SparseArray<FiveGServiceState> mCurrentServiceStates = new SparseArray<>();
    private Handler mHandler = new Handler() {
        /* class com.android.systemui.statusbar.policy.FiveGServiceClient.AnonymousClass1 */

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1024:
                    FiveGServiceClient.this.binderService();
                    return;
                case 1025:
                    FiveGServiceClient.this.initFiveGServiceState();
                    return;
                case 1026:
                    FiveGServiceClient.this.notifyMonitorCallback();
                    return;
                default:
                    return;
            }
        }
    };
    private int mInitRetryTimes = 0;
    private final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mKeyguardUpdateMonitorCallbacks = Lists.newArrayList();
    private final SparseArray<FiveGServiceState> mLastServiceStates = new SparseArray<>();
    private IExtTelephony mNetworkService;
    private String mPackageName;
    private boolean mServiceConnected;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        /* class com.android.systemui.statusbar.policy.FiveGServiceClient.AnonymousClass2 */

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("FiveGServiceClient", "onServiceConnected:" + iBinder);
            try {
                FiveGServiceClient.this.mNetworkService = IExtTelephony.Stub.asInterface(iBinder);
                FiveGServiceClient.this.mClient = FiveGServiceClient.this.mNetworkService.registerCallback(FiveGServiceClient.this.mPackageName, FiveGServiceClient.this.mCallback);
                FiveGServiceClient.this.mServiceConnected = true;
                FiveGServiceClient.this.initFiveGServiceState();
                Log.d("FiveGServiceClient", "Client = " + FiveGServiceClient.this.mClient);
            } catch (Exception e) {
                Log.d("FiveGServiceClient", "onServiceConnected: Exception = " + e);
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("FiveGServiceClient", "onServiceDisconnected:" + componentName);
            cleanup();
        }

        public void onBindingDied(ComponentName componentName) {
            Log.d("FiveGServiceClient", "onBindingDied:" + componentName);
            cleanup();
            if (FiveGServiceClient.this.mBindRetryTimes < 4) {
                Log.d("FiveGServiceClient", "try to re-bind");
                FiveGServiceClient.this.mHandler.sendEmptyMessageDelayed(1024, (long) ((FiveGServiceClient.this.mBindRetryTimes * 2000) + 3000));
            }
        }

        private void cleanup() {
            Log.d("FiveGServiceClient", "cleanup");
            FiveGServiceClient.this.mServiceConnected = false;
            FiveGServiceClient.this.mNetworkService = null;
            FiveGServiceClient.this.mClient = null;
        }
    };
    @VisibleForTesting
    final SparseArray<IFiveGStateListener> mStatesListeners = new SparseArray<>();

    public interface IFiveGStateListener {
        void onStateChanged(FiveGServiceState fiveGServiceState);
    }

    static {
        Log.isLoggable("FiveGServiceClient", 3);
    }

    public static class FiveGServiceState {
        private MobileSignalController.MobileIconGroup mIconGroup = TelephonyIcons.UNKNOWN;
        private int mLevel = 0;
        private int mNrConfigType = 0;
        private int mNrIconType = -1;

        public boolean isNrIconTypeValid() {
            int i = this.mNrIconType;
            return (i == -1 || i == 0) ? false : true;
        }

        @VisibleForTesting
        public MobileSignalController.MobileIconGroup getIconGroup() {
            return this.mIconGroup;
        }

        @VisibleForTesting
        public int getSignalLevel() {
            return this.mLevel;
        }

        /* access modifiers changed from: package-private */
        @VisibleForTesting
        public int getNrConfigType() {
            return this.mNrConfigType;
        }

        /* access modifiers changed from: package-private */
        @VisibleForTesting
        public int getNrIconType() {
            return this.mNrIconType;
        }

        public void copyFrom(FiveGServiceState fiveGServiceState) {
            this.mLevel = fiveGServiceState.mLevel;
            this.mNrConfigType = fiveGServiceState.mNrConfigType;
            this.mIconGroup = fiveGServiceState.mIconGroup;
            this.mNrIconType = fiveGServiceState.mNrIconType;
        }

        public boolean equals(FiveGServiceState fiveGServiceState) {
            return this.mLevel == fiveGServiceState.mLevel && this.mNrConfigType == fiveGServiceState.mNrConfigType && this.mIconGroup == fiveGServiceState.mIconGroup && this.mNrIconType == fiveGServiceState.mNrIconType;
        }

        public String toString() {
            return "mLevel=" + this.mLevel + ", " + "mNrConfigType=" + this.mNrConfigType + ", " + "mNrIconType=" + this.mNrIconType + ", " + "mIconGroup=" + this.mIconGroup;
        }
    }

    public FiveGServiceClient(Context context) {
        this.mContext = context;
        this.mPackageName = this.mContext.getPackageName();
    }

    public static FiveGServiceClient getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FiveGServiceClient(context);
        }
        return sInstance;
    }

    public void registerCallback(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        this.mKeyguardUpdateMonitorCallbacks.add(new WeakReference<>(keyguardUpdateMonitorCallback));
    }

    public void registerListener(int i, IFiveGStateListener iFiveGStateListener) {
        Log.d("FiveGServiceClient", "registerListener phoneId=" + i);
        this.mStatesListeners.put(i, iFiveGStateListener);
        if (!isServiceConnected()) {
            binderService();
        } else {
            initFiveGServiceState(i);
        }
    }

    public void unregisterListener(int i) {
        Log.d("FiveGServiceClient", "unregisterListener phoneId=" + i);
        this.mStatesListeners.remove(i);
        this.mCurrentServiceStates.remove(i);
        this.mLastServiceStates.remove(i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void binderService() {
        boolean bindService = ServiceUtil.bindService(this.mContext, this.mServiceConnection);
        Log.d("FiveGServiceClient", " bind service " + bindService);
        if (!bindService && this.mBindRetryTimes < 4 && !this.mHandler.hasMessages(1024)) {
            this.mHandler.sendEmptyMessageDelayed(1024, (long) ((this.mBindRetryTimes * 2000) + 3000));
            this.mBindRetryTimes++;
        }
    }

    public boolean isServiceConnected() {
        return this.mServiceConnected;
    }

    @VisibleForTesting
    public FiveGServiceState getCurrentServiceState(int i) {
        return getServiceState(i, this.mCurrentServiceStates);
    }

    private FiveGServiceState getLastServiceState(int i) {
        return getServiceState(i, this.mLastServiceStates);
    }

    private static FiveGServiceState getServiceState(int i, SparseArray<FiveGServiceState> sparseArray) {
        FiveGServiceState fiveGServiceState = sparseArray.get(i);
        if (fiveGServiceState != null) {
            return fiveGServiceState;
        }
        FiveGServiceState fiveGServiceState2 = new FiveGServiceState();
        sparseArray.put(i, fiveGServiceState2);
        return fiveGServiceState2;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyListenersIfNecessary(int i) {
        FiveGServiceState currentServiceState = getCurrentServiceState(i);
        FiveGServiceState lastServiceState = getLastServiceState(i);
        if (!currentServiceState.equals(lastServiceState)) {
            if (DEBUG) {
                Log.d("FiveGServiceClient", "phoneId(" + i + ") Change in state from " + lastServiceState + " \n\tto " + currentServiceState);
            }
            lastServiceState.copyFrom(currentServiceState);
            IFiveGStateListener iFiveGStateListener = this.mStatesListeners.get(i);
            if (iFiveGStateListener != null) {
                iFiveGStateListener.onStateChanged(currentServiceState);
            }
            this.mHandler.sendEmptyMessage(1026);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void initFiveGServiceState() {
        Log.d("FiveGServiceClient", "initFiveGServiceState size=" + this.mStatesListeners.size());
        for (int i = 0; i < this.mStatesListeners.size(); i++) {
            initFiveGServiceState(this.mStatesListeners.keyAt(i));
        }
    }

    private void initFiveGServiceState(int i) {
        Log.d("FiveGServiceClient", "mNetworkService=" + this.mNetworkService + " mClient=" + this.mClient);
        if (this.mNetworkService != null && this.mClient != null) {
            Log.d("FiveGServiceClient", "query 5G service state for phoneId " + i);
            try {
                queryNrSignalStrength(i);
                Log.d("FiveGServiceClient", "query5gConfigInfo result:" + this.mNetworkService.query5gConfigInfo(i, this.mClient));
                Log.d("FiveGServiceClient", "queryNrIconType result:" + this.mNetworkService.queryNrIconType(i, this.mClient));
            } catch (DeadObjectException e) {
                Log.e("FiveGServiceClient", "initFiveGServiceState: Exception = " + e);
                Log.d("FiveGServiceClient", "try to re-binder service");
                this.mInitRetryTimes = 0;
                this.mServiceConnected = false;
                this.mNetworkService = null;
                this.mClient = null;
                binderService();
            } catch (Exception e2) {
                Log.d("FiveGServiceClient", "initFiveGServiceState: Exception = " + e2);
                if (this.mInitRetryTimes < 4 && !this.mHandler.hasMessages(1025)) {
                    this.mHandler.sendEmptyMessageDelayed(1025, (long) ((this.mInitRetryTimes * 2000) + 3000));
                    this.mInitRetryTimes++;
                }
            }
        }
    }

    public void queryNrSignalStrength(int i) {
        if (this.mNetworkService == null || this.mClient == null) {
            Log.e("FiveGServiceClient", "query queryNrSignalStrength for phoneId " + i);
            return;
        }
        Log.d("FiveGServiceClient", "query queryNrSignalStrength for phoneId " + i);
        try {
            Token queryNrSignalStrength = this.mNetworkService.queryNrSignalStrength(i, this.mClient);
            Log.d("FiveGServiceClient", "queryNrSignalStrength result:" + queryNrSignalStrength);
        } catch (Exception e) {
            Log.e("FiveGServiceClient", "queryNrSignalStrength", e);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void update5GIcon(FiveGServiceState fiveGServiceState, int i) {
        fiveGServiceState.mIconGroup = getNrIconGroup(fiveGServiceState.mNrIconType, i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateLevel(int i, FiveGServiceState fiveGServiceState) {
        if (i == Integer.MAX_VALUE) {
            fiveGServiceState.mLevel = 0;
        } else if (i >= -95) {
            fiveGServiceState.mLevel = 4;
        } else if (i >= -105) {
            fiveGServiceState.mLevel = 3;
        } else if (i >= -115) {
            fiveGServiceState.mLevel = 2;
        } else {
            fiveGServiceState.mLevel = 1;
        }
    }

    private MobileSignalController.MobileIconGroup getNrIconGroup(int i, int i2) {
        MobileSignalController.MobileIconGroup mobileIconGroup = TelephonyIcons.UNKNOWN;
        if (i == 1) {
            return TelephonyIcons.FIVE_G_BASIC;
        }
        if (i != 2) {
            return mobileIconGroup;
        }
        return TelephonyIcons.FIVE_G_UWB;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyMonitorCallback() {
        for (int i = 0; i < this.mKeyguardUpdateMonitorCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mKeyguardUpdateMonitorCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
            }
        }
    }
}
