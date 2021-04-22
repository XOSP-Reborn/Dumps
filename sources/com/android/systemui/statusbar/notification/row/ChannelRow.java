package com.android.systemui.statusbar.notification.row;

import android.app.NotificationChannel;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.systemui.C0007R$id;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ChannelEditorListView.kt */
public final class ChannelRow extends LinearLayout {
    private NotificationChannel channel;
    private TextView channelDescription;
    private TextView channelName;
    public ChannelEditorDialogController controller;
    private boolean gentle;

    /* renamed from: switch  reason: not valid java name */
    private Switch f1switch;

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public ChannelRow(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Intrinsics.checkParameterIsNotNull(context, "c");
        Intrinsics.checkParameterIsNotNull(attributeSet, "attrs");
    }

    public final ChannelEditorDialogController getController() {
        ChannelEditorDialogController channelEditorDialogController = this.controller;
        if (channelEditorDialogController != null) {
            return channelEditorDialogController;
        }
        Intrinsics.throwUninitializedPropertyAccessException("controller");
        throw null;
    }

    public final void setController(ChannelEditorDialogController channelEditorDialogController) {
        Intrinsics.checkParameterIsNotNull(channelEditorDialogController, "<set-?>");
        this.controller = channelEditorDialogController;
    }

    public final NotificationChannel getChannel() {
        return this.channel;
    }

    public final void setChannel(NotificationChannel notificationChannel) {
        this.channel = notificationChannel;
        updateImportance();
        updateViews();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        View findViewById = findViewById(C0007R$id.channel_name);
        Intrinsics.checkExpressionValueIsNotNull(findViewById, "findViewById(R.id.channel_name)");
        this.channelName = (TextView) findViewById;
        View findViewById2 = findViewById(C0007R$id.channel_description);
        Intrinsics.checkExpressionValueIsNotNull(findViewById2, "findViewById(R.id.channel_description)");
        this.channelDescription = (TextView) findViewById2;
        View findViewById3 = findViewById(C0007R$id.toggle);
        Intrinsics.checkExpressionValueIsNotNull(findViewById3, "findViewById(R.id.toggle)");
        this.f1switch = (Switch) findViewById3;
        Switch r0 = this.f1switch;
        if (r0 != null) {
            r0.setOnCheckedChangeListener(new ChannelRow$onFinishInflate$1(this));
        } else {
            Intrinsics.throwUninitializedPropertyAccessException("switch");
            throw null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x006a  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0075  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final void updateViews() {
        /*
        // Method dump skipped, instructions count: 134
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.row.ChannelRow.updateViews():void");
    }

    private final void updateImportance() {
        NotificationChannel notificationChannel = this.channel;
        boolean z = false;
        int importance = notificationChannel != null ? notificationChannel.getImportance() : 0;
        if (importance != -1000 && importance < 3) {
            z = true;
        }
        this.gentle = z;
    }
}
