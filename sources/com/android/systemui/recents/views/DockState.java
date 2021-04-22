package com.android.systemui.recents.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.util.IntProperty;
import android.view.animation.Interpolator;
import com.android.internal.policy.DockedDividerUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.utilities.Utilities;
import java.util.ArrayList;

public class DockState implements DropTarget {
    public static final DockState BOTTOM = new DockState(4, 1, 80, 0, 0, new RectF(0.0f, 0.875f, 1.0f, 1.0f), new RectF(0.0f, 0.875f, 1.0f, 1.0f), new RectF(0.0f, 0.5f, 1.0f, 1.0f));
    public static final DockState LEFT = new DockState(1, 0, 80, 0, 1, new RectF(0.0f, 0.0f, 0.125f, 1.0f), new RectF(0.0f, 0.0f, 0.125f, 1.0f), new RectF(0.0f, 0.0f, 0.5f, 1.0f));
    public static final DockState NONE = new DockState(-1, -1, 80, 255, 0, null, null, null);
    public static final DockState RIGHT = new DockState(3, 1, 80, 0, 1, new RectF(0.875f, 0.0f, 1.0f, 1.0f), new RectF(0.875f, 0.0f, 1.0f, 1.0f), new RectF(0.5f, 0.0f, 1.0f, 1.0f));
    public static final DockState TOP = new DockState(2, 0, 80, 0, 0, new RectF(0.0f, 0.0f, 1.0f, 0.125f), new RectF(0.0f, 0.0f, 1.0f, 0.125f), new RectF(0.0f, 0.0f, 1.0f, 0.5f));
    private static final Rect mTmpRect = new Rect();
    public final int createMode;
    private final RectF dockArea;
    public final int dockSide;
    private final RectF expandedTouchDockArea;
    private final RectF touchArea;
    public final ViewState viewState;

    @Override // com.android.systemui.recents.views.DropTarget
    public boolean acceptsDrop(int i, int i2, int i3, int i4, Rect rect, boolean z) {
        if (z) {
            getMappedRect(this.expandedTouchDockArea, i3, i4, mTmpRect);
            return mTmpRect.contains(i, i2);
        }
        getMappedRect(this.touchArea, i3, i4, mTmpRect);
        updateBoundsWithSystemInsets(mTmpRect, rect);
        return mTmpRect.contains(i, i2);
    }

    public static class ViewState {
        private static final IntProperty<ViewState> HINT_ALPHA = new IntProperty<ViewState>("drawableAlpha") {
            /* class com.android.systemui.recents.views.DockState.ViewState.AnonymousClass1 */

            public void setValue(ViewState viewState, int i) {
                viewState.mHintTextAlpha = i;
                viewState.dockAreaOverlay.invalidateSelf();
            }

            public Integer get(ViewState viewState) {
                return Integer.valueOf(viewState.mHintTextAlpha);
            }
        };
        public final int dockAreaAlpha;
        public final ColorDrawable dockAreaOverlay;
        public final int hintTextAlpha;
        public final int hintTextOrientation;
        private AnimatorSet mDockAreaOverlayAnimator;
        private String mHintText;
        private int mHintTextAlpha;
        private Point mHintTextBounds;
        private Paint mHintTextPaint;
        private final int mHintTextResId;
        private Rect mTmpRect;

        private ViewState(int i, int i2, int i3, int i4) {
            this.mHintTextBounds = new Point();
            this.mHintTextAlpha = 255;
            this.mTmpRect = new Rect();
            this.dockAreaAlpha = i;
            this.dockAreaOverlay = new ColorDrawable(LegacyRecentsImpl.getConfiguration().isGridEnabled ? -16777216 : -1);
            this.dockAreaOverlay.setAlpha(0);
            this.hintTextAlpha = i2;
            this.hintTextOrientation = i3;
            this.mHintTextResId = i4;
            this.mHintTextPaint = new Paint(1);
            this.mHintTextPaint.setColor(-1);
        }

        public void update(Context context) {
            Resources resources = context.getResources();
            this.mHintText = context.getString(this.mHintTextResId);
            this.mHintTextPaint.setTextSize((float) resources.getDimensionPixelSize(2131166213));
            Paint paint = this.mHintTextPaint;
            String str = this.mHintText;
            paint.getTextBounds(str, 0, str.length(), this.mTmpRect);
            this.mHintTextBounds.set((int) this.mHintTextPaint.measureText(this.mHintText), this.mTmpRect.height());
        }

        public void draw(Canvas canvas) {
            if (this.dockAreaOverlay.getAlpha() > 0) {
                this.dockAreaOverlay.draw(canvas);
            }
            if (this.mHintTextAlpha > 0) {
                Rect bounds = this.dockAreaOverlay.getBounds();
                int width = bounds.left + ((bounds.width() - this.mHintTextBounds.x) / 2);
                int height = bounds.top + ((bounds.height() + this.mHintTextBounds.y) / 2);
                this.mHintTextPaint.setAlpha(this.mHintTextAlpha);
                if (this.hintTextOrientation == 1) {
                    canvas.save();
                    canvas.rotate(-90.0f, (float) bounds.centerX(), (float) bounds.centerY());
                }
                canvas.drawText(this.mHintText, (float) width, (float) height, this.mHintTextPaint);
                if (this.hintTextOrientation == 1) {
                    canvas.restore();
                }
            }
        }

