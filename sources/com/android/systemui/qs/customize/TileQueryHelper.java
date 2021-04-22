package com.android.systemui.qs.customize;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.widget.Button;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TileQueryHelper {
    private final Handler mBgHandler;
    private final Context mContext;
    private boolean mFinished;
    private final TileStateListener mListener;
    private final Handler mMainHandler;
    private final ArraySet<String> mSpecs = new ArraySet<>();
    private final ArrayList<TileInfo> mTiles = new ArrayList<>();

    public static class TileInfo {
        public boolean isSystem;
        public String spec;
        public QSTile.State state;
    }

    public interface TileStateListener {
        void onTilesChanged(List<TileInfo> list);
    }

    public TileQueryHelper(Context context, TileStateListener tileStateListener) {
        this.mContext = context;
        this.mListener = tileStateListener;
        this.mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mMainHandler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
    }

    public void queryTiles(QSTileHost qSTileHost) {
        this.mTiles.clear();
        this.mSpecs.clear();
        this.mFinished = false;
        addCurrentAndStockTiles(qSTileHost);
        addPackageTiles(qSTileHost);
    }

    public boolean isFinished() {
        return this.mFinished;
    }

    private void addCurrentAndStockTiles(QSTileHost qSTileHost) {
        String string = this.mContext.getString(C0014R$string.quick_settings_tiles_stock);
        String string2 = Settings.Secure.getString(this.mContext.getContentResolver(), "sysui_qs_tiles");
        ArrayList arrayList = new ArrayList();
        if (string2 != null) {
            arrayList.addAll(Arrays.asList(string2.split(",")));
        } else {
            string2 = "";
        }
        String[] split = string.split(",");
        for (String str : split) {
            if (!string2.contains(str)) {
                arrayList.add(str);
            }
        }
        if (Build.IS_DEBUGGABLE && !string2.contains("dbg:mem")) {
            arrayList.add("dbg:mem");
        }
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            String str2 = (String) it.next();
            QSTile createTile = qSTileHost.createTile(str2);
            if (createTile != null) {
                if (!createTile.isAvailable()) {
                    createTile.destroy();
                } else {
                    createTile.setListening(this, true);
                    createTile.refreshState();
                    createTile.setListening(this, false);
                    createTile.setTileSpec(str2);
                    arrayList2.add(createTile);
                }
            }
        }
        this.mBgHandler.post(new Runnable(arrayList2) {
            /* class com.android.systemui.qs.customize.$$Lambda$TileQueryHelper$sMzDfkcNEMwHLLe95kLdEn4WPkc */
            private final /* synthetic */ ArrayList f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                TileQueryHelper.this.lambda$addCurrentAndStockTiles$0$TileQueryHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$addCurrentAndStockTiles$0$TileQueryHelper(ArrayList arrayList) {
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            QSTile qSTile = (QSTile) it.next();
            QSTile.State copy = qSTile.getState().copy();
            copy.label = qSTile.getTileLabel();
            qSTile.destroy();
            addTile(qSTile.getTileSpec(), null, copy, true);
        }
        notifyTilesChanged(false);
    }

    private void addPackageTiles(QSTileHost qSTileHost) {
        this.mBgHandler.post(new Runnable(qSTileHost) {
            /* class com.android.systemui.qs.customize.$$Lambda$TileQueryHelper$7aqDrq4N73idi9gI_WE72bklw */
            private final /* synthetic */ QSTileHost f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                TileQueryHelper.this.lambda$addPackageTiles$1$TileQueryHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$addPackageTiles$1$TileQueryHelper(QSTileHost qSTileHost) {
        Collection<QSTile> tiles = qSTileHost.getTiles();
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ResolveInfo> queryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(new Intent("android.service.quicksettings.action.QS_TILE"), 0, ActivityManager.getCurrentUser());
        String string = this.mContext.getString(C0014R$string.quick_settings_tiles_stock);
        for (ResolveInfo resolveInfo : queryIntentServicesAsUser) {
            ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
            if (!string.contains(componentName.flattenToString())) {
                CharSequence loadLabel = resolveInfo.serviceInfo.applicationInfo.loadLabel(packageManager);
                String spec = CustomTile.toSpec(componentName);
                QSTile.State state = getState(tiles, spec);
                if (state != null) {
                    addTile(spec, loadLabel, state, false);
                } else if (resolveInfo.serviceInfo.icon != 0 || resolveInfo.serviceInfo.applicationInfo.icon != 0) {
                    Drawable loadIcon = resolveInfo.serviceInfo.loadIcon(packageManager);
                    if ("android.permission.BIND_QUICK_SETTINGS_TILE".equals(resolveInfo.serviceInfo.permission) && loadIcon != null) {
                        loadIcon.mutate();
                        loadIcon.setTint(this.mContext.getColor(17170443));
                        CharSequence loadLabel2 = resolveInfo.serviceInfo.loadLabel(packageManager);
                        createStateAndAddTile(spec, loadIcon, loadLabel2 != null ? loadLabel2.toString() : "null", loadLabel);
                    }
                }
            }
        }
        notifyTilesChanged(true);
    }

    private void notifyTilesChanged(boolean z) {
        this.mMainHandler.post(new Runnable(new ArrayList(this.mTiles), z) {
            /* class com.android.systemui.qs.customize.$$Lambda$TileQueryHelper$td1yVFso44MefBPUi6jpDHx3Yoc */
            private final /* synthetic */ ArrayList f$1;
            private final /* synthetic */ boolean f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                TileQueryHelper.this.lambda$notifyTilesChanged$2$TileQueryHelper(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$notifyTilesChanged$2$TileQueryHelper(ArrayList arrayList, boolean z) {
        this.mListener.onTilesChanged(arrayList);
        this.mFinished = z;
    }

    private QSTile.State getState(Collection<QSTile> collection, String str) {
        for (QSTile qSTile : collection) {
            if (str.equals(qSTile.getTileSpec())) {
                return qSTile.getState().copy();
            }
        }
        return null;
    }

    private void addTile(String str, CharSequence charSequence, QSTile.State state, boolean z) {
        if (!this.mSpecs.contains(str)) {
            TileInfo tileInfo = new TileInfo();
            tileInfo.state = state;
            QSTile.State state2 = tileInfo.state;
            state2.dualTarget = false;
            state2.expandedAccessibilityClassName = Button.class.getName();
            tileInfo.spec = str;
            QSTile.State state3 = tileInfo.state;
            if (z || TextUtils.equals(state.label, charSequence)) {
                charSequence = null;
            }
            state3.secondaryLabel = charSequence;
            tileInfo.isSystem = z;
            this.mTiles.add(tileInfo);
            this.mSpecs.add(str);
        }
    }

    private void createStateAndAddTile(String str, Drawable drawable, CharSequence charSequence, CharSequence charSequence2) {
        QSTile.State state = new QSTile.State();
        state.state = 1;
        state.label = charSequence;
        state.contentDescription = charSequence;
        state.icon = new QSTileImpl.DrawableIcon(drawable);
        addTile(str, charSequence2, state, false);
    }
}
