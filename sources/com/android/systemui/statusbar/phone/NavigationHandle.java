package com.android.systemui.statusbar.phone;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import com.android.settingslib.Utils;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0005R$dimen;

public class NavigationHandle extends View implements ButtonInterface {
    private final int mBottom;
    private final int mDarkColor;
    private float mDarkIntensity;
    private final int mLightColor;
    private final Paint mPaint;
    private final int mRadius;

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void abortCurrentGesture() {
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setDelayTouchFeedback(boolean z) {
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setImageDrawable(Drawable drawable) {
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setVertical(boolean z) {
    }

    public NavigationHandle(Context context) {
        this(context, null);
    }

    public NavigationHandle(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDarkIntensity = -1.0f;
        this.mPaint = new Paint();
        Resources resources = context.getResources();
        this.mRadius = resources.getDimensionPixelSize(C0005R$dimen.navigation_handle_radius);
        this.mBottom = resources.getDimensionPixelSize(C0005R$dimen.navigation_handle_bottom);
        int themeAttr = Utils.getThemeAttr(context, C0002R$attr.darkIconTheme);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, Utils.getThemeAttr(context, C0002R$attr.lightIconTheme));
        ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(context, themeAttr);
        this.mLightColor = Utils.getColorAttrDefaultColor(contextThemeWrapper, C0002R$attr.singleToneColor);
        this.mDarkColor = Utils.getColorAttrDefaultColor(contextThemeWrapper2, C0002R$attr.singleToneColor);
        this.mPaint.setAntiAlias(true);
        setFocusable(false);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight();
        int i = this.mRadius * 2;
        int width = getWidth();
        int i2 = (height - this.mBottom) - i;
        float f = (float) (i2 + i);
        int i3 = this.mRadius;
        canvas.drawRoundRect(0.0f, (float) i2, (float) width, f, (float) i3, (float) i3, this.mPaint);
    }

    @Override // com.android.systemui.statusbar.phone.ButtonInterface
    public void setDarkIntensity(float f) {
        if (this.mDarkIntensity != f) {
            this.mPaint.setColor(((Integer) ArgbEvaluator.getInstance().evaluate(f, Integer.valueOf(this.mLightColor), Integer.valueOf(this.mDarkColor))).intValue());
            this.mDarkIntensity = f;
            invalidate();
        }
    }
}
