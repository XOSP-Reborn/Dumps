package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.Dependency;
import com.android.systemui.DumpController;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QuickQSPanel extends QSPanel {
    private static int mDefaultMaxTiles;
    private boolean mDisabledByPolicy;
    protected QSPanel mFullPanel;
    private int mMaxTiles;
    private final TunerService.Tunable mNumTiles = new TunerService.Tunable() {
        /* class com.android.systemui.qs.QuickQSPanel.AnonymousClass1 */

        @Override // com.android.systemui.tuner.TunerService.Tunable
        public void onTuningChanged(String str, String str2) {
            QuickQSPanel quickQSPanel = QuickQSPanel.this;
            quickQSPanel.setMaxTiles(QuickQSPanel.getNumQuickTiles(quickQSPanel.mContext));
        }
    };

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.QSPanel
    public void addDivider() {
    }

    public void setPadding(int i, int i2, int i3, int i4) {
    }

    public QuickQSPanel(Context context, AttributeSet attributeSet, DumpController dumpController) {
        super(context, attributeSet, dumpController);
        QSSecurityFooter qSSecurityFooter = this.mFooter;
        if (qSSecurityFooter != null) {
            removeView(qSSecurityFooter.getView());
        }
        if (this.mTileLayout != null) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                this.mTileLayout.removeTile(this.mRecords.get(i));
            }
            removeView((View) this.mTileLayout);
        }
        mDefaultMaxTiles = getResources().getInteger(C0008R$integer.quick_qs_panel_max_columns);
        this.mTileLayout = new HeaderTileLayout(context);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout, 0);
        super.setPadding(0, 0, 0, 0);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.QSPanel
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this.mNumTiles, "sysui_qqs_count");
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.QSPanel
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this.mNumTiles);
    }

    public void setQSPanelAndHeader(QSPanel qSPanel, View view) {
        this.mFullPanel = qSPanel;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.QSPanel
    public boolean shouldShowDetail() {
        return !this.mExpanded;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.qs.QSPanel
    public void drawTile(QSPanel.TileRecord tileRecord, QSTile.State state) {
        if (state instanceof QSTile.SignalState) {
            QSTile.SignalState signalState = new QSTile.SignalState();
            state.copyTo(signalState);
            signalState.activityIn = false;
            signalState.activityOut = false;
            state = signalState;
        }
        super.drawTile(tileRecord, state);
    }

    @Override // com.android.systemui.qs.QSPanel
    public void setHost(QSTileHost qSTileHost, QSCustomizer qSCustomizer) {
        super.setHost(qSTileHost, qSCustomizer);
        setTiles(this.mHost.getTiles());
    }

    public void setMaxTiles(int i) {
        this.mMaxTiles = i;
        QSTileHost qSTileHost = this.mHost;
        if (qSTileHost != null) {
            setTiles(qSTileHost.getTiles());
        }
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable, com.android.systemui.qs.QSPanel
    public void onTuningChanged(String str, String str2) {
        if ("qs_show_brightness".equals(str)) {
            super.onTuningChanged(str, "0");
        }
    }

    @Override // com.android.systemui.qs.QSPanel
    public void setTiles(Collection<QSTile> collection) {
        ArrayList arrayList = new ArrayList();
        for (QSTile qSTile : collection) {
            arrayList.add(qSTile);
            if (arrayList.size() == this.mMaxTiles) {
                break;
            }
        }
        super.setTiles(arrayList, true);
    }

    public static int getNumQuickTiles(Context context) {
        return ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qqs_count", mDefaultMaxTiles);
    }

    /* access modifiers changed from: package-private */
    public void setDisabledByPolicy(boolean z) {
        if (z != this.mDisabledByPolicy) {
            this.mDisabledByPolicy = z;
            setVisibility(z ? 8 : 0);
        }
    }

    public void setVisibility(int i) {
        if (this.mDisabledByPolicy) {
            if (getVisibility() != 8) {
                i = 8;
            } else {
                return;
            }
        }
        super.setVisibility(i);
    }

    private static class HeaderTileLayout extends TileLayout {
        private Rect mClippingBounds = new Rect();

        public HeaderTileLayout(Context context) {
            super(context);
            setClipChildren(false);
            setClipToPadding(false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
            layoutParams.gravity = 1;
            setLayoutParams(layoutParams);
        }

        /* access modifiers changed from: protected */
        public void onConfigurationChanged(Configuration configuration) {
            super.onConfigurationChanged(configuration);
            updateResources();
        }

        public void onFinishInflate() {
            updateResources();
        }

        private ViewGroup.LayoutParams generateTileLayoutParams() {
            return new ViewGroup.LayoutParams(this.mCellWidth, this.mCellHeight);
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.qs.TileLayout
        public void addTileView(QSPanel.TileRecord tileRecord) {
            addView(tileRecord.tileView, getChildCount(), generateTileLayoutParams());
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.qs.TileLayout
        public void onLayout(boolean z, int i, int i2, int i3, int i4) {
            this.mClippingBounds.set(0, 0, i3 - i, 10000);
            setClipBounds(this.mClippingBounds);
            calculateColumns();
            int i5 = 0;
            while (i5 < this.mRecords.size()) {
                this.mRecords.get(i5).tileView.setVisibility(i5 < this.mColumns ? 0 : 8);
                i5++;
            }
            setAccessibilityOrder();
            layoutTileRecords(this.mColumns);
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout, com.android.systemui.qs.TileLayout
        public boolean updateResources() {
            this.mCellWidth = ((ViewGroup) this).mContext.getResources().getDimensionPixelSize(C0005R$dimen.qs_quick_tile_size);
            this.mCellHeight = this.mCellWidth;
            return false;
        }

        private boolean calculateColumns() {
            int i;
            int i2 = this.mColumns;
            int size = this.mRecords.size();
            if (size == 0) {
                this.mColumns = 0;
                return true;
            }
            int measuredWidth = (getMeasuredWidth() - getPaddingStart()) - getPaddingEnd();
            int max = (measuredWidth - (this.mCellWidth * size)) / Math.max(1, size - 1);
            if (max > 0) {
                this.mCellMarginHorizontal = max;
                this.mColumns = size;
            } else {
                int i3 = this.mCellWidth;
                if (i3 == 0) {
                    i = 1;
                } else {
                    i = Math.min(size, measuredWidth / i3);
                }
                this.mColumns = i;
                int i4 = this.mColumns;
                this.mCellMarginHorizontal = (measuredWidth - (this.mCellWidth * i4)) / (i4 - 1);
            }
            return this.mColumns != i2;
        }

        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:11:0x0011 */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:13:0x0011 */
        private void setAccessibilityOrder() {
            ArrayList<QSPanel.TileRecord> arrayList = this.mRecords;
            if (arrayList != null && arrayList.size() > 0) {
                Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
                View view = this;
                while (it.hasNext()) {
                    QSPanel.TileRecord next = it.next();
                    if (next.tileView.getVisibility() != 8) {
                        view = next.tileView.updateAccessibilityOrder(view);
                    }
                }
                ArrayList<QSPanel.TileRecord> arrayList2 = this.mRecords;
                arrayList2.get(arrayList2.size() - 1).tileView.setAccessibilityTraversalBefore(C0007R$id.expand_indicator);
            }
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.qs.TileLayout
        public void onMeasure(int i, int i2) {
            Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
            while (it.hasNext()) {
                QSPanel.TileRecord next = it.next();
                if (next.tileView.getVisibility() != 8) {
                    next.tileView.measure(TileLayout.exactly(this.mCellWidth), TileLayout.exactly(this.mCellHeight));
                }
            }
            int i3 = this.mCellHeight;
            if (i3 < 0) {
                i3 = 0;
            }
            setMeasuredDimension(View.MeasureSpec.getSize(i), i3);
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout, com.android.systemui.qs.TileLayout
        public int getNumVisibleTiles() {
            return this.mColumns;
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.qs.TileLayout
        public int getColumnStart(int i) {
            return getPaddingStart() + (i * (this.mCellWidth + this.mCellMarginHorizontal));
        }
    }
}
