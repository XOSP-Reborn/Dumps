package com.sonymobile.keyguard.clock.picker;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.C0003R$bool;
import java.lang.ref.WeakReference;

public class StartClockPickerTouchListener implements View.OnTouchListener {
    private boolean mActiveTouch = false;
    private final ClockPickerStarter mClockPickerStarter;
    private final Context mContext;
    private boolean mDidHandleClick = false;
    private final float mDismissTouchDistancePX;
    private final Handler mHandler;
    private final LongPressCompletedRunnable mLongPressCompletedRunnable;
    private float mStartTouchX = -1.0f;
    private float mStartTouchY = -1.0f;

    public StartClockPickerTouchListener(Context context, ClockPickerStarter clockPickerStarter) {
        this.mContext = context;
        this.mClockPickerStarter = clockPickerStarter;
        this.mLongPressCompletedRunnable = new LongPressCompletedRunnable(this);
        this.mHandler = new Handler();
        Resources resources = this.mContext.getResources();
        if (resources != null) {
            this.mDismissTouchDistancePX = resources.getDisplayMetrics().density * 20.0f;
        } else {
            this.mDismissTouchDistancePX = 20.0f;
        }
    }

    public final boolean onTouch(View view, MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            start(motionEvent);
            this.mLongPressCompletedRunnable.setView(view);
            this.mHandler.postDelayed(this.mLongPressCompletedRunnable, (long) ViewConfiguration.getLongPressTimeout());
        } else if (actionMasked == 1) {
            if (!this.mDidHandleClick) {
                view.performClick();
            }
            reset();
        } else if (actionMasked == 2) {
            float f = this.mStartTouchX;
            if (f > 0.0f && this.mStartTouchY > 0.0f) {
                float x = f - motionEvent.getX();
                float y = this.mStartTouchY - motionEvent.getY();
                float f2 = this.mDismissTouchDistancePX;
                if ((x * x) + (y * y) > f2 * f2) {
                    reset();
                }
            }
        } else if (actionMasked == 3) {
            reset();
        }
        return this.mActiveTouch;
    }

    private void start(MotionEvent motionEvent) {
        this.mStartTouchX = motionEvent.getX();
        this.mStartTouchY = motionEvent.getY();
        this.mActiveTouch = true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void reset() {
        this.mHandler.removeCallbacks(this.mLongPressCompletedRunnable);
        this.mStartTouchY = -1.0f;
        this.mStartTouchX = -1.0f;
        this.mActiveTouch = false;
        this.mDidHandleClick = false;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startPicker(View view) {
        this.mDidHandleClick = false;
        if (this.mActiveTouch && !this.mContext.getResources().getBoolean(C0003R$bool.somc_keyguard_use_default_clock) && view != null) {
            view.performHapticFeedback(0);
            this.mClockPickerStarter.displayClockPluginPicker();
            this.mDidHandleClick = true;
        }
    }

    /* access modifiers changed from: private */
    public static class LongPressCompletedRunnable implements Runnable {
        private final WeakReference<StartClockPickerTouchListener> mStartClockPickerTouchListener;
        private WeakReference<View> mView;

        public LongPressCompletedRunnable(StartClockPickerTouchListener startClockPickerTouchListener) {
            this.mStartClockPickerTouchListener = new WeakReference<>(startClockPickerTouchListener);
        }

        public void run() {
            StartClockPickerTouchListener startClockPickerTouchListener = this.mStartClockPickerTouchListener.get();
            WeakReference<View> weakReference = this.mView;
            View view = weakReference != null ? weakReference.get() : null;
            if (startClockPickerTouchListener != null) {
                if (view != null) {
                    startClockPickerTouchListener.startPicker(view);
                }
                startClockPickerTouchListener.reset();
            }
        }

        public void setView(View view) {
            if (view != null) {
                this.mView = new WeakReference<>(view);
            }
        }
    }
}
