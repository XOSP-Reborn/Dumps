package com.android.systemui.pip.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.Interpolators;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PipMenuActivity extends Activity {
    private AccessibilityManager mAccessibilityManager;
    private final List<RemoteAction> mActions = new ArrayList();
    private LinearLayout mActionsGroup;
    private boolean mAllowMenuTimeout = true;
    private boolean mAllowTouches = true;
    private Drawable mBackgroundDrawable;
    private int mBetweenActionPaddingLand;
    private View mDismissButton;
    private PointF mDownDelta = new PointF();
    private PointF mDownPosition = new PointF();
    private ImageView mExpandButton;
    private final Runnable mFinishRunnable = new Runnable() {
        /* class com.android.systemui.pip.phone.PipMenuActivity.AnonymousClass3 */

        public void run() {
            PipMenuActivity.this.hideMenu();
        }
    };
    private Handler mHandler = new Handler();
    private ValueAnimator.AnimatorUpdateListener mMenuBgUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        /* class com.android.systemui.pip.phone.PipMenuActivity.AnonymousClass1 */

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            PipMenuActivity.this.mBackgroundDrawable.setAlpha((int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * 0.3f * 255.0f));
        }
    };
    private View mMenuContainer;
    private AnimatorSet mMenuContainerAnimator;
    private int mMenuState;
    private Messenger mMessenger = new Messenger(new Handler() {
        /* class com.android.systemui.pip.phone.PipMenuActivity.AnonymousClass2 */

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Bundle bundle = (Bundle) message.obj;
                    PipMenuActivity.this.showMenu(bundle.getInt("menu_state"), (Rect) bundle.getParcelable("stack_bounds"), (Rect) bundle.getParcelable("movement_bounds"), bundle.getBoolean("allow_timeout"), bundle.getBoolean("resize_menu_on_show"));
                    return;
                case 2:
                    PipMenuActivity.this.cancelDelayedFinish();
                    return;
                case 3:
                    PipMenuActivity.this.hideMenu((Runnable) message.obj);
                    return;
                case 4:
                    Bundle bundle2 = (Bundle) message.obj;
                    ParceledListSlice parcelable = bundle2.getParcelable("actions");
                    PipMenuActivity.this.setActions((Rect) bundle2.getParcelable("stack_bounds"), parcelable != null ? parcelable.getList() : Collections.EMPTY_LIST);
                    return;
                case 5:
                    PipMenuActivity.this.updateDismissFraction(((Bundle) message.obj).getFloat("dismiss_fraction"));
                    return;
                case 6:
                    PipMenuActivity.this.mAllowTouches = true;
                    return;
                default:
                    return;
            }
        }
    });
    private View mSettingsButton;
    private Messenger mToControllerMessenger;
    private PipTouchState mTouchState;
    private ViewConfiguration mViewConfig;
    private View mViewRoot;

    static /* synthetic */ boolean lambda$updateActionViews$4(View view, MotionEvent motionEvent) {
        return true;
    }

    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        this.mViewConfig = ViewConfiguration.get(this);
        this.mTouchState = new PipTouchState(this.mViewConfig, this.mHandler, new Runnable() {
            /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$sJi0SxOuGngGF8xURDQ1Bnt0G_E */

            public final void run() {
                PipMenuActivity.this.lambda$onCreate$0$PipMenuActivity();
            }
        });
        getWindow().addFlags(537133056);
        super.onCreate(bundle);
        setContentView(C0010R$layout.pip_menu_activity);
        this.mAccessibilityManager = (AccessibilityManager) getSystemService(AccessibilityManager.class);
        this.mBackgroundDrawable = new ColorDrawable(-16777216);
        this.mBackgroundDrawable.setAlpha(0);
        this.mViewRoot = findViewById(C0007R$id.background);
        this.mViewRoot.setBackground(this.mBackgroundDrawable);
        this.mMenuContainer = findViewById(C0007R$id.menu_container);
        this.mMenuContainer.setAlpha(0.0f);
        this.mMenuContainer.setOnTouchListener(new View.OnTouchListener() {
            /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$WvtwNwFY4S4VeIJ5ZxsSTL51DAs */

            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return PipMenuActivity.this.lambda$onCreate$1$PipMenuActivity(view, motionEvent);
            }
        });
        this.mSettingsButton = findViewById(C0007R$id.settings);
        this.mSettingsButton.setAlpha(0.0f);
        this.mSettingsButton.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$70yHDyzrwE1GNEVEQrmSEL7H6fY */

            public final void onClick(View view) {
                PipMenuActivity.this.lambda$onCreate$2$PipMenuActivity(view);
            }
        });
        this.mDismissButton = findViewById(C0007R$id.dismiss);
        this.mDismissButton.setAlpha(0.0f);
        this.mDismissButton.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$XrbqAt128TykA2bnzcA2djOz8lo */

            public final void onClick(View view) {
                PipMenuActivity.this.lambda$onCreate$3$PipMenuActivity(view);
            }
        });
        this.mActionsGroup = (LinearLayout) findViewById(C0007R$id.actions_group);
        this.mBetweenActionPaddingLand = getResources().getDimensionPixelSize(C0005R$dimen.pip_between_action_padding_land);
        this.mExpandButton = (ImageView) findViewById(C0007R$id.expand_button);
        updateFromIntent(getIntent());
        setTitle(C0014R$string.pip_menu_title);
        setDisablePreviewScreenshots(true);
    }

    public /* synthetic */ void lambda$onCreate$0$PipMenuActivity() {
        if (this.mMenuState == 1) {
            showPipMenu();
        } else {
            expandPip();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:3:0x000d, code lost:
        if (r2 != 3) goto L_0x003c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public /* synthetic */ boolean lambda$onCreate$1$PipMenuActivity(android.view.View r2, android.view.MotionEvent r3) {
        /*
            r1 = this;
            com.android.systemui.pip.phone.PipTouchState r2 = r1.mTouchState
            r2.onTouchEvent(r3)
            int r2 = r3.getAction()
            r3 = 1
            if (r2 == r3) goto L_0x0010
            r0 = 3
            if (r2 == r0) goto L_0x0037
            goto L_0x003c
        L_0x0010:
            com.android.systemui.pip.phone.PipTouchState r2 = r1.mTouchState
            boolean r2 = r2.isDoubleTap()
            if (r2 != 0) goto L_0x0034
            int r2 = r1.mMenuState
            r0 = 2
            if (r2 != r0) goto L_0x001e
            goto L_0x0034
        L_0x001e:
            com.android.systemui.pip.phone.PipTouchState r2 = r1.mTouchState
            boolean r2 = r2.isWaitingForDoubleTap()
            if (r2 != 0) goto L_0x002e
            int r2 = r1.mMenuState
            if (r2 != r3) goto L_0x0037
            r1.showPipMenu()
            goto L_0x0037
        L_0x002e:
            com.android.systemui.pip.phone.PipTouchState r2 = r1.mTouchState
            r2.scheduleDoubleTapTimeoutCallback()
            goto L_0x0037
        L_0x0034:
            r1.expandPip()
        L_0x0037:
            com.android.systemui.pip.phone.PipTouchState r1 = r1.mTouchState
            r1.reset()
        L_0x003c:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.pip.phone.PipMenuActivity.lambda$onCreate$1$PipMenuActivity(android.view.View, android.view.MotionEvent):boolean");
    }

    public /* synthetic */ void lambda$onCreate$2$PipMenuActivity(View view) {
        if (view.getAlpha() != 0.0f) {
            showSettings();
        }
    }

    public /* synthetic */ void lambda$onCreate$3$PipMenuActivity(View view) {
        dismissPip();
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateFromIntent(intent);
    }

    public void onUserInteraction() {
        if (this.mAllowMenuTimeout) {
            repostDelayedFinish(2000);
        }
    }

    /* access modifiers changed from: protected */
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        hideMenu();
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        cancelDelayedFinish();
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        notifyActivityCallback(null);
    }

    public void onPictureInPictureModeChanged(boolean z) {
        if (!z) {
            finish();
        }
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (!this.mAllowTouches) {
            return super.dispatchTouchEvent(motionEvent);
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            this.mDownPosition.set(motionEvent.getX(), motionEvent.getY());
            this.mDownDelta.set(0.0f, 0.0f);
        } else if (action == 2) {
            this.mDownDelta.set(motionEvent.getX() - this.mDownPosition.x, motionEvent.getY() - this.mDownPosition.y);
            if (this.mDownDelta.length() > ((float) this.mViewConfig.getScaledTouchSlop()) && this.mMenuState != 0) {
                notifyRegisterInputConsumer();
                cancelDelayedFinish();
            }
        } else if (action == 4) {
            hideMenu();
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    public void finish() {
        notifyActivityCallback(null);
        super.finish();
        overridePendingTransition(0, 0);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showMenu(int i, Rect rect, Rect rect2, boolean z, boolean z2) {
        this.mAllowMenuTimeout = z;
        int i2 = this.mMenuState;
        if (i2 != i) {
            this.mAllowTouches = !(z2 && (i2 == 2 || i == 2));
            cancelDelayedFinish();
            updateActionViews(rect);
            AnimatorSet animatorSet = this.mMenuContainerAnimator;
            if (animatorSet != null) {
                animatorSet.cancel();
            }
            notifyMenuStateChange(i);
            this.mMenuContainerAnimator = new AnimatorSet();
            View view = this.mMenuContainer;
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 1.0f);
            ofFloat.addUpdateListener(this.mMenuBgUpdateListener);
            View view2 = this.mSettingsButton;
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(view2, View.ALPHA, view2.getAlpha(), 1.0f);
            View view3 = this.mDismissButton;
            ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(view3, View.ALPHA, view3.getAlpha(), 1.0f);
            if (i == 2) {
                this.mMenuContainerAnimator.playTogether(ofFloat, ofFloat2, ofFloat3);
            } else {
                this.mMenuContainerAnimator.playTogether(ofFloat3);
            }
            this.mMenuContainerAnimator.setInterpolator(Interpolators.ALPHA_IN);
            this.mMenuContainerAnimator.setDuration(125L);
            if (z) {
                this.mMenuContainerAnimator.addListener(new AnimatorListenerAdapter() {
                    /* class com.android.systemui.pip.phone.PipMenuActivity.AnonymousClass4 */

                    public void onAnimationEnd(Animator animator) {
                        PipMenuActivity.this.repostDelayedFinish(3500);
                    }
                });
            }
            this.mMenuContainerAnimator.start();
            return;
        }
        if (z) {
            repostDelayedFinish(2000);
        }
        notifyUnregisterInputConsumer();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void hideMenu() {
        hideMenu(null);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void hideMenu(Runnable runnable) {
        hideMenu(runnable, true, false);
    }

    private void hideMenu(final Runnable runnable, boolean z, final boolean z2) {
        if (this.mMenuState != 0) {
            cancelDelayedFinish();
            if (z) {
                notifyMenuStateChange(0);
            }
            this.mMenuContainerAnimator = new AnimatorSet();
            View view = this.mMenuContainer;
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 0.0f);
            ofFloat.addUpdateListener(this.mMenuBgUpdateListener);
            View view2 = this.mSettingsButton;
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(view2, View.ALPHA, view2.getAlpha(), 0.0f);
            View view3 = this.mDismissButton;
            ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(view3, View.ALPHA, view3.getAlpha(), 0.0f);
            this.mMenuContainerAnimator.playTogether(ofFloat, ofFloat2, ofFloat3);
            this.mMenuContainerAnimator.setInterpolator(Interpolators.ALPHA_OUT);
            this.mMenuContainerAnimator.setDuration(125L);
            this.mMenuContainerAnimator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.pip.phone.PipMenuActivity.AnonymousClass5 */

                public void onAnimationEnd(Animator animator) {
                    Runnable runnable = runnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                    if (!z2) {
                        PipMenuActivity.this.finish();
                    }
                }
            });
            this.mMenuContainerAnimator.start();
            return;
        }
        finish();
    }

    private void updateFromIntent(Intent intent) {
        this.mToControllerMessenger = (Messenger) intent.getParcelableExtra("messenger");
        if (this.mToControllerMessenger == null) {
            Log.w("PipMenuActivity", "Controller messenger is null. Stopping.");
            finish();
            return;
        }
        notifyActivityCallback(this.mMessenger);
        ParceledListSlice parcelableExtra = intent.getParcelableExtra("actions");
        if (parcelableExtra != null) {
            this.mActions.clear();
            this.mActions.addAll(parcelableExtra.getList());
        }
        int intExtra = intent.getIntExtra("menu_state", 0);
        if (intExtra != 0) {
            showMenu(intExtra, (Rect) intent.getParcelableExtra("stack_bounds"), (Rect) intent.getParcelableExtra("movement_bounds"), intent.getBooleanExtra("allow_timeout", true), intent.getBooleanExtra("resize_menu_on_show", false));
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setActions(Rect rect, List<RemoteAction> list) {
        this.mActions.clear();
        this.mActions.addAll(list);
        updateActionViews(rect);
    }

    private void updateActionViews(Rect rect) {
        ViewGroup viewGroup = (ViewGroup) findViewById(C0007R$id.expand_container);
        ViewGroup viewGroup2 = (ViewGroup) findViewById(C0007R$id.actions_container);
        viewGroup2.setOnTouchListener($$Lambda$PipMenuActivity$oqYZg3pvkgHv8RQZNYIeszXFk.INSTANCE);
        if (!this.mActions.isEmpty()) {
            boolean z = true;
            if (this.mMenuState != 1) {
                viewGroup2.setVisibility(0);
                if (this.mActionsGroup != null) {
                    LayoutInflater from = LayoutInflater.from(this);
                    while (this.mActionsGroup.getChildCount() < this.mActions.size()) {
                        this.mActionsGroup.addView((ImageView) from.inflate(C0010R$layout.pip_menu_action, (ViewGroup) this.mActionsGroup, false));
                    }
                    int i = 0;
                    while (i < this.mActionsGroup.getChildCount()) {
                        this.mActionsGroup.getChildAt(i).setVisibility(i < this.mActions.size() ? 0 : 8);
                        i++;
                    }
                    if (rect == null || rect.width() <= rect.height()) {
                        z = false;
                    }
                    int i2 = 0;
                    while (i2 < this.mActions.size()) {
                        RemoteAction remoteAction = this.mActions.get(i2);
                        ImageView imageView = (ImageView) this.mActionsGroup.getChildAt(i2);
                        remoteAction.getIcon().loadDrawableAsync(this, new Icon.OnDrawableLoadedListener(imageView) {
                            /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$7gqup1aBYwP3okQWsVlQjWziSZ0 */
                            private final /* synthetic */ ImageView f$0;

                            {
                                this.f$0 = r1;
                            }

                            public final void onDrawableLoaded(Drawable drawable) {
                                PipMenuActivity.lambda$updateActionViews$5(this.f$0, drawable);
                            }
                        }, this.mHandler);
                        imageView.setContentDescription(remoteAction.getContentDescription());
                        if (remoteAction.isEnabled()) {
                            imageView.setOnClickListener(new View.OnClickListener(remoteAction) {
                                /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$G0STaacRVSKlY2b4ryLhYqxLuQ */
                                private final /* synthetic */ RemoteAction f$0;

                                {
                                    this.f$0 = r1;
                                }

                                public final void onClick(View view) {
                                    PipMenuActivity.lambda$updateActionViews$6(this.f$0, view);
                                }
                            });
                        }
                        imageView.setEnabled(remoteAction.isEnabled());
                        imageView.setAlpha(remoteAction.isEnabled() ? 1.0f : 0.54f);
                        ((LinearLayout.LayoutParams) imageView.getLayoutParams()).leftMargin = (!z || i2 <= 0) ? 0 : this.mBetweenActionPaddingLand;
                        i2++;
                    }
                }
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
                layoutParams.topMargin = getResources().getDimensionPixelSize(C0005R$dimen.pip_action_padding);
                layoutParams.bottomMargin = getResources().getDimensionPixelSize(C0005R$dimen.pip_expand_container_edge_margin);
                viewGroup.requestLayout();
                return;
            }
        }
        viewGroup2.setVisibility(4);
    }

    static /* synthetic */ void lambda$updateActionViews$5(ImageView imageView, Drawable drawable) {
        drawable.setTint(-1);
        imageView.setImageDrawable(drawable);
    }

    static /* synthetic */ void lambda$updateActionViews$6(RemoteAction remoteAction, View view) {
        try {
            remoteAction.getActionIntent().send();
        } catch (PendingIntent.CanceledException e) {
            Log.w("PipMenuActivity", "Failed to send action", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDismissFraction(float f) {
        int i;
        float f2 = 1.0f - f;
        int i2 = this.mMenuState;
        if (i2 == 2) {
            this.mMenuContainer.setAlpha(f2);
            this.mSettingsButton.setAlpha(f2);
            this.mDismissButton.setAlpha(f2);
            i = (int) (((f2 * 0.3f) + (f * 0.6f)) * 255.0f);
        } else {
            if (i2 == 1) {
                this.mDismissButton.setAlpha(f2);
            }
            i = (int) (f * 0.6f * 255.0f);
        }
        this.mBackgroundDrawable.setAlpha(i);
    }

    private void notifyRegisterInputConsumer() {
        Message obtain = Message.obtain();
        obtain.what = 105;
        sendMessage(obtain, "Could not notify controller to register input consumer");
    }

    private void notifyUnregisterInputConsumer() {
        Message obtain = Message.obtain();
        obtain.what = 106;
        sendMessage(obtain, "Could not notify controller to unregister input consumer");
    }

    private void notifyMenuStateChange(int i) {
        this.mMenuState = i;
        Message obtain = Message.obtain();
        obtain.what = 100;
        obtain.arg1 = i;
        sendMessage(obtain, "Could not notify controller of PIP menu visibility");
    }

    private void expandPip() {
        hideMenu(new Runnable() {
            /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$gxeJOYpgn30UbyKen9nD4GpRdFQ */

            public final void run() {
                PipMenuActivity.this.lambda$expandPip$7$PipMenuActivity();
            }
        }, false, false);
    }

    public /* synthetic */ void lambda$expandPip$7$PipMenuActivity() {
        sendEmptyMessage(101, "Could not notify controller to expand PIP");
    }

    private void dismissPip() {
        hideMenu(new Runnable() {
            /* class com.android.systemui.pip.phone.$$Lambda$PipMenuActivity$guHLrBiStjvmB9r01MbFqRGaK3c */

            public final void run() {
                PipMenuActivity.this.lambda$dismissPip$8$PipMenuActivity();
            }
        }, false, true);
    }

    public /* synthetic */ void lambda$dismissPip$8$PipMenuActivity() {
        sendEmptyMessage(103, "Could not notify controller to dismiss PIP");
    }

    private void showPipMenu() {
        Message obtain = Message.obtain();
        obtain.what = 107;
        sendMessage(obtain, "Could not notify controller to show PIP menu");
    }

    private void showSettings() {
        Pair<ComponentName, Integer> topPinnedActivity = PipUtils.getTopPinnedActivity(this, ActivityManager.getService());
        if (topPinnedActivity.first != null) {
            UserHandle of = UserHandle.of(((Integer) topPinnedActivity.second).intValue());
            Intent intent = new Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.fromParts("package", ((ComponentName) topPinnedActivity.first).getPackageName(), null));
            intent.putExtra("android.intent.extra.user_handle", of);
            intent.setFlags(268468224);
            startActivity(intent);
        }
    }

    private void notifyActivityCallback(Messenger messenger) {
        Message obtain = Message.obtain();
        obtain.what = 104;
        obtain.replyTo = messenger;
        sendMessage(obtain, "Could not notify controller of activity finished");
    }

    private void sendEmptyMessage(int i, String str) {
        Message obtain = Message.obtain();
        obtain.what = i;
        sendMessage(obtain, str);
    }

    private void sendMessage(Message message, String str) {
        Messenger messenger = this.mToControllerMessenger;
        if (messenger != null) {
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                Log.e("PipMenuActivity", str, e);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void cancelDelayedFinish() {
        this.mHandler.removeCallbacks(this.mFinishRunnable);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void repostDelayedFinish(int i) {
        int recommendedTimeoutMillis = this.mAccessibilityManager.getRecommendedTimeoutMillis(i, 5);
        this.mHandler.removeCallbacks(this.mFinishRunnable);
        this.mHandler.postDelayed(this.mFinishRunnable, (long) recommendedTimeoutMillis);
    }
}
