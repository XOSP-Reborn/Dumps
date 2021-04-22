package com.android.systemui.statusbar.policy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.text.TextPaint;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.R$styleable;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.phone.KeyguardDismissUtil;
import com.android.systemui.statusbar.policy.SmartReplyView;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class SmartReplyView extends ViewGroup {
    private static final Comparator<View> DECREASING_MEASURED_WIDTH_WITHOUT_PADDING_COMPARATOR = $$Lambda$SmartReplyView$UA3QkbRzztEFRlbb86djKcGIV5E.INSTANCE;
    private static final int MEASURE_SPEC_ANY_LENGTH = View.MeasureSpec.makeMeasureSpec(0, 0);
    private ActivityStarter mActivityStarter;
    private final BreakIterator mBreakIterator;
    private PriorityQueue<Button> mCandidateButtonQueueForSqueezing;
    private final SmartReplyConstants mConstants = ((SmartReplyConstants) Dependency.get(SmartReplyConstants.class));
    private int mCurrentBackgroundColor;
    private final int mDefaultBackgroundColor;
    private final int mDefaultStrokeColor;
    private final int mDefaultTextColor;
    private final int mDefaultTextColorDarkBg;
    private final int mDoubleLineButtonPaddingHorizontal;
    private final int mHeightUpperLimit = NotificationUtils.getFontScaledHeight(((ViewGroup) this).mContext, C0005R$dimen.smart_reply_button_max_height);
    private final KeyguardDismissUtil mKeyguardDismissUtil = ((KeyguardDismissUtil) Dependency.get(KeyguardDismissUtil.class));
    private final double mMinStrokeContrast;
    private final NotificationRemoteInputManager mRemoteInputManager = ((NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class));
    private final int mRippleColor;
    private final int mRippleColorDarkBg;
    private final int mSingleLineButtonPaddingHorizontal;
    private final int mSingleToDoubleLineButtonWidthIncrease;
    private boolean mSmartRepliesGeneratedByAssistant = false;
    private View mSmartReplyContainer;
    private final int mSpacing;
    private final int mStrokeWidth;

    /* access modifiers changed from: private */
    public enum SmartButtonType {
        REPLY,
        ACTION
    }

    static /* synthetic */ int lambda$static$0(View view, View view2) {
        return ((view2.getMeasuredWidth() - view2.getPaddingLeft()) - view2.getPaddingRight()) - ((view.getMeasuredWidth() - view.getPaddingLeft()) - view.getPaddingRight());
    }

    public SmartReplyView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentBackgroundColor = context.getColor(C0004R$color.smart_reply_button_background);
        this.mDefaultBackgroundColor = this.mCurrentBackgroundColor;
        this.mDefaultTextColor = ((ViewGroup) this).mContext.getColor(C0004R$color.smart_reply_button_text);
        this.mDefaultTextColorDarkBg = ((ViewGroup) this).mContext.getColor(C0004R$color.smart_reply_button_text_dark_bg);
        this.mDefaultStrokeColor = ((ViewGroup) this).mContext.getColor(C0004R$color.smart_reply_button_stroke);
        this.mRippleColor = ((ViewGroup) this).mContext.getColor(C0004R$color.notification_ripple_untinted_color);
        this.mRippleColorDarkBg = Color.argb(Color.alpha(this.mRippleColor), 255, 255, 255);
        this.mMinStrokeContrast = ContrastColorUtil.calculateContrast(this.mDefaultStrokeColor, this.mDefaultBackgroundColor);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.SmartReplyView, 0, 0);
        int indexCount = obtainStyledAttributes.getIndexCount();
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < indexCount; i5++) {
            int index = obtainStyledAttributes.getIndex(i5);
            if (index == R$styleable.SmartReplyView_spacing) {
                i2 = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            } else if (index == R$styleable.SmartReplyView_singleLineButtonPaddingHorizontal) {
                i3 = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            } else if (index == R$styleable.SmartReplyView_doubleLineButtonPaddingHorizontal) {
                i4 = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            } else if (index == R$styleable.SmartReplyView_buttonStrokeWidth) {
                i = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            }
        }
        obtainStyledAttributes.recycle();
        this.mStrokeWidth = i;
        this.mSpacing = i2;
        this.mSingleLineButtonPaddingHorizontal = i3;
        this.mDoubleLineButtonPaddingHorizontal = i4;
        this.mSingleToDoubleLineButtonWidthIncrease = (i4 - i3) * 2;
        this.mBreakIterator = BreakIterator.getLineInstance();
        reallocateCandidateButtonQueueForSqueezing();
    }

    public int getHeightUpperLimit() {
        return this.mHeightUpperLimit;
    }

    private void reallocateCandidateButtonQueueForSqueezing() {
        this.mCandidateButtonQueueForSqueezing = new PriorityQueue<>(Math.max(getChildCount(), 1), DECREASING_MEASURED_WIDTH_WITHOUT_PADDING_COMPARATOR);
    }

    public void resetSmartSuggestions(View view) {
        this.mSmartReplyContainer = view;
        removeAllViews();
        this.mCurrentBackgroundColor = this.mDefaultBackgroundColor;
    }

    public void addPreInflatedButtons(List<Button> list) {
        for (Button button : list) {
            addView(button);
        }
        reallocateCandidateButtonQueueForSqueezing();
    }

    public List<Button> inflateRepliesFromRemoteInput(SmartReplies smartReplies, SmartReplyController smartReplyController, NotificationEntry notificationEntry, boolean z) {
        ArrayList arrayList = new ArrayList();
        if (!(smartReplies.remoteInput == null || smartReplies.pendingIntent == null || smartReplies.choices == null)) {
            for (int i = 0; i < smartReplies.choices.length; i++) {
                arrayList.add(inflateReplyButton(this, getContext(), i, smartReplies, smartReplyController, notificationEntry, z));
            }
            this.mSmartRepliesGeneratedByAssistant = smartReplies.fromAssistant;
        }
        return arrayList;
    }

    public List<Button> inflateSmartActions(SmartActions smartActions, SmartReplyController smartReplyController, NotificationEntry notificationEntry, HeadsUpManager headsUpManager, boolean z) {
        ArrayList arrayList = new ArrayList();
        int size = smartActions.actions.size();
        for (int i = 0; i < size; i++) {
            if (smartActions.actions.get(i).actionIntent != null) {
                arrayList.add(inflateActionButton(this, getContext(), i, smartActions, smartReplyController, notificationEntry, headsUpManager, z));
            }
        }
        return arrayList;
    }

    public static SmartReplyView inflate(Context context) {
        return (SmartReplyView) LayoutInflater.from(context).inflate(C0010R$layout.smart_reply_view, (ViewGroup) null);
    }

    @VisibleForTesting
    static Button inflateReplyButton(SmartReplyView smartReplyView, Context context, int i, SmartReplies smartReplies, SmartReplyController smartReplyController, NotificationEntry notificationEntry, boolean z) {
        Button button = (Button) LayoutInflater.from(context).inflate(C0010R$layout.smart_reply_button, (ViewGroup) smartReplyView, false);
        CharSequence charSequence = smartReplies.choices[i];
        button.setText(charSequence);
        View.OnClickListener r0 = new View.OnClickListener(new ActivityStarter.OnDismissAction(smartReplies, charSequence, i, button, smartReplyController, notificationEntry, context) {
            /* class com.android.systemui.statusbar.policy.$$Lambda$SmartReplyView$rVuoX0krAdMy7xAwdbzCHW8AzI */
            private final /* synthetic */ SmartReplyView.SmartReplies f$1;
            private final /* synthetic */ CharSequence f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ Button f$4;
            private final /* synthetic */ SmartReplyController f$5;
            private final /* synthetic */ NotificationEntry f$6;
            private final /* synthetic */ Context f$7;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
                this.f$6 = r7;
                this.f$7 = r8;
            }

            @Override // com.android.systemui.plugins.ActivityStarter.OnDismissAction
            public final boolean onDismiss() {
                return SmartReplyView.lambda$inflateReplyButton$1(SmartReplyView.this, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7);
            }
        }) {
            /* class com.android.systemui.statusbar.policy.$$Lambda$SmartReplyView$wCF0sVwFBEkCEZW3HU9INxvlFA */
            private final /* synthetic */ ActivityStarter.OnDismissAction f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                SmartReplyView.this.mKeyguardDismissUtil.executeWhenUnlocked(this.f$1);
            }
        };
        if (z) {
            r0 = new DelayedOnClickListener(r0, smartReplyView.mConstants.getOnClickInitDelay());
        }
        button.setOnClickListener(r0);
        button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            /* class com.android.systemui.statusbar.policy.SmartReplyView.AnonymousClass1 */

            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, SmartReplyView.this.getResources().getString(C0014R$string.accessibility_send_smart_reply)));
            }
        });
        setButtonColors(button, smartReplyView.mCurrentBackgroundColor, smartReplyView.mDefaultStrokeColor, smartReplyView.mDefaultTextColor, smartReplyView.mRippleColor, smartReplyView.mStrokeWidth);
        return button;
    }

    static /* synthetic */ boolean lambda$inflateReplyButton$1(SmartReplyView smartReplyView, SmartReplies smartReplies, CharSequence charSequence, int i, Button button, SmartReplyController smartReplyController, NotificationEntry notificationEntry, Context context) {
        if (smartReplyView.mConstants.getEffectiveEditChoicesBeforeSending(smartReplies.remoteInput.getEditChoicesBeforeSending())) {
            NotificationEntry.EditedSuggestionInfo editedSuggestionInfo = new NotificationEntry.EditedSuggestionInfo(charSequence, i);
            NotificationRemoteInputManager notificationRemoteInputManager = smartReplyView.mRemoteInputManager;
            RemoteInput remoteInput = smartReplies.remoteInput;
            notificationRemoteInputManager.activateRemoteInput(button, new RemoteInput[]{remoteInput}, remoteInput, smartReplies.pendingIntent, editedSuggestionInfo);
            return false;
        }
        smartReplyController.smartReplySent(notificationEntry, i, button.getText(), NotificationLogger.getNotificationLocation(notificationEntry).toMetricsEventEnum(), false);
        Bundle bundle = new Bundle();
        bundle.putString(smartReplies.remoteInput.getResultKey(), charSequence.toString());
        Intent addFlags = new Intent().addFlags(268435456);
        RemoteInput.addResultsToIntent(new RemoteInput[]{smartReplies.remoteInput}, addFlags, bundle);
        RemoteInput.setResultsSource(addFlags, 1);
        notificationEntry.setHasSentReply();
        try {
            smartReplies.pendingIntent.send(context, 0, addFlags);
        } catch (PendingIntent.CanceledException e) {
            Log.w("SmartReplyView", "Unable to send smart reply", e);
        }
        smartReplyView.mSmartReplyContainer.setVisibility(8);
        return false;
    }

    @VisibleForTesting
    static Button inflateActionButton(SmartReplyView smartReplyView, Context context, int i, SmartActions smartActions, SmartReplyController smartReplyController, NotificationEntry notificationEntry, HeadsUpManager headsUpManager, boolean z) {
        Notification.Action action = smartActions.actions.get(i);
        Button button = (Button) LayoutInflater.from(context).inflate(C0010R$layout.smart_action_button, (ViewGroup) smartReplyView, false);
        button.setText(action.title);
        Drawable loadDrawable = action.getIcon().loadDrawable(context);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(C0005R$dimen.smart_action_button_icon_size);
        loadDrawable.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize);
        button.setCompoundDrawables(loadDrawable, null, null, null);
        View.OnClickListener r10 = new View.OnClickListener(action, smartReplyController, notificationEntry, i, smartActions, headsUpManager) {
            /* class com.android.systemui.statusbar.policy.$$Lambda$SmartReplyView$tct0o0Zp_9czv90IHtUOrdcaxl0 */
            private final /* synthetic */ Notification.Action f$1;
            private final /* synthetic */ SmartReplyController f$2;
            private final /* synthetic */ NotificationEntry f$3;
            private final /* synthetic */ int f$4;
            private final /* synthetic */ SmartReplyView.SmartActions f$5;
            private final /* synthetic */ HeadsUpManager f$6;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
                this.f$6 = r7;
            }

            public final void onClick(View view) {
                Notification.Action action;
                NotificationEntry notificationEntry;
                SmartReplyView.this.getActivityStarter().startPendingIntentDismissingKeyguard(action.actionIntent, new Runnable(notificationEntry, this.f$4, this.f$1, this.f$5, this.f$6) {
                    /* class com.android.systemui.statusbar.policy.$$Lambda$SmartReplyView$TA933H11Yl_oDGgX0f0ntr5xGgI */
                    private final /* synthetic */ NotificationEntry f$1;
                    private final /* synthetic */ int f$2;
                    private final /* synthetic */ Notification.Action f$3;
                    private final /* synthetic */ SmartReplyView.SmartActions f$4;
                    private final /* synthetic */ HeadsUpManager f$5;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                        this.f$4 = r5;
                        this.f$5 = r6;
                    }

                    public final void run() {
                        SmartReplyView.lambda$inflateActionButton$3(SmartReplyController.this, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
                    }
                }, this.f$3.getRow());
            }
        };
        if (z) {
            r10 = new DelayedOnClickListener(r10, smartReplyView.mConstants.getOnClickInitDelay());
        }
        button.setOnClickListener(r10);
        ((LayoutParams) button.getLayoutParams()).buttonType = SmartButtonType.ACTION;
        return button;
    }

    static /* synthetic */ void lambda$inflateActionButton$3(SmartReplyController smartReplyController, NotificationEntry notificationEntry, int i, Notification.Action action, SmartActions smartActions, HeadsUpManager headsUpManager) {
        smartReplyController.smartActionClicked(notificationEntry, i, action, smartActions.fromAssistant);
        headsUpManager.removeNotification(notificationEntry.key, true);
    }

    @Override // android.view.ViewGroup
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(((ViewGroup) this).mContext, attributeSet);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    /* access modifiers changed from: protected */
    @Override // android.view.ViewGroup
    public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams.width, layoutParams.height);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        Iterator it;
        int i4;
        int i5;
        if (View.MeasureSpec.getMode(i) == 0) {
            i3 = Integer.MAX_VALUE;
        } else {
            i3 = View.MeasureSpec.getSize(i);
        }
        resetButtonsLayoutParams();
        if (!this.mCandidateButtonQueueForSqueezing.isEmpty()) {
            Log.wtf("SmartReplyView", "Single line button queue leaked between onMeasure calls");
            this.mCandidateButtonQueueForSqueezing.clear();
        }
        SmartSuggestionMeasures smartSuggestionMeasures = new SmartSuggestionMeasures(((ViewGroup) this).mPaddingLeft + ((ViewGroup) this).mPaddingRight, 0, this.mSingleLineButtonPaddingHorizontal);
        List<View> filterActionsOrReplies = filterActionsOrReplies(SmartButtonType.ACTION);
        List<View> filterActionsOrReplies2 = filterActionsOrReplies(SmartButtonType.REPLY);
        ArrayList<Button> arrayList = new ArrayList(filterActionsOrReplies);
        arrayList.addAll(filterActionsOrReplies2);
        ArrayList arrayList2 = new ArrayList();
        int maxNumActions = this.mConstants.getMaxNumActions();
        Iterator it2 = arrayList.iterator();
        int i6 = 0;
        SmartSuggestionMeasures smartSuggestionMeasures2 = null;
        SmartSuggestionMeasures smartSuggestionMeasures3 = smartSuggestionMeasures;
        int i7 = 0;
        while (it2.hasNext()) {
            View view = (View) it2.next();
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            if (maxNumActions == -1 || layoutParams.buttonType != SmartButtonType.ACTION || i7 < maxNumActions) {
                i4 = maxNumActions;
                it = it2;
                view.setPadding(smartSuggestionMeasures3.mButtonPaddingHorizontal, view.getPaddingTop(), smartSuggestionMeasures3.mButtonPaddingHorizontal, view.getPaddingBottom());
                view.measure(MEASURE_SPEC_ANY_LENGTH, i2);
                arrayList2.add(view);
                Button button = (Button) view;
                int lineCount = button.getLineCount();
                if (lineCount >= 1 && lineCount <= 2) {
                    if (lineCount == 1) {
                        this.mCandidateButtonQueueForSqueezing.add(button);
                    }
                    SmartSuggestionMeasures clone = smartSuggestionMeasures3.clone();
                    if (smartSuggestionMeasures2 == null && layoutParams.buttonType == SmartButtonType.REPLY) {
                        smartSuggestionMeasures2 = smartSuggestionMeasures3.clone();
                    }
                    if (i6 == 0) {
                        i5 = 0;
                    } else {
                        i5 = this.mSpacing;
                    }
                    int measuredWidth = view.getMeasuredWidth();
                    int measuredHeight = view.getMeasuredHeight();
                    smartSuggestionMeasures3.mMeasuredWidth += i5 + measuredWidth;
                    smartSuggestionMeasures3.mMaxChildHeight = Math.max(smartSuggestionMeasures3.mMaxChildHeight, measuredHeight);
                    if (smartSuggestionMeasures3.mButtonPaddingHorizontal == this.mSingleLineButtonPaddingHorizontal && (lineCount == 2 || smartSuggestionMeasures3.mMeasuredWidth > i3)) {
                        smartSuggestionMeasures3.mMeasuredWidth += (i6 + 1) * this.mSingleToDoubleLineButtonWidthIncrease;
                        smartSuggestionMeasures3.mButtonPaddingHorizontal = this.mDoubleLineButtonPaddingHorizontal;
                    }
                    if (smartSuggestionMeasures3.mMeasuredWidth > i3) {
                        while (smartSuggestionMeasures3.mMeasuredWidth > i3 && !this.mCandidateButtonQueueForSqueezing.isEmpty()) {
                            Button poll = this.mCandidateButtonQueueForSqueezing.poll();
                            int squeezeButton = squeezeButton(poll, i2);
                            if (squeezeButton != -1) {
                                smartSuggestionMeasures3.mMaxChildHeight = Math.max(smartSuggestionMeasures3.mMaxChildHeight, poll.getMeasuredHeight());
                                smartSuggestionMeasures3.mMeasuredWidth -= squeezeButton;
                            }
                        }
                        if (smartSuggestionMeasures3.mMeasuredWidth > i3) {
                            markButtonsWithPendingSqueezeStatusAs(3, arrayList2);
                            maxNumActions = i4;
                            it2 = it;
                            smartSuggestionMeasures3 = clone;
                        } else {
                            markButtonsWithPendingSqueezeStatusAs(2, arrayList2);
                        }
                    }
                    layoutParams.show = true;
                    i6++;
                    if (layoutParams.buttonType == SmartButtonType.ACTION) {
                        i7++;
                    }
                }
            } else {
                i4 = maxNumActions;
                it = it2;
            }
            maxNumActions = i4;
            it2 = it;
        }
        if (this.mSmartRepliesGeneratedByAssistant && !gotEnoughSmartReplies(filterActionsOrReplies2)) {
            for (View view2 : filterActionsOrReplies2) {
                ((LayoutParams) view2.getLayoutParams()).show = false;
            }
            smartSuggestionMeasures3 = smartSuggestionMeasures2;
        }
        this.mCandidateButtonQueueForSqueezing.clear();
        remeasureButtonsIfNecessary(smartSuggestionMeasures3.mButtonPaddingHorizontal, smartSuggestionMeasures3.mMaxChildHeight);
        int max = Math.max(getSuggestedMinimumHeight(), ((ViewGroup) this).mPaddingTop + smartSuggestionMeasures3.mMaxChildHeight + ((ViewGroup) this).mPaddingBottom);
        for (Button button2 : arrayList) {
            setCornerRadius(button2, ((float) max) / 2.0f);
        }
        setMeasuredDimension(ViewGroup.resolveSize(Math.max(getSuggestedMinimumWidth(), smartSuggestionMeasures3.mMeasuredWidth), i), ViewGroup.resolveSize(max, i2));
    }

    private static class SmartSuggestionMeasures {
        int mButtonPaddingHorizontal = -1;
        int mMaxChildHeight = -1;
        int mMeasuredWidth = -1;

        SmartSuggestionMeasures(int i, int i2, int i3) {
            this.mMeasuredWidth = i;
            this.mMaxChildHeight = i2;
            this.mButtonPaddingHorizontal = i3;
        }

        public SmartSuggestionMeasures clone() {
            return new SmartSuggestionMeasures(this.mMeasuredWidth, this.mMaxChildHeight, this.mButtonPaddingHorizontal);
        }
    }

    private boolean gotEnoughSmartReplies(List<View> list) {
        int i = 0;
        for (View view : list) {
            if (((LayoutParams) view.getLayoutParams()).show) {
                i++;
            }
        }
        if (i == 0 || i >= this.mConstants.getMinNumSystemGeneratedReplies()) {
            return true;
        }
        return false;
    }

    private List<View> filterActionsOrReplies(SmartButtonType smartButtonType) {
        ArrayList arrayList = new ArrayList();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            if (childAt.getVisibility() == 0 && (childAt instanceof Button) && layoutParams.buttonType == smartButtonType) {
                arrayList.add(childAt);
            }
        }
        return arrayList;
    }

    private void resetButtonsLayoutParams() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams layoutParams = (LayoutParams) getChildAt(i).getLayoutParams();
            layoutParams.show = false;
            layoutParams.squeezeStatus = 0;
        }
    }

    private int squeezeButton(Button button, int i) {
        int estimateOptimalSqueezedButtonTextWidth = estimateOptimalSqueezedButtonTextWidth(button);
        if (estimateOptimalSqueezedButtonTextWidth == -1) {
            return -1;
        }
        return squeezeButtonToTextWidth(button, i, estimateOptimalSqueezedButtonTextWidth);
    }

    private int estimateOptimalSqueezedButtonTextWidth(Button button) {
        String charSequence = button.getText().toString();
        TransformationMethod transformationMethod = button.getTransformationMethod();
        if (transformationMethod != null) {
            charSequence = transformationMethod.getTransformation(charSequence, button).toString();
        }
        int length = charSequence.length();
        this.mBreakIterator.setText(charSequence);
        if (this.mBreakIterator.preceding(length / 2) == -1 && this.mBreakIterator.next() == -1) {
            return -1;
        }
        TextPaint paint = button.getPaint();
        int current = this.mBreakIterator.current();
        float desiredWidth = Layout.getDesiredWidth(charSequence, 0, current, paint);
        float desiredWidth2 = Layout.getDesiredWidth(charSequence, current, length, paint);
        float max = Math.max(desiredWidth, desiredWidth2);
        int i = (desiredWidth > desiredWidth2 ? 1 : (desiredWidth == desiredWidth2 ? 0 : -1));
        if (i != 0) {
            boolean z = i > 0;
            int maxSqueezeRemeasureAttempts = this.mConstants.getMaxSqueezeRemeasureAttempts();
            float f = max;
            int i2 = 0;
            while (true) {
                if (i2 >= maxSqueezeRemeasureAttempts) {
                    break;
                }
                BreakIterator breakIterator = this.mBreakIterator;
                int previous = z ? breakIterator.previous() : breakIterator.next();
                if (previous != -1) {
                    float desiredWidth3 = Layout.getDesiredWidth(charSequence, 0, previous, paint);
                    float desiredWidth4 = Layout.getDesiredWidth(charSequence, previous, length, paint);
                    float max2 = Math.max(desiredWidth3, desiredWidth4);
                    if (max2 >= f) {
                        break;
                    }
                    if (!z ? desiredWidth3 >= desiredWidth4 : desiredWidth3 <= desiredWidth4) {
                        max = max2;
                        break;
                    }
                    i2++;
                    f = max2;
                } else {
                    break;
                }
            }
            max = f;
        }
        return (int) Math.ceil((double) max);
    }

    private int getLeftCompoundDrawableWidthWithPadding(Button button) {
        Drawable drawable = button.getCompoundDrawables()[0];
        if (drawable == null) {
            return 0;
        }
        return drawable.getBounds().width() + button.getCompoundDrawablePadding();
    }

    private int squeezeButtonToTextWidth(Button button, int i, int i2) {
        int measuredWidth = button.getMeasuredWidth();
        if (button.getPaddingLeft() != this.mDoubleLineButtonPaddingHorizontal) {
            measuredWidth += this.mSingleToDoubleLineButtonWidthIncrease;
        }
        button.setPadding(this.mDoubleLineButtonPaddingHorizontal, button.getPaddingTop(), this.mDoubleLineButtonPaddingHorizontal, button.getPaddingBottom());
        button.measure(View.MeasureSpec.makeMeasureSpec((this.mDoubleLineButtonPaddingHorizontal * 2) + i2 + getLeftCompoundDrawableWidthWithPadding(button), Integer.MIN_VALUE), i);
        int measuredWidth2 = button.getMeasuredWidth();
        LayoutParams layoutParams = (LayoutParams) button.getLayoutParams();
        if (button.getLineCount() > 2 || measuredWidth2 >= measuredWidth) {
            layoutParams.squeezeStatus = 3;
            return -1;
        }
        layoutParams.squeezeStatus = 1;
        return measuredWidth - measuredWidth2;
    }

    private void remeasureButtonsIfNecessary(int i, int i2) {
        boolean z;
        int makeMeasureSpec = View.MeasureSpec.makeMeasureSpec(i2, 1073741824);
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            if (layoutParams.show) {
                int measuredWidth = childAt.getMeasuredWidth();
                if (layoutParams.squeezeStatus == 3) {
                    measuredWidth = Integer.MAX_VALUE;
                    z = true;
                } else {
                    z = false;
                }
                if (childAt.getPaddingLeft() != i) {
                    if (measuredWidth != Integer.MAX_VALUE) {
                        if (i == this.mSingleLineButtonPaddingHorizontal) {
                            measuredWidth -= this.mSingleToDoubleLineButtonWidthIncrease;
                        } else {
                            measuredWidth += this.mSingleToDoubleLineButtonWidthIncrease;
                        }
                    }
                    childAt.setPadding(i, childAt.getPaddingTop(), i, childAt.getPaddingBottom());
                    z = true;
                }
                if (childAt.getMeasuredHeight() != i2) {
                    z = true;
                }
                if (z) {
                    childAt.measure(View.MeasureSpec.makeMeasureSpec(measuredWidth, Integer.MIN_VALUE), makeMeasureSpec);
                }
            }
        }
    }

    private void markButtonsWithPendingSqueezeStatusAs(int i, List<View> list) {
        for (View view : list) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            if (layoutParams.squeezeStatus == 1) {
                layoutParams.squeezeStatus = i;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        boolean z2 = true;
        if (getLayoutDirection() != 1) {
            z2 = false;
        }
        int i5 = z2 ? (i3 - i) - ((ViewGroup) this).mPaddingRight : ((ViewGroup) this).mPaddingLeft;
        int childCount = getChildCount();
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt = getChildAt(i6);
            if (((LayoutParams) childAt.getLayoutParams()).show) {
                int measuredWidth = childAt.getMeasuredWidth();
                int measuredHeight = childAt.getMeasuredHeight();
                int i7 = z2 ? i5 - measuredWidth : i5;
                childAt.layout(i7, 0, i7 + measuredWidth, measuredHeight);
                int i8 = measuredWidth + this.mSpacing;
                i5 = z2 ? i5 - i8 : i5 + i8;
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View view, long j) {
        return ((LayoutParams) view.getLayoutParams()).show && super.drawChild(canvas, view, j);
    }

    public void setBackgroundTintColor(int i) {
        if (i != this.mCurrentBackgroundColor) {
            this.mCurrentBackgroundColor = i;
            boolean z = !ContrastColorUtil.isColorLight(i);
            int i2 = -16777216 | i;
            int ensureTextContrast = ContrastColorUtil.ensureTextContrast(z ? this.mDefaultTextColorDarkBg : this.mDefaultTextColor, i2, z);
            int ensureContrast = ContrastColorUtil.ensureContrast(this.mDefaultStrokeColor, i2, z, this.mMinStrokeContrast);
            int i3 = z ? this.mRippleColorDarkBg : this.mRippleColor;
            int childCount = getChildCount();
            for (int i4 = 0; i4 < childCount; i4++) {
                setButtonColors((Button) getChildAt(i4), i, ensureContrast, ensureTextContrast, i3, this.mStrokeWidth);
            }
        }
    }

    private static void setButtonColors(Button button, int i, int i2, int i3, int i4, int i5) {
        Drawable background = button.getBackground();
        if (background instanceof RippleDrawable) {
            Drawable mutate = background.mutate();
            RippleDrawable rippleDrawable = (RippleDrawable) mutate;
            rippleDrawable.setColor(ColorStateList.valueOf(i4));
            Drawable drawable = rippleDrawable.getDrawable(0);
            if (drawable instanceof InsetDrawable) {
                Drawable drawable2 = ((InsetDrawable) drawable).getDrawable();
                if (drawable2 instanceof GradientDrawable) {
                    GradientDrawable gradientDrawable = (GradientDrawable) drawable2;
                    gradientDrawable.setColor(i);
                    gradientDrawable.setStroke(i5, i2);
                }
            }
            button.setBackground(mutate);
        }
        button.setTextColor(i3);
    }

    private void setCornerRadius(Button button, float f) {
        Drawable background = button.getBackground();
        if (background instanceof RippleDrawable) {
            Drawable drawable = ((RippleDrawable) background.mutate()).getDrawable(0);
            if (drawable instanceof InsetDrawable) {
                Drawable drawable2 = ((InsetDrawable) drawable).getDrawable();
                if (drawable2 instanceof GradientDrawable) {
                    ((GradientDrawable) drawable2).setCornerRadius(f);
                }
            }
        }
    }

    private ActivityStarter getActivityStarter() {
        if (this.mActivityStarter == null) {
            this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        }
        return this.mActivityStarter;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public static class LayoutParams extends ViewGroup.LayoutParams {
        private SmartButtonType buttonType;
        private boolean show;
        private int squeezeStatus;

        private LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.show = false;
            this.squeezeStatus = 0;
            this.buttonType = SmartButtonType.REPLY;
        }

        private LayoutParams(int i, int i2) {
            super(i, i2);
            this.show = false;
            this.squeezeStatus = 0;
            this.buttonType = SmartButtonType.REPLY;
        }

        /* access modifiers changed from: package-private */
        @VisibleForTesting
        public boolean isShown() {
            return this.show;
        }
    }

    public static class SmartReplies {
        public final CharSequence[] choices;
        public final boolean fromAssistant;
        public final PendingIntent pendingIntent;
        public final RemoteInput remoteInput;

        public SmartReplies(CharSequence[] charSequenceArr, RemoteInput remoteInput2, PendingIntent pendingIntent2, boolean z) {
            this.choices = charSequenceArr;
            this.remoteInput = remoteInput2;
            this.pendingIntent = pendingIntent2;
            this.fromAssistant = z;
        }
    }

    public static class SmartActions {
        public final List<Notification.Action> actions;
        public final boolean fromAssistant;

        public SmartActions(List<Notification.Action> list, boolean z) {
            this.actions = list;
            this.fromAssistant = z;
        }
    }

    /* access modifiers changed from: private */
    public static class DelayedOnClickListener implements View.OnClickListener {
        private final View.OnClickListener mActualListener;
        private final long mInitDelayMs;
        private final long mInitTimeMs = SystemClock.elapsedRealtime();

        DelayedOnClickListener(View.OnClickListener onClickListener, long j) {
            this.mActualListener = onClickListener;
            this.mInitDelayMs = j;
        }

        public void onClick(View view) {
            if (hasFinishedInitialization()) {
                this.mActualListener.onClick(view);
                return;
            }
            Log.i("SmartReplyView", "Accidental Smart Suggestion click registered, delay: " + this.mInitDelayMs);
        }

        private boolean hasFinishedInitialization() {
            return SystemClock.elapsedRealtime() >= this.mInitTimeMs + this.mInitDelayMs;
        }
    }
}
