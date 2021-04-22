package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.IconDrawableFactory;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewDebug;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0007R$id;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.PackageManagerWrapper;

public class TaskViewHeader extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {
    private static IconDrawableFactory sDrawableFactory;
    ImageView mAppIconView;
    String mAppInfoDescFormat;
    ImageView mAppInfoView;
    FrameLayout mAppOverlayView;
    TextView mAppTitleView;
    private HighlightColorDrawable mBackground;
    int mCornerRadius;
    Drawable mDarkDismissDrawable;
    Drawable mDarkFullscreenIcon;
    Drawable mDarkInfoIcon;
    @ViewDebug.ExportedProperty(category = "recents")
    float mDimAlpha;
    private Paint mDimLayerPaint;
    int mDisabledTaskBarBackgroundColor;
    ImageView mDismissButton;
    String mDismissDescFormat;
    private CountDownTimer mFocusTimerCountDown;
    ProgressBar mFocusTimerIndicator;
    int mHeaderBarHeight;
    int mHeaderButtonPadding;
    int mHighlightHeight;
    ImageView mIconView;
    Drawable mLightDismissDrawable;
    Drawable mLightFullscreenIcon;
    Drawable mLightInfoIcon;
    ImageView mMoveTaskButton;
    private HighlightColorDrawable mOverlayBackground;
    private boolean mShouldDarkenBackgroundColor;
    Task mTask;
    int mTaskBarViewDarkTextColor;
    int mTaskBarViewLightTextColor;
    @ViewDebug.ExportedProperty(category = "recents")
    Rect mTaskViewRect;
    int mTaskWindowingMode;
    TextView mTitleView;
    private float[] mTmpHSL;

    /* access modifiers changed from: protected */
    public int[] onCreateDrawableState(int i) {
        return new int[0];
    }

    /* access modifiers changed from: private */
    public class HighlightColorDrawable extends Drawable {
        private Paint mBackgroundPaint = new Paint();
        private int mColor;
        private float mDimAlpha;
        private Paint mHighlightPaint = new Paint();

        public int getOpacity() {
            return -1;
        }

        public void setAlpha(int i) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        public HighlightColorDrawable() {
            this.mBackgroundPaint.setColor(Color.argb(255, 0, 0, 0));
            this.mBackgroundPaint.setAntiAlias(true);
            this.mHighlightPaint.setColor(Color.argb(255, 255, 255, 255));
            this.mHighlightPaint.setAntiAlias(true);
        }

        public void setColorAndDim(int i, float f) {
            if (this.mColor != i || Float.compare(this.mDimAlpha, f) != 0) {
                this.mColor = i;
                this.mDimAlpha = f;
                if (TaskViewHeader.this.mShouldDarkenBackgroundColor) {
                    i = TaskViewHeader.this.getSecondaryColor(i, false);
                }
                this.mBackgroundPaint.setColor(i);
                ColorUtils.colorToHSL(i, TaskViewHeader.this.mTmpHSL);
                TaskViewHeader.this.mTmpHSL[2] = Math.min(1.0f, TaskViewHeader.this.mTmpHSL[2] + ((1.0f - f) * 0.075f));
                this.mHighlightPaint.setColor(ColorUtils.HSLToColor(TaskViewHeader.this.mTmpHSL));
                invalidateSelf();
            }
        }

        public void draw(Canvas canvas) {
            float width = (float) TaskViewHeader.this.mTaskViewRect.width();
            TaskViewHeader taskViewHeader = TaskViewHeader.this;
            float max = (float) (Math.max(taskViewHeader.mHighlightHeight, taskViewHeader.mCornerRadius) * 2);
            int i = TaskViewHeader.this.mCornerRadius;
            canvas.drawRoundRect(0.0f, 0.0f, width, max, (float) i, (float) i, this.mHighlightPaint);
            TaskViewHeader taskViewHeader2 = TaskViewHeader.this;
            float f = (float) taskViewHeader2.mHighlightHeight;
            float width2 = (float) taskViewHeader2.mTaskViewRect.width();
            int height = TaskViewHeader.this.getHeight();
            int i2 = TaskViewHeader.this.mCornerRadius;
            canvas.drawRoundRect(0.0f, f, width2, (float) (height + i2), (float) i2, (float) i2, this.mBackgroundPaint);
        }

