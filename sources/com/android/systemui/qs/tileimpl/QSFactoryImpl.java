package com.android.systemui.qs.tileimpl;

import android.os.Build;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.android.systemui.C0015R$style;
import com.android.systemui.plugins.qs.QSFactory;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BatterySaverTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DataSaverTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.NfcTile;
import com.android.systemui.qs.tiles.NightDisplayTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.UiModeNightTile;
import com.android.systemui.qs.tiles.UserTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.qs.tiles.WorkModeTile;
import com.android.systemui.util.leak.GarbageMonitor;
import javax.inject.Provider;

public class QSFactoryImpl implements QSFactory {
    private final Provider<AirplaneModeTile> mAirplaneModeTileProvider;
    private final Provider<BatterySaverTile> mBatterySaverTileProvider;
    private final Provider<BluetoothTile> mBluetoothTileProvider;
    private final Provider<CastTile> mCastTileProvider;
    private final Provider<CellularTile> mCellularTileProvider;
    private final Provider<ColorInversionTile> mColorInversionTileProvider;
    private final Provider<DataSaverTile> mDataSaverTileProvider;
    private final Provider<DndTile> mDndTileProvider;
    private final Provider<FlashlightTile> mFlashlightTileProvider;
    private QSTileHost mHost;
    private final Provider<HotspotTile> mHotspotTileProvider;
    private final Provider<LocationTile> mLocationTileProvider;
    private final Provider<GarbageMonitor.MemoryTile> mMemoryTileProvider;
    private final Provider<NfcTile> mNfcTileProvider;
    private final Provider<NightDisplayTile> mNightDisplayTileProvider;
    private final Provider<RotationLockTile> mRotationLockTileProvider;
    private final Provider<UiModeNightTile> mUiModeNightTileProvider;
    private final Provider<UserTile> mUserTileProvider;
    private final Provider<WifiTile> mWifiTileProvider;
    private final Provider<WorkModeTile> mWorkModeTileProvider;

    public QSFactoryImpl(Provider<WifiTile> provider, Provider<BluetoothTile> provider2, Provider<CellularTile> provider3, Provider<DndTile> provider4, Provider<ColorInversionTile> provider5, Provider<AirplaneModeTile> provider6, Provider<WorkModeTile> provider7, Provider<RotationLockTile> provider8, Provider<FlashlightTile> provider9, Provider<LocationTile> provider10, Provider<CastTile> provider11, Provider<HotspotTile> provider12, Provider<UserTile> provider13, Provider<BatterySaverTile> provider14, Provider<DataSaverTile> provider15, Provider<NightDisplayTile> provider16, Provider<NfcTile> provider17, Provider<GarbageMonitor.MemoryTile> provider18, Provider<UiModeNightTile> provider19) {
        this.mWifiTileProvider = provider;
        this.mBluetoothTileProvider = provider2;
        this.mCellularTileProvider = provider3;
        this.mDndTileProvider = provider4;
        this.mColorInversionTileProvider = provider5;
        this.mAirplaneModeTileProvider = provider6;
        this.mWorkModeTileProvider = provider7;
        this.mRotationLockTileProvider = provider8;
        this.mFlashlightTileProvider = provider9;
        this.mLocationTileProvider = provider10;
        this.mCastTileProvider = provider11;
        this.mHotspotTileProvider = provider12;
        this.mUserTileProvider = provider13;
        this.mBatterySaverTileProvider = provider14;
        this.mDataSaverTileProvider = provider15;
        this.mNightDisplayTileProvider = provider16;
        this.mNfcTileProvider = provider17;
        this.mMemoryTileProvider = provider18;
        this.mUiModeNightTileProvider = provider19;
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    @Override // com.android.systemui.plugins.qs.QSFactory
    public QSTile createTile(String str) {
        QSTileImpl createTileInternal = createTileInternal(str);
        if (createTileInternal != null) {
            createTileInternal.handleStale();
        }
        return createTileInternal;
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    private QSTileImpl createTileInternal(String str) {
        char c;
        switch (str.hashCode()) {
            case -2016941037:
                if (str.equals("inversion")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -1183073498:
                if (str.equals("flashlight")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -677011630:
                if (str.equals("airplane")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -331239923:
                if (str.equals("battery")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -40300674:
                if (str.equals("rotation")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 3154:
                if (str.equals("bt")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 99610:
                if (str.equals("dnd")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 108971:
                if (str.equals("nfc")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case 3046207:
                if (str.equals("cast")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 3049826:
                if (str.equals("cell")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 3075958:
                if (str.equals("dark")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 3599307:
                if (str.equals("user")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 3649301:
                if (str.equals("wifi")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 3655441:
                if (str.equals("work")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 104817688:
                if (str.equals("night")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 109211285:
                if (str.equals("saver")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case 1099603663:
                if (str.equals("hotspot")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 1901043637:
                if (str.equals("location")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return this.mWifiTileProvider.get();
            case 1:
                return this.mBluetoothTileProvider.get();
            case 2:
                return this.mCellularTileProvider.get();
            case 3:
                return this.mDndTileProvider.get();
            case 4:
                return this.mColorInversionTileProvider.get();
            case 5:
                return this.mAirplaneModeTileProvider.get();
            case 6:
                return this.mWorkModeTileProvider.get();
            case 7:
                return this.mRotationLockTileProvider.get();
            case '\b':
                return this.mFlashlightTileProvider.get();
            case '\t':
                return this.mLocationTileProvider.get();
            case '\n':
                return this.mCastTileProvider.get();
            case 11:
                return this.mHotspotTileProvider.get();
            case '\f':
                return this.mUserTileProvider.get();
            case '\r':
                return this.mBatterySaverTileProvider.get();
            case 14:
                return this.mDataSaverTileProvider.get();
            case 15:
                return this.mNightDisplayTileProvider.get();
            case 16:
                return this.mNfcTileProvider.get();
            case 17:
                return this.mUiModeNightTileProvider.get();
            default:
                if (str.startsWith("intent(")) {
                    return IntentTile.create(this.mHost, str);
                }
                if (str.startsWith("custom(")) {
                    return CustomTile.create(this.mHost, str);
                }
                if (Build.IS_DEBUGGABLE && str.equals("dbg:mem")) {
                    return this.mMemoryTileProvider.get();
                }
                Log.w("QSFactory", "No stock tile spec: " + str);
                return null;
        }
    }

    @Override // com.android.systemui.plugins.qs.QSFactory
    public QSTileView createTileView(QSTile qSTile, boolean z) {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.mHost.getContext(), C0015R$style.qs_theme);
        QSIconView createTileView = qSTile.createTileView(contextThemeWrapper);
        if (z) {
            return new QSTileBaseView(contextThemeWrapper, createTileView, z);
        }
        return new QSTileView(contextThemeWrapper, createTileView);
    }
}
