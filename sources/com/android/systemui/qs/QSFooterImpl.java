package com.android.systemui.qs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.Utils;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.drawable.UserIconDrawable;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.phone.MultiUserSwitch;
import com.android.systemui.statusbar.phone.SettingsButton;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.tuner.TunerService;
import com.sonymobile.systemui.emergencymode.EmergencyModeStatus;

public class QSFooterImpl extends FrameLayout implements QSFooter, View.OnClickListener, UserInfoController.OnUserInfoChangedListener {
    private View mActionsContainer;
    private final ActivityStarter mActivityStarter;
    private final ContentObserver mDeveloperSettingsObserver;
    private final DeviceProvisionedController mDeviceProvisionedController;
    private View mDragHandle;
    protected View mEdit;
    protected View mEditContainer;
    private View.OnClickListener mExpandClickListener;
    private boolean mExpanded;
    private float mExpansionAmount;
    protected TouchAnimator mFooterAnimator;
    private boolean mListening;
    private ImageView mMultiUserAvatar;
    protected MultiUserSwitch mMultiUserSwitch;
    private PageIndicator mPageIndicator;
    private boolean mQsDisabled;
    private QSPanel mQsPanel;
    private SettingsButton mSettingsButton;
    private TouchAnimator mSettingsCogAnimator;
    protected View mSettingsContainer;
    private final UserInfoController mUserInfoController;

    static /* synthetic */ void lambda$onClick$4() {
    }

    public QSFooterImpl(Context context, AttributeSet attributeSet, ActivityStarter activityStarter, UserInfoController userInfoController, DeviceProvisionedController deviceProvisionedController) {
        super(context, attributeSet);
        this.mDeveloperSettingsObserver = new ContentObserver(new Handler(((FrameLayout) this).mContext.getMainLooper())) {
            /* class com.android.systemui.qs.QSFooterImpl.AnonymousClass1 */

            public void onChange(boolean z, Uri uri) {
                super.onChange(z, uri);
                QSFooterImpl.this.setBuildText();
            }
        };
        this.mActivityStarter = activityStarter;
        this.mUserInfoController = userInfoController;
        this.mDeviceProvisionedController = deviceProvisionedController;
    }

    public QSFooterImpl(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, (ActivityStarter) Dependency.get(ActivityStarter.class), (UserInfoController) Dependency.get(UserInfoController.class), (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mEdit = findViewById(16908291);
        this.mEdit.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.qs.$$Lambda$QSFooterImpl$3QBg0cgvu2IRpUDq3RvpL257x8c */

            public final void onClick(View view) {
                QSFooterImpl.this.lambda$onFinishInflate$1$QSFooterImpl(view);
            }
        });
        this.mPageIndicator = (PageIndicator) findViewById(C0007R$id.footer_page_indicator);
        this.mSettingsButton = (SettingsButton) findViewById(C0007R$id.settings_button);
        this.mSettingsContainer = findViewById(C0007R$id.settings_button_container);
        this.mSettingsButton.setOnClickListener(this);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(C0007R$id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) this.mMultiUserSwitch.findViewById(C0007R$id.multi_user_avatar);
        this.mDragHandle = findViewById(C0007R$id.qs_drag_handle_view);
        this.mActionsContainer = findViewById(C0007R$id.qs_footer_actions_container);
        this.mEditContainer = findViewById(C0007R$id.qs_footer_actions_edit_container);
        ((RippleDrawable) this.mSettingsButton.getBackground()).setForceSoftware(true);
        updateResources();
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            /* class com.android.systemui.qs.$$Lambda$QSFooterImpl$GSAG9gEF755NpvH4khVvAa75uPs */

            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                QSFooterImpl.this.lambda$onFinishInflate$2$QSFooterImpl(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        setImportantForAccessibility(1);
        updateEverything();
        setBuildText();
    }

