package com.android.systemui.qs.tileimpl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.Prefs;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTile.State;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSHost;
import com.sonymobile.settingslib.logging.LoggingManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class QSTileImpl<TState extends QSTile.State> implements QSTile, LifecycleOwner, Dumpable {
    protected static final Object ARG_SHOW_TRANSIENT_ENABLING = new Object();
    protected static final boolean DEBUG = Log.isLoggable("Tile", 3);
    protected final String TAG = ("Tile." + getClass().getSimpleName());
    private boolean mAnnounceNextStateChange;
    private final ArrayList<QSTile.Callback> mCallbacks = new ArrayList<>();
    protected final Context mContext;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    protected QSTileImpl<TState>.H mHandler = new H((Looper) Dependency.get(Dependency.BG_LOOPER));
    protected final QSHost mHost;
    private int mIsFullQs;
    private final LifecycleRegistry mLifecycle = new LifecycleRegistry(this);
    private final ArraySet<Object> mListeners = new ArraySet<>();
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private boolean mShowingDetail;
    private final Object mStaleListener = new Object();
    protected TState mState = newTileState();
    private final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    private String mTileSpec;
    private TState mTmpState = newTileState();
    protected final Handler mUiHandler = new Handler(Looper.getMainLooper());

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        return null;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public DetailAdapter getDetailAdapter() {
        return null;
    }

    public abstract Intent getLongClickIntent();

    @Override // com.android.systemui.plugins.qs.QSTile
    public abstract int getMetricsCategory();

    /* access modifiers changed from: protected */
    public long getStaleTimeout() {
        return 600000;
    }

    /* access modifiers changed from: protected */
    public abstract void handleClick();

    /* access modifiers changed from: protected */
    public abstract void handleSetListening(boolean z);

    /* access modifiers changed from: protected */
    public abstract void handleUpdateState(TState tstate, Object obj);

    @Override // com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return true;
    }

    public abstract TState newTileState();

    @Override // com.android.systemui.plugins.qs.QSTile
    public void setDetailListening(boolean z) {
    }

    /* access modifiers changed from: protected */
    public boolean shouldAnnouncementBeDelayed() {
        return false;
    }

    protected QSTileImpl(QSHost qSHost) {
        this.mHost = qSHost;
        this.mContext = qSHost.getContext();
    }

    @Override // androidx.lifecycle.LifecycleOwner
    public Lifecycle getLifecycle() {
        return this.mLifecycle;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void setListening(Object obj, boolean z) {
        this.mHandler.obtainMessage(13, z ? 1 : 0, 0, obj).sendToTarget();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void handleStale() {
        setListening(this.mStaleListener, true);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public String getTileSpec() {
        return this.mTileSpec;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void setTileSpec(String str) {
        this.mTileSpec = str;
    }

    public QSHost getHost() {
        return this.mHost;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public QSIconView createTileView(Context context) {
        return new QSIconViewImpl(context);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void addCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(1, callback).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void removeCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(12, callback).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void removeCallbacks() {
        this.mHandler.sendEmptyMessage(11);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void click() {
        this.mMetricsLogger.write(populate(new LogMaker(925).setType(4).addTaggedData(1592, Integer.valueOf(this.mStatusBarStateController.getState()))));
        this.mHandler.sendEmptyMessage(2);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void secondaryClick() {
        this.mMetricsLogger.write(populate(new LogMaker(926).setType(4).addTaggedData(1592, Integer.valueOf(this.mStatusBarStateController.getState()))));
        this.mHandler.sendEmptyMessage(3);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void longClick() {
        this.mMetricsLogger.write(populate(new LogMaker(366).setType(4).addTaggedData(1592, Integer.valueOf(this.mStatusBarStateController.getState()))));
        this.mHandler.sendEmptyMessage(4);
        Prefs.putInt(this.mContext, "QsLongPressTooltipShownCount", 2);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public LogMaker populate(LogMaker logMaker) {
        TState tstate = this.mState;
        if (tstate instanceof QSTile.BooleanState) {
            logMaker.addTaggedData(928, Integer.valueOf(((QSTile.BooleanState) tstate).value ? 1 : 0));
        }
        return logMaker.setSubtype(getMetricsCategory()).addTaggedData(1593, Integer.valueOf(this.mIsFullQs)).addTaggedData(927, Integer.valueOf(this.mHost.indexOf(this.mTileSpec)));
    }

    public void showDetail(boolean z) {
        this.mHandler.obtainMessage(6, z ? 1 : 0, 0).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void refreshState() {
        refreshState(null);
    }

    /* access modifiers changed from: protected */
    public final void refreshState(Object obj) {
        this.mHandler.obtainMessage(5, obj).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void userSwitch(int i) {
        this.mHandler.obtainMessage(7, i, 0).sendToTarget();
    }

    public void fireToggleStateChanged(boolean z) {
        this.mHandler.obtainMessage(8, z ? 1 : 0, 0).sendToTarget();
    }

    public void fireScanStateChanged(boolean z) {
        this.mHandler.obtainMessage(9, z ? 1 : 0, 0).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void destroy() {
        this.mHandler.sendEmptyMessage(10);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public TState getState() {
        return this.mState;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleAddCallback(QSTile.Callback callback) {
        this.mCallbacks.add(callback);
        callback.onStateChanged(this.mState);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleRemoveCallback(QSTile.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleRemoveCallbacks() {
        this.mCallbacks.clear();
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        handleClick();
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        LoggingManager.logQSEvent(this.mContext, getTileSpec(), "long_click", null);
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(getLongClickIntent(), 0);
    }

    /* access modifiers changed from: protected */
    public void handleRefreshState(Object obj) {
        handleUpdateState(this.mTmpState, obj);
        if (this.mTmpState.copyTo(this.mState)) {
            handleStateChanged();
        }
        this.mHandler.removeMessages(14);
        this.mHandler.sendEmptyMessageDelayed(14, getStaleTimeout());
        setListening(this.mStaleListener, false);
    }

    private void handleStateChanged() {
        String composeChangeAnnouncement;
        boolean shouldAnnouncementBeDelayed = shouldAnnouncementBeDelayed();
        boolean z = false;
        if (this.mCallbacks.size() != 0) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                this.mCallbacks.get(i).onStateChanged(this.mState);
            }
            if (this.mAnnounceNextStateChange && !shouldAnnouncementBeDelayed && (composeChangeAnnouncement = composeChangeAnnouncement()) != null) {
                this.mCallbacks.get(0).onAnnouncementRequested(composeChangeAnnouncement);
            }
        }
        if (this.mAnnounceNextStateChange && shouldAnnouncementBeDelayed) {
            z = true;
        }
        this.mAnnounceNextStateChange = z;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleShowDetail(boolean z) {
        this.mShowingDetail = z;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onShowDetail(z);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isShowingDetail() {
        return this.mShowingDetail;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleToggleStateChanged(boolean z) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onToggleStateChanged(z);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleScanStateChanged(boolean z) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onScanStateChanged(z);
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int i) {
        handleRefreshState(null);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleSetListeningInternal(Object obj, boolean z) {
        if (z) {
            if (this.mListeners.add(obj) && this.mListeners.size() == 1) {
                if (DEBUG) {
                    Log.d(this.TAG, "handleSetListening true");
                }
                this.mLifecycle.markState(Lifecycle.State.RESUMED);
                handleSetListening(z);
                refreshState();
            }
        } else if (this.mListeners.remove(obj) && this.mListeners.size() == 0) {
            if (DEBUG) {
                Log.d(this.TAG, "handleSetListening false");
            }
            this.mLifecycle.markState(Lifecycle.State.DESTROYED);
            handleSetListening(z);
        }
        updateIsFullQs();
    }

    private void updateIsFullQs() {
        Iterator<Object> it = this.mListeners.iterator();
        while (it.hasNext()) {
            if (PagedTileLayout.TilePage.class.equals(it.next().getClass())) {
                this.mIsFullQs = 1;
                return;
            }
        }
        this.mIsFullQs = 0;
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        if (this.mListeners.size() != 0) {
            handleSetListening(false);
        }
        this.mCallbacks.clear();
    }

    /* access modifiers changed from: protected */
    public void checkIfRestrictionEnforcedByAdminOnly(QSTile.State state, String str) {
        RestrictedLockUtils.EnforcedAdmin checkIfRestrictionEnforced = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(this.mContext, str, ActivityManager.getCurrentUser());
        if (checkIfRestrictionEnforced == null || RestrictedLockUtilsInternal.hasBaseUserRestriction(this.mContext, str, ActivityManager.getCurrentUser())) {
            state.disabledByPolicy = false;
            this.mEnforcedAdmin = null;
            return;
        }
        state.disabledByPolicy = true;
        this.mEnforcedAdmin = checkIfRestrictionEnforced;
    }

    public static int getColorForState(Context context, int i) {
        if (i == 0) {
            return Utils.getDisabled(context, Utils.getColorAttrDefaultColor(context, 16842808));
        }
        if (i == 1) {
            return Utils.getColorAttrDefaultColor(context, 16842808);
        }
        if (i == 2) {
            return Utils.getColorAttrDefaultColor(context, 16843827);
        }
        Log.e("QSTile", "Invalid state " + i);
        return 0;
    }

    /* access modifiers changed from: protected */
    public final class H extends Handler {
        @VisibleForTesting
        protected H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            try {
                boolean z = true;
                if (message.what == 1) {
                    QSTileImpl.this.handleAddCallback((QSTile.Callback) message.obj);
                } else if (message.what == 11) {
                    QSTileImpl.this.handleRemoveCallbacks();
                } else if (message.what == 12) {
                    QSTileImpl.this.handleRemoveCallback((QSTile.Callback) message.obj);
                } else if (message.what == 2) {
                    if (QSTileImpl.this.mState.disabledByPolicy) {
                        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(RestrictedLockUtils.getShowAdminSupportDetailsIntent(QSTileImpl.this.mContext, QSTileImpl.this.mEnforcedAdmin), 0);
                        return;
                    }
                    QSTileImpl.this.handleClick();
                } else if (message.what == 3) {
                    QSTileImpl.this.handleSecondaryClick();
                } else if (message.what == 4) {
                    QSTileImpl.this.handleLongClick();
                } else if (message.what == 5) {
                    QSTileImpl.this.handleRefreshState(message.obj);
                } else if (message.what == 6) {
                    QSTileImpl qSTileImpl = QSTileImpl.this;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl.handleShowDetail(z);
                } else if (message.what == 7) {
                    QSTileImpl.this.handleUserSwitch(message.arg1);
                } else if (message.what == 8) {
                    QSTileImpl qSTileImpl2 = QSTileImpl.this;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl2.handleToggleStateChanged(z);
                } else if (message.what == 9) {
                    QSTileImpl qSTileImpl3 = QSTileImpl.this;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl3.handleScanStateChanged(z);
                } else if (message.what == 10) {
                    QSTileImpl.this.handleDestroy();
                } else if (message.what == 13) {
                    QSTileImpl qSTileImpl4 = QSTileImpl.this;
                    Object obj = message.obj;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl4.handleSetListeningInternal(obj, z);
                } else if (message.what == 14) {
                    QSTileImpl.this.handleStale();
                } else {
                    throw new IllegalArgumentException("Unknown msg: " + message.what);
                }
            } catch (Throwable th) {
                String str = "Error in " + ((String) null);
                Log.w(QSTileImpl.this.TAG, str, th);
                QSTileImpl.this.mHost.warn(str, th);
            }
        }
    }

    public static class DrawableIcon extends QSTile.Icon {
        protected final Drawable mDrawable;
        protected final Drawable mInvisibleDrawable;

        public DrawableIcon(Drawable drawable) {
            this.mDrawable = drawable;
            this.mInvisibleDrawable = drawable.getConstantState().newDrawable();
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            return this.mDrawable;
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getInvisibleDrawable(Context context) {
            return this.mInvisibleDrawable;
        }
    }

    public static class ResourceIcon extends QSTile.Icon {
        private static final SparseArray<QSTile.Icon> ICONS = new SparseArray<>();
        protected final int mResId;

        private ResourceIcon(int i) {
            this.mResId = i;
        }

        public static synchronized QSTile.Icon get(int i) {
            QSTile.Icon icon;
            synchronized (ResourceIcon.class) {
                icon = ICONS.get(i);
                if (icon == null) {
                    icon = new ResourceIcon(i);
                    ICONS.put(i, icon);
                }
            }
            return icon;
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getInvisibleDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        public boolean equals(Object obj) {
            return (obj instanceof ResourceIcon) && ((ResourceIcon) obj).mResId == this.mResId;
        }

        public String toString() {
            return String.format("ResourceIcon[resId=0x%08x]", Integer.valueOf(this.mResId));
        }
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(getClass().getSimpleName() + ":");
        printWriter.print("    ");
        printWriter.println(getState().toString());
    }
}
