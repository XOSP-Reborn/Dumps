package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.android.settingslib.Utils;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;

public class GameButton extends ContextualButton {
    private int mDarkColor;
    private String mDrawablePackage;
    private int mDrawableResId;
    private int mLightColor;
    private final Runnable mUpdater;

    public GameButton(int i, Runnable runnable) {
        super(i, C0006R$drawable.f2android);
        this.mUpdater = runnable;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.phone.ContextualButton
    public KeyButtonDrawable getNewDrawable() {
        try {
            if (this.mDrawableResId > 0) {
                Context applicationContext = getContext().getApplicationContext();
                Drawable drawable = applicationContext.getPackageManager().getResourcesForApplication(this.mDrawablePackage).getDrawable(this.mDrawableResId, null);
                return new KeyButtonDrawable(drawable, this.mLightColor, this.mDarkColor, (applicationContext.getResources().getConfiguration().getLayoutDirection() == 1) && drawable.isAutoMirrored(), null);
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException unused) {
            Log.e("GameButton", "Error loading icon from " + this.mDrawablePackage);
        }
        return super.getNewDrawable();
    }

    public void setDrawable(String str, int i, int i2, int i3, boolean z) {
        this.mDrawablePackage = str;
        this.mDrawableResId = i;
        this.mLightColor = i2;
        this.mDarkColor = i3;
        if (!z) {
            Context applicationContext = getContext().getApplicationContext();
            int themeAttr = Utils.getThemeAttr(applicationContext, C0002R$attr.darkIconTheme);
            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(applicationContext, Utils.getThemeAttr(applicationContext, C0002R$attr.lightIconTheme));
            ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(applicationContext, themeAttr);
            this.mLightColor = Utils.getColorAttrDefaultColor(contextThemeWrapper, C0002R$attr.singleToneColor);
            this.mDarkColor = Utils.getColorAttrDefaultColor(contextThemeWrapper2, C0002R$attr.singleToneColor);
        }
        if (this.mDrawablePackage != null) {
            show();
        } else {
            hide();
        }
        this.mUpdater.run();
    }
}