    public /* synthetic */ void lambda$onFinishInflate$1$QSFooterImpl(View view) {
        this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable(view) {
            /* class com.android.systemui.qs.$$Lambda$QSFooterImpl$BPGtDaa2eUtTCTVDpjGrKOXYOs */
            private final /* synthetic */ View f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                QSFooterImpl.this.lambda$onFinishInflate$0$QSFooterImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$onFinishInflate$0$QSFooterImpl(View view) {
        this.mQsPanel.showEdit(view);
    }

    public /* synthetic */ void lambda$onFinishInflate$2$QSFooterImpl(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updateAnimator(i3 - i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setBuildText() {
        TextView textView = (TextView) findViewById(C0007R$id.build);
        if (textView != null) {
            if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(((FrameLayout) this).mContext)) {
                textView.setText(((FrameLayout) this).mContext.getString(17039649, Build.VERSION.RELEASE, Build.ID));
                textView.setVisibility(0);
                return;
            }
            textView.setVisibility(8);
        }
    }

    private void updateAnimator(int i) {
        int numQuickTiles = QuickQSPanel.getNumQuickTiles(((FrameLayout) this).mContext);
        int dimensionPixelSize = (i - ((((FrameLayout) this).mContext.getResources().getDimensionPixelSize(C0005R$dimen.qs_quick_tile_size) - ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(C0005R$dimen.qs_quick_tile_padding)) * numQuickTiles)) / (numQuickTiles - 1);
        int dimensionPixelOffset = ((FrameLayout) this).mContext.getResources().getDimensionPixelOffset(C0005R$dimen.default_gear_space);
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        View view = this.mSettingsContainer;
        float[] fArr = new float[2];
        int i2 = dimensionPixelSize - dimensionPixelOffset;
        if (!isLayoutRtl()) {
            i2 = -i2;
        }
        fArr[0] = (float) i2;
        fArr[1] = 0.0f;
        builder.addFloat(view, "translationX", fArr);
        builder.addFloat(this.mSettingsButton, "rotation", -120.0f, 0.0f);
        this.mSettingsCogAnimator = builder.build();
        setExpansion(this.mExpansionAmount);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateResources();
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        updateResources();
    }

    private void updateResources() {
        updateFooterAnimator();
    }

    private void updateFooterAnimator() {
        this.mFooterAnimator = createFooterAnimator();
    }

    private TouchAnimator createFooterAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        builder.addFloat(this.mActionsContainer, "alpha", 0.0f, 1.0f);
        builder.addFloat(this.mEditContainer, "alpha", 0.0f, 1.0f);
        builder.addFloat(this.mDragHandle, "alpha", 1.0f, 0.0f, 0.0f);
        builder.addFloat(this.mPageIndicator, "alpha", 0.0f, 1.0f);
        builder.setStartDelay(0.15f);
        return builder.build();
    }

    @Override // com.android.systemui.qs.QSFooter
    public void setKeyguardShowing(boolean z) {
        setExpansion(this.mExpansionAmount);
    }

    @Override // com.android.systemui.qs.QSFooter
    public void setExpandClickListener(View.OnClickListener onClickListener) {
        this.mExpandClickListener = onClickListener;
    }

    @Override // com.android.systemui.qs.QSFooter
    public void setExpanded(boolean z) {
        if (this.mExpanded != z) {
            this.mExpanded = z;
            updateEverything();
        }
    }

    @Override // com.android.systemui.qs.QSFooter
    public void setExpansion(float f) {
        this.mExpansionAmount = f;
        TouchAnimator touchAnimator = this.mSettingsCogAnimator;
        if (touchAnimator != null) {
            touchAnimator.setPosition(f);
        }
        TouchAnimator touchAnimator2 = this.mFooterAnimator;
        if (touchAnimator2 != null) {
            touchAnimator2.setPosition(f);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((FrameLayout) this).mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("development_settings_enabled"), false, this.mDeveloperSettingsObserver, -1);
    }

    public void onDetachedFromWindow() {
        setListening(false);
        ((FrameLayout) this).mContext.getContentResolver().unregisterContentObserver(this.mDeveloperSettingsObserver);
        super.onDetachedFromWindow();
    }

    @Override // com.android.systemui.qs.QSFooter
    public void setListening(boolean z) {
        if (z != this.mListening) {
            this.mListening = z;
            updateListeners();
        }
    }

    public boolean performAccessibilityAction(int i, Bundle bundle) {
        View.OnClickListener onClickListener;
        if (i != 262144 || (onClickListener = this.mExpandClickListener) == null) {
            return super.performAccessibilityAction(i, bundle);
        }
        onClickListener.onClick(null);
        return true;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
    }

    @Override // com.android.systemui.qs.QSFooter
    public void disable(int i, int i2, boolean z) {
        boolean z2 = true;
        if ((i2 & 1) == 0) {
            z2 = false;
        }
        if (z2 != this.mQsDisabled) {
            this.mQsDisabled = z2;
            updateEverything();
        }
    }

    public void updateEverything() {
        post(new Runnable() {
            /* class com.android.systemui.qs.$$Lambda$QSFooterImpl$FK1In3zY3ppRrcllMggnruYa_s */

            public final void run() {
                QSFooterImpl.this.lambda$updateEverything$3$QSFooterImpl();
            }
        });
    }

    public /* synthetic */ void lambda$updateEverything$3$QSFooterImpl() {
        updateVisibilities();
        updateClickabilities();
        setClickable(false);
    }

    private void updateClickabilities() {
        MultiUserSwitch multiUserSwitch = this.mMultiUserSwitch;
        boolean z = true;
        multiUserSwitch.setClickable(multiUserSwitch.getVisibility() == 0);
        View view = this.mEdit;
        view.setClickable(view.getVisibility() == 0);
        SettingsButton settingsButton = this.mSettingsButton;
        if (settingsButton.getVisibility() != 0) {
            z = false;
        }
        settingsButton.setClickable(z);
    }

    private void updateVisibilities() {
        boolean isEmergencyModeOn = EmergencyModeStatus.isEmergencyModeOn(((FrameLayout) this).mContext);
        int i = 0;
        this.mSettingsContainer.setVisibility(this.mQsDisabled ? 8 : !isEmergencyModeOn ? 0 : 4);
        this.mSettingsContainer.findViewById(C0007R$id.tuner_icon).setVisibility(TunerService.isTunerEnabled(((FrameLayout) this).mContext) ? 0 : 4);
        boolean isDeviceInDemoMode = UserManager.isDeviceInDemoMode(((FrameLayout) this).mContext);
        this.mMultiUserSwitch.setVisibility((!showUserSwitcher() || isEmergencyModeOn) ? 4 : 0);
        this.mEditContainer.setVisibility((isDeviceInDemoMode || !this.mExpanded || isEmergencyModeOn) ? 4 : 0);
        SettingsButton settingsButton = this.mSettingsButton;
        if (isDeviceInDemoMode || !this.mExpanded || isEmergencyModeOn) {
            i = 4;
        }
        settingsButton.setVisibility(i);
    }

    private boolean showUserSwitcher() {
        return this.mExpanded && this.mMultiUserSwitch.isMultiUserEnabled();
    }

    private void updateListeners() {
        if (this.mListening) {
            this.mUserInfoController.addCallback(this);
        } else {
            this.mUserInfoController.removeCallback(this);
        }
    }

    @Override // com.android.systemui.qs.QSFooter
    public void setQSPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
        if (this.mQsPanel != null) {
            this.mMultiUserSwitch.setQsPanel(qSPanel);
            this.mQsPanel.setFooterPageIndicator(this.mPageIndicator);
        }
    }

    public void onClick(View view) {
        if (!this.mExpanded || view != this.mSettingsButton) {
            return;
        }
        if (!this.mDeviceProvisionedController.isCurrentUserSetup()) {
            this.mActivityStarter.postQSRunnableDismissingKeyguard($$Lambda$QSFooterImpl$ORlOcuwnOcEc1bdhJcTagEFJfI4.INSTANCE);
            return;
        }
        MetricsLogger.action(((FrameLayout) this).mContext, this.mExpanded ? 406 : 490);
        if (this.mSettingsButton.isTunerClick()) {
            this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable() {
                /* class com.android.systemui.qs.$$Lambda$QSFooterImpl$QqFCwKmpQEaqoIsbaA3_odDeJWo */

                public final void run() {
                    QSFooterImpl.this.lambda$onClick$6$QSFooterImpl();
                }
            });
        } else {
            lambda$onClick$5$QSFooterImpl();
        }
    }

