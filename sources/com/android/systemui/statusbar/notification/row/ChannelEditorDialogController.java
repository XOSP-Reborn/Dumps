package com.android.systemui.statusbar.notification.row;

import android.app.Dialog;
import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.statusbar.notification.row.NotificationInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

/* compiled from: ChannelEditorDialogController.kt */
public final class ChannelEditorDialogController {
    private Drawable appIcon;
    private String appName;
    private boolean appNotificationsEnabled = true;
    private Integer appUid;
    private final List<NotificationChannelGroup> channelGroupList = new ArrayList();
    private final Context context;
    public Dialog dialog;
    private final Map<NotificationChannel, Integer> edits = new LinkedHashMap();
    private final HashMap<String, CharSequence> groupNameLookup = new HashMap<>();
    private final INotificationManager noMan;
    private OnChannelEditorDialogFinishedListener onFinishListener;
    private NotificationInfo.OnSettingsClickListener onSettingsClickListener;
    private String packageName;
    private final List<NotificationChannel> providedChannels = new ArrayList();
    private final int wmFlags = -2130444032;

    @VisibleForTesting
    public static /* synthetic */ void groupNameLookup$annotations() {
    }

    @VisibleForTesting
    public static /* synthetic */ void providedChannels$annotations() {
    }

    public ChannelEditorDialogController(Context context2, INotificationManager iNotificationManager) {
        Intrinsics.checkParameterIsNotNull(context2, "c");
        Intrinsics.checkParameterIsNotNull(iNotificationManager, "noMan");
        this.noMan = iNotificationManager;
        Context applicationContext = context2.getApplicationContext();
        Intrinsics.checkExpressionValueIsNotNull(applicationContext, "c.applicationContext");
        this.context = applicationContext;
    }

    public final void setOnFinishListener(OnChannelEditorDialogFinishedListener onChannelEditorDialogFinishedListener) {
        this.onFinishListener = onChannelEditorDialogFinishedListener;
    }

    public final List<NotificationChannel> getProvidedChannels$name() {
        return this.providedChannels;
    }

    public final boolean getAppNotificationsEnabled() {
        return this.appNotificationsEnabled;
    }

    public final void setAppNotificationsEnabled(boolean z) {
        this.appNotificationsEnabled = z;
    }

    public final void prepareDialogForApp(String str, String str2, int i, Set<NotificationChannel> set, Drawable drawable, NotificationInfo.OnSettingsClickListener onSettingsClickListener2) {
        Intrinsics.checkParameterIsNotNull(str, "appName");
        Intrinsics.checkParameterIsNotNull(str2, "packageName");
        Intrinsics.checkParameterIsNotNull(set, "channels");
        Intrinsics.checkParameterIsNotNull(drawable, "appIcon");
        this.appName = str;
        this.packageName = str2;
        this.appUid = Integer.valueOf(i);
        this.appIcon = drawable;
        this.appNotificationsEnabled = checkAreAppNotificationsOn();
        this.onSettingsClickListener = onSettingsClickListener2;
        this.channelGroupList.clear();
        this.channelGroupList.addAll(fetchNotificationChannelGroups());
        buildGroupNameLookup();
        padToFourChannels(set);
    }

    private final void buildGroupNameLookup() {
        for (T t : this.channelGroupList) {
            if (t.getId() != null) {
                HashMap<String, CharSequence> hashMap = this.groupNameLookup;
                String id = t.getId();
                Intrinsics.checkExpressionValueIsNotNull(id, "group.id");
                CharSequence name = t.getName();
                Intrinsics.checkExpressionValueIsNotNull(name, "group.name");
                hashMap.put(id, name);
            }
        }
    }

    private final void padToFourChannels(Set<NotificationChannel> set) {
        this.providedChannels.clear();
        CollectionsKt.addAll(this.providedChannels, SequencesKt.take(CollectionsKt.asSequence(set), 4));
        CollectionsKt.addAll(this.providedChannels, SequencesKt.take(SequencesKt.distinct(SequencesKt.filterNot(getDisplayableChannels(CollectionsKt.asSequence(this.channelGroupList)), new ChannelEditorDialogController$padToFourChannels$1(this))), 4 - this.providedChannels.size()));
        if (this.providedChannels.size() == 1 && Intrinsics.areEqual("miscellaneous", this.providedChannels.get(0).getId())) {
            this.providedChannels.clear();
        }
    }

