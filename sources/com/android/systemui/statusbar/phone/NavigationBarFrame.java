package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.policy.DeadZone;
import com.sonymobile.onehand.OneHandTriggerConfig;
import com.sonymobile.onehand.OneHandTriggerGestureDetector;

public class NavigationBarFrame extends FrameLayout {
    private static final boolean SupportOneHandTrigger = OneHandTriggerConfig.shouldEnterBySwipeNavigationBar();
    private DeadZone mDeadZone = null;
    private OneHandTriggerGestureDetector mOneHandTriggerDetector = null;

    public NavigationBarFrame(Context context) {
        super(context);
        if (SupportOneHandTrigger && this.mOneHandTriggerDetector == null) {
            this.mOneHandTriggerDetector = new OneHandTriggerGestureDetector(context);
        }
    }

    public NavigationBarFrame(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        if (SupportOneHandTrigger && this.mOneHandTriggerDetector == null) {
            this.mOneHandTriggerDetector = new OneHandTriggerGestureDetector(context);
        }
    }

    public NavigationBarFrame(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        if (SupportOneHandTrigger && this.mOneHandTriggerDetector == null) {
            this.mOneHandTriggerDetector = new OneHandTriggerGestureDetector(context);
        }
    }

    public void setDeadZone(DeadZone deadZone) {
        this.mDeadZone = deadZone;
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        DeadZone deadZone;
        OneHandTriggerGestureDetector oneHandTriggerGestureDetector = this.mOneHandTriggerDetector;
        if (oneHandTriggerGestureDetector != null) {
            oneHandTriggerGestureDetector.onTouchEvent(motionEvent);
        }
        if (motionEvent.getAction() != 4 || (deadZone = this.mDeadZone) == null) {
            return super.dispatchTouchEvent(motionEvent);
        }
        return deadZone.onTouchEvent(motionEvent);
    }
}
