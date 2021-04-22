package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class KeyguardStatusBarView extends RelativeLayout implements BatteryController.BatteryStateChangeCallback, UserInfoController.OnUserInfoChangedListener, ConfigurationController.ConfigurationListener {
    private boolean mBatteryCharging;
    private BatteryController mBatteryController;
    private boolean mBatteryListening;
    private BatteryMeterView mBatteryView;
    private TextView mCarrierLabel;
    private int mCarrierLabelColor = -1;
    private int mCutoutSideNudge = 0;
    private View mCutoutSpace;
    private final Rect mEmptyRect = new Rect(0, 0, 0, 0);
    private StatusBarIconController.TintedIconManager mIconManager;
    private boolean mIsInversion = false;
    private boolean mIsSkinningEnabled;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mKeyguardUserSwitcherShowing;
    private int mLayoutState = 0;
    private ImageView mMultiUserAvatar;
    private MultiUserSwitch mMultiUserSwitch;
    private boolean mShowPercentAvailable;
    private ViewGroup mStatusIconArea;
    private StatusIconContainer mStatusIconContainer;
    private int mSystemIconsBaseMargin;
    private View mSystemIconsContainer;
    private int mSystemIconsSwitcherHiddenExpandedMargin;
    private UserSwitcherController mUserSwitcherController;

    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
    public void onPowerSaveChanged(boolean z) {
    }

    public KeyguardStatusBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsSkinningEnabled = context.getResources().getBoolean(C0003R$bool.somc_keyguard_theme_enabled);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSystemIconsContainer = findViewById(C0007R$id.system_icons_container);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(C0007R$id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) findViewById(C0007R$id.multi_user_avatar);
        this.mCarrierLabel = (TextView) findViewById(C0007R$id.keyguard_carrier_text);
        this.mBatteryView = (BatteryMeterView) this.mSystemIconsContainer.findViewById(C0007R$id.battery);
        this.mCutoutSpace = findViewById(C0007R$id.cutout_space_view);
        this.mStatusIconArea = (ViewGroup) findViewById(C0007R$id.status_icon_area);
        this.mStatusIconContainer = (StatusIconContainer) findViewById(C0007R$id.statusIcons);
        loadDimens();
        updateUserSwitcher();
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mMultiUserAvatar.getLayoutParams();
        int dimensionPixelSize = getResources().getDimensionPixelSize(C0005R$dimen.multi_user_avatar_keyguard_size);
        marginLayoutParams.height = dimensionPixelSize;
        marginLayoutParams.width = dimensionPixelSize;
        this.mMultiUserAvatar.setLayoutParams(marginLayoutParams);
        ViewGroup.MarginLayoutParams marginLayoutParams2 = (ViewGroup.MarginLayoutParams) this.mMultiUserSwitch.getLayoutParams();
        marginLayoutParams2.width = getResources().getDimensionPixelSize(C0005R$dimen.multi_user_switch_width_keyguard);
        marginLayoutParams2.setMarginEnd(getResources().getDimensionPixelSize(C0005R$dimen.multi_user_switch_keyguard_margin));
        this.mMultiUserSwitch.setLayoutParams(marginLayoutParams2);
        ViewGroup.MarginLayoutParams marginLayoutParams3 = (ViewGroup.MarginLayoutParams) this.mSystemIconsContainer.getLayoutParams();
        marginLayoutParams3.setMarginStart(getResources().getDimensionPixelSize(C0005R$dimen.system_icons_super_container_margin_start));
        this.mSystemIconsContainer.setLayoutParams(marginLayoutParams3);
        View view = this.mSystemIconsContainer;
        view.setPaddingRelative(view.getPaddingStart(), this.mSystemIconsContainer.getPaddingTop(), getResources().getDimensionPixelSize(C0005R$dimen.system_icons_keyguard_padding_end), this.mSystemIconsContainer.getPaddingBottom());
        this.mCarrierLabel.setTextSize(0, (float) getResources().getDimensionPixelSize(17105455));
        ViewGroup.MarginLayoutParams marginLayoutParams4 = (ViewGroup.MarginLayoutParams) this.mCarrierLabel.getLayoutParams();
        marginLayoutParams4.setMarginStart(getResources().getDimensionPixelSize(C0005R$dimen.keyguard_carrier_text_margin));
        this.mCarrierLabel.setLayoutParams(marginLayoutParams4);
        ViewGroup.MarginLayoutParams marginLayoutParams5 = (ViewGroup.MarginLayoutParams) getLayoutParams();
        marginLayoutParams5.height = getResources().getDimensionPixelSize(C0005R$dimen.status_bar_header_height_keyguard);
        setLayoutParams(marginLayoutParams5);
    }

    private void loadDimens() {
        Resources resources = getResources();
        this.mSystemIconsSwitcherHiddenExpandedMargin = resources.getDimensionPixelSize(C0005R$dimen.system_icons_switcher_hidden_expanded_margin);
        this.mSystemIconsBaseMargin = resources.getDimensionPixelSize(C0005R$dimen.system_icons_super_container_avatarless_margin_end);
        this.mCutoutSideNudge = getResources().getDimensionPixelSize(C0005R$dimen.display_cutout_margin_consumption);
        this.mShowPercentAvailable = getContext().getResources().getBoolean(17891373);
    }

    private void updateVisibilities() {
        boolean z = false;
        if (this.mMultiUserSwitch.getParent() == this.mStatusIconArea || this.mKeyguardUserSwitcherShowing) {
            ViewParent parent = this.mMultiUserSwitch.getParent();
            ViewGroup viewGroup = this.mStatusIconArea;
            if (parent == viewGroup && this.mKeyguardUserSwitcherShowing) {
                viewGroup.removeView(this.mMultiUserSwitch);
            }
        } else {
            if (this.mMultiUserSwitch.getParent() != null) {
                getOverlay().remove(this.mMultiUserSwitch);
            }
            this.mStatusIconArea.addView(this.mMultiUserSwitch, 0);
        }
        if (this.mKeyguardUserSwitcher == null) {
            if (this.mMultiUserSwitch.isMultiUserEnabled()) {
                this.mMultiUserSwitch.setVisibility(0);
            } else {
                this.mMultiUserSwitch.setVisibility(8);
            }
        }
        BatteryMeterView batteryMeterView = this.mBatteryView;
        if (this.mBatteryCharging && this.mShowPercentAvailable) {
            z = true;
        }
        batteryMeterView.setForceShowPercent(z);
    }

    private void updateSystemIconsLayoutParams() {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mSystemIconsContainer.getLayoutParams();
        int i = this.mMultiUserSwitch.getVisibility() == 8 ? this.mSystemIconsBaseMargin : 0;
        if (this.mKeyguardUserSwitcherShowing) {
            i = this.mSystemIconsSwitcherHiddenExpandedMargin;
        }
        if (i != layoutParams.getMarginEnd()) {
            layoutParams.setMarginEnd(i);
            this.mSystemIconsContainer.setLayoutParams(layoutParams);
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mLayoutState = 0;
        if (updateLayoutConsideringCutout()) {
            requestLayout();
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    private boolean updateLayoutConsideringCutout() {
        DisplayCutout displayCutout = getRootWindowInsets().getDisplayCutout();
        Pair<Integer, Integer> cornerCutoutMargins = PhoneStatusBarView.cornerCutoutMargins(displayCutout, getDisplay());
        updateCornerCutoutPadding(cornerCutoutMargins);
        if (displayCutout == null || cornerCutoutMargins != null) {
            return updateLayoutParamsNoCutout();
        }
        return updateLayoutParamsForCutout(displayCutout);
    }

    private void updateCornerCutoutPadding(Pair<Integer, Integer> pair) {
        if (pair != null) {
            setPadding(((Integer) pair.first).intValue(), 0, ((Integer) pair.second).intValue(), 0);
        } else {
            setPadding(0, 0, 0, 0);
        }
    }

    private boolean updateLayoutParamsNoCutout() {
        if (this.mLayoutState == 2) {
            return false;
        }
        this.mLayoutState = 2;
        View view = this.mCutoutSpace;
        if (view != null) {
            view.setVisibility(8);
        }
        ((RelativeLayout.LayoutParams) this.mCarrierLabel.getLayoutParams()).addRule(16, C0007R$id.status_icon_area);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mStatusIconArea.getLayoutParams();
        layoutParams.removeRule(1);
        layoutParams.width = -2;
        ((LinearLayout.LayoutParams) this.mSystemIconsContainer.getLayoutParams()).setMarginStart(getResources().getDimensionPixelSize(C0005R$dimen.system_icons_super_container_margin_start));
        return true;
    }

    private boolean updateLayoutParamsForCutout(DisplayCutout displayCutout) {
        if (this.mLayoutState == 1) {
            return false;
        }
        this.mLayoutState = 1;
        if (this.mCutoutSpace == null) {
            updateLayoutParamsNoCutout();
        }
        Rect rect = new Rect();
        ScreenDecorations.DisplayCutoutView.boundsFromDirection(displayCutout, 48, rect);
        this.mCutoutSpace.setVisibility(0);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mCutoutSpace.getLayoutParams();
        int i = rect.left;
        int i2 = this.mCutoutSideNudge;
        rect.left = i + i2;
        rect.right -= i2;
        layoutParams.width = rect.width();
        layoutParams.height = rect.height();
        layoutParams.addRule(13);
        ((RelativeLayout.LayoutParams) this.mCarrierLabel.getLayoutParams()).addRule(16, C0007R$id.cutout_space_view);
        RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) this.mStatusIconArea.getLayoutParams();
        layoutParams2.addRule(1, C0007R$id.cutout_space_view);
        layoutParams2.width = -1;
        ((LinearLayout.LayoutParams) this.mSystemIconsContainer.getLayoutParams()).setMarginStart(0);
        return true;
    }

    public void setListening(boolean z) {
        if (z != this.mBatteryListening) {
            this.mBatteryListening = z;
            if (this.mBatteryListening) {
                this.mBatteryController.addCallback(this);
            } else {
                this.mBatteryController.removeCallback(this);
            }
        }
    }

    private void updateUserSwitcher() {
        boolean z = this.mKeyguardUserSwitcher != null;
        this.mMultiUserSwitch.setClickable(z);
        this.mMultiUserSwitch.setFocusable(z);
        this.mMultiUserSwitch.setKeyguardMode(z);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        UserInfoController userInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        userInfoController.addCallback(this);
        this.mUserSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        this.mMultiUserSwitch.setUserSwitcherController(this.mUserSwitcherController);
        userInfoController.reloadUserInfo();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mIconManager = new StatusBarIconController.TintedIconManager((ViewGroup) findViewById(C0007R$id.statusIcons));
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mIconManager);
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((UserInfoController) Dependency.get(UserInfoController.class)).removeCallback(this);
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mIconManager);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    @Override // com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener
    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        this.mMultiUserAvatar.setImageDrawable(drawable);
    }

    public void setQSPanel(QSPanel qSPanel) {
        this.mMultiUserSwitch.setQsPanel(qSPanel);
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        if (this.mBatteryCharging != z2) {
            this.mBatteryCharging = z2;
            updateVisibilities();
        }
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        this.mMultiUserSwitch.setKeyguardUserSwitcher(keyguardUserSwitcher);
        updateUserSwitcher();
    }

    public void setKeyguardUserSwitcherShowing(boolean z, boolean z2) {
        this.mKeyguardUserSwitcherShowing = z;
        if (z2) {
            animateNextLayoutChange();
        }
        updateVisibilities();
        updateLayoutConsideringCutout();
        updateSystemIconsLayoutParams();
    }

    private void animateNextLayoutChange() {
        final int left = this.mSystemIconsContainer.getLeft();
        final boolean z = this.mMultiUserSwitch.getParent() == this.mStatusIconArea;
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            /* class com.android.systemui.statusbar.phone.KeyguardStatusBarView.AnonymousClass1 */

            public boolean onPreDraw() {
                KeyguardStatusBarView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                boolean z = z && KeyguardStatusBarView.this.mMultiUserSwitch.getParent() != KeyguardStatusBarView.this.mStatusIconArea;
                KeyguardStatusBarView.this.mSystemIconsContainer.setX((float) left);
                KeyguardStatusBarView.this.mSystemIconsContainer.animate().translationX(0.0f).setDuration(400).setStartDelay(z ? 300 : 0).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).start();
                if (z) {
                    KeyguardStatusBarView.this.getOverlay().add(KeyguardStatusBarView.this.mMultiUserSwitch);
                    KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(0.0f).setDuration(300).setStartDelay(0).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
                        /* class com.android.systemui.statusbar.phone.$$Lambda$KeyguardStatusBarView$1$DyabYtIeJMptnepd5jqXSnZ7UZ0 */

                        public final void run() {
                            KeyguardStatusBarView.AnonymousClass1.this.lambda$onPreDraw$0$KeyguardStatusBarView$1();
                        }
                    }).start();
                } else {
                    KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(0.0f);
                    KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(1.0f).setDuration(300).setStartDelay(200).setInterpolator(Interpolators.ALPHA_IN);
                }
                return true;
            }

            public /* synthetic */ void lambda$onPreDraw$0$KeyguardStatusBarView$1() {
                KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(1.0f);
                KeyguardStatusBarView.this.getOverlay().remove(KeyguardStatusBarView.this.mMultiUserSwitch);
            }
        });
    }

    public void setVisibility(int i) {
        super.setVisibility(i);
        if (i != 0) {
            this.mSystemIconsContainer.animate().cancel();
            this.mSystemIconsContainer.setTranslationX(0.0f);
            this.mMultiUserSwitch.animate().cancel();
            this.mMultiUserSwitch.setAlpha(1.0f);
            return;
        }
        updateVisibilities();
        updateSystemIconsLayoutParams();
    }

    public void updateSkinnedResources(Resources resources) {
        this.mIsInversion = resources.getBoolean(C0003R$bool.somc_keyguard_statusbar_inversion);
        this.mCarrierLabelColor = resources.getColor(C0004R$color.somc_keyguard_theme_color_secondary_text);
        onThemeChanged();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onThemeChanged() {
        this.mBatteryView.setColorsFromContext(((RelativeLayout) this).mContext);
        updateIconsAndTextColors();
        ((UserInfoControllerImpl) Dependency.get(UserInfoController.class)).onDensityOrFontScaleChanged();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        loadDimens();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onOverlayChanged() {
        this.mCarrierLabel.setTextAppearance(Utils.getThemeAttr(((RelativeLayout) this).mContext, 16842818));
        onThemeChanged();
        this.mBatteryView.updatePercentView();
    }

    private void updateIconsAndTextColors() {
        int i;
        int colorAttrDefaultColor = Utils.getColorAttrDefaultColor(((RelativeLayout) this).mContext, C0002R$attr.wallpaperTextColor);
        if (this.mIsSkinningEnabled) {
            colorAttrDefaultColor = this.mIsInversion ? -16777216 : -1;
        }
        Context context = ((RelativeLayout) this).mContext;
        if (((double) Color.luminance(colorAttrDefaultColor)) < 0.5d) {
            i = C0004R$color.dark_mode_icon_color_single_tone;
        } else {
            i = C0004R$color.light_mode_icon_color_single_tone;
        }
        int colorStateListDefaultColor = Utils.getColorStateListDefaultColor(context, i);
        float f = colorAttrDefaultColor == -1 ? 0.0f : 1.0f;
        if (this.mIsSkinningEnabled) {
            this.mCarrierLabel.setTextColor(this.mCarrierLabelColor);
        } else {
            this.mCarrierLabel.setTextColor(colorStateListDefaultColor);
        }
        this.mCarrierLabel.setTextColor(colorStateListDefaultColor);
        StatusBarIconController.TintedIconManager tintedIconManager = this.mIconManager;
        if (tintedIconManager != null) {
            tintedIconManager.setTint(colorStateListDefaultColor);
        }
        applyDarkness(C0007R$id.battery, this.mEmptyRect, f, colorStateListDefaultColor);
        applyDarkness(C0007R$id.clock, this.mEmptyRect, f, colorStateListDefaultColor);
    }

    private void applyDarkness(int i, Rect rect, float f, int i2) {
        View findViewById = findViewById(i);
        if (findViewById instanceof DarkIconDispatcher.DarkReceiver) {
            ((DarkIconDispatcher.DarkReceiver) findViewById).onDarkChanged(rect, f, i2);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("KeyguardStatusBarView:");
        printWriter.println("  mBatteryCharging: " + this.mBatteryCharging);
        printWriter.println("  mKeyguardUserSwitcherShowing: " + this.mKeyguardUserSwitcherShowing);
        printWriter.println("  mBatteryListening: " + this.mBatteryListening);
        printWriter.println("  mLayoutState: " + this.mLayoutState);
        BatteryMeterView batteryMeterView = this.mBatteryView;
        if (batteryMeterView != null) {
            batteryMeterView.dump(fileDescriptor, printWriter, strArr);
        }
    }
}