    private final Sequence<NotificationChannel> getDisplayableChannels(Sequence<NotificationChannelGroup> sequence) {
        return SequencesKt.sortedWith(SequencesKt.flatMap(sequence, ChannelEditorDialogController$getDisplayableChannels$channels$1.INSTANCE), new ChannelEditorDialogController$getDisplayableChannels$$inlined$compareBy$1());
    }

    public final void show() {
        initDialog();
        Dialog dialog2 = this.dialog;
        if (dialog2 != null) {
            dialog2.show();
        } else {
            Intrinsics.throwUninitializedPropertyAccessException("dialog");
            throw null;
        }
    }

    public final void close() {
        done();
    }

    /* access modifiers changed from: private */
    public final void done() {
        resetState();
        Dialog dialog2 = this.dialog;
        if (dialog2 != null) {
            dialog2.dismiss();
            OnChannelEditorDialogFinishedListener onChannelEditorDialogFinishedListener = this.onFinishListener;
            if (onChannelEditorDialogFinishedListener != null) {
                onChannelEditorDialogFinishedListener.onChannelEditorDialogFinished();
                return;
            }
            return;
        }
        Intrinsics.throwUninitializedPropertyAccessException("dialog");
        throw null;
    }

    private final void resetState() {
        this.appIcon = null;
        this.appUid = null;
        this.packageName = null;
        this.appName = null;
        this.edits.clear();
        this.providedChannels.clear();
        this.groupNameLookup.clear();
    }

    public final CharSequence groupNameForId(String str) {
        CharSequence charSequence = this.groupNameLookup.get(str);
        return charSequence != null ? charSequence : "";
    }

    public final void proposeEditForChannel(NotificationChannel notificationChannel, int i) {
        Intrinsics.checkParameterIsNotNull(notificationChannel, "channel");
        if (notificationChannel.getImportance() == i) {
            this.edits.remove(notificationChannel);
        } else {
            this.edits.put(notificationChannel, Integer.valueOf(i));
        }
    }

    private final List<NotificationChannelGroup> fetchNotificationChannelGroups() {
        try {
            INotificationManager iNotificationManager = this.noMan;
            String str = this.packageName;
            if (str != null) {
                Integer num = this.appUid;
                if (num != null) {
                    ParceledListSlice notificationChannelGroupsForPackage = iNotificationManager.getNotificationChannelGroupsForPackage(str, num.intValue(), false);
                    Intrinsics.checkExpressionValueIsNotNull(notificationChannelGroupsForPackage, "noMan.getNotificationCha…eName!!, appUid!!, false)");
                    List<NotificationChannelGroup> list = notificationChannelGroupsForPackage.getList();
                    if (!(list instanceof List)) {
                        list = null;
                    }
                    if (list != null) {
                        return list;
                    }
                    return CollectionsKt.emptyList();
                }
                Intrinsics.throwNpe();
                throw null;
            }
            Intrinsics.throwNpe();
            throw null;
        } catch (Exception e) {
            Log.e("ChannelDialogController", "Error fetching channel groups", e);
            return CollectionsKt.emptyList();
        }
    }

    private final boolean checkAreAppNotificationsOn() {
        try {
            INotificationManager iNotificationManager = this.noMan;
            String str = this.packageName;
            if (str != null) {
                Integer num = this.appUid;
                if (num != null) {
                    return iNotificationManager.areNotificationsEnabledForPackage(str, num.intValue());
                }
                Intrinsics.throwNpe();
                throw null;
            }
            Intrinsics.throwNpe();
            throw null;
        } catch (Exception e) {
            Log.e("ChannelDialogController", "Error calling NoMan", e);
            return false;
        }
    }