        public void startAnimation(Rect rect, int i, int i2, int i3, Interpolator interpolator, boolean z, boolean z2) {
            Interpolator interpolator2;
            AnimatorSet animatorSet = this.mDockAreaOverlayAnimator;
            if (animatorSet != null) {
                animatorSet.cancel();
            }
            ArrayList arrayList = new ArrayList();
            if (this.dockAreaOverlay.getAlpha() != i) {
                if (z) {
                    ColorDrawable colorDrawable = this.dockAreaOverlay;
                    ObjectAnimator ofInt = ObjectAnimator.ofInt(colorDrawable, Utilities.DRAWABLE_ALPHA, colorDrawable.getAlpha(), i);
                    ofInt.setDuration((long) i3);
                    ofInt.setInterpolator(interpolator);
                    arrayList.add(ofInt);
                } else {
                    this.dockAreaOverlay.setAlpha(i);
                }
            }
            int i4 = this.mHintTextAlpha;
            if (i4 != i2) {
                if (z) {
                    ObjectAnimator ofInt2 = ObjectAnimator.ofInt(this, HINT_ALPHA, i4, i2);
                    ofInt2.setDuration(150L);
                    if (i2 > this.mHintTextAlpha) {
                        interpolator2 = Interpolators.ALPHA_IN;
                    } else {
                        interpolator2 = Interpolators.ALPHA_OUT;
                    }
                    ofInt2.setInterpolator(interpolator2);
                    arrayList.add(ofInt2);
                } else {
                    this.mHintTextAlpha = i2;
                    this.dockAreaOverlay.invalidateSelf();
                }
            }
            if (rect != null && !this.dockAreaOverlay.getBounds().equals(rect)) {
                if (z2) {
                    PropertyValuesHolder ofObject = PropertyValuesHolder.ofObject(Utilities.DRAWABLE_RECT, Utilities.RECT_EVALUATOR, new Rect(this.dockAreaOverlay.getBounds()), rect);
                    ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(this.dockAreaOverlay, ofObject);
                    ofPropertyValuesHolder.setDuration((long) i3);
                    ofPropertyValuesHolder.setInterpolator(interpolator);
                    arrayList.add(ofPropertyValuesHolder);
                } else {
                    this.dockAreaOverlay.setBounds(rect);
                }
            }
            if (!arrayList.isEmpty()) {
                this.mDockAreaOverlayAnimator = new AnimatorSet();
                this.mDockAreaOverlayAnimator.playTogether(arrayList);
                this.mDockAreaOverlayAnimator.start();
            }
        }
    }

    DockState(int i, int i2, int i3, int i4, int i5, RectF rectF, RectF rectF2, RectF rectF3) {
        this.dockSide = i;
        this.createMode = i2;
        this.viewState = new ViewState(i3, i4, i5, 2131821942);
        this.dockArea = rectF2;
        this.touchArea = rectF;
        this.expandedTouchDockArea = rectF3;
    }

    public void update(Context context) {
        this.viewState.update(context);
    }

    public Rect getPreDockedBounds(int i, int i2, Rect rect) {
        getMappedRect(this.dockArea, i, i2, mTmpRect);
        Rect rect2 = mTmpRect;
        updateBoundsWithSystemInsets(rect2, rect);
        return rect2;
    }

    public Rect getDockedBounds(int i, int i2, int i3, Rect rect, Resources resources) {
        boolean z = true;
        if (resources.getConfiguration().orientation != 1) {
            z = false;
        }
        int calculateMiddlePosition = DockedDividerUtils.calculateMiddlePosition(z, rect, i, i2, i3);
        Rect rect2 = new Rect();
        DockedDividerUtils.calculateBoundsForPosition(calculateMiddlePosition, this.dockSide, rect2, i, i2, i3);
        return rect2;
    }

    public Rect getDockedTaskStackBounds(Rect rect, int i, int i2, int i3, Rect rect2, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm, Resources resources, Rect rect3) {
        int i4;
        boolean z;
        int i5;
        int i6;
        int i7 = 0;
        if (resources.getConfiguration().orientation == 1) {
            i6 = i;
            i4 = i3;
            z = true;
            i5 = i2;
        } else {
            i6 = i;
            i5 = i2;
            i4 = i3;
            z = false;
        }
        DockedDividerUtils.calculateBoundsForPosition(DockedDividerUtils.calculateMiddlePosition(z, rect2, i6, i5, i4), DockedDividerUtils.invertDockSide(this.dockSide), rect3, i, i2, i3);
        Rect rect4 = new Rect();
        if (this.dockArea.bottom >= 1.0f) {
            i7 = rect2.top;
        }
        taskStackLayoutAlgorithm.getTaskStackBounds(rect, rect3, i7, 0, rect2.right, rect4);
        return rect4;
    }

    private Rect updateBoundsWithSystemInsets(Rect rect, Rect rect2) {
        int i = this.dockSide;
        if (i == 1) {
            rect.right += rect2.left;
        } else if (i == 3) {
            rect.left -= rect2.right;
        }
        return rect;
    }

    private void getMappedRect(RectF rectF, int i, int i2, Rect rect) {
        float f = (float) i;
        float f2 = (float) i2;
        rect.set((int) (rectF.left * f), (int) (rectF.top * f2), (int) (rectF.right * f), (int) (rectF.bottom * f2));
    }
}
