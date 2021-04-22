package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0008R$integer;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;
import java.util.Iterator;

public class TileLayout extends ViewGroup implements QSPanel.QSTileLayout {
    protected int mCellHeight;
    protected int mCellMarginHorizontal;
    private int mCellMarginTop;
    protected int mCellMarginVertical;
    protected int mCellWidth;
    protected int mColumns;
    private boolean mListening;
    protected int mMaxAllowedRows;
    protected final ArrayList<QSPanel.TileRecord> mRecords;
    protected int mRows;
    protected int mSidePadding;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRows = 1;
        this.mRecords = new ArrayList<>();
        this.mMaxAllowedRows = 3;
        setFocusableInTouchMode(true);
        updateResources();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public int getOffsetTop(QSPanel.TileRecord tileRecord) {
        return getTop();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void setListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
            while (it.hasNext()) {
                it.next().tile.setListening(this, this.mListening);
            }
        }
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void addTile(QSPanel.TileRecord tileRecord) {
        this.mRecords.add(tileRecord);
        tileRecord.tile.setListening(this, this.mListening);
        addTileView(tileRecord);
    }

    /* access modifiers changed from: protected */
    public void addTileView(QSPanel.TileRecord tileRecord) {
        addView(tileRecord.tileView);
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void removeTile(QSPanel.TileRecord tileRecord) {
        this.mRecords.remove(tileRecord);
        tileRecord.tile.setListening(this, false);
        removeView(tileRecord.tileView);
    }

    public void removeAllViews() {
        Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.setListening(this, false);
        }
        this.mRecords.clear();
        super.removeAllViews();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public boolean updateResources() {
        Resources resources = ((ViewGroup) this).mContext.getResources();
        int max = Math.max(1, resources.getInteger(C0008R$integer.quick_settings_num_columns));
        this.mCellHeight = ((ViewGroup) this).mContext.getResources().getDimensionPixelSize(C0005R$dimen.qs_tile_height);
        this.mCellMarginHorizontal = resources.getDimensionPixelSize(C0005R$dimen.qs_tile_margin_horizontal);
        this.mCellMarginVertical = resources.getDimensionPixelSize(C0005R$dimen.qs_tile_margin_vertical);
        this.mCellMarginTop = resources.getDimensionPixelSize(C0005R$dimen.qs_tile_margin_top);
        this.mSidePadding = resources.getDimensionPixelOffset(C0005R$dimen.qs_tile_layout_margin_side);
        this.mMaxAllowedRows = Math.max(1, getResources().getInteger(C0008R$integer.quick_settings_max_rows));
        if (this.mColumns == max) {
            return false;
        }
        this.mColumns = max;
        requestLayout();
        return true;
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:18:0x0038 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:20:0x0038 */
    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int size = this.mRecords.size();
        int size2 = View.MeasureSpec.getSize(i);
        int paddingStart = (size2 - getPaddingStart()) - getPaddingEnd();
        if (View.MeasureSpec.getMode(i2) == 0) {
            int i3 = this.mColumns;
            this.mRows = ((size + i3) - 1) / i3;
        }
        int i4 = paddingStart - (this.mSidePadding * 2);
        int i5 = this.mCellMarginHorizontal;
        int i6 = this.mColumns;
        this.mCellWidth = (i4 - (i5 * i6)) / i6;
        Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
        View view = this;
        while (it.hasNext()) {
            QSPanel.TileRecord next = it.next();
            if (next.tileView.getVisibility() != 8) {
                next.tileView.measure(exactly(this.mCellWidth), exactly(this.mCellHeight));
                view = next.tileView.updateAccessibilityOrder(view);
            }
        }
        int i7 = this.mCellHeight;
        int i8 = this.mCellMarginVertical;
        int i9 = this.mRows;
        int i10 = ((i7 + i8) * i9) + (i9 != 0 ? this.mCellMarginTop - i8 : 0);
        if (i10 < 0) {
            i10 = 0;
        }
        setMeasuredDimension(size2, i10);
    }

    public boolean updateMaxRows(int i, int i2) {
        int size = View.MeasureSpec.getSize(i) - this.mCellMarginTop;
        int i3 = this.mCellMarginVertical;
        int i4 = this.mRows;
        this.mRows = (size + i3) / (this.mCellHeight + i3);
        int i5 = this.mRows;
        int i6 = this.mMaxAllowedRows;
        if (i5 >= i6) {
            this.mRows = i6;
        } else if (i5 <= 1) {
            this.mRows = 1;
        }
        int i7 = this.mRows;
        int i8 = this.mColumns;
        if (i7 > ((i2 + i8) - 1) / i8) {
            this.mRows = ((i2 + i8) - 1) / i8;
        }
        if (i4 != this.mRows) {
            return true;
        }
        return false;
    }

    protected static int exactly(int i) {
        return View.MeasureSpec.makeMeasureSpec(i, 1073741824);
    }

    /* access modifiers changed from: protected */
    public void layoutTileRecords(int i) {
        boolean z = getLayoutDirection() == 1;
        int min = Math.min(i, this.mRows * this.mColumns);
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (i2 < min) {
            if (i3 == this.mColumns) {
                i4++;
                i3 = 0;
            }
            QSPanel.TileRecord tileRecord = this.mRecords.get(i2);
            int rowTop = getRowTop(i4);
            int columnStart = getColumnStart(z ? (this.mColumns - i3) - 1 : i3);
            QSTileView qSTileView = tileRecord.tileView;
            qSTileView.layout(columnStart, rowTop, this.mCellWidth + columnStart, qSTileView.getMeasuredHeight() + rowTop);
            i2++;
            i3++;
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        layoutTileRecords(this.mRecords.size());
    }

    private int getRowTop(int i) {
        return (i * (this.mCellHeight + this.mCellMarginVertical)) + this.mCellMarginTop;
    }

    /* access modifiers changed from: protected */
    public int getColumnStart(int i) {
        int paddingStart = getPaddingStart() + this.mSidePadding;
        int i2 = this.mCellMarginHorizontal;
        return paddingStart + (i2 / 2) + (i * (this.mCellWidth + i2));
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public int getNumVisibleTiles() {
        return this.mRecords.size();
    }
}
