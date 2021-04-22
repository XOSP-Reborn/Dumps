package com.android.systemui.screenrecord;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import androidx.core.content.FileProvider;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class RecordingService extends Service {
    private Surface mInputSurface;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaRecorder mMediaRecorder;
    private Notification.Builder mRecordingNotificationBuilder;
    private boolean mShowTaps;
    private File mTempFile;
    private boolean mUseAudio;
    private VirtualDisplay mVirtualDisplay;

    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent getStartIntent(Context context, int i, Intent intent, boolean z, boolean z2) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.START").putExtra("extra_resultCode", i).putExtra("extra_data", intent).putExtra("extra_useAudio", z).putExtra("extra_showTaps", z2);
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0028, code lost:
        if (r0.equals("com.android.systemui.screenrecord.STOP") != false) goto L_0x0068;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onStartCommand(android.content.Intent r7, int r8, int r9) {
        /*
        // Method dump skipped, instructions count: 470
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.screenrecord.RecordingService.onStartCommand(android.content.Intent, int, int):int");
    }

    public void onCreate() {
        super.onCreate();
        this.mMediaProjectionManager = (MediaProjectionManager) getSystemService("media_projection");
    }

    private void startRecording() {
        try {
            this.mTempFile = File.createTempFile("temp", ".mp4");
            Log.d("RecordingService", "Writing video output to: " + this.mTempFile.getAbsolutePath());
            setTapsVisible(this.mShowTaps);
            this.mMediaRecorder = new MediaRecorder();
            if (this.mUseAudio) {
                this.mMediaRecorder.setAudioSource(1);
            }
            this.mMediaRecorder.setVideoSource(2);
            this.mMediaRecorder.setOutputFormat(2);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int i = displayMetrics.widthPixels;
            int i2 = displayMetrics.heightPixels;
            this.mMediaRecorder.setVideoEncoder(2);
            this.mMediaRecorder.setVideoSize(i, i2);
            this.mMediaRecorder.setVideoFrameRate(30);
            this.mMediaRecorder.setVideoEncodingBitRate(6000000);
            if (this.mUseAudio) {
                this.mMediaRecorder.setAudioEncoder(1);
                this.mMediaRecorder.setAudioChannels(1);
                this.mMediaRecorder.setAudioEncodingBitRate(16);
                this.mMediaRecorder.setAudioSamplingRate(44100);
            }
            this.mMediaRecorder.setOutputFile(this.mTempFile);
            this.mMediaRecorder.prepare();
            this.mInputSurface = this.mMediaRecorder.getSurface();
            this.mVirtualDisplay = this.mMediaProjection.createVirtualDisplay("Recording Display", i, i2, displayMetrics.densityDpi, 16, this.mInputSurface, null, null);
            this.mMediaRecorder.start();
            createRecordingNotification();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void createRecordingNotification() {
        NotificationChannel notificationChannel = new NotificationChannel("screen_record", getString(C0014R$string.screenrecord_name), 4);
        notificationChannel.setDescription(getString(C0014R$string.screenrecord_channel_description));
        notificationChannel.enableVibration(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
        notificationManager.createNotificationChannel(notificationChannel);
        this.mRecordingNotificationBuilder = new Notification.Builder(this, "screen_record").setSmallIcon(C0006R$drawable.ic_android).setContentTitle(getResources().getString(C0014R$string.screenrecord_name)).setUsesChronometer(true).setOngoing(true);
        setNotificationActions(false, notificationManager);
        startForeground(1, this.mRecordingNotificationBuilder.build());
    }

    private void setNotificationActions(boolean z, NotificationManager notificationManager) {
        int i;
        Resources resources = getResources();
        if (z) {
            i = C0014R$string.screenrecord_resume_label;
        } else {
            i = C0014R$string.screenrecord_pause_label;
        }
        this.mRecordingNotificationBuilder.setActions(new Notification.Action.Builder(Icon.createWithResource(this, C0006R$drawable.ic_android), getResources().getString(C0014R$string.screenrecord_stop_label), PendingIntent.getService(this, 2, getStopIntent(this), 134217728)).build(), new Notification.Action.Builder(Icon.createWithResource(this, C0006R$drawable.ic_android), resources.getString(i), PendingIntent.getService(this, 2, z ? getResumeIntent(this) : getPauseIntent(this), 134217728)).build(), new Notification.Action.Builder(Icon.createWithResource(this, C0006R$drawable.ic_android), getResources().getString(C0014R$string.screenrecord_cancel_label), PendingIntent.getService(this, 2, getCancelIntent(this), 134217728)).build());
        notificationManager.notify(1, this.mRecordingNotificationBuilder.build());
    }

    private Notification createSaveNotification(Path path) {
        Uri uriForFile = FileProvider.getUriForFile(this, "com.android.systemui.fileprovider", path.toFile());
        Log.d("RecordingService", "Screen recording saved to " + path.toString());
        Intent dataAndType = new Intent("android.intent.action.VIEW").setFlags(268435457).setDataAndType(uriForFile, "video/mp4");
        Notification.Action build = new Notification.Action.Builder(Icon.createWithResource(this, C0006R$drawable.ic_android), getResources().getString(C0014R$string.screenrecord_share_label), PendingIntent.getService(this, 2, getShareIntent(this, path.toString()), 134217728)).build();
        Notification.Builder autoCancel = new Notification.Builder(this, "screen_record").setSmallIcon(C0006R$drawable.ic_android).setContentTitle(getResources().getString(C0014R$string.screenrecord_name)).setContentText(getResources().getString(C0014R$string.screenrecord_save_message)).setContentIntent(PendingIntent.getActivity(this, 2, dataAndType, 1)).addAction(build).addAction(new Notification.Action.Builder(Icon.createWithResource(this, C0006R$drawable.ic_android), getResources().getString(C0014R$string.screenrecord_delete_label), PendingIntent.getService(this, 2, getDeleteIntent(this, path.toString()), 134217728)).build()).setAutoCancel(true);
        Bitmap createVideoThumbnail = ThumbnailUtils.createVideoThumbnail(path.toString(), 1);
        if (createVideoThumbnail != null) {
            autoCancel.setLargeIcon(createVideoThumbnail).setStyle(new Notification.BigPictureStyle().bigPicture(createVideoThumbnail).bigLargeIcon((Bitmap) null));
        }
        return autoCancel.build();
    }

    private void stopRecording() {
        setTapsVisible(false);
        this.mMediaRecorder.stop();
        this.mMediaRecorder.release();
        this.mMediaRecorder = null;
        this.mMediaProjection.stop();
        this.mMediaProjection = null;
        this.mInputSurface.release();
        this.mVirtualDisplay.release();
        stopSelf();
    }

    private void setTapsVisible(boolean z) {
        Settings.System.putInt(getApplicationContext().getContentResolver(), "show_touches", z ? 1 : 0);
    }

    private static Intent getStopIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.STOP");
    }

    private static Intent getPauseIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.PAUSE");
    }

    private static Intent getResumeIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.RESUME");
    }

    private static Intent getCancelIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.CANCEL");
    }

    private static Intent getShareIntent(Context context, String str) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.SHARE").putExtra("extra_path", str);
    }

    private static Intent getDeleteIntent(Context context, String str) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.DELETE").putExtra("extra_path", str);
    }
}