    public /* synthetic */ void lambda$onClick$6$QSFooterImpl() {
        if (TunerService.isTunerEnabled(((FrameLayout) this).mContext)) {
            TunerService.showResetRequest(((FrameLayout) this).mContext, new Runnable() {
                /* class com.android.systemui.qs.$$Lambda$QSFooterImpl$p6Eelc3uV5Rv_Va6Mn0QpjivHN4 */

                public final void run() {
                    QSFooterImpl.this.lambda$onClick$5$QSFooterImpl();
                }
            });
        } else {
            Toast.makeText(getContext(), C0014R$string.tuner_toast, 1).show();
            TunerService.setTunerEnabled(((FrameLayout) this).mContext, true);
        }
        lambda$onClick$5$QSFooterImpl();
    }

    /* access modifiers changed from: private */
    /* renamed from: startSettingsActivity */
    public void lambda$onClick$5$QSFooterImpl() {
        this.mActivityStarter.startActivity(new Intent("android.settings.SETTINGS"), true);
    }

    @Override // com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener
    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        if (drawable != null && UserManager.get(((FrameLayout) this).mContext).isGuestUser(KeyguardUpdateMonitor.getCurrentUser()) && !(drawable instanceof UserIconDrawable)) {
            drawable = drawable.getConstantState().newDrawable(((FrameLayout) this).mContext.getResources()).mutate();
            drawable.setColorFilter(Utils.getColorAttrDefaultColor(((FrameLayout) this).mContext, 16842800), PorterDuff.Mode.SRC_IN);
        }
        this.mMultiUserAvatar.setImageDrawable(drawable);
    }
}
