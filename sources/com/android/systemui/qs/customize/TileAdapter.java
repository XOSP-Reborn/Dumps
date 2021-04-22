package com.android.systemui.qs.customize;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.customize.TileQueryHelper;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSIconViewImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.sonymobile.settingslib.logging.LoggingManager;
import java.util.ArrayList;
import java.util.List;

public class TileAdapter extends RecyclerView.Adapter<Holder> implements TileQueryHelper.TileStateListener {
    private int mAccessibilityAction = 0;
    private int mAccessibilityFromIndex;
    private CharSequence mAccessibilityFromLabel;
    private final AccessibilityManager mAccessibilityManager;
    private List<TileQueryHelper.TileInfo> mAllTiles;
    private final ItemTouchHelper.Callback mCallbacks = new ItemTouchHelper.Callback() {
        /* class com.android.systemui.qs.customize.TileAdapter.AnonymousClass6 */

        @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        }

        @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int i) {
            super.onSelectedChanged(viewHolder, i);
            if (i != 2) {
                viewHolder = null;
            }
            if (viewHolder != TileAdapter.this.mCurrentDrag) {
                if (TileAdapter.this.mCurrentDrag != null) {
                    int adapterPosition = TileAdapter.this.mCurrentDrag.getAdapterPosition();
                    if (adapterPosition != -1) {
                        TileAdapter.this.mCurrentDrag.mTileView.setShowAppLabel(adapterPosition > TileAdapter.this.mEditIndex && !((TileQueryHelper.TileInfo) TileAdapter.this.mTiles.get(adapterPosition)).isSystem);
                        TileAdapter.this.mCurrentDrag.stopDrag();
                        TileAdapter.this.mCurrentDrag = null;
                    } else {
                        return;
                    }
                }
                if (viewHolder != null) {
                    TileAdapter.this.mCurrentDrag = (Holder) viewHolder;
                    TileAdapter.this.mCurrentDrag.startDrag();
                }
                TileAdapter.this.mHandler.post(new Runnable() {
                    /* class com.android.systemui.qs.customize.TileAdapter.AnonymousClass6.AnonymousClass1 */

                    public void run() {
                        TileAdapter tileAdapter = TileAdapter.this;
                        tileAdapter.notifyItemChanged(tileAdapter.mEditIndex);
                    }
                });
            }
        }

        @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
            int adapterPosition = viewHolder2.getAdapterPosition();
            if (adapterPosition == 0 || adapterPosition == -1) {
                return false;
            }
            if (TileAdapter.this.canRemoveTiles() || viewHolder.getAdapterPosition() >= TileAdapter.this.mEditIndex) {
                if (adapterPosition <= TileAdapter.this.mEditIndex + 1) {
                    return true;
                }
                return false;
            } else if (adapterPosition < TileAdapter.this.mEditIndex) {
                return true;
            } else {
                return false;
            }
        }

        @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int itemViewType = viewHolder.getItemViewType();
            if (itemViewType == 1 || itemViewType == 3 || itemViewType == 4) {
                return ItemTouchHelper.Callback.makeMovementFlags(0, 0);
            }
            return ItemTouchHelper.Callback.makeMovementFlags(15, 0);
        }

        @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
            int adapterPosition = viewHolder.getAdapterPosition();
            int adapterPosition2 = viewHolder2.getAdapterPosition();
            if (adapterPosition == 0 || adapterPosition == -1 || adapterPosition2 == 0 || adapterPosition2 == -1) {
                return false;
            }
            return TileAdapter.this.move((TileAdapter) adapterPosition, adapterPosition2, (int) viewHolder2.itemView);
        }
    };
    private final Context mContext;
    private Holder mCurrentDrag;
    private List<String> mCurrentSpecs;
    private final RecyclerView.ItemDecoration mDecoration;
    private int mEditIndex;
    private final Handler mHandler = new Handler();
    private QSTileHost mHost;
    private final ItemTouchHelper mItemTouchHelper;
    private final int mMinNumTiles;
    private boolean mNeedsFocus;
    private List<TileQueryHelper.TileInfo> mOtherTiles;
    private final GridLayoutManager.SpanSizeLookup mSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        /* class com.android.systemui.qs.customize.TileAdapter.AnonymousClass5 */

        @Override // androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
        public int getSpanSize(int i) {
            int itemViewType = TileAdapter.this.getItemViewType(i);
            return (itemViewType == 1 || itemViewType == 4 || itemViewType == 3) ? 3 : 1;
        }
    };
    private int mTileDividerIndex;
    private final List<TileQueryHelper.TileInfo> mTiles = new ArrayList();

    public TileAdapter(Context context) {
        this.mContext = context;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        this.mItemTouchHelper = new ItemTouchHelper(this.mCallbacks);
        this.mDecoration = new TileItemDecoration(context);
        this.mMinNumTiles = context.getResources().getInteger(C0008R$integer.quick_settings_min_num_tiles);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public ItemTouchHelper getItemTouchHelper() {
        return this.mItemTouchHelper;
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return this.mDecoration;
    }

    public void saveSpecs(QSTileHost qSTileHost) {
        ArrayList arrayList = new ArrayList();
        clearAccessibilityState();
        int i = 1;
        while (i < this.mTiles.size() && this.mTiles.get(i) != null) {
            arrayList.add(this.mTiles.get(i).spec);
            i++;
        }
        qSTileHost.changeTiles(this.mCurrentSpecs, arrayList);
        this.mCurrentSpecs = arrayList;
    }

    private void clearAccessibilityState() {
        if (this.mAccessibilityAction == 1) {
            List<TileQueryHelper.TileInfo> list = this.mTiles;
            int i = this.mEditIndex - 1;
            this.mEditIndex = i;
            list.remove(i);
            this.mTileDividerIndex--;
            notifyDataSetChanged();
        }
        this.mAccessibilityAction = 0;
    }

    public void resetTileSpecs(QSTileHost qSTileHost, List<String> list) {
        qSTileHost.changeTiles(this.mCurrentSpecs, list);
        setTileSpecs(list);
    }

    public void setTileSpecs(List<String> list) {
        if (!list.equals(this.mCurrentSpecs)) {
            this.mCurrentSpecs = list;
            recalcSpecs();
        }
    }

    @Override // com.android.systemui.qs.customize.TileQueryHelper.TileStateListener
    public void onTilesChanged(List<TileQueryHelper.TileInfo> list) {
        this.mAllTiles = list;
        recalcSpecs();
    }

    private void recalcSpecs() {
        List<TileQueryHelper.TileInfo> list;
        if (!(this.mCurrentSpecs == null || (list = this.mAllTiles) == null)) {
            this.mOtherTiles = new ArrayList(list);
            this.mTiles.clear();
            this.mTiles.add(null);
            int i = 0;
            for (int i2 = 0; i2 < this.mCurrentSpecs.size(); i2++) {
                TileQueryHelper.TileInfo andRemoveOther = getAndRemoveOther(this.mCurrentSpecs.get(i2));
                if (andRemoveOther != null) {
                    this.mTiles.add(andRemoveOther);
                }
            }
            this.mTiles.add(null);
            while (i < this.mOtherTiles.size()) {
                TileQueryHelper.TileInfo tileInfo = this.mOtherTiles.get(i);
                if (tileInfo.isSystem) {
                    this.mOtherTiles.remove(i);
                    this.mTiles.add(tileInfo);
                    i--;
                }
                i++;
            }
            this.mTileDividerIndex = this.mTiles.size();
            this.mTiles.add(null);
            this.mTiles.addAll(this.mOtherTiles);
            updateDividerLocations();
            notifyDataSetChanged();
        }
    }

    private TileQueryHelper.TileInfo getAndRemoveOther(String str) {
        for (int i = 0; i < this.mOtherTiles.size(); i++) {
            if (this.mOtherTiles.get(i).spec.equals(str)) {
                return this.mOtherTiles.remove(i);
            }
        }
        return null;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int i) {
        if (i == 0) {
            return 3;
        }
        if (this.mAccessibilityAction == 1 && i == this.mEditIndex - 1) {
            return 2;
        }
        if (i == this.mTileDividerIndex) {
            return 4;
        }
        if (this.mTiles.get(i) == null) {
            return 1;
        }
        return 0;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater from = LayoutInflater.from(context);
        if (i == 3) {
            return new Holder(from.inflate(C0010R$layout.qs_customize_header, viewGroup, false));
        }
        if (i == 4) {
            return new Holder(from.inflate(C0010R$layout.qs_customize_tile_divider, viewGroup, false));
        }
        if (i == 1) {
            return new Holder(from.inflate(C0010R$layout.qs_customize_divider, viewGroup, false));
        }
        FrameLayout frameLayout = (FrameLayout) from.inflate(C0010R$layout.qs_customize_tile_frame, viewGroup, false);
        frameLayout.addView(new CustomizeTileView(context, new QSIconViewImpl(context)));
        return new Holder(frameLayout);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mTiles.size();
    }

    public boolean onFailedToRecycleView(Holder holder) {
        holder.clearDrag();
        return true;
    }

    public void onBindViewHolder(final Holder holder, int i) {
        String str;
        if (holder.getItemViewType() != 3) {
            int i2 = 4;
            boolean z = false;
            if (holder.getItemViewType() == 4) {
                View view = holder.itemView;
                if (this.mTileDividerIndex < this.mTiles.size() - 1) {
                    i2 = 0;
                }
                view.setVisibility(i2);
            } else if (holder.getItemViewType() == 1) {
                Resources resources = this.mContext.getResources();
                if (this.mCurrentDrag == null) {
                    str = resources.getString(C0014R$string.drag_to_add_tiles);
                } else if (canRemoveTiles() || this.mCurrentDrag.getAdapterPosition() >= this.mEditIndex) {
                    str = resources.getString(C0014R$string.drag_to_remove_tiles);
                } else {
                    str = resources.getString(C0014R$string.drag_to_remove_disabled, Integer.valueOf(this.mMinNumTiles));
                }
                ((TextView) holder.itemView.findViewById(16908310)).setText(str);
            } else if (holder.getItemViewType() == 2) {
                holder.mTileView.setClickable(true);
                holder.mTileView.setFocusable(true);
                holder.mTileView.setFocusableInTouchMode(true);
                holder.mTileView.setVisibility(0);
                holder.mTileView.setImportantForAccessibility(1);
                holder.mTileView.setContentDescription(this.mContext.getString(C0014R$string.accessibility_qs_edit_tile_add, this.mAccessibilityFromLabel, Integer.valueOf(i)));
                holder.mTileView.setOnClickListener(new View.OnClickListener() {
                    /* class com.android.systemui.qs.customize.TileAdapter.AnonymousClass1 */

                    public void onClick(View view) {
                        TileAdapter.this.selectPosition(holder.getAdapterPosition(), view);
                    }
                });
                if (this.mNeedsFocus) {
                    holder.mTileView.requestLayout();
                    holder.mTileView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                        /* class com.android.systemui.qs.customize.TileAdapter.AnonymousClass2 */

                        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                            holder.mTileView.removeOnLayoutChangeListener(this);
                            holder.mTileView.requestFocus();
                        }
                    });
                    this.mNeedsFocus = false;
                }
            } else {
                TileQueryHelper.TileInfo tileInfo = this.mTiles.get(i);
                if (i > this.mEditIndex) {
                    QSTile.State state = tileInfo.state;
                    state.contentDescription = this.mContext.getString(C0014R$string.accessibility_qs_edit_add_tile_label, state.label);
                } else {
                    int i3 = this.mAccessibilityAction;
                    if (i3 == 1) {
                        tileInfo.state.contentDescription = this.mContext.getString(C0014R$string.accessibility_qs_edit_tile_add, this.mAccessibilityFromLabel, Integer.valueOf(i));
                    } else if (i3 == 2) {
                        tileInfo.state.contentDescription = this.mContext.getString(C0014R$string.accessibility_qs_edit_tile_move, this.mAccessibilityFromLabel, Integer.valueOf(i));
                    } else {
                        tileInfo.state.contentDescription = this.mContext.getString(C0014R$string.accessibility_qs_edit_tile_label, Integer.valueOf(i), tileInfo.state.label);
                    }
                }
                holder.mTileView.handleStateChanged(tileInfo.state);
                holder.mTileView.setShowAppLabel(i > this.mEditIndex && !tileInfo.isSystem);
                if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
                    if (this.mAccessibilityAction == 0 || i < this.mEditIndex) {
                        z = true;
                    }
                    holder.mTileView.setClickable(z);
                    holder.mTileView.setFocusable(z);
                    CustomizeTileView customizeTileView = holder.mTileView;
                    if (z) {
                        i2 = 1;
                    }
                    customizeTileView.setImportantForAccessibility(i2);
                    if (z) {
                        holder.mTileView.setOnClickListener(new View.OnClickListener() {
                            /* class com.android.systemui.qs.customize.TileAdapter.AnonymousClass3 */

                            public void onClick(View view) {
                                int adapterPosition = holder.getAdapterPosition();
                                if (adapterPosition != -1) {
                                    if (TileAdapter.this.mAccessibilityAction != 0) {
                                        TileAdapter.this.selectPosition(adapterPosition, view);
                                    } else if (adapterPosition >= TileAdapter.this.mEditIndex || !TileAdapter.this.canRemoveTiles()) {
                                        TileAdapter.this.startAccessibleAdd(adapterPosition);
                                    } else {
                                        TileAdapter.this.showAccessibilityDialog(adapterPosition, view);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean canRemoveTiles() {
        return this.mCurrentSpecs.size() > this.mMinNumTiles;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void selectPosition(int i, View view) {
        if (this.mAccessibilityAction == 1) {
            List<TileQueryHelper.TileInfo> list = this.mTiles;
            int i2 = this.mEditIndex;
            this.mEditIndex = i2 - 1;
            list.remove(i2);
            notifyItemRemoved(this.mEditIndex);
        }
        this.mAccessibilityAction = 0;
        move(this.mAccessibilityFromIndex, i, view);
        notifyDataSetChanged();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showAccessibilityDialog(final int i, final View view) {
        final TileQueryHelper.TileInfo tileInfo = this.mTiles.get(i);
        AlertDialog create = new AlertDialog.Builder(this.mContext).setItems(new CharSequence[]{this.mContext.getString(C0014R$string.accessibility_qs_edit_move_tile, tileInfo.state.label), this.mContext.getString(C0014R$string.accessibility_qs_edit_remove_tile, tileInfo.state.label)}, new DialogInterface.OnClickListener() {
            /* class com.android.systemui.qs.customize.TileAdapter.AnonymousClass4 */

            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    TileAdapter.this.startAccessibleMove(i);
                    return;
                }
                TileAdapter tileAdapter = TileAdapter.this;
                tileAdapter.move((TileAdapter) i, tileInfo.isSystem ? tileAdapter.mEditIndex : tileAdapter.mTileDividerIndex, (int) view);
                TileAdapter tileAdapter2 = TileAdapter.this;
                tileAdapter2.notifyItemChanged(tileAdapter2.mTileDividerIndex);
                TileAdapter.this.notifyDataSetChanged();
            }
        }).setNegativeButton(17039360, (DialogInterface.OnClickListener) null).create();
        SystemUIDialog.setShowForAllUsers(create, true);
        SystemUIDialog.applyFlags(create);
        create.show();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startAccessibleAdd(int i) {
        this.mAccessibilityFromIndex = i;
        this.mAccessibilityFromLabel = this.mTiles.get(i).state.label;
        this.mAccessibilityAction = 1;
        List<TileQueryHelper.TileInfo> list = this.mTiles;
        int i2 = this.mEditIndex;
        this.mEditIndex = i2 + 1;
        list.add(i2, null);
        this.mTileDividerIndex++;
        this.mNeedsFocus = true;
        notifyDataSetChanged();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startAccessibleMove(int i) {
        this.mAccessibilityFromIndex = i;
        this.mAccessibilityFromLabel = this.mTiles.get(i).state.label;
        this.mAccessibilityAction = 2;
        notifyDataSetChanged();
    }

    public GridLayoutManager.SpanSizeLookup getSizeLookup() {
        return this.mSizeLookup;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean move(int i, int i2, View view) {
        if (i2 == i) {
            return true;
        }
        CharSequence charSequence = this.mTiles.get(i).state.label;
        move(i, i2, this.mTiles);
        updateDividerLocations();
        int i3 = this.mEditIndex;
        if (i2 >= i3) {
            MetricsLogger.action(this.mContext, 360, strip(this.mTiles.get(i2)));
            MetricsLogger.action(this.mContext, 361, i);
            LoggingManager.logQSEvent(this.mContext, "edit", "remove", strip(this.mTiles.get(i2)));
        } else if (i >= i3) {
            MetricsLogger.action(this.mContext, 362, strip(this.mTiles.get(i2)));
            MetricsLogger.action(this.mContext, 363, i2);
            LoggingManager.logQSEvent(this.mContext, "edit", "add", strip(this.mTiles.get(i2)));
        } else {
            MetricsLogger.action(this.mContext, 364, strip(this.mTiles.get(i2)));
            MetricsLogger.action(this.mContext, 365, i2);
        }
        saveSpecs(this.mHost);
        return true;
    }

    private void updateDividerLocations() {
        this.mEditIndex = -1;
        this.mTileDividerIndex = this.mTiles.size();
        for (int i = 1; i < this.mTiles.size(); i++) {
            if (this.mTiles.get(i) == null) {
                if (this.mEditIndex == -1) {
                    this.mEditIndex = i;
                } else {
                    this.mTileDividerIndex = i;
                }
            }
        }
        int size = this.mTiles.size() - 1;
        int i2 = this.mTileDividerIndex;
        if (size == i2) {
            notifyItemChanged(i2);
        }
    }

    private static String strip(TileQueryHelper.TileInfo tileInfo) {
        String str = tileInfo.spec;
        return str.startsWith("custom(") ? CustomTile.getComponentFromSpec(str).getPackageName() : str;
    }

    private <T> void move(int i, int i2, List<T> list) {
        list.add(i2, list.remove(i));
        notifyItemMoved(i, i2);
    }

    public class Holder extends RecyclerView.ViewHolder {
        private CustomizeTileView mTileView;

        public Holder(View view) {
            super(view);
            if (view instanceof FrameLayout) {
                this.mTileView = (CustomizeTileView) ((FrameLayout) view).getChildAt(0);
                this.mTileView.setBackground(null);
                this.mTileView.getIcon().disableAnimation();
            }
        }

        public void clearDrag() {
            this.itemView.clearAnimation();
            this.mTileView.findViewById(C0007R$id.tile_label).clearAnimation();
            this.mTileView.findViewById(C0007R$id.tile_label).setAlpha(1.0f);
            this.mTileView.getAppLabel().clearAnimation();
            this.mTileView.getAppLabel().setAlpha(0.6f);
        }

        public void startDrag() {
            this.itemView.animate().setDuration(100).scaleX(1.2f).scaleY(1.2f);
            this.mTileView.findViewById(C0007R$id.tile_label).animate().setDuration(100).alpha(0.0f);
            this.mTileView.getAppLabel().animate().setDuration(100).alpha(0.0f);
        }

        public void stopDrag() {
            this.itemView.animate().setDuration(100).scaleX(1.0f).scaleY(1.0f);
            this.mTileView.findViewById(C0007R$id.tile_label).animate().setDuration(100).alpha(1.0f);
            this.mTileView.getAppLabel().animate().setDuration(100).alpha(0.6f);
        }
    }

    private class TileItemDecoration extends RecyclerView.ItemDecoration {
        private final Drawable mDrawable;

        private TileItemDecoration(Context context) {
            this.mDrawable = context.getDrawable(C0006R$drawable.qs_customize_tile_decoration);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
        public void onDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.State state) {
            super.onDraw(canvas, recyclerView, state);
            int childCount = recyclerView.getChildCount();
            int width = recyclerView.getWidth();
            int bottom = recyclerView.getBottom();
            for (int i = 0; i < childCount; i++) {
                View childAt = recyclerView.getChildAt(i);
                RecyclerView.ViewHolder childViewHolder = recyclerView.getChildViewHolder(childAt);
                if (childViewHolder.getAdapterPosition() != 0 && (childViewHolder.getAdapterPosition() >= TileAdapter.this.mEditIndex || (childAt instanceof TextView))) {
                    this.mDrawable.setBounds(0, childAt.getTop() + ((ViewGroup.MarginLayoutParams) ((RecyclerView.LayoutParams) childAt.getLayoutParams())).topMargin + Math.round(ViewCompat.getTranslationY(childAt)), width, bottom);
                    this.mDrawable.draw(canvas);
                    return;
                }
            }
        }
    }
}
