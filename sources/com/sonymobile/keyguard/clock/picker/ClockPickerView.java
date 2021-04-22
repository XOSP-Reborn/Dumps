package com.sonymobile.keyguard.clock.picker;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.SomcKeyguardClockScaleContainer;
import com.sonymobile.keyguard.SomcKeyguardClockScaleContainerCallback;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactoryEntry;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

public class ClockPickerView extends HorizontalScrollView implements SomcKeyguardClockScaleContainerCallback {
    private final View.OnClickListener mClockItemOnClickListener;
    private ClockPickerViewContent mClockPickerViewContent;
    private ViewGroup mClocksContainer;
    private ClockPickerController mController;
    private final View.OnClickListener mDismissPickerListener;
    private Handler mHandler;
    private boolean mPageSnapHandled;
    private int mPageWidthPixels;
    private SomcKeyguardClockScaleContainer mScaleContainer;
    private final SelectionTimeOut mSelectionTimeOut;

    /* access modifiers changed from: private */
    public enum Motion {
        FLING_RIGHT {
            @Override // com.sonymobile.keyguard.clock.picker.ClockPickerView.Motion
            public int getModifiedScrollPosition(int i, int i2) {
                return i - Math.round(((float) i2) / 2.0f);
            }
        },
        FLING_LEFT {
            @Override // com.sonymobile.keyguard.clock.picker.ClockPickerView.Motion
            public int getModifiedScrollPosition(int i, int i2) {
                return i + Math.round(((float) i2) / 2.0f);
            }
        },
        DRAG {
            @Override // com.sonymobile.keyguard.clock.picker.ClockPickerView.Motion
            public int getModifiedScrollPosition(int i, int i2) {
                return i;
            }
        };

        public abstract int getModifiedScrollPosition(int i, int i2);
    }

    public ClockPickerView(Context context) {
        this(context, null);
    }

