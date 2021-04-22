package com.android.systemui.shared.system;

import android.os.Looper;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.InputChannel;
import android.view.InputEvent;

public class InputChannelCompat$InputEventReceiver {
    private final InputChannel mInputChannel;
    private final BatchedInputEventReceiver mReceiver;

    public InputChannelCompat$InputEventReceiver(InputChannel inputChannel, Looper looper, Choreographer choreographer, final InputChannelCompat$InputEventListener inputChannelCompat$InputEventListener) {
        this.mInputChannel = inputChannel;
        this.mReceiver = new BatchedInputEventReceiver(inputChannel, looper, choreographer) {
            /* class com.android.systemui.shared.system.InputChannelCompat$InputEventReceiver.AnonymousClass1 */

            public void onInputEvent(InputEvent inputEvent) {
                inputChannelCompat$InputEventListener.onInputEvent(inputEvent);
                finishInputEvent(inputEvent, true);
            }
        };
    }

    public void dispose() {
        this.mReceiver.dispose();
        this.mInputChannel.dispose();
    }
}
