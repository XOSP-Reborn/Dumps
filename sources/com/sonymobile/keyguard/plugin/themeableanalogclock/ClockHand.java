package com.sonymobile.keyguard.plugin.themeableanalogclock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.R$styleable;

public class ClockHand extends ViewGroup {
    private int mHandType;

    public ClockHand(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ClockHand(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mHandType = -1;
        applyAttributes(context, attributeSet);
    }

    private void applyAttributes(Context context, AttributeSet attributeSet) {
        TypedArray obtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R$styleable.ClockHand, 0, 0);
        try {
            this.mHandType = obtainStyledAttributes.getInteger(R$styleable.ClockHand_handType, -1);
        } finally {
            obtainStyledAttributes.recycle();
        }
    }

    public final int getHandType() {
        return this.mHandType;
    }

    /* access modifiers changed from: protected */
    public final void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        int i6 = i4 - i2;
        int childCount = getChildCount();
        for (int i7 = 0; i7 < childCount; i7++) {
            View childAt = getChildAt(i7);
            if (childAt.getVisibility() != 8) {
                centerChildAroundPivot(i5, i6, childAt, 0);
            }
        }
    }

    /* access modifiers changed from: protected */
    public final void onFinishInflate() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            padImageView(getChildAt(i));
        }
        super.onFinishInflate();
    }

    private void padImageView(View view) {
        if (view instanceof ImageView) {
            ((ImageView) view).setPadding(1, 1, 1, 1);
        }
    }

    private void centerChildAroundPivot(int i, int i2, View view, int i3) {
        if (view.getMeasuredHeight() == 0) {
            view.measure(0, 0);
        }
        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();
        int pivotX = (int) view.getPivotX();
        int pivotY = (int) view.getPivotY();
        if (view instanceof ImageView) {
            pivotX++;
            pivotY++;
        }
        int i4 = (i / 2) - pivotX;
        int i5 = (i2 / 2) - pivotY;
        view.layout(i4, i5 - i3, measuredWidth + i4, (i5 + measuredHeight) - i3);
    }

    public void updateThemeResources(Drawable drawable, float f, float f2, String str) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof ImageView) {
                updateThemeResourcesToChild((ImageView) childAt, drawable, f, f2, str);
            }
        }
    }

    private void updateThemeResourcesToChild(ImageView imageView, Drawable drawable, float f, float f2, String str) {
        imageView.setImageDrawable(drawable);
        imageView.setPivotX(f);
        imageView.setPivotY(f2);
        imageView.setContentDescription(str);
    }
}
