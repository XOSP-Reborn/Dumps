package com.android.systemui.volume;

import android.content.Context;
import android.media.AudioSystem;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.plugins.VolumeDialogController;
import java.util.Arrays;

public class Events {
    public static final String[] DISMISS_REASONS = {"unknown", "touch_outside", "volume_controller", "timeout", "screen_off", "settings_clicked", "done_clicked", "a11y_stream_changed", "output_chooser", "usb_temperature_below_threshold"};
    private static final String[] EVENT_TAGS = {"show_dialog", "dismiss_dialog", "active_stream_changed", "expand", "key", "collection_started", "collection_stopped", "icon_click", "settings_click", "touch_level_changed", "level_changed", "internal_ringer_mode_changed", "external_ringer_mode_changed", "zen_mode_changed", "suppressor_changed", "mute_changed", "touch_level_done", "zen_mode_config_changed", "ringer_toggle", "show_usb_overheat_alarm", "dismiss_usb_overheat_alarm", "odi_captions_click", "odi_captions_tooltip_click"};
    public static final String[] SHOW_REASONS = {"unknown", "volume_changed", "remote_volume_changed", "usb_temperature_above_threshold"};
    private static final String TAG = Util.logTag(Events.class);
    public static Callback sCallback;

    public interface Callback {
        void writeEvent(long j, int i, Object[] objArr);

        void writeState(long j, VolumeDialogController.State state);
    }

    private static String ringerModeToString(int i) {
        return i != 0 ? i != 1 ? i != 2 ? "unknown" : "normal" : "vibrate" : "silent";
    }

    private static String zenModeToString(int i) {
        return i != 0 ? i != 1 ? i != 2 ? i != 3 ? "unknown" : "alarms" : "no_interruptions" : "important_interruptions" : "off";
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public static void writeEvent(Context context, int i, Object... objArr) {
        MetricsLogger metricsLogger = new MetricsLogger();
        long currentTimeMillis = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("writeEvent ");
        sb.append(EVENT_TAGS[i]);
        if (objArr != null && objArr.length > 0) {
            sb.append(" ");
            switch (i) {
                case 0:
                    MetricsLogger.visible(context, 207);
                    MetricsLogger.histogram(context, "volume_from_keyguard", ((Boolean) objArr[1]).booleanValue() ? 1 : 0);
                    sb.append(SHOW_REASONS[((Integer) objArr[0]).intValue()]);
                    sb.append(" keyguard=");
                    sb.append(objArr[1]);
                    break;
                case 1:
                    MetricsLogger.hidden(context, 207);
                    sb.append(DISMISS_REASONS[((Integer) objArr[0]).intValue()]);
                    break;
                case 2:
                    MetricsLogger.action(context, 210, ((Integer) objArr[0]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    break;
                case 3:
                    MetricsLogger.visibility(context, 208, ((Boolean) objArr[0]).booleanValue());
                    sb.append(objArr[0]);
                    break;
                case 4:
                    MetricsLogger.action(context, 211, ((Integer) objArr[0]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 5:
                case 6:
                case 17:
                default:
                    sb.append(Arrays.asList(objArr));
                    break;
                case 7:
                    MetricsLogger.action(context, 212, ((Integer) objArr[0]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(iconStateToString(((Integer) objArr[1]).intValue()));
                    break;
                case 8:
                    metricsLogger.action(1386);
                    break;
                case 9:
                case 10:
                case 15:
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 11:
                    sb.append(ringerModeToString(((Integer) objArr[0]).intValue()));
                    break;
                case 12:
                    MetricsLogger.action(context, 213, ((Integer) objArr[0]).intValue());
                    sb.append(ringerModeToString(((Integer) objArr[0]).intValue()));
                    break;
                case 13:
                    sb.append(zenModeToString(((Integer) objArr[0]).intValue()));
                    break;
                case 14:
                    sb.append(objArr[0]);
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 16:
                    MetricsLogger.action(context, 209, ((Integer) objArr[1]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 18:
                    metricsLogger.action(1385, ((Integer) objArr[0]).intValue());
                    break;
                case 19:
                    MetricsLogger.visible(context, 1457);
                    MetricsLogger.histogram(context, "show_usb_overheat_alarm", ((Boolean) objArr[1]).booleanValue() ? 1 : 0);
                    sb.append(SHOW_REASONS[((Integer) objArr[0]).intValue()]);
                    sb.append(" keyguard=");
                    sb.append(objArr[1]);
                    break;
                case 20:
                    MetricsLogger.hidden(context, 1457);
                    MetricsLogger.histogram(context, "dismiss_usb_overheat_alarm", ((Boolean) objArr[1]).booleanValue() ? 1 : 0);
                    sb.append(DISMISS_REASONS[((Integer) objArr[0]).intValue()]);
                    sb.append(" keyguard=");
                    sb.append(objArr[1]);
                    break;
            }
        }
        Log.i(TAG, sb.toString());
        Callback callback = sCallback;
        if (callback != null) {
            callback.writeEvent(currentTimeMillis, i, objArr);
        }
    }

    public static void writeState(long j, VolumeDialogController.State state) {
        Callback callback = sCallback;
        if (callback != null) {
            callback.writeState(j, state);
        }
    }

    private static String iconStateToString(int i) {
        if (i == 1) {
            return "unmute";
        }
        if (i == 2) {
            return "mute";
        }
        if (i == 3) {
            return "vibrate";
        }
        return "unknown_state_" + i;
    }
}
