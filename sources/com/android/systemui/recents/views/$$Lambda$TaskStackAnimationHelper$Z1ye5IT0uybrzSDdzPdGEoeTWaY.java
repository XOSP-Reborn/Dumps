package com.android.systemui.recents.views;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;

/* renamed from: com.android.systemui.recents.views.-$$Lambda$TaskStackAnimationHelper$Z1ye5IT0uybrzSDdzPdGEoeTWaY  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$TaskStackAnimationHelper$Z1ye5IT0uybrzSDdzPdGEoeTWaY implements Runnable {
    public static final /* synthetic */ $$Lambda$TaskStackAnimationHelper$Z1ye5IT0uybrzSDdzPdGEoeTWaY INSTANCE = new $$Lambda$TaskStackAnimationHelper$Z1ye5IT0uybrzSDdzPdGEoeTWaY();

    private /* synthetic */ $$Lambda$TaskStackAnimationHelper$Z1ye5IT0uybrzSDdzPdGEoeTWaY() {
    }

    public final void run() {
        EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
    }
}
