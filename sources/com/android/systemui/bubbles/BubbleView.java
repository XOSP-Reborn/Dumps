package com.android.systemui.bubbles;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.internal.graphics.ColorUtils;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;

public class BubbleView extends FrameLayout {
    private int mBadgeColor;
    private BadgedImageView mBadgedImageView;
    private Context mContext;
    private NotificationEntry mEntry;
    private int mIconInset;
    private int mPadding;
    private boolean mSuppressDot;

    public BubbleView(Context context) {
        this(context, null);
    }

    public BubbleView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BubbleView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public BubbleView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mSuppressDot = false;
        this.mContext = context;
        this.mPadding = getResources().getDimensionPixelSize(C0005R$dimen.bubble_view_padding);
        this.mIconInset = getResources().getDimensionPixelSize(C0005R$dimen.bubble_icon_inset);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBadgedImageView = (BadgedImageView) findViewById(C0007R$id.bubble_image);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void setNotif(NotificationEntry notificationEntry) {
        this.mEntry = notificationEntry;
        updateViews();
    }

    public NotificationEntry getEntry() {
        return this.mEntry;
    }

    public String getKey() {
        NotificationEntry notificationEntry = this.mEntry;
        if (notificationEntry != null) {
            return notificationEntry.key;
        }
        return null;
    }

    public void update(NotificationEntry notificationEntry) {
        this.mEntry = notificationEntry;
        updateViews();
    }

    /* access modifiers changed from: package-private */
    public void updateDotVisibility(boolean z) {
        updateDotVisibility(z, null);
    }

    /* access modifiers changed from: package-private */
    public void setSuppressDot(boolean z, boolean z2) {
        this.mSuppressDot = z;
        updateDotVisibility(z2);
    }

    /* access modifiers changed from: package-private */
    public void setDotPosition(boolean z, boolean z2) {
        if (!z2 || z == this.mBadgedImageView.getDotPosition() || this.mSuppressDot) {
            this.mBadgedImageView.setDotPosition(z);
        } else {
            animateDot(false, new Runnable(z) {
                /* class com.android.systemui.bubbles.$$Lambda$BubbleView$8ligGzJrQjv0I1bfTGAaM0HpBQ */
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    BubbleView.this.lambda$setDotPosition$0$BubbleView(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$setDotPosition$0$BubbleView(boolean z) {
        this.mBadgedImageView.setDotPosition(z);
        animateDot(true, null);
    }

    /* access modifiers changed from: package-private */
    public boolean getDotPositionOnLeft() {
        return this.mBadgedImageView.getDotPosition();
    }

    private void updateDotVisibility(boolean z, Runnable runnable) {
        boolean z2 = getEntry().showInShadeWhenBubble() && !this.mSuppressDot;
        if (z) {
            animateDot(z2, runnable);
        } else {
            this.mBadgedImageView.setShowDot(z2);
        }
    }

    private void animateDot(boolean z, Runnable runnable) {
        if (this.mBadgedImageView.isShowingDot() != z) {
            if (z) {
                this.mBadgedImageView.setShowDot(true);
            }
            this.mBadgedImageView.clearAnimation();
            this.mBadgedImageView.animate().setDuration(200).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setUpdateListener(new ValueAnimator.AnimatorUpdateListener(z) {
                /* class com.android.systemui.bubbles.$$Lambda$BubbleView$ZKBfBG7GijRplpU_8yCIB0NuyLk */
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    BubbleView.this.lambda$animateDot$1$BubbleView(this.f$1, valueAnimator);
                }
            }).withEndAction(new Runnable(z, runnable) {
                /* class com.android.systemui.bubbles.$$Lambda$BubbleView$xA_RAS_mSp_JlDsmI3wc705YhoQ */
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ Runnable f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    BubbleView.this.lambda$animateDot$2$BubbleView(this.f$1, this.f$2);
                }
            }).start();
        }
    }

    public /* synthetic */ void lambda$animateDot$1$BubbleView(boolean z, ValueAnimator valueAnimator) {
        float animatedFraction = valueAnimator.getAnimatedFraction();
        if (!z) {
            animatedFraction = 1.0f - animatedFraction;
        }
        this.mBadgedImageView.setDotScale(animatedFraction);
    }

    public /* synthetic */ void lambda$animateDot$2$BubbleView(boolean z, Runnable runnable) {
        if (!z) {
            this.mBadgedImageView.setShowDot(false);
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    /* access modifiers changed from: package-private */
    public void updateViews() {
        Icon icon;
        NotificationEntry notificationEntry = this.mEntry;
        if (notificationEntry != null) {
            Notification.BubbleMetadata bubbleMetadata = notificationEntry.getBubbleMetadata();
            Notification notification = this.mEntry.notification.getNotification();
            boolean z = true;
            if (bubbleMetadata != null) {
                icon = bubbleMetadata.getIcon();
                if (icon.getType() == 5) {
                    z = false;
                }
            } else {
                if (notification.getLargeIcon() != null) {
                    z = false;
                }
                icon = z ? notification.getSmallIcon() : notification.getLargeIcon();
            }
            Drawable loadDrawable = icon.loadDrawable(this.mContext);
            if (z) {
                this.mBadgedImageView.setImageDrawable(buildIconWithTint(loadDrawable, notification.color));
            } else {
                this.mBadgedImageView.setImageDrawable(loadDrawable);
            }
            int determineDominateColor = determineDominateColor(loadDrawable, notification.color);
            this.mBadgeColor = determineDominateColor;
            this.mBadgedImageView.setDotColor(determineDominateColor);
            animateDot(this.mEntry.showInShadeWhenBubble(), null);
        }
    }

    /* access modifiers changed from: package-private */
    public int getBadgeColor() {
        return this.mBadgeColor;
    }

    private Drawable buildIconWithTint(Drawable drawable, int i) {
        checkTint(drawable, i);
        return new AdaptiveIconDrawable(new ColorDrawable(i), new InsetDrawable(drawable, this.mIconInset));
    }

    private Drawable checkTint(Drawable drawable, int i) {
        int alphaComponent = ColorUtils.setAlphaComponent(i, 255);
        if (alphaComponent == 0) {
            alphaComponent = -3355444;
        }
        drawable.setTint(-1);
        if (ColorUtils.calculateContrast(-1, alphaComponent) < 4.1d) {
            drawable.setTint(ColorUtils.setAlphaComponent(-16777216, 180));
        }
        return drawable;
    }

    private int determineDominateColor(Drawable drawable, int i) {
        return ColorUtils.blendARGB(i, -1, 0.54f);
    }
}