    private final void applyAppNotificationsOn(boolean z) {
        try {
            INotificationManager iNotificationManager = this.noMan;
            String str = this.packageName;
            if (str != null) {
                Integer num = this.appUid;
                if (num != null) {
                    iNotificationManager.setNotificationsEnabledForPackage(str, num.intValue(), z);
                } else {
                    Intrinsics.throwNpe();
                    throw null;
                }
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        } catch (Exception e) {
            Log.e("ChannelDialogController", "Error calling NoMan", e);
        }
    }

    private final void setChannelImportance(NotificationChannel notificationChannel, int i) {
        try {
            notificationChannel.setImportance(i);
            INotificationManager iNotificationManager = this.noMan;
            String str = this.packageName;
            if (str != null) {
                Integer num = this.appUid;
                if (num != null) {
                    iNotificationManager.updateNotificationChannelForPackage(str, num.intValue(), notificationChannel);
                } else {
                    Intrinsics.throwNpe();
                    throw null;
                }
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        } catch (Exception e) {
            Log.e("ChannelDialogController", "Unable to update notification importance", e);
        }
    }

    @VisibleForTesting
    public final void apply() {
        for (Map.Entry<NotificationChannel, Integer> entry : this.edits.entrySet()) {
            NotificationChannel key = entry.getKey();
            int intValue = entry.getValue().intValue();
            if (key.getImportance() != intValue) {
                setChannelImportance(key, intValue);
            }
        }
        if (this.appNotificationsEnabled != checkAreAppNotificationsOn()) {
            applyAppNotificationsOn(this.appNotificationsEnabled);
        }
    }

    @VisibleForTesting
    public final void launchSettings(View view) {
        Intrinsics.checkParameterIsNotNull(view, "sender");
        NotificationInfo.OnSettingsClickListener onSettingsClickListener2 = this.onSettingsClickListener;
        if (onSettingsClickListener2 != null) {
            Integer num = this.appUid;
            if (num != null) {
                onSettingsClickListener2.onClick(view, null, num.intValue());
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        }
    }

    private final void initDialog() {
        this.dialog = new Dialog(this.context);
        Dialog dialog2 = this.dialog;
        if (dialog2 != null) {
            Window window = dialog2.getWindow();
            if (window != null) {
                window.requestFeature(1);
            }
            Dialog dialog3 = this.dialog;
            if (dialog3 != null) {
                dialog3.setTitle(" ");
                Dialog dialog4 = this.dialog;
                if (dialog4 != null) {
                    dialog4.setContentView(C0010R$layout.notif_half_shelf);
                    dialog4.setCanceledOnTouchOutside(true);
                    ChannelEditorListView channelEditorListView = (ChannelEditorListView) dialog4.findViewById(C0007R$id.half_shelf_container);
                    channelEditorListView.setController(this);
                    channelEditorListView.setAppIcon(this.appIcon);
                    channelEditorListView.setAppName(this.appName);
                    channelEditorListView.setChannels(this.providedChannels);
                    TextView textView = (TextView) dialog4.findViewById(C0007R$id.done_button);
                    if (textView != null) {
                        textView.setOnClickListener(new ChannelEditorDialogController$initDialog$$inlined$apply$lambda$1(this));
                    }
                    TextView textView2 = (TextView) dialog4.findViewById(C0007R$id.see_more_button);
                    if (textView2 != null) {
                        textView2.setOnClickListener(new ChannelEditorDialogController$initDialog$$inlined$apply$lambda$2(this));
                    }
                    Window window2 = dialog4.getWindow();
                    if (window2 != null) {
                        window2.setBackgroundDrawable(new ColorDrawable(0));
                        window2.addFlags(this.wmFlags);
                        window2.setType(2014);
                        window2.setWindowAnimations(16973910);
                        WindowManager.LayoutParams attributes = window2.getAttributes();
                        attributes.format = -3;
                        attributes.setTitle(ChannelEditorDialogController.class.getSimpleName());
                        attributes.gravity = 81;
                        attributes.width = -1;
                        attributes.height = -2;
                        window2.setAttributes(attributes);
                        return;
                    }
                    return;
                }
                Intrinsics.throwUninitializedPropertyAccessException("dialog");
                throw null;
            }
            Intrinsics.throwUninitializedPropertyAccessException("dialog");
            throw null;
        }
        Intrinsics.throwUninitializedPropertyAccessException("dialog");
        throw null;
    }
}
