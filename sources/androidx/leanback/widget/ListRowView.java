package androidx.leanback.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.leanback.R$id;
import androidx.leanback.R$layout;

public final class ListRowView extends LinearLayout {
    private HorizontalGridView mGridView;

    public ListRowView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ListRowView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        LayoutInflater.from(context).inflate(R$layout.lb_list_row, this);
        this.mGridView = (HorizontalGridView) findViewById(R$id.row_content);
        this.mGridView.setHasFixedSize(false);
        setOrientation(1);
        setDescendantFocusability(262144);
    }
}
