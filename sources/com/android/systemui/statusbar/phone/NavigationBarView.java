package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.C0015R$style;
import com.android.systemui.Dependency;
import com.android.systemui.DockedStackExistsListener;
import com.android.systemui.Interpolators;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsOnboarding;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.sonymobile.runtimeskinning.SkinningBridge;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class NavigationBarView extends FrameLayout implements NavigationModeController.ModeChangedListener {
    private final Region mActiveRegion = new Region();
    private Rect mBackButtonBounds = new Rect();
    private KeyButtonDrawable mBackIcon;
    private final NavigationBarTransitions mBarTransitions;
    private final SparseArray<ButtonDispatcher> mButtonDispatchers;
    private View mCenterGroup;
    private Configuration mConfiguration;
    private final ContextualButtonGroup mContextualButtonGroup;
    private int mCurrentRotation = -1;
    View mCurrentView = null;
    private final DeadZone mDeadZone;
    private boolean mDeadZoneConsuming = false;
    int mDisabledFlags = 0;
    private KeyButtonDrawable mDockedIcon;
    private final Consumer<Boolean> mDockedListener;
    private boolean mDockedStackExists;
    private final EdgeBackGestureHandler mEdgeBackGestureHandler;
    private View mEndsGroup;
    private FloatingRotationButton mFloatingRotationButton;
    private Rect mHomeButtonBounds = new Rect();
    private KeyButtonDrawable mHomeDefaultIcon;
    private View mHorizontal;
    private final View.OnClickListener mImeSwitcherClickListener;
    private boolean mImeVisible;
    private boolean mInCarMode;
    private final boolean mIsEmergencyModeOn;
    private boolean mIsVertical;
    private boolean mLayoutTransitionsEnabled;
    private NavigationBarLockController mLockController;
    boolean mLongClickableAccessibilityButton;
    private int mNavBarMode = 0;
    private final int mNavigationBarPaddingX;
    private final int mNavigationBarPaddingY;
    int mNavigationIconHints = 0;
    private NavigationBarInflaterView mNavigationInflaterView;
    private final ViewTreeObserver.OnComputeInternalInsetsListener mOnComputeInternalInsetsListener;
    private OnVerticalChangedListener mOnVerticalChangedListener;
    private final OverviewProxyService mOverviewProxyService;
    private NotificationPanelView mPanelView;
    private final View.AccessibilityDelegate mQuickStepAccessibilityDelegate;
    private KeyButtonDrawable mRecentIcon;
    private Rect mRecentsButtonBounds = new Rect();
    private RecentsOnboarding mRecentsOnboarding;
    private Rect mRotationButtonBounds = new Rect();
    private RotationButtonController mRotationButtonController;
    private ScreenPinningNotify mScreenPinningNotify;
    private NavBarTintController mTintController;
    private Configuration mTmpLastConfiguration;
    private int[] mTmpPosition = new int[2];
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();
    private final View.OnClickListener mUnlockClickListener;
    private KeyButtonDrawable mUnlockIcon;
    private boolean mUseCarModeUi;
    private View mVertical;
    private boolean mWakeAndUnlocking;

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean z);
    }

    private static String visibilityToString(int i) {
        return i != 4 ? i != 8 ? "VISIBLE" : "GONE" : "INVISIBLE";
    }

    /* access modifiers changed from: private */
    public class NavTransitionListener implements LayoutTransition.TransitionListener {
        private boolean mBackTransitioning;
        private long mDuration;
        private boolean mHomeAppearing;
        private TimeInterpolator mInterpolator;
        private long mStartDelay;

        private NavTransitionListener() {
        }

        public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            if (view.getId() == C0007R$id.back) {
                this.mBackTransitioning = true;
            } else if (view.getId() == C0007R$id.home && i == 2) {
                this.mHomeAppearing = true;
                this.mStartDelay = layoutTransition.getStartDelay(i);
                this.mDuration = layoutTransition.getDuration(i);
                this.mInterpolator = layoutTransition.getInterpolator(i);
            }
        }

        public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            if (view.getId() == C0007R$id.back) {
                this.mBackTransitioning = false;
            } else if (view.getId() == C0007R$id.home && i == 2) {
                this.mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            ButtonDispatcher backButton = NavigationBarView.this.getBackButton();
            if (!this.mBackTransitioning && backButton.getVisibility() == 0 && this.mHomeAppearing && NavigationBarView.this.getHomeButton().getAlpha() == 0.0f) {
                NavigationBarView.this.getBackButton().setAlpha(0.0f);
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(backButton, "alpha", 0.0f, 1.0f);
                ofFloat.setStartDelay(this.mStartDelay);
                ofFloat.setDuration(this.mDuration);
                ofFloat.setInterpolator(this.mInterpolator);
                ofFloat.start();
            }
        }
    }

    public /* synthetic */ void lambda$new$0$NavigationBarView(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
        if (!QuickStepContract.isGesturalMode(this.mNavBarMode) || this.mImeVisible) {
            internalInsetsInfo.setTouchableInsets(0);
            return;
        }
        internalInsetsInfo.setTouchableInsets(3);
        ButtonDispatcher imeSwitchButton = getImeSwitchButton();
        if (imeSwitchButton.getVisibility() == 0) {
            int[] iArr = new int[2];
            View currentView = imeSwitchButton.getCurrentView();
            currentView.getLocationInWindow(iArr);
            internalInsetsInfo.touchableRegion.set(iArr[0], iArr[1], iArr[0] + currentView.getWidth(), iArr[1] + currentView.getHeight());
            return;
        }
        internalInsetsInfo.touchableRegion.setEmpty();
    }

    public NavigationBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        boolean z = true;
        this.mLayoutTransitionsEnabled = true;
        this.mUseCarModeUi = false;
        this.mInCarMode = false;
        this.mButtonDispatchers = new SparseArray<>();
        this.mImeSwitcherClickListener = new View.OnClickListener() {
            /* class com.android.systemui.statusbar.phone.NavigationBarView.AnonymousClass1 */

            public void onClick(View view) {
                ((InputMethodManager) ((FrameLayout) NavigationBarView.this).mContext.getSystemService(InputMethodManager.class)).showInputMethodPickerFromSystem(true, NavigationBarView.this.getContext().getDisplayId());
            }
        };
        this.mUnlockClickListener = new View.OnClickListener() {
            /* class com.android.systemui.statusbar.phone.NavigationBarView.AnonymousClass2 */

            public void onClick(View view) {
                NavigationBarView.this.mLockController.unlock();
            }
        };
        this.mQuickStepAccessibilityDelegate = new View.AccessibilityDelegate() {
            /* class com.android.systemui.statusbar.phone.NavigationBarView.AnonymousClass3 */
            private AccessibilityNodeInfo.AccessibilityAction mToggleOverviewAction;

            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                if (this.mToggleOverviewAction == null) {
                    this.mToggleOverviewAction = new AccessibilityNodeInfo.AccessibilityAction(C0007R$id.action_toggle_overview, NavigationBarView.this.getContext().getString(C0014R$string.quick_step_accessibility_toggle_overview));
                }
                accessibilityNodeInfo.addAction(this.mToggleOverviewAction);
            }

            public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                if (i != C0007R$id.action_toggle_overview) {
                    return super.performAccessibilityAction(view, i, bundle);
                }
                ((Recents) SysUiServiceProvider.getComponent(NavigationBarView.this.getContext(), Recents.class)).toggleRecentApps();
                return true;
            }
        };
        this.mOnComputeInternalInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarView$khIxhJwBd7pJnFFXnq8zupcHrv8 */

            public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                NavigationBarView.this.lambda$new$0$NavigationBarView(internalInsetsInfo);
            }
        };
        this.mDockedListener = new Consumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarView$3_rm_LYAhHXvCBhrsX10ry5w8OA */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                NavigationBarView.this.lambda$new$2$NavigationBarView((Boolean) obj);
            }
        };
        this.mIsVertical = false;
        this.mLongClickableAccessibilityButton = false;
        this.mNavBarMode = ((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(this);
        boolean isGesturalMode = QuickStepContract.isGesturalMode(this.mNavBarMode);
        this.mContextualButtonGroup = new ContextualButtonGroup(C0007R$id.menu_container);
        GameButton gameButton = new GameButton(C0007R$id.game_button, new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$WrUd8iBVzCnkNGlDjVh6Yvbf6CM */

            public final void run() {
                NavigationBarView.this.updateStates();
            }
        });
        this.mContextualButtonGroup.addButton(gameButton);
        this.mButtonDispatchers.put(C0007R$id.game_button, gameButton);
        ContextualButton contextualButton = new ContextualButton(C0007R$id.ime_switcher, C0006R$drawable.ic_ime_switcher_default);
        RotationContextButton rotationContextButton = new RotationContextButton(C0007R$id.rotate_suggestion, C0006R$drawable.ic_sysbar_rotate_button);
        ContextualButton contextualButton2 = new ContextualButton(C0007R$id.accessibility_button, C0006R$drawable.ic_sysbar_accessibility_button);
        this.mContextualButtonGroup.addButton(contextualButton);
        if (!isGesturalMode) {
            this.mContextualButtonGroup.addButton(rotationContextButton);
        }
        this.mContextualButtonGroup.addButton(contextualButton2);
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mRecentsOnboarding = new RecentsOnboarding(context, this.mOverviewProxyService);
        this.mFloatingRotationButton = new FloatingRotationButton(context);
        this.mRotationButtonController = new RotationButtonController(context, C0015R$style.RotateButtonCCWStart90, isGesturalMode ? this.mFloatingRotationButton : rotationContextButton);
        ContextualButton contextualButton3 = new ContextualButton(C0007R$id.back, 0);
        this.mConfiguration = new Configuration();
        this.mTmpLastConfiguration = new Configuration();
        this.mConfiguration.updateFrom(context.getResources().getConfiguration());
        this.mScreenPinningNotify = new ScreenPinningNotify(((FrameLayout) this).mContext);
        this.mBarTransitions = new NavigationBarTransitions(this);
        this.mButtonDispatchers.put(C0007R$id.back, contextualButton3);
        SparseArray<ButtonDispatcher> sparseArray = this.mButtonDispatchers;
        int i = C0007R$id.home;
        sparseArray.put(i, new ButtonDispatcher(i));
        SparseArray<ButtonDispatcher> sparseArray2 = this.mButtonDispatchers;
        int i2 = C0007R$id.home_handle;
        sparseArray2.put(i2, new ButtonDispatcher(i2));
        SparseArray<ButtonDispatcher> sparseArray3 = this.mButtonDispatchers;
        int i3 = C0007R$id.recent_apps;
        sparseArray3.put(i3, new ButtonDispatcher(i3));
        this.mButtonDispatchers.put(C0007R$id.ime_switcher, contextualButton);
        this.mButtonDispatchers.put(C0007R$id.accessibility_button, contextualButton2);
        this.mButtonDispatchers.put(C0007R$id.rotate_suggestion, rotationContextButton);
        this.mButtonDispatchers.put(C0007R$id.menu_container, this.mContextualButtonGroup);
        this.mDeadZone = new DeadZone(this);
        this.mEdgeBackGestureHandler = new EdgeBackGestureHandler(context, this.mOverviewProxyService);
        this.mTintController = new NavBarTintController(this, getLightTransitionsController());
        this.mNavigationBarPaddingX = context.getResources().getDimensionPixelSize(C0005R$dimen.navigationbar_burn_in_prevention_padding_x_max);
        this.mNavigationBarPaddingY = context.getResources().getDimensionPixelSize(C0005R$dimen.navigationbar_burn_in_prevention_padding_y_max);
        SparseArray<ButtonDispatcher> sparseArray4 = this.mButtonDispatchers;
        int i4 = C0007R$id.unlock;
        sparseArray4.put(i4, new ButtonDispatcher(i4));
        this.mIsEmergencyModeOn = Settings.Secure.getInt(context.getContentResolver(), "somc.emergency_mode", 0) == 0 ? false : z;
    }

    public NavBarTintController getTintController() {
        return this.mTintController;
    }

    public NavigationBarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public LightBarTransitionsController getLightTransitionsController() {
        return this.mBarTransitions.getLightTransitionsController();
    }

    public void setComponents(NotificationPanelView notificationPanelView, AssistManager assistManager) {
        this.mPanelView = notificationPanelView;
        updateSystemUiStateFlags();
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        this.mTintController.onDraw();
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        this.mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(this.mIsVertical);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        this.mLockController.onTouchEvent(motionEvent);
        return shouldDeadZoneConsumeTouchEvents(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        shouldDeadZoneConsumeTouchEvents(motionEvent);
        return super.onTouchEvent(motionEvent);
    }

    /* access modifiers changed from: package-private */
    public void onBarTransition(int i) {
        if (i == 0) {
            this.mTintController.stop();
            getLightTransitionsController().setIconsDark(false, true);
            return;
        }
        this.mTintController.start();
    }

    private boolean shouldDeadZoneConsumeTouchEvents(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mDeadZoneConsuming = false;
        }
        if (!this.mDeadZone.onTouchEvent(motionEvent) && !this.mDeadZoneConsuming) {
            return false;
        }
        if (actionMasked == 0) {
            setSlippery(true);
            this.mDeadZoneConsuming = true;
        } else if (actionMasked == 1 || actionMasked == 3) {
            updateSlippery();
            this.mDeadZoneConsuming = false;
        }
        return true;
    }

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public RotationButtonController getRotationButtonController() {
        return this.mRotationButtonController;
    }

    public ButtonDispatcher getRecentsButton() {
        return this.mButtonDispatchers.get(C0007R$id.recent_apps);
    }

    public ButtonDispatcher getBackButton() {
        return this.mButtonDispatchers.get(C0007R$id.back);
    }

    public ButtonDispatcher getHomeButton() {
        return this.mButtonDispatchers.get(C0007R$id.home);
    }

    public ButtonDispatcher getImeSwitchButton() {
        return this.mButtonDispatchers.get(C0007R$id.ime_switcher);
    }

    public ButtonDispatcher getUnlockButton() {
        return this.mButtonDispatchers.get(C0007R$id.unlock);
    }

    public GameButton getGameButton() {
        return (GameButton) this.mButtonDispatchers.get(C0007R$id.game_button);
    }

    public ButtonDispatcher getAccessibilityButton() {
        return this.mButtonDispatchers.get(C0007R$id.accessibility_button);
    }

    public RotationContextButton getRotateSuggestionButton() {
        return (RotationContextButton) this.mButtonDispatchers.get(C0007R$id.rotate_suggestion);
    }

    public ButtonDispatcher getHomeHandle() {
        return this.mButtonDispatchers.get(C0007R$id.home_handle);
    }

    public SparseArray<ButtonDispatcher> getButtonDispatchers() {
        return this.mButtonDispatchers;
    }

    public boolean isRecentsButtonVisible() {
        return getRecentsButton().getVisibility() == 0;
    }

    public boolean isOverviewEnabled() {
        return (this.mDisabledFlags & 16777216) == 0;
    }

    public boolean isQuickStepSwipeUpEnabled() {
        return this.mOverviewProxyService.shouldShowSwipeUpUI() && isOverviewEnabled();
    }

    private void reloadNavIcons() {
        updateIcons(Configuration.EMPTY);
    }

    private void updateIcons(Configuration configuration) {
        boolean z = true;
        boolean z2 = configuration.orientation != this.mConfiguration.orientation;
        boolean z3 = configuration.densityDpi != this.mConfiguration.densityDpi;
        if (configuration.getLayoutDirection() == this.mConfiguration.getLayoutDirection()) {
            z = false;
        }
        if (z2 || z3) {
            this.mDockedIcon = getDrawable(C0006R$drawable.ic_sysbar_docked);
            this.mHomeDefaultIcon = getHomeDrawable();
        }
        if (z3 || z) {
            this.mRecentIcon = getDrawable(C0006R$drawable.ic_sysbar_recent);
            this.mContextualButtonGroup.updateIcons();
            this.mUnlockIcon = getDrawable(C0006R$drawable.ic_navbar_lock);
        }
        if (z2 || z3 || z) {
            this.mBackIcon = getBackDrawable();
        }
    }

    public KeyButtonDrawable getBackDrawable() {
        KeyButtonDrawable drawable = getDrawable(getBackDrawableRes());
        orientBackButton(drawable);
        return drawable;
    }

    public int getBackDrawableRes() {
        return chooseNavigationIconDrawableRes(C0006R$drawable.ic_sysbar_back, C0006R$drawable.ic_sysbar_back_quick_step);
    }

    public KeyButtonDrawable getHomeDrawable() {
        KeyButtonDrawable keyButtonDrawable;
        if (this.mOverviewProxyService.shouldShowSwipeUpUI()) {
            keyButtonDrawable = getDrawable(C0006R$drawable.ic_sysbar_home_quick_step);
        } else {
            keyButtonDrawable = getDrawable(C0006R$drawable.ic_sysbar_home);
        }
        orientHomeButton(keyButtonDrawable);
        return keyButtonDrawable;
    }

    private void orientBackButton(KeyButtonDrawable keyButtonDrawable) {
        float f;
        boolean z = (this.mNavigationIconHints & 1) != 0;
        boolean z2 = this.mConfiguration.getLayoutDirection() == 1;
        float f2 = 0.0f;
        if (z) {
            f = (float) (z2 ? 90 : -90);
        } else {
            f = 0.0f;
        }
        if (keyButtonDrawable.getRotation() != f) {
            if (QuickStepContract.isGesturalMode(this.mNavBarMode)) {
                keyButtonDrawable.setRotation(f);
                return;
            }
            if (!this.mOverviewProxyService.shouldShowSwipeUpUI() && !this.mIsVertical && z) {
                f2 = -getResources().getDimension(C0005R$dimen.navbar_back_button_ime_offset);
            }
            ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(keyButtonDrawable, PropertyValuesHolder.ofFloat(KeyButtonDrawable.KEY_DRAWABLE_ROTATE, f), PropertyValuesHolder.ofFloat(KeyButtonDrawable.KEY_DRAWABLE_TRANSLATE_Y, f2));
            ofPropertyValuesHolder.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            ofPropertyValuesHolder.setDuration(200L);
            ofPropertyValuesHolder.start();
        }
    }

    private void orientHomeButton(KeyButtonDrawable keyButtonDrawable) {
        keyButtonDrawable.setRotation(this.mIsVertical ? 90.0f : 0.0f);
    }

    private int chooseNavigationIconDrawableRes(int i, int i2) {
        return this.mOverviewProxyService.shouldShowSwipeUpUI() ? i2 : i;
    }

    private KeyButtonDrawable getDrawable(int i) {
        return KeyButtonDrawable.create(((FrameLayout) this).mContext, i, true);
    }

    public void setWindowVisible(boolean z) {
        this.mTintController.setWindowVisible(z);
        this.mRotationButtonController.onNavigationBarWindowVisibilityChange(z);
    }

    public void setLayoutDirection(int i) {
        reloadNavIcons();
        super.setLayoutDirection(i);
    }

    public void setNavigationIconHints(int i) {
        if (i != this.mNavigationIconHints) {
            boolean z = false;
            boolean z2 = (i & 1) != 0;
            if ((this.mNavigationIconHints & 1) != 0) {
                z = true;
            }
            if (z2 != z) {
                onImeVisibilityChanged(z2);
            }
            this.mNavigationIconHints = i;
            updateNavButtonIcons();
        }
    }

    private void onImeVisibilityChanged(boolean z) {
        if (!z) {
            this.mTransitionListener.onBackAltCleared();
        }
        this.mImeVisible = z;
        this.mRotationButtonController.getRotationButton().setCanShowRotationButton(!this.mImeVisible);
    }

    public void setDisabledFlags(int i) {
        if (this.mDisabledFlags != i) {
            boolean isOverviewEnabled = isOverviewEnabled();
            this.mDisabledFlags = i;
            if (!isOverviewEnabled && isOverviewEnabled()) {
                reloadNavIcons();
            }
            updateNavButtonIcons();
            updateSlippery();
            setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
            updateSystemUiStateFlags();
        }
    }

    public void updateNavButtonIcons() {
        boolean z;
        LayoutTransition layoutTransition;
        int i = 0;
        boolean z2 = (this.mNavigationIconHints & 1) != 0;
        KeyButtonDrawable keyButtonDrawable = this.mBackIcon;
        orientBackButton(keyButtonDrawable);
        KeyButtonDrawable keyButtonDrawable2 = this.mHomeDefaultIcon;
        if (!this.mUseCarModeUi) {
            orientHomeButton(keyButtonDrawable2);
        }
        getHomeButton().setImageDrawable(keyButtonDrawable2);
        getBackButton().setImageDrawable(keyButtonDrawable);
        updateRecentsIcon();
        String str = "car";
        SkinningBridge.onButtonVariantChanged(C0007R$id.back, this.mUseCarModeUi ? z2 ? "closeImeCar" : str : z2 ? "closeIme" : "normal");
        int i2 = C0007R$id.home;
        if (!this.mUseCarModeUi) {
            str = "normal";
        }
        SkinningBridge.onButtonVariantChanged(i2, str);
        this.mContextualButtonGroup.setButtonVisibility(C0007R$id.ime_switcher, (this.mNavigationIconHints & 2) != 0);
        getUnlockButton().setImageDrawable(this.mUnlockIcon);
        this.mBarTransitions.reapplyDarkIntensity();
        boolean z3 = QuickStepContract.isGesturalMode(this.mNavBarMode) || (this.mDisabledFlags & 2097152) != 0;
        boolean isRecentsButtonDisabled = isRecentsButtonDisabled();
        boolean z4 = !z2 && (QuickStepContract.isGesturalMode(this.mNavBarMode) || (this.mDisabledFlags & 4194304) != 0);
        boolean isScreenPinningActive = ActivityManagerWrapper.getInstance().isScreenPinningActive();
        if (this.mOverviewProxyService.isEnabled()) {
            z = (true ^ QuickStepContract.isLegacyMode(this.mNavBarMode)) | isRecentsButtonDisabled;
            if (isScreenPinningActive) {
                z4 = false;
                z3 = false;
            }
        } else if (isScreenPinningActive) {
            z4 = false;
            z = false;
        } else {
            z = isRecentsButtonDisabled;
        }
        ViewGroup viewGroup = (ViewGroup) getCurrentView().findViewById(C0007R$id.nav_buttons);
        if (!(viewGroup == null || (layoutTransition = viewGroup.getLayoutTransition()) == null || layoutTransition.getTransitionListeners().contains(this.mTransitionListener))) {
            layoutTransition.addTransitionListener(this.mTransitionListener);
        }
        getBackButton().setVisibility(z4 ? 4 : 0);
        getHomeButton().setVisibility(z3 ? 4 : 0);
        ButtonDispatcher recentsButton = getRecentsButton();
        if (z || this.mIsEmergencyModeOn) {
            i = 4;
        }
        recentsButton.setVisibility(i);
        SkinningBridge.onButtonVisibilityChanged(C0007R$id.home, z3 ? "false" : "true");
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean isRecentsButtonDisabled() {
        return this.mUseCarModeUi || !isOverviewEnabled() || getContext().getDisplayId() != 0;
    }

    private Display getContextDisplay() {
        return getContext().getDisplay();
    }

    public void setLayoutTransitionsEnabled(boolean z) {
        this.mLayoutTransitionsEnabled = z;
        updateLayoutTransitionsEnabled();
    }

    public void setWakeAndUnlocking(boolean z) {
        setUseFadingAnimations(z);
        this.mWakeAndUnlocking = z;
        updateLayoutTransitionsEnabled();
    }

    private void updateLayoutTransitionsEnabled() {
        boolean z = !this.mWakeAndUnlocking && this.mLayoutTransitionsEnabled;
        LayoutTransition layoutTransition = ((ViewGroup) getCurrentView().findViewById(C0007R$id.nav_buttons)).getLayoutTransition();
        if (layoutTransition == null) {
            return;
        }
        if (z) {
            layoutTransition.enableTransitionType(2);
            layoutTransition.enableTransitionType(3);
            layoutTransition.enableTransitionType(0);
            layoutTransition.enableTransitionType(1);
            return;
        }
        layoutTransition.disableTransitionType(2);
        layoutTransition.disableTransitionType(3);
        layoutTransition.disableTransitionType(0);
        layoutTransition.disableTransitionType(1);
    }

    private void setUseFadingAnimations(boolean z) {
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) ((ViewGroup) getParent()).getLayoutParams();
        if (layoutParams != null) {
            boolean z2 = layoutParams.windowAnimations != 0;
            if (!z2 && z) {
                layoutParams.windowAnimations = C0015R$style.Animation_NavigationBarFadeIn;
            } else if (z2 && !z) {
                layoutParams.windowAnimations = 0;
            } else {
                return;
            }
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout((View) getParent(), layoutParams);
        }
    }

    public void onPanelExpandedChange() {
        updateSlippery();
        updateSystemUiStateFlags();
    }

    public void updateSystemUiStateFlags() {
        int displayId = ((FrameLayout) this).mContext.getDisplayId();
        boolean z = true;
        this.mOverviewProxyService.setSystemUiStateFlag(1, ActivityManagerWrapper.getInstance().isScreenPinningActive(), displayId);
        this.mOverviewProxyService.setSystemUiStateFlag(128, (this.mDisabledFlags & 16777216) != 0, displayId);
        this.mOverviewProxyService.setSystemUiStateFlag(256, (this.mDisabledFlags & 2097152) != 0, displayId);
        NotificationPanelView notificationPanelView = this.mPanelView;
        if (notificationPanelView != null) {
            OverviewProxyService overviewProxyService = this.mOverviewProxyService;
            if (!notificationPanelView.isFullyExpanded() || this.mPanelView.isInSettings()) {
                z = false;
            }
            overviewProxyService.setSystemUiStateFlag(4, z, displayId);
        }
    }

    public void updateStates() {
        boolean shouldShowSwipeUpUI = this.mOverviewProxyService.shouldShowSwipeUpUI();
        NavigationBarInflaterView navigationBarInflaterView = this.mNavigationInflaterView;
        if (navigationBarInflaterView != null) {
            navigationBarInflaterView.onLikelyDefaultLayoutChange();
        }
        updateSlippery();
        reloadNavIcons();
        updateNavButtonIcons();
        setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
        WindowManagerWrapper.getInstance().setNavBarVirtualKeyHapticFeedbackEnabled(!shouldShowSwipeUpUI);
        getHomeButton().setAccessibilityDelegate(shouldShowSwipeUpUI ? this.mQuickStepAccessibilityDelegate : null);
    }

    public void updateSlippery() {
        setSlippery(!isQuickStepSwipeUpEnabled() || (this.mPanelView.isFullyExpanded() && !this.mPanelView.isCollapsing()));
    }

    private void setSlippery(boolean z) {
        setWindowFlag(536870912, z);
    }

    private void setWindowFlag(int i, boolean z) {
        WindowManager.LayoutParams layoutParams;
        ViewGroup viewGroup = (ViewGroup) getParent();
        if (viewGroup != null && (layoutParams = (WindowManager.LayoutParams) viewGroup.getLayoutParams()) != null) {
            if (z != ((layoutParams.flags & i) != 0)) {
                if (z) {
                    layoutParams.flags = i | layoutParams.flags;
                } else {
                    layoutParams.flags = (~i) & layoutParams.flags;
                }
                ((WindowManager) getContext().getSystemService("window")).updateViewLayout(viewGroup, layoutParams);
            }
        }
    }

    @Override // com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener
    public void onNavigationModeChanged(int i) {
        Context currentUserContext = ((NavigationModeController) Dependency.get(NavigationModeController.class)).getCurrentUserContext();
        this.mNavBarMode = i;
        this.mBarTransitions.onNavigationModeChanged(this.mNavBarMode);
        this.mEdgeBackGestureHandler.onNavigationModeChanged(this.mNavBarMode, currentUserContext);
        this.mRecentsOnboarding.onNavigationModeChanged(this.mNavBarMode);
        getRotateSuggestionButton().onNavigationModeChanged(this.mNavBarMode);
        this.mTintController.onNavigationModeChanged(this.mNavBarMode);
        if (QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            this.mTintController.start();
        } else {
            this.mTintController.stop();
        }
    }

    public void setAccessibilityButtonState(boolean z, boolean z2) {
        this.mLongClickableAccessibilityButton = z2;
        getAccessibilityButton().setLongClickable(z2);
        this.mContextualButtonGroup.setButtonVisibility(C0007R$id.accessibility_button, z);
        NavigationBarLockController navigationBarLockController = this.mLockController;
        if (navigationBarLockController != null) {
            navigationBarLockController.onAccessibilityButtonState(z);
        }
        SkinningBridge.onButtonVisibilityChanged(C0007R$id.accessibility_button, z ? "true" : "false");
    }

    /* access modifiers changed from: package-private */
    public void hideRecentsOnboarding() {
        this.mRecentsOnboarding.hide(true);
    }

    public void onFinishInflate() {
        this.mNavigationInflaterView = (NavigationBarInflaterView) findViewById(C0007R$id.navigation_inflater);
        this.mNavigationInflaterView.setButtonDispatchers(this.mButtonDispatchers);
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        getUnlockButton().setOnClickListener(this.mUnlockClickListener);
        DockedStackExistsListener.register(this.mDockedListener);
        updateOrientationViews();
        reloadNavIcons();
        this.mCenterGroup = getCurrentView().findViewById(C0007R$id.center_group);
        this.mEndsGroup = getCurrentView().findViewById(C0007R$id.ends_group);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        this.mDeadZone.onDraw(canvas);
        super.onDraw(canvas);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mActiveRegion.setEmpty();
        updateButtonLocation(getBackButton(), this.mBackButtonBounds, true);
        updateButtonLocation(getHomeButton(), this.mHomeButtonBounds, false);
        updateButtonLocation(getRecentsButton(), this.mRecentsButtonBounds, false);
        updateButtonLocation(getRotateSuggestionButton(), this.mRotationButtonBounds, true);
        this.mOverviewProxyService.onActiveNavBarRegionChanges(this.mActiveRegion);
        this.mRecentsOnboarding.setNavBarHeight(getMeasuredHeight());
        this.mCenterGroup = getCurrentView().findViewById(C0007R$id.center_group);
        this.mEndsGroup = getCurrentView().findViewById(C0007R$id.ends_group);
    }

    private void updateButtonLocation(ButtonDispatcher buttonDispatcher, Rect rect, boolean z) {
        View currentView = buttonDispatcher.getCurrentView();
        if (currentView == null) {
            rect.setEmpty();
            return;
        }
        float translationX = currentView.getTranslationX();
        float translationY = currentView.getTranslationY();
        currentView.setTranslationX(0.0f);
        currentView.setTranslationY(0.0f);
        if (z) {
            currentView.getLocationOnScreen(this.mTmpPosition);
            int[] iArr = this.mTmpPosition;
            rect.set(iArr[0], iArr[1], iArr[0] + currentView.getMeasuredWidth(), this.mTmpPosition[1] + currentView.getMeasuredHeight());
            this.mActiveRegion.op(rect, Region.Op.UNION);
        }
        currentView.getLocationInWindow(this.mTmpPosition);
        int[] iArr2 = this.mTmpPosition;
        rect.set(iArr2[0], iArr2[1], iArr2[0] + currentView.getMeasuredWidth(), this.mTmpPosition[1] + currentView.getMeasuredHeight());
        currentView.setTranslationX(translationX);
        currentView.setTranslationY(translationY);
    }

    private void updateOrientationViews() {
        this.mHorizontal = findViewById(C0007R$id.horizontal);
        this.mVertical = findViewById(C0007R$id.vertical);
        updateCurrentView();
    }

    /* access modifiers changed from: package-private */
    public boolean needsReorient(int i) {
        return this.mCurrentRotation != i;
    }

    private void updateCurrentView() {
        resetViews();
        this.mCurrentView = this.mIsVertical ? this.mVertical : this.mHorizontal;
        boolean z = false;
        this.mCurrentView.setVisibility(0);
        this.mNavigationInflaterView.setVertical(this.mIsVertical);
        this.mCurrentRotation = getContextDisplay().getRotation();
        NavigationBarInflaterView navigationBarInflaterView = this.mNavigationInflaterView;
        if (this.mCurrentRotation == 1) {
            z = true;
        }
        navigationBarInflaterView.setAlternativeOrder(z);
        this.mNavigationInflaterView.updateButtonDispatchersCurrentView();
        updateLayoutTransitionsEnabled();
    }

    private void resetViews() {
        this.mHorizontal.setVisibility(8);
        this.mVertical.setVisibility(8);
    }

    private void updateRecentsIcon() {
        this.mDockedIcon.setRotation((!this.mDockedStackExists || !this.mIsVertical) ? 0.0f : 90.0f);
        getRecentsButton().setImageDrawable(this.mDockedStackExists ? this.mDockedIcon : this.mRecentIcon);
        this.mBarTransitions.reapplyDarkIntensity();
        SkinningBridge.onButtonVariantChanged(C0007R$id.recent_apps, this.mDockedStackExists ? "splitScreen" : "normal");
    }

    public void showPinningEnterExitToast(boolean z) {
        if (z) {
            this.mScreenPinningNotify.showPinningStartToast();
        } else {
            this.mScreenPinningNotify.showPinningExitToast();
        }
    }

    public void showPinningEscapeToast() {
        this.mScreenPinningNotify.showEscapeToast(isRecentsButtonVisible());
    }

    public boolean isVertical() {
        return this.mIsVertical;
    }

    public void reorient() {
        updateCurrentView();
        ((NavigationBarFrame) getRootView()).setDeadZone(this.mDeadZone);
        this.mDeadZone.onConfigurationChanged(this.mCurrentRotation);
        this.mBarTransitions.init();
        if (!isLayoutDirectionResolved()) {
            resolveLayoutDirection();
        }
        updateNavButtonIcons();
        getHomeButton().setVertical(this.mIsVertical);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        boolean z = size > 0 && size2 > size && !QuickStepContract.isGesturalMode(this.mNavBarMode);
        if (z != this.mIsVertical) {
            this.mIsVertical = z;
            reorient();
            notifyVerticalChangedListener(z);
        }
        if (QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            if (this.mIsVertical) {
                i3 = getResources().getDimensionPixelSize(17105295);
            } else {
                i3 = getResources().getDimensionPixelSize(17105293);
            }
            this.mBarTransitions.setBackgroundFrame(new Rect(0, getResources().getDimensionPixelSize(17105290) - i3, size, size2));
        }
        super.onMeasure(i, i2);
    }

    private void notifyVerticalChangedListener(boolean z) {
        OnVerticalChangedListener onVerticalChangedListener = this.mOnVerticalChangedListener;
        if (onVerticalChangedListener != null) {
            onVerticalChangedListener.onVerticalChanged(z);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mTmpLastConfiguration.updateFrom(this.mConfiguration);
        this.mConfiguration.updateFrom(configuration);
        boolean updateCarMode = updateCarMode();
        updateIcons(this.mTmpLastConfiguration);
        updateRecentsIcon();
        this.mRecentsOnboarding.onConfigurationChanged(this.mConfiguration);
        if (!updateCarMode) {
            Configuration configuration2 = this.mTmpLastConfiguration;
            if (configuration2.densityDpi == this.mConfiguration.densityDpi && configuration2.getLayoutDirection() == this.mConfiguration.getLayoutDirection()) {
                return;
            }
        }
        updateNavButtonIcons();
    }

    private boolean updateCarMode() {
        Configuration configuration = this.mConfiguration;
        if (configuration != null) {
            boolean z = (configuration.uiMode & 15) == 3;
            if (z != this.mInCarMode) {
                this.mInCarMode = z;
                this.mUseCarModeUi = false;
            }
        }
        return false;
    }

    private String getResourceName(int i) {
        if (i == 0) {
            return "(null)";
        }
        try {
            return getContext().getResources().getResourceName(i);
        } catch (Resources.NotFoundException unused) {
            return "(unknown)";
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
        reorient();
        onNavigationModeChanged(this.mNavBarMode);
        setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
        RotationButtonController rotationButtonController = this.mRotationButtonController;
        if (rotationButtonController != null) {
            rotationButtonController.registerListeners();
        }
        this.mEdgeBackGestureHandler.onNavBarAttached();
        getViewTreeObserver().addOnComputeInternalInsetsListener(this.mOnComputeInternalInsetsListener);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((NavigationModeController) Dependency.get(NavigationModeController.class)).removeListener(this);
        setUpSwipeUpOnboarding(false);
        for (int i = 0; i < this.mButtonDispatchers.size(); i++) {
            this.mButtonDispatchers.valueAt(i).onDestroy();
        }
        RotationButtonController rotationButtonController = this.mRotationButtonController;
        if (rotationButtonController != null) {
            rotationButtonController.unregisterListeners();
        }
        this.mEdgeBackGestureHandler.onNavBarDetached();
        getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mOnComputeInternalInsetsListener);
    }

    private void setUpSwipeUpOnboarding(boolean z) {
        if (z) {
            this.mRecentsOnboarding.onConnectedToLauncher();
        } else {
            this.mRecentsOnboarding.onDisconnectedFromLauncher();
        }
    }

    public void setLockController(NavigationBarLockController navigationBarLockController) {
        this.mLockController = navigationBarLockController;
    }

    public NavigationBarLockController getLockController() {
        return this.mLockController;
    }

    private void setGroupVisible(View view, int i, boolean z) {
        view.findViewById(i).setVisibility(z ? 0 : 4);
    }

    public void setLocked(boolean z) {
        this.mOverviewProxyService.lockSet(z);
        setGroupVisible(this.mHorizontal, C0007R$id.nav_buttons, !z);
        setGroupVisible(this.mVertical, C0007R$id.nav_buttons, !z);
        setGroupVisible(this.mHorizontal, C0007R$id.nav_lock_buttons, z);
        setGroupVisible(this.mVertical, C0007R$id.nav_lock_buttons, z);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NavigationBarView {");
        Rect rect = new Rect();
        Point point = new Point();
        getContextDisplay().getRealSize(point);
        printWriter.println(String.format("      this: " + StatusBar.viewInfo(this) + " " + visibilityToString(getVisibility()), new Object[0]));
        getWindowVisibleDisplayFrame(rect);
        boolean z = rect.right > point.x || rect.bottom > point.y;
        StringBuilder sb = new StringBuilder();
        sb.append("      window: ");
        sb.append(rect.toShortString());
        sb.append(" ");
        sb.append(visibilityToString(getWindowVisibility()));
        sb.append(z ? " OFFSCREEN!" : "");
        printWriter.println(sb.toString());
        printWriter.println(String.format("      mCurrentView: id=%s (%dx%d) %s %f", getResourceName(getCurrentView().getId()), Integer.valueOf(getCurrentView().getWidth()), Integer.valueOf(getCurrentView().getHeight()), visibilityToString(getCurrentView().getVisibility()), Float.valueOf(getCurrentView().getAlpha())));
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(this.mDisabledFlags);
        objArr[1] = this.mIsVertical ? "true" : "false";
        objArr[2] = Float.valueOf(getLightTransitionsController().getCurrentDarkIntensity());
        printWriter.println(String.format("      disabled=0x%08x vertical=%s darkIntensity=%.2f", objArr));
        dumpButton(printWriter, "back", getBackButton());
        dumpButton(printWriter, "home", getHomeButton());
        dumpButton(printWriter, "rcnt", getRecentsButton());
        dumpButton(printWriter, "rota", getRotateSuggestionButton());
        dumpButton(printWriter, "a11y", getAccessibilityButton());
        printWriter.println("    }");
        this.mContextualButtonGroup.dump(printWriter);
        this.mRecentsOnboarding.dump(printWriter);
        this.mTintController.dump(printWriter);
        this.mEdgeBackGestureHandler.dump(printWriter);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        int systemWindowInsetLeft = windowInsets.getSystemWindowInsetLeft();
        int systemWindowInsetRight = windowInsets.getSystemWindowInsetRight();
        setPadding(systemWindowInsetLeft, windowInsets.getSystemWindowInsetTop(), systemWindowInsetRight, windowInsets.getSystemWindowInsetBottom());
        this.mEdgeBackGestureHandler.setInsets(systemWindowInsetLeft, systemWindowInsetRight);
        return super.onApplyWindowInsets(windowInsets);
    }

    private static void dumpButton(PrintWriter printWriter, String str, ButtonDispatcher buttonDispatcher) {
        printWriter.print("      " + str + ": ");
        if (buttonDispatcher == null) {
            printWriter.print("null");
        } else {
            printWriter.print(visibilityToString(buttonDispatcher.getVisibility()) + " alpha=" + buttonDispatcher.getAlpha());
        }
        printWriter.println();
    }

    public /* synthetic */ void lambda$new$2$NavigationBarView(Boolean bool) {
        post(new Runnable(bool) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarView$seINE1MF9Wb6jBs3U7jhkEzAV4 */
            private final /* synthetic */ Boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                NavigationBarView.this.lambda$new$1$NavigationBarView(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$new$1$NavigationBarView(Boolean bool) {
        this.mDockedStackExists = bool.booleanValue();
        updateRecentsIcon();
    }

    public final void moveNavigationBar() {
        int random = (int) (Math.random() * ((double) this.mNavigationBarPaddingX));
        int random2 = (int) (Math.random() * ((double) this.mNavigationBarPaddingY));
        if (isVertical()) {
            random2 = random;
            random = random2;
        }
        if (System.currentTimeMillis() % 2 > 0) {
            int i = -random;
            int i2 = -random2;
            this.mCenterGroup.setPaddingRelative(random, random2, i, i2);
            this.mEndsGroup.setPaddingRelative(random, random2, i, i2);
            return;
        }
        int i3 = -random;
        int i4 = -random2;
        this.mCenterGroup.setPaddingRelative(i3, i4, random, random2);
        this.mEndsGroup.setPaddingRelative(i3, i4, random, random2);
    }
}
