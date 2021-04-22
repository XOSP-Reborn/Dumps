package com.android.systemui.statusbar.notification.row.wrapper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.util.ContrastColorUtil;
import com.android.internal.widget.NotificationActionListLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;
import com.android.systemui.statusbar.notification.ImageTransformState;
import com.android.systemui.statusbar.notification.TransformState;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.HybridNotificationView;

public class NotificationTemplateViewWrapper extends NotificationHeaderViewWrapper {
    private NotificationActionListLayout mActions;
    protected View mActionsContainer;
    private ArraySet<PendingIntent> mCancelledPendingIntents = new ArraySet<>();
    private int mContentHeight;
    private float mHeaderTranslation;
    private int mMinHeightHint;
    protected ImageView mPicture;
    private ProgressBar mProgressBar;
    private View mRemoteInputHistory;
    private ImageView mReplyAction;
    private TextView mText;
    private TextView mTitle;
    private Rect mTmpRect = new Rect();
    private final int mTranslationForHeader;
    private UiOffloadThread mUiOffloadThread;

    protected NotificationTemplateViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
        this.mTransformationHelper.setCustomTransformation(new ViewTransformationHelper.CustomTransformation() {
            /* class com.android.systemui.statusbar.notification.row.wrapper.NotificationTemplateViewWrapper.AnonymousClass1 */

            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean transformTo(TransformState transformState, TransformableView transformableView, float f) {
                if (!(transformableView instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState currentState = transformableView.getCurrentState(1);
                CrossFadeHelper.fadeOut(transformState.getTransformedView(), f);
                if (currentState != null) {
                    transformState.transformViewVerticalTo(currentState, this, f);
                    currentState.recycle();
                }
                return true;
            }

            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean customTransformTarget(TransformState transformState, TransformState transformState2) {
                transformState.setTransformationEndY(getTransformationY(transformState, transformState2));
                return true;
            }

            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean transformFrom(TransformState transformState, TransformableView transformableView, float f) {
                if (!(transformableView instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState currentState = transformableView.getCurrentState(1);
                CrossFadeHelper.fadeIn(transformState.getTransformedView(), f);
                if (currentState != null) {
                    transformState.transformViewVerticalFrom(currentState, this, f);
                    currentState.recycle();
                }
                return true;
            }

            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean initTransformation(TransformState transformState, TransformState transformState2) {
                transformState.setTransformationStartY(getTransformationY(transformState, transformState2));
                return true;
            }

            private float getTransformationY(TransformState transformState, TransformState transformState2) {
                return ((float) ((transformState2.getLaidOutLocationOnScreen()[1] + transformState2.getTransformedView().getHeight()) - transformState.getLaidOutLocationOnScreen()[1])) * 0.33f;
            }
        }, 2);
        this.mTranslationForHeader = context.getResources().getDimensionPixelSize(17105311) - context.getResources().getDimensionPixelSize(17105314);
    }

    private void resolveTemplateViews(StatusBarNotification statusBarNotification) {
        this.mPicture = (ImageView) this.mView.findViewById(16909300);
        ImageView imageView = this.mPicture;
        if (imageView != null) {
            imageView.setTag(ImageTransformState.ICON_TAG, statusBarNotification.getNotification().getLargeIcon());
        }
        this.mTitle = (TextView) this.mView.findViewById(16908310);
        this.mText = (TextView) this.mView.findViewById(16909435);
        View findViewById = this.mView.findViewById(16908301);
        if (findViewById instanceof ProgressBar) {
            this.mProgressBar = (ProgressBar) findViewById;
        } else {
            this.mProgressBar = null;
        }
        this.mActionsContainer = this.mView.findViewById(16908695);
        this.mActions = this.mView.findViewById(16908694);
        this.mReplyAction = (ImageView) this.mView.findViewById(16909290);
        this.mRemoteInputHistory = this.mView.findViewById(16909171);
        updatePendingIntentCancellations();
    }

    private void updatePendingIntentCancellations() {
        NotificationActionListLayout notificationActionListLayout = this.mActions;
        if (notificationActionListLayout != null) {
            int childCount = notificationActionListLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                Button button = (Button) this.mActions.getChildAt(i);
                performOnPendingIntentCancellation(button, new Runnable(button) {
                    /* class com.android.systemui.statusbar.notification.row.wrapper.$$Lambda$NotificationTemplateViewWrapper$JRq0wlJLDK40PaCOgvvnny6lB0w */
                    private final /* synthetic */ Button f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NotificationTemplateViewWrapper.this.lambda$updatePendingIntentCancellations$0$NotificationTemplateViewWrapper(this.f$1);
                    }
                });
            }
        }
        ImageView imageView = this.mReplyAction;
        if (imageView != null) {
            imageView.setEnabled(true);
            performOnPendingIntentCancellation(this.mReplyAction, new Runnable() {
                /* class com.android.systemui.statusbar.notification.row.wrapper.$$Lambda$NotificationTemplateViewWrapper$Znytf0R_oPxyrIENjI1T5rfvZf4 */

                public final void run() {
                    NotificationTemplateViewWrapper.this.lambda$updatePendingIntentCancellations$1$NotificationTemplateViewWrapper();
                }
            });
        }
    }

    public /* synthetic */ void lambda$updatePendingIntentCancellations$0$NotificationTemplateViewWrapper(Button button) {
        if (button.isEnabled()) {
            button.setEnabled(false);
            ColorStateList textColors = button.getTextColors();
            int[] colors = textColors.getColors();
            int[] iArr = new int[colors.length];
            float f = this.mView.getResources().getFloat(17105300);
            for (int i = 0; i < colors.length; i++) {
                iArr[i] = blendColorWithBackground(colors[i], f);
            }
            button.setTextColor(new ColorStateList(textColors.getStates(), iArr));
        }
    }

    public /* synthetic */ void lambda$updatePendingIntentCancellations$1$NotificationTemplateViewWrapper() {
        ImageView imageView = this.mReplyAction;
        if (imageView != null && imageView.isEnabled()) {
            this.mReplyAction.setEnabled(false);
            Drawable mutate = this.mReplyAction.getDrawable().mutate();
            PorterDuffColorFilter porterDuffColorFilter = (PorterDuffColorFilter) mutate.getColorFilter();
            float f = this.mView.getResources().getFloat(17105300);
            if (porterDuffColorFilter != null) {
                mutate.mutate().setColorFilter(blendColorWithBackground(porterDuffColorFilter.getColor(), f), porterDuffColorFilter.getMode());
                return;
            }
            this.mReplyAction.setAlpha(f);
        }
    }

    private int blendColorWithBackground(int i, float f) {
        return ContrastColorUtil.compositeColors(Color.argb((int) (f * 255.0f), Color.red(i), Color.green(i), Color.blue(i)), resolveBackgroundColor());
    }

    private void performOnPendingIntentCancellation(View view, Runnable runnable) {
        final PendingIntent pendingIntent = (PendingIntent) view.getTag(16909225);
        if (pendingIntent != null) {
            if (this.mCancelledPendingIntents.contains(pendingIntent)) {
                runnable.run();
                return;
            }
            final $$Lambda$NotificationTemplateViewWrapper$JW7SqyfmhP6HCTJ8F1p53b90n6s r1 = new PendingIntent.CancelListener(pendingIntent, runnable) {
                /* class com.android.systemui.statusbar.notification.row.wrapper.$$Lambda$NotificationTemplateViewWrapper$JW7SqyfmhP6HCTJ8F1p53b90n6s */
                private final /* synthetic */ PendingIntent f$1;
                private final /* synthetic */ Runnable f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void onCancelled(PendingIntent pendingIntent) {
                    NotificationTemplateViewWrapper.this.lambda$performOnPendingIntentCancellation$3$NotificationTemplateViewWrapper(this.f$1, this.f$2, pendingIntent);
                }
            };
            if (this.mUiOffloadThread == null) {
                this.mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
            }
            if (view.isAttachedToWindow()) {
                this.mUiOffloadThread.submit(new Runnable(pendingIntent, r1) {
                    /* class com.android.systemui.statusbar.notification.row.wrapper.$$Lambda$NotificationTemplateViewWrapper$qLtzjAQEVXJmd7CTS0Q7hmNVWkU */
                    private final /* synthetic */ PendingIntent f$0;
                    private final /* synthetic */ PendingIntent.CancelListener f$1;

                    {
                        this.f$0 = r1;
                        this.f$1 = r2;
                    }

                    public final void run() {
                        this.f$0.registerCancelListener(this.f$1);
                    }
                });
            }
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                /* class com.android.systemui.statusbar.notification.row.wrapper.NotificationTemplateViewWrapper.AnonymousClass2 */

                public void onViewAttachedToWindow(View view) {
                    NotificationTemplateViewWrapper.this.mUiOffloadThread.submit(new Runnable(pendingIntent, r1) {
                        /* class com.android.systemui.statusbar.notification.row.wrapper.$$Lambda$NotificationTemplateViewWrapper$2$GihuSx3OPFqk7UFX7W5ZofmkRI */
                        private final /* synthetic */ PendingIntent f$0;
                        private final /* synthetic */ PendingIntent.CancelListener f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void run() {
                            this.f$0.registerCancelListener(this.f$1);
                        }
                    });
                }

                public void onViewDetachedFromWindow(View view) {
                    NotificationTemplateViewWrapper.this.mUiOffloadThread.submit(new Runnable(pendingIntent, r1) {
                        /* class com.android.systemui.statusbar.notification.row.wrapper.$$Lambda$NotificationTemplateViewWrapper$2$YHJcr04bTyX63VZ5BMhNHsutz1Y */
                        private final /* synthetic */ PendingIntent f$0;
                        private final /* synthetic */ PendingIntent.CancelListener f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void run() {
                            this.f$0.unregisterCancelListener(this.f$1);
                        }
                    });
                }
            });
        }
    }

    public /* synthetic */ void lambda$performOnPendingIntentCancellation$3$NotificationTemplateViewWrapper(PendingIntent pendingIntent, Runnable runnable, PendingIntent pendingIntent2) {
        this.mView.post(new Runnable(pendingIntent, runnable) {
            /* class com.android.systemui.statusbar.notification.row.wrapper.$$Lambda$NotificationTemplateViewWrapper$W5E5gGqEebINqbELkmQR7ZZYP8Q */
            private final /* synthetic */ PendingIntent f$1;
            private final /* synthetic */ Runnable f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                NotificationTemplateViewWrapper.this.lambda$performOnPendingIntentCancellation$2$NotificationTemplateViewWrapper(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$performOnPendingIntentCancellation$2$NotificationTemplateViewWrapper(PendingIntent pendingIntent, Runnable runnable) {
        this.mCancelledPendingIntents.add(pendingIntent);
        runnable.run();
    }

    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper
    public boolean disallowSingleClick(float f, float f2) {
        ImageView imageView = this.mReplyAction;
        if (imageView == null || imageView.getVisibility() != 0 || (!isOnView(this.mReplyAction, f, f2) && !isOnView(this.mPicture, f, f2))) {
            return super.disallowSingleClick(f, f2);
        }
        return true;
    }

    private boolean isOnView(View view, float f, float f2) {
        View view2 = (View) view.getParent();
        while (view2 != null && !(view2 instanceof ExpandableNotificationRow)) {
            view2.getHitRect(this.mTmpRect);
            Rect rect = this.mTmpRect;
            f -= (float) rect.left;
            f2 -= (float) rect.top;
            view2 = (View) view2.getParent();
        }
        view.getHitRect(this.mTmpRect);
        return this.mTmpRect.contains((int) f, (int) f2);
    }

    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper, com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper
    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        resolveTemplateViews(expandableNotificationRow.getStatusBarNotification());
        super.onContentUpdated(expandableNotificationRow);
        if (expandableNotificationRow.getHeaderVisibleAmount() != 1.0f) {
            setHeaderVisibleAmount(expandableNotificationRow.getHeaderVisibleAmount());
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper
    public void updateTransformedTypes() {
        super.updateTransformedTypes();
        TextView textView = this.mTitle;
        if (textView != null) {
            this.mTransformationHelper.addTransformedView(1, textView);
        }
        TextView textView2 = this.mText;
        if (textView2 != null) {
            this.mTransformationHelper.addTransformedView(2, textView2);
        }
        ImageView imageView = this.mPicture;
        if (imageView != null) {
            this.mTransformationHelper.addTransformedView(3, imageView);
        }
        ProgressBar progressBar = this.mProgressBar;
        if (progressBar != null) {
            this.mTransformationHelper.addTransformedView(4, progressBar);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper
    public void setContentHeight(int i, int i2) {
        super.setContentHeight(i, i2);
        this.mContentHeight = i;
        this.mMinHeightHint = i2;
        updateActionOffset();
    }

    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper
    public boolean shouldClipToRounding(boolean z, boolean z2) {
        View view;
        if (super.shouldClipToRounding(z, z2)) {
            return true;
        }
        if (!z2 || (view = this.mActionsContainer) == null || view.getVisibility() == 8) {
            return false;
        }
        return true;
    }

    private void updateActionOffset() {
        if (this.mActionsContainer != null) {
            this.mActionsContainer.setTranslationY((float) ((Math.max(this.mContentHeight, this.mMinHeightHint) - this.mView.getHeight()) - getHeaderTranslation()));
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper
    public int getHeaderTranslation() {
        return (int) this.mHeaderTranslation;
    }

    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper
    public void setHeaderVisibleAmount(float f) {
        super.setHeaderVisibleAmount(f);
        this.mNotificationHeader.setAlpha(f);
        this.mHeaderTranslation = (1.0f - f) * ((float) this.mTranslationForHeader);
        this.mView.setTranslationY(this.mHeaderTranslation);
    }

    @Override // com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper
    public int getExtraMeasureHeight() {
        NotificationActionListLayout notificationActionListLayout = this.mActions;
        int extraMeasureHeight = notificationActionListLayout != null ? notificationActionListLayout.getExtraMeasureHeight() : 0;
        View view = this.mRemoteInputHistory;
        if (!(view == null || view.getVisibility() == 8)) {
            extraMeasureHeight += this.mRow.getContext().getResources().getDimensionPixelSize(C0005R$dimen.remote_input_history_extra_height);
        }
        return extraMeasureHeight + super.getExtraMeasureHeight();
    }
}
