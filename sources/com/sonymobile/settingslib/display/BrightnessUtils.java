package com.sonymobile.settingslib.display;

import android.content.res.Resources;
import android.util.MathUtils;
import com.android.settingslib.R$dimen;

public class BrightnessUtils {
    private final float mGamma;
    private final float mIntercept;
    private final float mSlope;

    public BrightnessUtils(Resources resources) {
        this.mSlope = resources.getFloat(R$dimen.config_brightnessUtilsSlope);
        this.mIntercept = resources.getFloat(R$dimen.config_brightnessUtilsIntercept);
        this.mGamma = resources.getFloat(R$dimen.config_brightnessUtilsGamma);
    }

    public int convertGammaToLinear(int i, int i2, int i3) {
        return MathUtils.constrain(Math.round((this.mSlope * MathUtils.pow((float) MathUtils.constrain(i, 0, 1023), this.mGamma)) + this.mIntercept), i2, i3);
    }

    public int convertLinearToGamma(int i, int i2, int i3) {
        return MathUtils.constrain(Math.round(MathUtils.pow((((float) MathUtils.constrain(i, i2, i3)) - this.mIntercept) / this.mSlope, 1.0f / this.mGamma)), 0, 1023);
    }
}