        public int getColor() {
            return this.mColor;
        }
    }

    public TaskViewHeader(Context context) {
        this(context, null);
    }

    public TaskViewHeader(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        int i3;
        this.mTaskViewRect = new Rect();
        this.mTaskWindowingMode = 0;
        this.mTmpHSL = new float[3];
        this.mDimLayerPaint = new Paint();
        this.mShouldDarkenBackgroundColor = false;
        setWillNotDraw(false);
        Resources resources = context.getResources();
        this.mLightDismissDrawable = context.getDrawable(2131231758);
        this.mDarkDismissDrawable = context.getDrawable(2131231757);
        if (LegacyRecentsImpl.getConfiguration().isGridEnabled) {
            i3 = resources.getDimensionPixelSize(2131166225);
        } else {
            i3 = resources.getDimensionPixelSize(2131166261);
        }
        this.mCornerRadius = i3;
        this.mHighlightHeight = resources.getDimensionPixelSize(2131166258);
        this.mTaskBarViewLightTextColor = context.getColor(2131100043);
        this.mTaskBarViewDarkTextColor = context.getColor(2131100039);
        this.mLightFullscreenIcon = context.getDrawable(2131231768);
        this.mDarkFullscreenIcon = context.getDrawable(2131231767);
        this.mLightInfoIcon = context.getDrawable(2131231762);
        this.mDarkInfoIcon = context.getDrawable(2131231761);
        this.mDisabledTaskBarBackgroundColor = context.getColor(2131100041);
        this.mDismissDescFormat = ((FrameLayout) this).mContext.getString(2131820759);
        this.mAppInfoDescFormat = ((FrameLayout) this).mContext.getString(2131820758);
        this.mBackground = new HighlightColorDrawable();
        this.mBackground.setColorAndDim(Color.argb(255, 0, 0, 0), 0.0f);
        setBackground(this.mBackground);
        this.mOverlayBackground = new HighlightColorDrawable();
        this.mDimLayerPaint.setColor(Color.argb(255, 0, 0, 0));
        this.mDimLayerPaint.setAntiAlias(true);
    }

    public void reset() {
        hideAppOverlay(true);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        LegacyRecentsImpl.getSystemServices();
        this.mIconView = (ImageView) findViewById(2131362286);
        this.mIconView.setOnLongClickListener(this);
        this.mTitleView = (TextView) findViewById(2131362906);
        this.mDismissButton = (ImageView) findViewById(2131362144);
        onConfigurationChanged();
    }

