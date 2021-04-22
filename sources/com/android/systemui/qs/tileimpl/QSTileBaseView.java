package com.android.systemui.qs.tileimpl;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.PathParser;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import com.android.settingslib.Utils;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0014R$string;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;

public class QSTileBaseView extends QSTileView {
    private String mAccessibilityClass;
    private final ImageView mBg;
    private int mCircleColor;
    private boolean mClicked;
    private boolean mCollapsedView;
    private final int mColorActive;
    private final int mColorDisabled;
    private final int mColorInactive;
    private final H mHandler = new H();
    protected QSIconView mIcon;
    private final FrameLayout mIconFrame;
    private final int[] mLocInScreen = new int[2];
    protected RippleDrawable mRipple;
    private boolean mShowRippleEffect = true;
    private Drawable mTileBackground;
    private boolean mTileState;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public QSTileBaseView(Context context, QSIconView qSIconView, boolean z) {
        super(context);
        context.getResources().getDimensionPixelSize(C0005R$dimen.qs_quick_tile_padding);
        this.mIconFrame = new FrameLayout(context);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(C0005R$dimen.qs_quick_tile_size);
        addView(this.mIconFrame, new LinearLayout.LayoutParams(dimensionPixelSize, dimensionPixelSize));
        this.mBg = new ImageView(getContext());
        ShapeDrawable shapeDrawable = new ShapeDrawable(new PathShape(new Path(PathParser.createPathFromPathData(context.getResources().getString(17039775))), 100.0f, 100.0f));
        shapeDrawable.setTintList(ColorStateList.valueOf(0));
        int dimensionPixelSize2 = context.getResources().getDimensionPixelSize(C0005R$dimen.qs_tile_background_size);
        shapeDrawable.setIntrinsicHeight(dimensionPixelSize2);
        shapeDrawable.setIntrinsicWidth(dimensionPixelSize2);
        this.mBg.setImageDrawable(shapeDrawable);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dimensionPixelSize2, dimensionPixelSize2, 17);
        this.mIconFrame.addView(this.mBg, layoutParams);
        this.mBg.setLayoutParams(layoutParams);
        this.mIcon = qSIconView;
        this.mIconFrame.addView(this.mIcon, new FrameLayout.LayoutParams(-2, -2, 17));
        this.mIconFrame.setClipChildren(false);
        this.mIconFrame.setClipToPadding(false);
        this.mTileBackground = newTileBackground();
        Drawable drawable = this.mTileBackground;
        if (drawable instanceof RippleDrawable) {
            setRipple((RippleDrawable) drawable);
        }
        setImportantForAccessibility(1);
        setBackground(this.mTileBackground);
        this.mColorActive = Utils.getColorAttrDefaultColor(context, 16843829);
        this.mColorDisabled = Utils.getDisabled(context, Utils.getColorAttrDefaultColor(context, 16843282));
        this.mColorInactive = Utils.getColorAttrDefaultColor(context, 16842808);
        setPadding(0, 0, 0, 0);
        setClipChildren(false);
        setClipToPadding(false);
        this.mCollapsedView = z;
        setFocusable(true);
    }

    /* access modifiers changed from: protected */
    public Drawable newTileBackground() {
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(new int[]{16843868});
        Drawable drawable = obtainStyledAttributes.getDrawable(0);
        obtainStyledAttributes.recycle();
        return drawable;
    }

    private void setRipple(RippleDrawable rippleDrawable) {
        this.mRipple = rippleDrawable;
        if (getWidth() != 0) {
            updateRippleSize();
        }
    }

    private void updateRippleSize() {
        int measuredWidth = (this.mIconFrame.getMeasuredWidth() / 2) + this.mIconFrame.getLeft();
        int measuredHeight = (this.mIconFrame.getMeasuredHeight() / 2) + this.mIconFrame.getTop();
        int height = (int) (((float) this.mIcon.getHeight()) * 0.85f);
        this.mRipple.setHotspotBounds(measuredWidth - height, measuredHeight - height, measuredWidth + height, measuredHeight + height);
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public void init(QSTile qSTile) {
        init(new View.OnClickListener() {
            /* class com.android.systemui.qs.tileimpl.$$Lambda$QSTileBaseView$aVxKNvlJE7IFS8nVmOyLdAcByFA */

            public final void onClick(View view) {
                QSTile.this.click();
            }
        }, new View.OnClickListener() {
            /* class com.android.systemui.qs.tileimpl.$$Lambda$QSTileBaseView$W9w1scJAVZm5V6Q1VB4ZO5o3C8A */

            public final void onClick(View view) {
                QSTile.this.secondaryClick();
            }
        }, new View.OnLongClickListener() {
            /* class com.android.systemui.qs.tileimpl.$$Lambda$QSTileBaseView$STEfvGmwtIL_pMrVYwBQuK3x1jo */

            public final boolean onLongClick(View view) {
                return QSTile.this.longClick();
            }
        });
    }

    public void init(View.OnClickListener onClickListener, View.OnClickListener onClickListener2, View.OnLongClickListener onLongClickListener) {
        setOnClickListener(onClickListener);
        setOnLongClickListener(onLongClickListener);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mRipple != null) {
            updateRippleSize();
        }
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public View updateAccessibilityOrder(View view) {
        setAccessibilityTraversalAfter(view.getId());
        return this;
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public void onStateChanged(QSTile.State state) {
        this.mHandler.obtainMessage(1, state).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void handleStateChanged(QSTile.State state) {
        boolean z;
        int circleColor = getCircleColor(state.state);
        boolean animationsEnabled = animationsEnabled();
        int i = this.mCircleColor;
        boolean z2 = true;
        if (circleColor != i) {
            if (animationsEnabled) {
                ValueAnimator duration = ValueAnimator.ofArgb(i, circleColor).setDuration(350L);
                duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    /* class com.android.systemui.qs.tileimpl.$$Lambda$QSTileBaseView$R4RxHhlQ5aUQCBgq0kdDEHJXn14 */

                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        QSTileBaseView.this.lambda$handleStateChanged$3$QSTileBaseView(valueAnimator);
                    }
                });
                duration.start();
            } else {
                QSIconViewImpl.setTint(this.mBg, circleColor);
            }
            this.mCircleColor = circleColor;
        }
        this.mShowRippleEffect = state.showRippleEffect;
        if (state.state == 0) {
            z2 = false;
        }
        setClickable(z2);
        setLongClickable(state.handlesLongClick);
        this.mIcon.setIcon(state, animationsEnabled);
        setContentDescription(state.contentDescription);
        this.mAccessibilityClass = state.state == 0 ? null : state.expandedAccessibilityClassName;
        if ((state instanceof QSTile.BooleanState) && this.mTileState != (z = ((QSTile.BooleanState) state).value)) {
            this.mClicked = false;
            this.mTileState = z;
        }
    }

    public /* synthetic */ void lambda$handleStateChanged$3$QSTileBaseView(ValueAnimator valueAnimator) {
        this.mBg.setImageTintList(ColorStateList.valueOf(((Integer) valueAnimator.getAnimatedValue()).intValue()));
    }

    /* access modifiers changed from: protected */
    public boolean animationsEnabled() {
        if (!isShown() || getAlpha() != 1.0f) {
            return false;
        }
        getLocationOnScreen(this.mLocInScreen);
        if (this.mLocInScreen[1] >= (-getHeight())) {
            return true;
        }
        return false;
    }

    private int getCircleColor(int i) {
        if (i == 0 || i == 1) {
            return this.mColorDisabled;
        }
        if (i == 2) {
            return this.mColorActive;
        }
        Log.e("QSTileBaseView", "Invalid state " + i);
        return 0;
    }

    public void setClickable(boolean z) {
        super.setClickable(z);
        setBackground((!z || !this.mShowRippleEffect) ? null : this.mRipple);
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public int getDetailY() {
        return getTop() + (getHeight() / 2);
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public QSIconView getIcon() {
        return this.mIcon;
    }

    @Override // com.android.systemui.plugins.qs.QSTileView
    public View getIconWithBackground() {
        return this.mIconFrame;
    }

    public boolean performClick() {
        this.mClicked = true;
        return super.performClick();
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            accessibilityEvent.setClassName(this.mAccessibilityClass);
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                boolean z = this.mClicked ? !this.mTileState : this.mTileState;
                accessibilityEvent.setContentDescription(getResources().getString(z ? C0014R$string.switch_bar_on : C0014R$string.switch_bar_off));
                accessibilityEvent.setChecked(z);
            }
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        boolean z = false;
        accessibilityNodeInfo.setSelected(false);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            accessibilityNodeInfo.setClassName(this.mAccessibilityClass);
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                if (!this.mClicked) {
                    z = this.mTileState;
                } else if (!this.mTileState) {
                    z = true;
                }
                accessibilityNodeInfo.setText(getResources().getString(z ? C0014R$string.switch_bar_on : C0014R$string.switch_bar_off));
                accessibilityNodeInfo.setChecked(z);
                accessibilityNodeInfo.setCheckable(true);
                if (isLongClickable()) {
                    accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK.getId(), getResources().getString(C0014R$string.accessibility_long_click_tile)));
                }
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('[');
        sb.append("locInScreen=(" + this.mLocInScreen[0] + ", " + this.mLocInScreen[1] + ")");
        StringBuilder sb2 = new StringBuilder();
        sb2.append(", iconView=");
        sb2.append(this.mIcon.toString());
        sb.append(sb2.toString());
        sb.append(", tileState=" + this.mTileState);
        sb.append("]");
        return sb.toString();
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            if (message.what == 1) {
                QSTileBaseView.this.handleStateChanged((QSTile.State) message.obj);
            }
        }
    }
}