    public ClockPickerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ClockPickerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPageWidthPixels = -1;
        this.mPageSnapHandled = false;
        this.mClockItemOnClickListener = new View.OnClickListener() {
            /* class com.sonymobile.keyguard.clock.picker.ClockPickerView.AnonymousClass1 */

            public void onClick(View view) {
                ClockPickerView clockPickerView = ClockPickerView.this;
                clockPickerView.selectPage(clockPickerView.mClocksContainer.indexOfChild(view));
            }
        };
        this.mDismissPickerListener = new View.OnClickListener() {
            /* class com.sonymobile.keyguard.clock.picker.ClockPickerView.AnonymousClass2 */

            public void onClick(View view) {
                ClockPickerView.this.mController.exitClockPicker(null, false);
            }
        };
        this.mHandler = new Handler();
        this.mSelectionTimeOut = new SelectionTimeOut(this);
    }

    public final void setController(ClockPickerController clockPickerController) {
        this.mController = clockPickerController;
    }

    public final boolean onTouchEvent(MotionEvent motionEvent) {
        boolean onTouchEvent = super.onTouchEvent(motionEvent);
        ensureTouchEndsOnAPage(motionEvent);
        return onTouchEvent;
    }

    public final void fling(int i) {
        Motion motion = i > 0 ? Motion.FLING_LEFT : Motion.FLING_RIGHT;
        this.mPageSnapHandled = true;
        snapToPage(motion);
    }

    private void ensureTouchEndsOnAPage(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 1 || action == 3) {
            if (!this.mPageSnapHandled) {
                snapToPage(Motion.DRAG);
            }
            this.mPageSnapHandled = false;
        }
    }

    public final void initPages() {
        this.mScaleContainer = (SomcKeyguardClockScaleContainer) findViewById(C0007R$id.somc_keyguard_clockplugin_picker_scale_container);
        this.mScaleContainer.setPivotXViewStart(true);
        this.mScaleContainer.setSomcKeyguardClockScaleContainerCallback(this);
        this.mClockPickerViewContent = (ClockPickerViewContent) findViewById(C0007R$id.somc_keyguard_clock_picker_content);
        this.mClockPickerViewContent.setOnClickListener(this.mDismissPickerListener);
        this.mClocksContainer = (ViewGroup) findViewById(C0007R$id.somc_keyguard_clock_picker_scaled_clocks);
        createViews(this.mController.loadClockPlugins());
        this.mPageWidthPixels = calculatePageWidth();
        this.mScaleContainer.setAnimateScaling(false);
    }

    public final void enableScaleContainerScaling() {
        this.mScaleContainer.setAnimateScaling(true);
    }

    @Override // com.sonymobile.keyguard.SomcKeyguardClockScaleContainerCallback
    public final void onScalingFinished(float f) {
        this.mController.resizeDismissView((ViewGroup) getParent());
    }

    @Override // com.sonymobile.keyguard.SomcKeyguardClockScaleContainerCallback
    public final void onScalingStarted(float f) {
        updateContentPadding(f);
        ensureSamePageIsCenteredAfterScaling(f);
        this.mClockPickerViewContent.setScaleLevel(f);
    }

    @Override // com.sonymobile.keyguard.SomcKeyguardClockScaleContainerCallback
    public final void onScalingCancelled(float f) {
        updateContentPadding(1.0f);
        requestLayout();
    }

    private int getPageWidthInPixels() {
        return Math.round(((float) this.mPageWidthPixels) * this.mScaleContainer.getScaleY());
    }

    private void createViews(LinkedList<KeyguardComponentFactoryEntry> linkedList) {
        Iterator<KeyguardComponentFactoryEntry> it = linkedList.iterator();
        while (it.hasNext()) {
            addClock(it.next(), this.mClocksContainer);
        }
    }

    /* access modifiers changed from: protected */
    public final void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateContentPadding(1.0f);
        ensureSamePageIsCenteredAfterScaling(1.0f);
    }

    private int calculatePageWidth() {
        if (this.mScaleContainer.getChildCount() <= 0) {
            return 0;
        }
        ViewGroup viewGroup = (ViewGroup) this.mScaleContainer.getChildAt(0);
        if (viewGroup.getChildCount() <= 0) {
            return 0;
        }
        ClockItem clockItem = (ClockItem) viewGroup.getChildAt(0);
        return clockItem.getWidth() == 0 ? clockItem.getPageWidthInPixels() : clockItem.getWidth();
    }

    private void ensureSamePageIsCenteredAfterScaling(float f) {
        int round = Math.round(((float) this.mPageWidthPixels) * f);
        int round2 = Math.round(((float) getScrollX()) / ((float) getPageWidthInPixels()));
        ((HorizontalScrollView) this).mScrollX = round * round2;
        createSelectionTimeOut(round2);
    }

    private void updateContentPadding(float f) {
        if (getParent() != null && getContext().getResources() != null) {
            float width = (((float) ((View) getParent()).getWidth()) - (((float) this.mPageWidthPixels) * f)) / 2.0f;
            this.mClockPickerViewContent.setPadding(Math.round(width), 0, Math.round(width), 0);
        }
    }

    private void addClock(KeyguardComponentFactoryEntry keyguardComponentFactoryEntry, ViewGroup viewGroup) {
        LayoutInflater.from(getContext()).inflate(C0010R$layout.somc_keyguard_clock_container_clock_item, viewGroup, true);
        ClockItem clockItem = (ClockItem) viewGroup.getChildAt(viewGroup.getChildCount() - 1);
        clockItem.initWithClock(keyguardComponentFactoryEntry, this.mController);
        clockItem.setOnClickListener(this.mClockItemOnClickListener);
    }

    public final void positionPicker(String str) {
        int childCount = this.mClocksContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (isViewDepictingClock(this.mClocksContainer.getChildAt(i), str)) {
                ((HorizontalScrollView) this).mScrollX = i * getPageWidthInPixels();
                return;
            }
        }
    }

    public final void createSelectionTimeoutForSelectedPage() {
        createSelectionTimeOut(Math.round(((float) getScrollX()) / ((float) getPageWidthInPixels())));
    }

    private boolean isViewDepictingClock(View view, String str) {
        String fullyQualifiedClassName;
        return (view instanceof ClockItem) && (fullyQualifiedClassName = ((ClockItem) view).getClock().getFullyQualifiedClassName()) != null && fullyQualifiedClassName.equals(str);
    }

    private void snapToPage(Motion motion) {
        int pageWidthInPixels = getPageWidthInPixels();
        int round = Math.round(((float) motion.getModifiedScrollPosition(getScrollX(), pageWidthInPixels)) / ((float) pageWidthInPixels));
        smoothScrollTo(pageWidthInPixels * round, getScrollY());
        createSelectionTimeOut(round);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void selectPage(int i) {
        View childAt = this.mClocksContainer.getChildAt(i);
        if (childAt instanceof ClockItem) {
            this.mController.exitClockPicker(((ClockItem) childAt).getClock(), true);
        }
    }

    private void createSelectionTimeOut(int i) {
        clearSelectionTimeout();
        Resources resources = getContext().getResources();
        if (resources != null && resources.getConfiguration().getLayoutDirection() == 1) {
            i = (this.mClocksContainer.getChildCount() - 1) - i;
        }
        this.mSelectionTimeOut.setForSelectedPageIndex(i);
        this.mHandler.postDelayed(this.mSelectionTimeOut, 5000);
    }

    public final void clearSelectionTimeout() {
        this.mHandler.removeCallbacks(this.mSelectionTimeOut);
    }

    /* access modifiers changed from: private */
    public static class SelectionTimeOut implements Runnable {
        private final WeakReference<ClockPickerView> mClockPickerView;
        private int mForSelectedPageIndex = -1;

        public SelectionTimeOut(ClockPickerView clockPickerView) {
            this.mClockPickerView = new WeakReference<>(clockPickerView);
        }

        public void setForSelectedPageIndex(int i) {
            this.mForSelectedPageIndex = i;
        }

        public void run() {
            int i;
            ClockPickerView clockPickerView = this.mClockPickerView.get();
            if (clockPickerView != null) {
                if ((clockPickerView.getParent() != null) && (i = this.mForSelectedPageIndex) != -1) {
                    clockPickerView.selectPage(i);
                }
            }
        }
    }
}
