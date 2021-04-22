package com.android.systemui.statusbar.notification.row;

import android.app.NotificationChannel;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;

/* access modifiers changed from: package-private */
/* compiled from: ChannelEditorDialogController.kt */
public final class ChannelEditorDialogController$padToFourChannels$1 extends Lambda implements Function1<NotificationChannel, Boolean> {
    final /* synthetic */ ChannelEditorDialogController this$0;

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    ChannelEditorDialogController$padToFourChannels$1(ChannelEditorDialogController channelEditorDialogController) {
        super(1);
        this.this$0 = channelEditorDialogController;
    }

    /* Return type fixed from 'java.lang.Object' to match base method */
    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.lang.Object] */
    @Override // kotlin.jvm.functions.Function1
    public /* bridge */ /* synthetic */ Boolean invoke(NotificationChannel notificationChannel) {
        return Boolean.valueOf(invoke(notificationChannel));
    }

    public final boolean invoke(NotificationChannel notificationChannel) {
        Intrinsics.checkParameterIsNotNull(notificationChannel, "it");
        return this.this$0.getProvidedChannels$name().contains(notificationChannel);
    }
}