    private void updateLayoutParams(View view, View view2, View view3, View view4) {
        int i;
        setLayoutParams(new FrameLayout.LayoutParams(-1, this.mHeaderBarHeight, 48));
        int i2 = this.mHeaderBarHeight;
        view.setLayoutParams(new FrameLayout.LayoutParams(i2, i2, 8388611));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2, 8388627);
        layoutParams.setMarginStart(this.mHeaderBarHeight);
        if (this.mMoveTaskButton != null) {
            i = this.mHeaderBarHeight * 2;
        } else {
            i = this.mHeaderBarHeight;
        }
        layoutParams.setMarginEnd(i);
        view2.setLayoutParams(layoutParams);
        if (view3 != null) {
            int i3 = this.mHeaderBarHeight;
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(i3, i3, 8388613);
            layoutParams2.setMarginEnd(this.mHeaderBarHeight);
            view3.setLayoutParams(layoutParams2);
            int i4 = this.mHeaderButtonPadding;
            view3.setPadding(i4, i4, i4, i4);
        }
        int i5 = this.mHeaderBarHeight;
        view4.setLayoutParams(new FrameLayout.LayoutParams(i5, i5, 8388613));
        int i6 = this.mHeaderButtonPadding;
        view4.setPadding(i6, i6, i6, i6);
    }

    public void onConfigurationChanged() {
        getResources();
        int dimensionForDevice = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), 2131166256, 2131166256, 2131166256, 2131166257, 2131166256, 2131166257, 2131166224);
        int dimensionForDevice2 = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), 2131166254, 2131166254, 2131166254, 2131166255, 2131166254, 2131166255, 2131166223);
        if (dimensionForDevice != this.mHeaderBarHeight || dimensionForDevice2 != this.mHeaderButtonPadding) {
            this.mHeaderBarHeight = dimensionForDevice;
            this.mHeaderButtonPadding = dimensionForDevice2;
            updateLayoutParams(this.mIconView, this.mTitleView, this.mMoveTaskButton, this.mDismissButton);
            if (this.mAppOverlayView != null) {
                updateLayoutParams(this.mAppIconView, this.mAppTitleView, null, this.mAppInfoView);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        onTaskViewSizeChanged(this.mTaskViewRect.width(), this.mTaskViewRect.height());
    }

    public void onTaskViewSizeChanged(int i, int i2) {
        this.mTaskViewRect.set(0, 0, i, i2);
        int measuredWidth = i - getMeasuredWidth();
        this.mTitleView.setVisibility(0);
        ImageView imageView = this.mMoveTaskButton;
        if (imageView != null) {
            imageView.setVisibility(0);
            this.mMoveTaskButton.setTranslationX((float) measuredWidth);
        }
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.setTranslationX((float) measuredWidth);
        setLeftTopRightBottom(0, 0, i, getMeasuredHeight());
    }

    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        float width = (float) this.mTaskViewRect.width();
        int height = getHeight();
        int i = this.mCornerRadius;
        canvas.drawRoundRect(0.0f, 0.0f, width, (float) (height + i), (float) i, (float) i, this.mDimLayerPaint);
    }

    public void startFocusTimerIndicator(int i) {
        ProgressBar progressBar = this.mFocusTimerIndicator;
        if (progressBar != null) {
            progressBar.setVisibility(0);
            this.mFocusTimerIndicator.setMax(i);
            this.mFocusTimerIndicator.setProgress(i);
            CountDownTimer countDownTimer = this.mFocusTimerCountDown;
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            this.mFocusTimerCountDown = new CountDownTimer((long) i, 30) {
                /* class com.android.systemui.recents.views.TaskViewHeader.AnonymousClass1 */

                public void onFinish() {
                }

                public void onTick(long j) {
                    TaskViewHeader.this.mFocusTimerIndicator.setProgress((int) j);
                }
            }.start();
        }
    }

    public void cancelFocusTimerIndicator() {
        CountDownTimer countDownTimer;
        if (this.mFocusTimerIndicator != null && (countDownTimer = this.mFocusTimerCountDown) != null) {
            countDownTimer.cancel();
            this.mFocusTimerIndicator.setProgress(0);
            this.mFocusTimerIndicator.setVisibility(4);
        }
    }

    public ImageView getIconView() {
        return this.mIconView;
    }

    /* access modifiers changed from: package-private */
    public int getSecondaryColor(int i, boolean z) {
        return Utilities.getColorWithOverlay(i, z ? -1 : -16777216, 0.8f);
    }

    public void setDimAlpha(float f) {
        if (Float.compare(this.mDimAlpha, f) != 0) {
            this.mDimAlpha = f;
            this.mTitleView.setAlpha(1.0f - f);
            updateBackgroundColor(this.mBackground.getColor(), f);
        }
    }

    private void updateBackgroundColor(int i, float f) {
        if (this.mTask != null) {
            this.mBackground.setColorAndDim(i, f);
            ColorUtils.colorToHSL(i, this.mTmpHSL);
            float[] fArr = this.mTmpHSL;
            fArr[2] = Math.min(1.0f, fArr[2] + ((1.0f - f) * -0.0625f));
            this.mOverlayBackground.setColorAndDim(ColorUtils.HSLToColor(this.mTmpHSL), f);
            this.mDimLayerPaint.setAlpha((int) (f * 255.0f));
            invalidate();
        }
    }

    public void setShouldDarkenBackgroundColor(boolean z) {
        this.mShouldDarkenBackgroundColor = z;
    }

    public void bindToTask(Task task, boolean z, boolean z2) {
        int i;
        this.mTask = task;
        if (z2) {
            i = this.mDisabledTaskBarBackgroundColor;
        } else {
            i = task.colorPrimary;
        }
        if (this.mBackground.getColor() != i) {
            updateBackgroundColor(i, this.mDimAlpha);
        }
        if (!this.mTitleView.getText().toString().equals(task.title)) {
            this.mTitleView.setText(task.title);
        }
        this.mTitleView.setContentDescription(task.titleDescription);
        this.mTitleView.setTextColor(task.useLightOnPrimaryColor ? this.mTaskBarViewLightTextColor : this.mTaskBarViewDarkTextColor);
        this.mDismissButton.setImageDrawable(task.useLightOnPrimaryColor ? this.mLightDismissDrawable : this.mDarkDismissDrawable);
        this.mDismissButton.setContentDescription(String.format(this.mDismissDescFormat, task.titleDescription));
        this.mDismissButton.setOnClickListener(this);
        this.mDismissButton.setClickable(false);
        ((RippleDrawable) this.mDismissButton.getBackground()).setForceSoftware(true);
        if (z) {
            this.mIconView.setContentDescription(String.format(this.mAppInfoDescFormat, task.titleDescription));
            this.mIconView.setOnClickListener(this);
            this.mIconView.setClickable(true);
        }
    }

    public void onTaskDataLoaded() {
        Drawable drawable;
        Task task = this.mTask;
        if (task != null && (drawable = task.icon) != null) {
            this.mIconView.setImageDrawable(drawable);
        }
    }

    /* access modifiers changed from: package-private */
    public void unbindFromTask(boolean z) {
        this.mTask = null;
        this.mIconView.setImageDrawable(null);
        if (z) {
            this.mIconView.setClickable(false);
        }
    }

    /* access modifiers changed from: package-private */
    public void startNoUserInteractionAnimation() {
        int integer = getResources().getInteger(2131427451);
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.setClickable(true);
        if (this.mDismissButton.getVisibility() == 0) {
            this.mDismissButton.animate().alpha(1.0f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration((long) integer).start();
        } else {
            this.mDismissButton.setAlpha(1.0f);
        }
        ImageView imageView = this.mMoveTaskButton;
        if (imageView == null) {
            return;
        }
        if (imageView.getVisibility() == 0) {
            this.mMoveTaskButton.setVisibility(0);
            this.mMoveTaskButton.setClickable(true);
            this.mMoveTaskButton.animate().alpha(1.0f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration((long) integer).start();
            return;
        }
        this.mMoveTaskButton.setAlpha(1.0f);
    }

    public void setNoUserInteractionState() {
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.animate().cancel();
        this.mDismissButton.setAlpha(1.0f);
        this.mDismissButton.setClickable(true);
        ImageView imageView = this.mMoveTaskButton;
        if (imageView != null) {
            imageView.setVisibility(0);
            this.mMoveTaskButton.animate().cancel();
            this.mMoveTaskButton.setAlpha(1.0f);
            this.mMoveTaskButton.setClickable(true);
        }
    }

    /* access modifiers changed from: package-private */
    public void resetNoUserInteractionState() {
        this.mDismissButton.setVisibility(4);
        this.mDismissButton.setAlpha(0.0f);
        this.mDismissButton.setClickable(false);
        ImageView imageView = this.mMoveTaskButton;
        if (imageView != null) {
            imageView.setVisibility(4);
            this.mMoveTaskButton.setAlpha(0.0f);
            this.mMoveTaskButton.setClickable(false);
        }
    }

    public void onClick(View view) {
        if (view == this.mIconView) {
            EventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
        } else if (view == this.mDismissButton) {
            ((TaskView) Utilities.findParent(this, TaskView.class)).dismissTask();
            MetricsLogger.histogram(getContext(), "overview_task_dismissed_source", 2);
        } else if (view == this.mMoveTaskButton) {
            EventBus.getDefault().send(new LaunchTaskEvent((TaskView) Utilities.findParent(this, TaskView.class), this.mTask, null, false, this.mTaskWindowingMode, 0));
        } else if (view == this.mAppInfoView) {
            EventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
        } else if (view == this.mAppIconView) {
            hideAppOverlay(false);
        }
    }

    public boolean onLongClick(View view) {
        if (view == this.mIconView) {
            showAppOverlay();
            return true;
        } else if (view != this.mAppIconView) {
            return false;
        } else {
            hideAppOverlay(false);
            return true;
        }
    }

    private void showAppOverlay() {
        Drawable drawable;
        LegacyRecentsImpl.getSystemServices();
        ComponentName component = this.mTask.key.getComponent();
        int i = this.mTask.key.userId;
        ActivityInfo activityInfo = PackageManagerWrapper.getInstance().getActivityInfo(component, i);
        if (activityInfo != null) {
            if (this.mAppOverlayView == null) {
                this.mAppOverlayView = (FrameLayout) Utilities.findViewStubById(this, 2131361926).inflate();
                this.mAppOverlayView.setBackground(this.mOverlayBackground);
                this.mAppIconView = (ImageView) this.mAppOverlayView.findViewById(C0007R$id.app_icon);
                this.mAppIconView.setOnClickListener(this);
                this.mAppIconView.setOnLongClickListener(this);
                this.mAppInfoView = (ImageView) this.mAppOverlayView.findViewById(2131361921);
                this.mAppInfoView.setOnClickListener(this);
                this.mAppTitleView = (TextView) this.mAppOverlayView.findViewById(2131361929);
                updateLayoutParams(this.mAppIconView, this.mAppTitleView, null, this.mAppInfoView);
            }
            this.mAppTitleView.setText(ActivityManagerWrapper.getInstance().getBadgedApplicationLabel(activityInfo.applicationInfo, i));
            this.mAppTitleView.setTextColor(this.mTask.useLightOnPrimaryColor ? this.mTaskBarViewLightTextColor : this.mTaskBarViewDarkTextColor);
            this.mAppIconView.setImageDrawable(getIconDrawableFactory().getBadgedIcon(activityInfo.applicationInfo, i));
            ImageView imageView = this.mAppInfoView;
            if (this.mTask.useLightOnPrimaryColor) {
                drawable = this.mLightInfoIcon;
            } else {
                drawable = this.mDarkInfoIcon;
            }
            imageView.setImageDrawable(drawable);
            this.mAppOverlayView.setVisibility(0);
            Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this.mAppOverlayView, this.mIconView.getLeft() + (this.mIconView.getWidth() / 2), this.mIconView.getTop() + (this.mIconView.getHeight() / 2), 0.0f, (float) getWidth());
            createCircularReveal.setDuration(250);
            createCircularReveal.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            createCircularReveal.start();
        }
    }

    private void hideAppOverlay(boolean z) {
        FrameLayout frameLayout = this.mAppOverlayView;
        if (frameLayout != null) {
            if (z) {
                frameLayout.setVisibility(8);
                return;
            }
            Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this.mAppOverlayView, this.mIconView.getLeft() + (this.mIconView.getWidth() / 2), this.mIconView.getTop() + (this.mIconView.getHeight() / 2), (float) getWidth(), 0.0f);
            createCircularReveal.setDuration(250);
            createCircularReveal.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            createCircularReveal.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.recents.views.TaskViewHeader.AnonymousClass2 */

                public void onAnimationEnd(Animator animator) {
                    TaskViewHeader.this.mAppOverlayView.setVisibility(8);
                }
            });
            createCircularReveal.start();
        }
    }

    private static IconDrawableFactory getIconDrawableFactory() {
        if (sDrawableFactory == null) {
            sDrawableFactory = IconDrawableFactory.newInstance(AppGlobals.getInitialApplication());
        }
        return sDrawableFactory;
    }
}
