package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.qs.tiles.UserDetailItemView;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class KeyguardUserSwitcher {
    private final Adapter mAdapter;
    private boolean mAnimating;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private final KeyguardUserSwitcherScrim mBackground;
    private ObjectAnimator mBgAnimator;
    public final DataSetObserver mDataSetObserver = new DataSetObserver() {
        /* class com.android.systemui.statusbar.policy.KeyguardUserSwitcher.AnonymousClass4 */

        public void onChanged() {
            KeyguardUserSwitcher.this.refresh();
        }
    };
    private final KeyguardStatusBarView mStatusBarView;
    private ViewGroup mUserSwitcher;
    private final Container mUserSwitcherContainer;
    private UserSwitcherController mUserSwitcherController;

    public KeyguardUserSwitcher(Context context, ViewStub viewStub, KeyguardStatusBarView keyguardStatusBarView, NotificationPanelView notificationPanelView) {
        boolean z = context.getResources().getBoolean(C0003R$bool.config_keyguardUserSwitcher);
        UserSwitcherController userSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        if (userSwitcherController == null || !z) {
            this.mUserSwitcherContainer = null;
            this.mStatusBarView = null;
            this.mAdapter = null;
            this.mAppearAnimationUtils = null;
            this.mBackground = null;
            return;
        }
        this.mUserSwitcherContainer = (Container) viewStub.inflate();
        this.mBackground = new KeyguardUserSwitcherScrim(context);
        reinflateViews();
        this.mStatusBarView = keyguardStatusBarView;
        this.mStatusBarView.setKeyguardUserSwitcher(this);
        notificationPanelView.setKeyguardUserSwitcher(this);
        this.mAdapter = new Adapter(context, userSwitcherController, this);
        this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
        this.mUserSwitcherController = userSwitcherController;
        this.mAppearAnimationUtils = new AppearAnimationUtils(context, 400, -0.5f, 0.5f, Interpolators.FAST_OUT_SLOW_IN);
        this.mUserSwitcherContainer.setKeyguardUserSwitcher(this);
    }

    private void reinflateViews() {
        ViewGroup viewGroup = this.mUserSwitcher;
        if (viewGroup != null) {
            viewGroup.setBackground(null);
            this.mUserSwitcher.removeOnLayoutChangeListener(this.mBackground);
        }
        this.mUserSwitcherContainer.removeAllViews();
        LayoutInflater.from(this.mUserSwitcherContainer.getContext()).inflate(C0010R$layout.keyguard_user_switcher_inner, this.mUserSwitcherContainer);
        this.mUserSwitcher = (ViewGroup) this.mUserSwitcherContainer.findViewById(C0007R$id.keyguard_user_switcher_inner);
        this.mUserSwitcher.addOnLayoutChangeListener(this.mBackground);
        this.mUserSwitcher.setBackground(this.mBackground);
    }

    public void setKeyguard(boolean z, boolean z2) {
        if (this.mUserSwitcher == null) {
            return;
        }
        if (!z || !shouldExpandByDefault()) {
            hide(z2);
        } else {
            show(z2);
        }
    }

    private boolean shouldExpandByDefault() {
        UserSwitcherController userSwitcherController = this.mUserSwitcherController;
        return userSwitcherController != null && userSwitcherController.isSimpleUserSwitcher();
    }

    public void show(boolean z) {
        if (this.mUserSwitcher != null && this.mUserSwitcherContainer.getVisibility() != 0) {
            cancelAnimations();
            this.mAdapter.refresh();
            this.mUserSwitcherContainer.setVisibility(0);
            this.mStatusBarView.setKeyguardUserSwitcherShowing(true, z);
            if (z) {
                startAppearAnimation();
            }
        }
    }

    private boolean hide(boolean z) {
        if (this.mUserSwitcher == null || this.mUserSwitcherContainer.getVisibility() != 0) {
            return false;
        }
        cancelAnimations();
        if (z) {
            startDisappearAnimation();
        } else {
            this.mUserSwitcherContainer.setVisibility(8);
        }
        this.mStatusBarView.setKeyguardUserSwitcherShowing(false, z);
        return true;
    }

    private void cancelAnimations() {
        int childCount = this.mUserSwitcher.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mUserSwitcher.getChildAt(i).animate().cancel();
        }
        ObjectAnimator objectAnimator = this.mBgAnimator;
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        this.mUserSwitcher.animate().cancel();
        this.mAnimating = false;
    }

    private void startAppearAnimation() {
        int childCount = this.mUserSwitcher.getChildCount();
        View[] viewArr = new View[childCount];
        for (int i = 0; i < childCount; i++) {
            viewArr[i] = this.mUserSwitcher.getChildAt(i);
        }
        this.mUserSwitcher.setClipChildren(false);
        this.mUserSwitcher.setClipToPadding(false);
        this.mAppearAnimationUtils.startAnimation(viewArr, new Runnable() {
            /* class com.android.systemui.statusbar.policy.KeyguardUserSwitcher.AnonymousClass1 */

            public void run() {
                KeyguardUserSwitcher.this.mUserSwitcher.setClipChildren(true);
                KeyguardUserSwitcher.this.mUserSwitcher.setClipToPadding(true);
            }
        });
        this.mAnimating = true;
        this.mBgAnimator = ObjectAnimator.ofInt(this.mBackground, "alpha", 0, 255);
        this.mBgAnimator.setDuration(400L);
        this.mBgAnimator.setInterpolator(Interpolators.ALPHA_IN);
        this.mBgAnimator.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.statusbar.policy.KeyguardUserSwitcher.AnonymousClass2 */

            public void onAnimationEnd(Animator animator) {
                KeyguardUserSwitcher.this.mBgAnimator = null;
                KeyguardUserSwitcher.this.mAnimating = false;
            }
        });
        this.mBgAnimator.start();
    }

    private void startDisappearAnimation() {
        this.mAnimating = true;
        this.mUserSwitcher.animate().alpha(0.0f).setDuration(300).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            /* class com.android.systemui.statusbar.policy.KeyguardUserSwitcher.AnonymousClass3 */

            public void run() {
                KeyguardUserSwitcher.this.mUserSwitcherContainer.setVisibility(8);
                KeyguardUserSwitcher.this.mUserSwitcher.setAlpha(1.0f);
                KeyguardUserSwitcher.this.mAnimating = false;
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void refresh() {
        int childCount = this.mUserSwitcher.getChildCount();
        int count = this.mAdapter.getCount();
        int max = Math.max(childCount, count);
        for (int i = 0; i < max; i++) {
            if (i < count) {
                View view = null;
                if (i < childCount) {
                    view = this.mUserSwitcher.getChildAt(i);
                }
                View view2 = this.mAdapter.getView(i, view, this.mUserSwitcher);
                if (view == null) {
                    this.mUserSwitcher.addView(view2);
                } else if (view != view2) {
                    this.mUserSwitcher.removeViewAt(i);
                    this.mUserSwitcher.addView(view2, i);
                }
            } else {
                this.mUserSwitcher.removeViewAt(this.mUserSwitcher.getChildCount() - 1);
            }
        }
    }

    public boolean hideIfNotSimple(boolean z) {
        if (this.mUserSwitcherContainer == null || this.mUserSwitcherController.isSimpleUserSwitcher()) {
            return false;
        }
        return hide(z);
    }

    /* access modifiers changed from: package-private */
    public boolean isAnimating() {
        return this.mAnimating;
    }

    public void onDensityOrFontScaleChanged() {
        if (this.mUserSwitcherContainer != null) {
            reinflateViews();
            refresh();
        }
    }

    public static class Adapter extends UserSwitcherController.BaseUserAdapter implements View.OnClickListener {
        private Context mContext;
        private KeyguardUserSwitcher mKeyguardUserSwitcher;

        public Adapter(Context context, UserSwitcherController userSwitcherController, KeyguardUserSwitcher keyguardUserSwitcher) {
            super(userSwitcherController);
            this.mContext = context;
            this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            UserSwitcherController.UserRecord item = getItem(i);
            if (!(view instanceof UserDetailItemView) || !(view.getTag() instanceof UserSwitcherController.UserRecord)) {
                view = LayoutInflater.from(this.mContext).inflate(C0010R$layout.keyguard_user_switcher_item, viewGroup, false);
                view.setOnClickListener(this);
            }
            UserDetailItemView userDetailItemView = (UserDetailItemView) view;
            String name = getName(this.mContext, item);
            Bitmap bitmap = item.picture;
            if (bitmap == null) {
                userDetailItemView.bind(name, getDrawable(this.mContext, item).mutate(), item.resolveId());
            } else {
                userDetailItemView.bind(name, bitmap, item.info.id);
            }
            userDetailItemView.setAvatarEnabled(item.isSwitchToEnabled);
            view.setActivated(item.isCurrent);
            view.setTag(item);
            return view;
        }

        public void onClick(View view) {
            UserSwitcherController.UserRecord userRecord = (UserSwitcherController.UserRecord) view.getTag();
            if (userRecord.isCurrent && !userRecord.isGuest) {
                this.mKeyguardUserSwitcher.hideIfNotSimple(true);
            } else if (userRecord.isSwitchToEnabled) {
                switchTo(userRecord);
            }
        }
    }

    public static class Container extends FrameLayout {
        private KeyguardUserSwitcher mKeyguardUserSwitcher;

        public Container(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            setClipChildren(false);
        }

        public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
            this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        }

        public boolean onTouchEvent(MotionEvent motionEvent) {
            KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
            if (keyguardUserSwitcher == null || keyguardUserSwitcher.isAnimating()) {
                return false;
            }
            this.mKeyguardUserSwitcher.hideIfNotSimple(true);
            return false;
        }
    }
}
