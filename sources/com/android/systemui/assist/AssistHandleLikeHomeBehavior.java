package com.android.systemui.assist;

import android.content.Context;
import com.android.systemui.Dependency;
import com.android.systemui.assist.AssistHandleBehaviorController;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.recents.OverviewProxyService;
import java.io.PrintWriter;

final class AssistHandleLikeHomeBehavior implements AssistHandleBehaviorController.BehaviorController {
    private AssistHandleCallbacks mAssistHandleCallbacks;
    private boolean mIsDozing;
    private boolean mIsHomeHandleHiding;
    private final OverviewProxyService.OverviewProxyListener mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() {
        /* class com.android.systemui.assist.AssistHandleLikeHomeBehavior.AnonymousClass2 */

        @Override // com.android.systemui.recents.OverviewProxyService.OverviewProxyListener
        public void onSystemUiStateChanged(int i) {
            AssistHandleLikeHomeBehavior.this.handleSystemUiStateChange(i);
        }
    };
    private final OverviewProxyService mOverviewProxyService = ((OverviewProxyService) Dependency.get(OverviewProxyService.class));
    private final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    private final StatusBarStateController.StateListener mStatusBarStateListener = new StatusBarStateController.StateListener() {
        /* class com.android.systemui.assist.AssistHandleLikeHomeBehavior.AnonymousClass1 */

        @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
        public void onDozingChanged(boolean z) {
            AssistHandleLikeHomeBehavior.this.handleDozingChanged(z);
        }
    };

    private static boolean isHomeHandleHiding(int i) {
        return (i & 2) != 0;
    }

    AssistHandleLikeHomeBehavior() {
    }

    @Override // com.android.systemui.assist.AssistHandleBehaviorController.BehaviorController
    public void onModeActivated(Context context, AssistHandleCallbacks assistHandleCallbacks) {
        this.mAssistHandleCallbacks = assistHandleCallbacks;
        this.mIsDozing = this.mStatusBarStateController.isDozing();
        this.mStatusBarStateController.addCallback(this.mStatusBarStateListener);
        this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
        callbackForCurrentState();
    }

    @Override // com.android.systemui.assist.AssistHandleBehaviorController.BehaviorController
    public void onModeDeactivated() {
        this.mAssistHandleCallbacks = null;
        this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleDozingChanged(boolean z) {
        if (this.mIsDozing != z) {
            this.mIsDozing = z;
            callbackForCurrentState();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleSystemUiStateChange(int i) {
        boolean isHomeHandleHiding = isHomeHandleHiding(i);
        if (this.mIsHomeHandleHiding != isHomeHandleHiding) {
            this.mIsHomeHandleHiding = isHomeHandleHiding;
            callbackForCurrentState();
        }
    }

    private void callbackForCurrentState() {
        AssistHandleCallbacks assistHandleCallbacks = this.mAssistHandleCallbacks;
        if (assistHandleCallbacks != null) {
            if (this.mIsHomeHandleHiding || this.mIsDozing) {
                this.mAssistHandleCallbacks.hide();
            } else {
                assistHandleCallbacks.showAndStay();
            }
        }
    }

    @Override // com.android.systemui.assist.AssistHandleBehaviorController.BehaviorController
    public void dump(PrintWriter printWriter, String str) {
        printWriter.println("Current AssistHandleLikeHomeBehavior State:");
        printWriter.println(str + "   mIsDozing=" + this.mIsDozing);
        printWriter.println(str + "   mIsHomeHandleHiding=" + this.mIsHomeHandleHiding);
    }
}
