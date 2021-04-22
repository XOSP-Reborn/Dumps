package com.android.systemui.qs;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.metrics.LogMaker;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.Dependency;
import com.android.systemui.DumpController;
import com.android.systemui.Dumpable;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSDetail;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.MaxBrightnessHeadsUpController;
import com.android.systemui.settings.MaxBrightnessHeadsUpImpl;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.settings.ToggleSliderView;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.tuner.TunerService;
import com.sonymobile.settingslib.logging.LoggingManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QSPanel extends LinearLayout implements TunerService.Tunable, QSHost.Callback, BrightnessMirrorController.BrightnessMirrorListener, Dumpable {
    private BrightnessController mBrightnessController;
    private BrightnessMirrorController mBrightnessMirrorController;
    protected final View mBrightnessView;
    private QSDetail.Callback mCallback;
    protected final Context mContext;
    private QSCustomizer mCustomizePanel;
    private Record mDetailRecord;
    private View mDivider;
    private DumpController mDumpController;
    protected boolean mExpanded;
    protected QSSecurityFooter mFooter;
    private PageIndicator mFooterPageIndicator;
    private boolean mGridContentVisible;
    private final H mHandler;
    protected QSTileHost mHost;
    protected boolean mListening;
    private MaxBrightnessHeadsUpController mMaxBrightnessHeadsUpController;
    private final MetricsLogger mMetricsLogger;
    private final QSTileRevealController mQsTileRevealController;
    protected final ArrayList<TileRecord> mRecords;
    protected QSTileLayout mTileLayout;

    public interface QSTileLayout {
        void addTile(TileRecord tileRecord);

        int getNumVisibleTiles();

        int getOffsetTop(TileRecord tileRecord);

        void removeTile(TileRecord tileRecord);

        default void restoreInstanceState(Bundle bundle) {
        }

        default void saveInstanceState(Bundle bundle) {
        }

        default void setExpansion(float f) {
        }

        void setListening(boolean z);

        boolean updateResources();
    }

    public static final class TileRecord extends Record {
        public QSTile.Callback callback;
        public boolean scanState;
        public QSTile tile;
        public QSTileView tileView;
    }

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, null);
    }

    public QSPanel(Context context, AttributeSet attributeSet, DumpController dumpController) {
        super(context, attributeSet);
        this.mRecords = new ArrayList<>();
        this.mHandler = new H();
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mGridContentVisible = true;
        this.mContext = context;
        setOrientation(1);
        this.mBrightnessView = LayoutInflater.from(this.mContext).inflate(C0010R$layout.quick_settings_brightness_dialog, (ViewGroup) this, false);
        addView(this.mBrightnessView);
        this.mTileLayout = (QSTileLayout) LayoutInflater.from(this.mContext).inflate(C0010R$layout.qs_paged_tile_layout, (ViewGroup) this, false);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout);
        this.mQsTileRevealController = new QSTileRevealController(this.mContext, this, (PagedTileLayout) this.mTileLayout);
        addDivider();
        this.mFooter = new QSSecurityFooter(this, context);
        addView(this.mFooter.getView());
        this.mBrightnessController = new BrightnessController(getContext(), (ToggleSlider) findViewById(C0007R$id.brightness_slider));
        this.mDumpController = dumpController;
        if (this.mContext.getResources().getBoolean(C0003R$bool.config_enable_max_brightness_heads_up)) {
            MaxBrightnessHeadsUpImpl maxBrightnessHeadsUpImpl = new MaxBrightnessHeadsUpImpl(this.mContext);
            this.mMaxBrightnessHeadsUpController = new MaxBrightnessHeadsUpController(this.mContext, maxBrightnessHeadsUpImpl);
            maxBrightnessHeadsUpImpl.setOnUserRespondedListener(this.mMaxBrightnessHeadsUpController);
            ((ToggleSlider) findViewById(C0007R$id.brightness_slider)).setOnSliderMaximizedListener(this.mMaxBrightnessHeadsUpController);
        }
        updateResources();
    }

    /* access modifiers changed from: protected */
    public void addDivider() {
        this.mDivider = LayoutInflater.from(this.mContext).inflate(C0010R$layout.qs_divider, (ViewGroup) this, false);
        View view = this.mDivider;
        view.setBackgroundColor(Utils.applyAlpha(view.getAlpha(), QSTileImpl.getColorForState(this.mContext, 2)));
        addView(this.mDivider);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int paddingBottom = getPaddingBottom() + getPaddingTop();
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                paddingBottom += childAt.getMeasuredHeight();
            }
        }
        setMeasuredDimension(getMeasuredWidth(), paddingBottom);
    }

    public View getDivider() {
        return this.mDivider;
    }

    public QSTileRevealController getQsTileRevealController() {
        return this.mQsTileRevealController;
    }

    public boolean isShowingCustomize() {
        QSCustomizer qSCustomizer = this.mCustomizePanel;
        return qSCustomizer != null && qSCustomizer.isCustomizing();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "qs_show_brightness");
        QSTileHost qSTileHost = this.mHost;
        if (qSTileHost != null) {
            setTiles(qSTileHost.getTiles());
        }
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.addCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
        DumpController dumpController = this.mDumpController;
        if (dumpController != null) {
            dumpController.addListener(this);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        QSTileHost qSTileHost = this.mHost;
        if (qSTileHost != null) {
            qSTileHost.removeCallback(this);
        }
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.removeCallbacks();
        }
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.removeCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
        DumpController dumpController = this.mDumpController;
        if (dumpController != null) {
            dumpController.removeListener(this);
        }
        super.onDetachedFromWindow();
    }

    @Override // com.android.systemui.qs.QSHost.Callback
    public void onTilesChanged() {
        setTiles(this.mHost.getTiles());
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        if ("qs_show_brightness".equals(str)) {
            updateViewVisibilityForTuningValue(this.mBrightnessView, str2);
        }
    }

    private void updateViewVisibilityForTuningValue(View view, String str) {
        view.setVisibility(TunerService.parseIntegerSwitch(str, true) ? 0 : 8);
    }

    public void openDetails(String str) {
        QSTile tile = getTile(str);
        if (tile != null) {
            showDetailAdapter(true, tile.getDetailAdapter(), new int[]{getWidth() / 2, 0});
        }
    }

    private QSTile getTile(String str) {
        for (int i = 0; i < this.mRecords.size(); i++) {
            if (str.equals(this.mRecords.get(i).tile.getTileSpec())) {
                return this.mRecords.get(i).tile;
            }
        }
        return this.mHost.createTile(str);
    }

    public void setBrightnessMirror(BrightnessMirrorController brightnessMirrorController) {
        BrightnessMirrorController brightnessMirrorController2 = this.mBrightnessMirrorController;
        if (brightnessMirrorController2 != null) {
            brightnessMirrorController2.removeCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
        this.mBrightnessMirrorController = brightnessMirrorController;
        BrightnessMirrorController brightnessMirrorController3 = this.mBrightnessMirrorController;
        if (brightnessMirrorController3 != null) {
            brightnessMirrorController3.addCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
        updateBrightnessMirror();
    }

    @Override // com.android.systemui.statusbar.policy.BrightnessMirrorController.BrightnessMirrorListener
    public void onBrightnessMirrorReinflated(View view) {
        updateBrightnessMirror();
    }

    /* access modifiers changed from: package-private */
    public View getBrightnessView() {
        return this.mBrightnessView;
    }

    public void setCallback(QSDetail.Callback callback) {
        this.mCallback = callback;
    }

    public void setHost(QSTileHost qSTileHost, QSCustomizer qSCustomizer) {
        this.mHost = qSTileHost;
        this.mHost.addCallback(this);
        setTiles(this.mHost.getTiles());
        this.mFooter.setHostEnvironment(qSTileHost);
        this.mCustomizePanel = qSCustomizer;
        QSCustomizer qSCustomizer2 = this.mCustomizePanel;
        if (qSCustomizer2 != null) {
            qSCustomizer2.setHost(this.mHost);
        }
    }

    public void setFooterPageIndicator(PageIndicator pageIndicator) {
        if (this.mTileLayout instanceof PagedTileLayout) {
            this.mFooterPageIndicator = pageIndicator;
            updatePageIndicator();
        }
    }

    private void updatePageIndicator() {
        PageIndicator pageIndicator;
        if ((this.mTileLayout instanceof PagedTileLayout) && (pageIndicator = this.mFooterPageIndicator) != null) {
            pageIndicator.setVisibility(8);
            ((PagedTileLayout) this.mTileLayout).setPageIndicator(this.mFooterPageIndicator);
        }
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public void updateResources() {
        Resources resources = this.mContext.getResources();
        setPadding(0, resources.getDimensionPixelSize(C0005R$dimen.qs_panel_padding_top), 0, resources.getDimensionPixelSize(C0005R$dimen.qs_panel_padding_bottom));
        updatePageIndicator();
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.clearState();
        }
        if (this.mListening) {
            refreshAllTiles();
        }
        QSTileLayout qSTileLayout = this.mTileLayout;
        if (qSTileLayout != null) {
            qSTileLayout.updateResources();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mFooter.onConfigurationChanged();
        updateResources();
        updateBrightnessMirror();
    }

    public void updateBrightnessMirror() {
        if (this.mBrightnessMirrorController != null) {
            ToggleSliderView toggleSliderView = (ToggleSliderView) findViewById(C0007R$id.brightness_slider);
            toggleSliderView.setMirror((ToggleSliderView) this.mBrightnessMirrorController.getMirror().findViewById(C0007R$id.brightness_slider));
            toggleSliderView.setMirrorController(this.mBrightnessMirrorController);
        }
    }

    public void setExpanded(boolean z) {
        if (this.mExpanded != z) {
            this.mExpanded = z;
            if (!this.mExpanded) {
                QSTileLayout qSTileLayout = this.mTileLayout;
                if (qSTileLayout instanceof PagedTileLayout) {
                    ((PagedTileLayout) qSTileLayout).setCurrentItem(0, false);
                }
            }
            this.mMetricsLogger.visibility(111, this.mExpanded);
            LoggingManager.logQSEvent(this.mContext, "panel", this.mExpanded ? "open" : "close", null);
            if (!this.mExpanded) {
                closeDetail();
            } else {
                logTiles();
            }
        }
    }

    public void setPageListener(PagedTileLayout.PageListener pageListener) {
        QSTileLayout qSTileLayout = this.mTileLayout;
        if (qSTileLayout instanceof PagedTileLayout) {
            ((PagedTileLayout) qSTileLayout).setPageListener(pageListener);
        }
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }

    public void setListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            QSTileLayout qSTileLayout = this.mTileLayout;
            if (qSTileLayout != null) {
                qSTileLayout.setListening(z);
            }
            if (this.mListening) {
                refreshAllTiles();
            }
        }
    }

    public void setListening(boolean z, boolean z2) {
        setListening(z && z2);
        getFooter().setListening(z);
        setBrightnessListening(z);
    }

    public void setBrightnessListening(boolean z) {
        if (z) {
            this.mBrightnessController.registerCallbacks();
        } else {
            this.mBrightnessController.unregisterCallbacks();
        }
    }

    public void refreshAllTiles() {
        this.mBrightnessController.checkRestrictionAndSetEnabled();
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.refreshState();
        }
        this.mFooter.refreshState();
    }

    public void showDetailAdapter(boolean z, DetailAdapter detailAdapter, int[] iArr) {
        int i = iArr[0];
        int i2 = iArr[1];
        ((View) getParent()).getLocationInWindow(iArr);
        Record record = new Record();
        record.detailAdapter = detailAdapter;
        record.x = i - iArr[0];
        record.y = i2 - iArr[1];
        iArr[0] = i;
        iArr[1] = i2;
        showDetail(z, record);
    }

    /* access modifiers changed from: protected */
    public void showDetail(boolean z, Record record) {
        this.mHandler.obtainMessage(1, z ? 1 : 0, 0, record).sendToTarget();
    }

    public void setTiles(Collection<QSTile> collection) {
        setTiles(collection, false);
    }

    public void setTiles(Collection<QSTile> collection, boolean z) {
        if (!z) {
            this.mQsTileRevealController.updateRevealedTiles(collection);
        }
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord next = it.next();
            this.mTileLayout.removeTile(next);
            next.tile.removeCallback(next.callback);
        }
        this.mRecords.clear();
        for (QSTile qSTile : collection) {
            addTile(qSTile, z);
        }
    }

    /* access modifiers changed from: protected */
    public void drawTile(TileRecord tileRecord, QSTile.State state) {
        tileRecord.tileView.onStateChanged(state);
    }

    /* access modifiers changed from: protected */
    public QSTileView createTileView(QSTile qSTile, boolean z) {
        return this.mHost.createTileView(qSTile, z);
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowDetail() {
        return this.mExpanded;
    }

    /* access modifiers changed from: protected */
    public TileRecord addTile(QSTile qSTile, boolean z) {
        final TileRecord tileRecord = new TileRecord();
        tileRecord.tile = qSTile;
        tileRecord.tileView = createTileView(qSTile, z);
        AnonymousClass1 r2 = new QSTile.Callback() {
            /* class com.android.systemui.qs.QSPanel.AnonymousClass1 */

            @Override // com.android.systemui.plugins.qs.QSTile.Callback
            public void onStateChanged(QSTile.State state) {
                QSPanel.this.drawTile(tileRecord, state);
            }

            @Override // com.android.systemui.plugins.qs.QSTile.Callback
            public void onShowDetail(boolean z) {
                if (QSPanel.this.shouldShowDetail()) {
                    QSPanel.this.showDetail(z, tileRecord);
                }
            }

            @Override // com.android.systemui.plugins.qs.QSTile.Callback
            public void onToggleStateChanged(boolean z) {
                if (QSPanel.this.mDetailRecord == tileRecord) {
                    QSPanel.this.fireToggleStateChanged(z);
                }
            }

            @Override // com.android.systemui.plugins.qs.QSTile.Callback
            public void onScanStateChanged(boolean z) {
                tileRecord.scanState = z;
                Record record = QSPanel.this.mDetailRecord;
                TileRecord tileRecord = tileRecord;
                if (record == tileRecord) {
                    QSPanel.this.fireScanStateChanged(tileRecord.scanState);
                }
            }

            @Override // com.android.systemui.plugins.qs.QSTile.Callback
            public void onAnnouncementRequested(CharSequence charSequence) {
                if (charSequence != null) {
                    QSPanel.this.mHandler.obtainMessage(3, charSequence).sendToTarget();
                }
            }
        };
        tileRecord.tile.addCallback(r2);
        tileRecord.callback = r2;
        tileRecord.tileView.init(tileRecord.tile);
        tileRecord.tile.refreshState();
        this.mRecords.add(tileRecord);
        QSTileLayout qSTileLayout = this.mTileLayout;
        if (qSTileLayout != null) {
            qSTileLayout.addTile(tileRecord);
        }
        return tileRecord;
    }

    public void showEdit(final View view) {
        view.post(new Runnable() {
            /* class com.android.systemui.qs.QSPanel.AnonymousClass2 */

            public void run() {
                if (QSPanel.this.mCustomizePanel != null && !QSPanel.this.mCustomizePanel.isCustomizing()) {
                    int[] locationOnScreen = view.getLocationOnScreen();
                    QSPanel.this.mCustomizePanel.show(locationOnScreen[0] + (view.getWidth() / 2), locationOnScreen[1] + (view.getHeight() / 2));
                }
            }
        });
    }

    public void closeDetail() {
        QSCustomizer qSCustomizer = this.mCustomizePanel;
        if (qSCustomizer == null || !qSCustomizer.isShown()) {
            showDetail(false, this.mDetailRecord);
        } else {
            this.mCustomizePanel.hide();
        }
    }

    /* access modifiers changed from: protected */
    public void handleShowDetail(Record record, boolean z) {
        int i;
        if (record instanceof TileRecord) {
            handleShowDetailTile((TileRecord) record, z);
            return;
        }
        int i2 = 0;
        if (record != null) {
            i2 = record.x;
            i = record.y;
        } else {
            i = 0;
        }
        handleShowDetailImpl(record, z, i2, i);
    }

    private void handleShowDetailTile(TileRecord tileRecord, boolean z) {
        if ((this.mDetailRecord != null) != z || this.mDetailRecord != tileRecord) {
            if (z) {
                tileRecord.detailAdapter = tileRecord.tile.getDetailAdapter();
                if (tileRecord.detailAdapter == null) {
                    return;
                }
            }
            tileRecord.tile.setDetailListening(z);
            handleShowDetailImpl(tileRecord, z, tileRecord.tileView.getLeft() + (tileRecord.tileView.getWidth() / 2), tileRecord.tileView.getDetailY() + this.mTileLayout.getOffsetTop(tileRecord) + getTop());
        }
    }

    private void handleShowDetailImpl(Record record, boolean z, int i, int i2) {
        DetailAdapter detailAdapter = null;
        setDetailRecord(z ? record : null);
        if (z) {
            detailAdapter = record.detailAdapter;
        }
        fireShowingDetail(detailAdapter, i, i2);
    }

    /* access modifiers changed from: protected */
    public void setDetailRecord(Record record) {
        if (record != this.mDetailRecord) {
            this.mDetailRecord = record;
            Record record2 = this.mDetailRecord;
            fireScanStateChanged((record2 instanceof TileRecord) && ((TileRecord) record2).scanState);
        }
    }

    /* access modifiers changed from: package-private */
    public void setGridContentVisibility(boolean z) {
        int i = z ? 0 : 4;
        setVisibility(i);
        if (this.mGridContentVisible != z) {
            this.mMetricsLogger.visibility(111, i);
        }
        this.mGridContentVisible = z;
    }

    private void logTiles() {
        for (int i = 0; i < this.mRecords.size(); i++) {
            QSTile qSTile = this.mRecords.get(i).tile;
            this.mMetricsLogger.write(qSTile.populate(new LogMaker(qSTile.getMetricsCategory()).setType(1)));
        }
    }

    private void fireShowingDetail(DetailAdapter detailAdapter, int i, int i2) {
        QSDetail.Callback callback = this.mCallback;
        if (callback != null) {
            callback.onShowingDetail(detailAdapter, i, i2);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void fireToggleStateChanged(boolean z) {
        QSDetail.Callback callback = this.mCallback;
        if (callback != null) {
            callback.onToggleStateChanged(z);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void fireScanStateChanged(boolean z) {
        QSDetail.Callback callback = this.mCallback;
        if (callback != null) {
            callback.onScanStateChanged(z);
        }
    }

    public void clickTile(ComponentName componentName) {
        String spec = CustomTile.toSpec(componentName);
        int size = this.mRecords.size();
        for (int i = 0; i < size; i++) {
            if (this.mRecords.get(i).tile.getTileSpec().equals(spec)) {
                this.mRecords.get(i).tile.click();
                return;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public QSTileLayout getTileLayout() {
        return this.mTileLayout;
    }

    /* access modifiers changed from: package-private */
    public QSTileView getTileView(QSTile qSTile) {
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord next = it.next();
            if (next.tile == qSTile) {
                return next.tileView;
            }
        }
        return null;
    }

    public QSSecurityFooter getFooter() {
        return this.mFooter;
    }

    public void showDeviceMonitoringDialog() {
        this.mFooter.showDeviceMonitoringDialog();
    }

    public void setMargins(int i) {
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt = getChildAt(i2);
            if (childAt != this.mTileLayout) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) childAt.getLayoutParams();
                layoutParams.leftMargin = i;
                layoutParams.rightMargin = i;
            }
        }
    }

    /* access modifiers changed from: private */
    public class H extends Handler {
        private H() {
        }

        public void handleMessage(Message message) {
            int i = message.what;
            boolean z = true;
            if (i == 1) {
                QSPanel qSPanel = QSPanel.this;
                Record record = (Record) message.obj;
                if (message.arg1 == 0) {
                    z = false;
                }
                qSPanel.handleShowDetail(record, z);
            } else if (i == 3) {
                QSPanel.this.announceForAccessibility((CharSequence) message.obj);
            }
        }
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(getClass().getSimpleName() + ":");
        printWriter.println("  Tile records:");
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord next = it.next();
            if (next.tile instanceof Dumpable) {
                printWriter.print("    ");
                ((Dumpable) next.tile).dump(fileDescriptor, printWriter, strArr);
                printWriter.print("    ");
                printWriter.println(next.tileView.toString());
            }
        }
    }

    /* access modifiers changed from: protected */
    public static class Record {
        DetailAdapter detailAdapter;
        int x;
        int y;

        protected Record() {
        }
    }
}
