package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;

public class NearestTouchFrame extends FrameLayout {
    private final ArrayList<View> mClickableChildren;
    private final Context mContext;
    private final boolean mIsActive;
    private final int[] mOffset;
    private final int[] mTmpInt;
    private View mTouchingChild;

    public NearestTouchFrame(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, context.getResources().getConfiguration());
    }

    NearestTouchFrame(Context context, AttributeSet attributeSet, Configuration configuration) {
        super(context, attributeSet);
        this.mClickableChildren = new ArrayList<>();
        this.mTmpInt = new int[2];
        this.mOffset = new int[2];
        this.mIsActive = configuration.smallestScreenWidthDp < 600;
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        this.mClickableChildren.clear();
        addClickableChildren(this);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        getLocationInWindow(this.mOffset);
    }

    private void addClickableChildren(ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt.isClickable()) {
                this.mClickableChildren.add(childAt);
            } else if (childAt instanceof ViewGroup) {
                addClickableChildren((ViewGroup) childAt);
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0054, code lost:
        if ((((float) getHeight()) - r8.getY()) >= r3) goto L_0x005a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x003b, code lost:
        if ((((float) getWidth()) - r8.getX()) >= r5) goto L_0x003d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(android.view.MotionEvent r8) {
        /*
        // Method dump skipped, instructions count: 151
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NearestTouchFrame.onTouchEvent(android.view.MotionEvent):boolean");
    }

    private View findNearestChild(MotionEvent motionEvent) {
        if (this.mClickableChildren.isEmpty()) {
            return null;
        }
        return (View) this.mClickableChildren.stream().filter($$Lambda$dFYK0EjGBZUG5FTAJ9pyZPnsifY.INSTANCE).map(new Function(motionEvent) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NearestTouchFrame$c68uozdLu3LZYhrzFrFQdtMIM */
            private final /* synthetic */ MotionEvent f$1;

            {
                this.f$1 = r2;
            }

            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return NearestTouchFrame.this.lambda$findNearestChild$0$NearestTouchFrame(this.f$1, (View) obj);
            }
        }).min(Comparator.comparingInt($$Lambda$NearestTouchFrame$NP6mvtRuXVTLLChUNbbl4JUIMyU.INSTANCE)).map($$Lambda$NearestTouchFrame$KtkvB6kuUFBlaLB_chuEtrCrZqA.INSTANCE).orElse(null);
    }

    public /* synthetic */ Pair lambda$findNearestChild$0$NearestTouchFrame(MotionEvent motionEvent, View view) {
        return new Pair(Integer.valueOf(distance(view, motionEvent)), view);
    }

    static /* synthetic */ View lambda$findNearestChild$2(Pair pair) {
        return (View) pair.second;
    }

    private int distance(View view, MotionEvent motionEvent) {
        view.getLocationInWindow(this.mTmpInt);
        int[] iArr = this.mTmpInt;
        int i = iArr[0];
        int[] iArr2 = this.mOffset;
        int i2 = i - iArr2[0];
        int i3 = iArr[1] - iArr2[1];
        return Math.max(Math.min(Math.abs(i2 - ((int) motionEvent.getX())), Math.abs(((int) motionEvent.getX()) - (view.getWidth() + i2))), Math.min(Math.abs(i3 - ((int) motionEvent.getY())), Math.abs(((int) motionEvent.getY()) - (view.getHeight() + i3))));
    }
}
