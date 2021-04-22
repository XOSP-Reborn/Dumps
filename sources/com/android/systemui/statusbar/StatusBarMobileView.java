package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.DualToneHandler;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;

public class StatusBarMobileView extends FrameLayout implements DarkIconDispatcher.DarkReceiver, StatusIconDisplayable {
    private StatusBarIconView mDotView;
    private DualToneHandler mDualToneHandler;
    private ImageView mIn;
    private View mInoutContainer;
    private ImageView mMobile;
    private SignalDrawable mMobileDrawable;
    private LinearLayout mMobileGroup;
    private View mMobileIconSpace;
    private ImageView mMobileRoaming;
    private View mMobileRoamingSpace;
    private ImageView mMobileType;
    private ImageView mMobileVolte;
    private ImageView mOut;
    private String mSlot;
    private StatusBarSignalPolicy.MobileIconState mState;
    private int mVisibleState = -1;
    private ImageView mWifiCalling;

    public static StatusBarMobileView fromContext(Context context, String str) {
        StatusBarMobileView statusBarMobileView = (StatusBarMobileView) LayoutInflater.from(context).inflate(C0010R$layout.somc_status_bar_mobile_signal_group, (ViewGroup) null);
        statusBarMobileView.setSlot(str);
        statusBarMobileView.init();
        statusBarMobileView.setVisibleState(0);
        return statusBarMobileView;
    }

    public StatusBarMobileView(Context context) {
        super(context);
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public void getDrawingRect(Rect rect) {
        super.getDrawingRect(rect);
        float translationX = getTranslationX();
        float translationY = getTranslationY();
        rect.left = (int) (((float) rect.left) + translationX);
        rect.right = (int) (((float) rect.right) + translationX);
        rect.top = (int) (((float) rect.top) + translationY);
        rect.bottom = (int) (((float) rect.bottom) + translationY);
    }

    private void init() {
        this.mDualToneHandler = new DualToneHandler(getContext());
        this.mMobileGroup = (LinearLayout) findViewById(C0007R$id.mobile_group);
        this.mMobile = (ImageView) findViewById(C0007R$id.mobile_signal);
        this.mMobileType = (ImageView) findViewById(C0007R$id.mobile_type);
        this.mMobileRoaming = (ImageView) findViewById(C0007R$id.mobile_roaming);
        this.mMobileVolte = (ImageView) findViewById(C0007R$id.volte);
        this.mMobileRoamingSpace = findViewById(C0007R$id.mobile_roaming_space);
        this.mMobileIconSpace = findViewById(C0007R$id.mobile_icon_space);
        this.mIn = (ImageView) findViewById(C0007R$id.mobile_in);
        this.mOut = (ImageView) findViewById(C0007R$id.mobile_out);
        this.mInoutContainer = findViewById(C0007R$id.inout_container);
        this.mWifiCalling = (ImageView) findViewById(C0007R$id.wifi_calling);
        this.mMobileDrawable = new SignalDrawable(getContext());
        this.mMobile.setImageDrawable(this.mMobileDrawable);
        initDotView();
    }

    private void initDotView() {
        this.mDotView = new StatusBarIconView(((FrameLayout) this).mContext, this.mSlot, null);
        this.mDotView.setVisibleState(1);
        int dimensionPixelSize = ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(C0005R$dimen.status_bar_icon_size);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dimensionPixelSize, dimensionPixelSize);
        layoutParams.gravity = 8388627;
        addView(this.mDotView, layoutParams);
    }

    public void applyMobileState(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        boolean z = true;
        if (mobileIconState == null) {
            if (getVisibility() == 8) {
                z = false;
            }
            setVisibility(8);
            this.mState = null;
        } else {
            StatusBarSignalPolicy.MobileIconState mobileIconState2 = this.mState;
            if (mobileIconState2 == null) {
                this.mState = mobileIconState.copy();
                initViewState();
            } else {
                z = !mobileIconState2.equals(mobileIconState) ? updateState(mobileIconState.copy()) : false;
            }
        }
        if (z) {
            requestLayout();
        }
    }

    private void initViewState() {
        setContentDescription(this.mState.contentDescription);
        int i = 8;
        if (!this.mState.visible) {
            this.mMobileGroup.setVisibility(8);
        } else {
            this.mMobileGroup.setVisibility(0);
        }
        this.mMobileDrawable.setLevel(this.mState.strengthId);
        StatusBarSignalPolicy.MobileIconState mobileIconState = this.mState;
        if (mobileIconState.typeId > 0) {
            this.mMobileType.setContentDescription(mobileIconState.typeContentDescription);
            this.mMobileType.setImageResource(this.mState.typeId);
            this.mMobileType.setVisibility(0);
        } else {
            this.mMobileType.setVisibility(8);
        }
        this.mMobileRoaming.setVisibility(this.mState.roaming ? 0 : 8);
        this.mMobileRoamingSpace.setVisibility(this.mState.roaming ? 0 : 8);
        this.mMobileIconSpace.setVisibility(this.mState.mobileIconSpacerVisible ? 0 : 8);
        int i2 = 255;
        this.mIn.setImageAlpha(this.mState.activityIn ? 255 : 76);
        ImageView imageView = this.mOut;
        if (!this.mState.activityOut) {
            i2 = 76;
        }
        imageView.setImageAlpha(i2);
        this.mMobileVolte.setVisibility(this.mState.mobileVolteVisible ? 0 : 8);
        ImageView imageView2 = this.mWifiCalling;
        if (this.mState.wifiCallingVisible) {
            i = 0;
        }
        imageView2.setVisibility(i);
    }

    private boolean updateState(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        boolean z;
        setContentDescription(mobileIconState.contentDescription);
        boolean z2 = true;
        int i = 8;
        if (this.mState.visible == mobileIconState.visible && mobileIconState.wifiCallingVisible == this.mState.wifiCallingVisible) {
            z = false;
        } else {
            this.mMobileGroup.setVisibility(mobileIconState.visible ? 0 : 8);
            z = true;
        }
        int i2 = this.mState.strengthId;
        int i3 = mobileIconState.strengthId;
        if (i2 != i3) {
            this.mMobileDrawable.setLevel(i3);
        }
        int i4 = this.mState.typeId;
        int i5 = mobileIconState.typeId;
        if (i4 != i5) {
            z |= i5 == 0 || i4 == 0;
            if (mobileIconState.typeId != 0) {
                this.mMobileType.setContentDescription(mobileIconState.typeContentDescription);
                this.mMobileType.setImageResource(mobileIconState.typeId);
                this.mMobileType.setVisibility(0);
            } else {
                this.mMobileType.setVisibility(8);
            }
        }
        this.mMobileRoaming.setVisibility(mobileIconState.roaming ? 0 : 8);
        this.mMobileRoamingSpace.setVisibility(mobileIconState.roaming ? 0 : 8);
        this.mMobileIconSpace.setVisibility(mobileIconState.mobileIconSpacerVisible ? 0 : 8);
        this.mWifiCalling.setVisibility(mobileIconState.wifiCallingVisible ? 0 : 8);
        if (mobileIconState.roaming == this.mState.roaming && mobileIconState.activityIn == this.mState.activityIn && mobileIconState.activityOut == this.mState.activityOut && mobileIconState.mobileVolteVisible == this.mState.mobileVolteVisible) {
            z2 = false;
        }
        boolean z3 = z | z2;
        if (mobileIconState.wifiCallingVisible) {
            this.mWifiCalling.setImageResource(C0006R$drawable.stat_sys_vowifi);
        }
        ImageView imageView = this.mMobileVolte;
        if (mobileIconState.mobileVolteVisible) {
            i = 0;
        }
        imageView.setVisibility(i);
        if (mobileIconState.mobileVolteVisible) {
            this.mMobileVolte.setImageResource(C0006R$drawable.stat_sys_volte);
        }
        this.mState = mobileIconState;
        int i6 = 255;
        this.mIn.setImageAlpha(this.mState.activityIn ? 255 : 76);
        ImageView imageView2 = this.mOut;
        if (!this.mState.activityOut) {
            i6 = 76;
        }
        imageView2.setImageAlpha(i6);
        return z3;
    }

    @Override // com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver
    public void onDarkChanged(Rect rect, float f, int i) {
        if (!DarkIconDispatcher.isInArea(rect, this)) {
            f = 0.0f;
        }
        this.mMobileDrawable.setTintList(ColorStateList.valueOf(this.mDualToneHandler.getSingleColor(f)));
        ColorStateList valueOf = ColorStateList.valueOf(DarkIconDispatcher.getTint(rect, this, i));
        this.mIn.setImageTintList(valueOf);
        this.mOut.setImageTintList(valueOf);
        this.mMobileType.setImageTintList(valueOf);
        this.mMobileRoaming.setImageTintList(valueOf);
        this.mDotView.setDecorColor(i);
        this.mDotView.setIconColor(i, false);
        this.mWifiCalling.setImageTintList(valueOf);
        this.mMobileVolte.setImageTintList(valueOf);
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public String getSlot() {
        return this.mSlot;
    }

    public void setSlot(String str) {
        this.mSlot = str;
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public void setStaticDrawableColor(int i) {
        ColorStateList valueOf = ColorStateList.valueOf(i);
        this.mMobileDrawable.setTintList(ColorStateList.valueOf(this.mDualToneHandler.getSingleColor((i & 16777215) == 16777215 ? 0.0f : 1.0f)));
        this.mIn.setImageTintList(valueOf);
        this.mOut.setImageTintList(valueOf);
        this.mMobileType.setImageTintList(valueOf);
        this.mMobileRoaming.setImageTintList(valueOf);
        this.mDotView.setDecorColor(i);
        this.mWifiCalling.setImageTintList(valueOf);
        this.mMobileVolte.setImageTintList(valueOf);
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public void setDecorColor(int i) {
        this.mDotView.setDecorColor(i);
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public boolean isIconVisible() {
        return this.mState.visible || this.mState.wifiCallingVisible;
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public void setVisibleState(int i, boolean z) {
        if (i != this.mVisibleState) {
            this.mVisibleState = i;
            int i2 = 0;
            if (i == 0) {
                this.mMobileGroup.setVisibility(0);
                StatusBarSignalPolicy.MobileIconState mobileIconState = this.mState;
                if (mobileIconState != null) {
                    this.mMobileGroup.setVisibility(mobileIconState.visible ? 0 : 8);
                    ImageView imageView = this.mWifiCalling;
                    if (!this.mState.wifiCallingVisible) {
                        i2 = 8;
                    }
                    imageView.setVisibility(i2);
                }
                this.mDotView.setVisibility(8);
            } else if (i != 1) {
                this.mMobileGroup.setVisibility(4);
                this.mWifiCalling.setVisibility(4);
                this.mDotView.setVisibility(4);
            } else {
                this.mMobileGroup.setVisibility(4);
                this.mWifiCalling.setVisibility(4);
                this.mDotView.setVisibility(0);
            }
        }
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public int getVisibleState() {
        return this.mVisibleState;
    }

    @VisibleForTesting
    public StatusBarSignalPolicy.MobileIconState getState() {
        return this.mState;
    }

    public String toString() {
        return "StatusBarMobileView(slot=" + this.mSlot + " state=" + this.mState + ")";
    }
}
