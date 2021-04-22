package com.android.systemui.screenshot;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.os.AsyncTask;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.SystemUI;
import com.android.systemui.util.NotificationChannels;
import java.text.SimpleDateFormat;
import java.util.Date;

/* compiled from: GlobalScreenshot */
class SaveImageInBackgroundTask extends AsyncTask<Void, Void, Void> {
    private final String mImageFileName = String.format("Screenshot_%s.png", new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(this.mImageTime)));
    private final int mImageHeight;
    private final long mImageTime = System.currentTimeMillis();
    private final int mImageWidth;
    private final Notification.Builder mNotificationBuilder;
    private final NotificationManager mNotificationManager;
    private final Notification.BigPictureStyle mNotificationStyle;
    private final SaveImageInBackgroundData mParams;
    private final Notification.Builder mPublicNotificationBuilder;

    SaveImageInBackgroundTask(Context context, SaveImageInBackgroundData saveImageInBackgroundData, NotificationManager notificationManager) {
        Resources resources = context.getResources();
        this.mParams = saveImageInBackgroundData;
        this.mImageWidth = saveImageInBackgroundData.image.getWidth();
        this.mImageHeight = saveImageInBackgroundData.image.getHeight();
        int i = saveImageInBackgroundData.iconSize;
        int i2 = saveImageInBackgroundData.previewWidth;
        int i3 = saveImageInBackgroundData.previewheight;
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.25f);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        Matrix matrix = new Matrix();
        float f = saveImageInBackgroundData.previewScale;
        matrix.setScale(f, f);
        float f2 = saveImageInBackgroundData.previewScale;
        matrix.postTranslate((((float) i2) - (((float) this.mImageWidth) * f2)) / 2.0f, (((float) i3) - (((float) this.mImageHeight) * f2)) / 2.0f);
        Bitmap generateAdjustedHwBitmap = generateAdjustedHwBitmap(saveImageInBackgroundData.image, i2, i3, matrix, paint, 1090519039);
        float f3 = (float) i;
        float min = f3 / ((float) Math.min(this.mImageWidth, this.mImageHeight));
        matrix.setScale(min, min);
        matrix.postTranslate((f3 - (((float) this.mImageWidth) * min)) / 2.0f, (f3 - (min * ((float) this.mImageHeight))) / 2.0f);
        Bitmap generateAdjustedHwBitmap2 = generateAdjustedHwBitmap(saveImageInBackgroundData.image, i, i, matrix, paint, 1090519039);
        this.mNotificationManager = notificationManager;
        long currentTimeMillis = System.currentTimeMillis();
        this.mNotificationStyle = new Notification.BigPictureStyle().bigPicture(generateAdjustedHwBitmap.createAshmemBitmap());
        this.mPublicNotificationBuilder = new Notification.Builder(context, NotificationChannels.SCREENSHOTS_HEADSUP).setContentTitle(resources.getString(C0014R$string.screenshot_saving_title)).setSmallIcon(C0006R$drawable.stat_notify_image).setCategory("progress").setWhen(currentTimeMillis).setShowWhen(true).setColor(resources.getColor(17170460));
        SystemUI.overrideNotificationAppName(context, this.mPublicNotificationBuilder, true);
        this.mNotificationBuilder = new Notification.Builder(context, NotificationChannels.SCREENSHOTS_HEADSUP).setContentTitle(resources.getString(C0014R$string.screenshot_saving_title)).setSmallIcon(C0006R$drawable.stat_notify_image).setWhen(currentTimeMillis).setShowWhen(true).setColor(resources.getColor(17170460)).setStyle(this.mNotificationStyle).setPublicVersion(this.mPublicNotificationBuilder.build());
        this.mNotificationBuilder.setFlag(32, true);
        SystemUI.overrideNotificationAppName(context, this.mNotificationBuilder, true);
        this.mNotificationManager.notify(1, this.mNotificationBuilder.build());
        this.mNotificationBuilder.setLargeIcon(generateAdjustedHwBitmap2.createAshmemBitmap());
        this.mNotificationStyle.bigLargeIcon((Bitmap) null);
    }

    private Bitmap generateAdjustedHwBitmap(Bitmap bitmap, int i, int i2, Matrix matrix, Paint paint, int i3) {
        Picture picture = new Picture();
        Canvas beginRecording = picture.beginRecording(i, i2);
        beginRecording.drawColor(i3);
        beginRecording.drawBitmap(bitmap, matrix, paint);
        picture.endRecording();
        return Bitmap.createBitmap(picture);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x018b, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x018c, code lost:
        if (r7 != null) goto L_0x018e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:?, code lost:
        r7.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0192, code lost:
        r3 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0193, code lost:
        r15.addSuppressed(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0196, code lost:
        throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.Void doInBackground(java.lang.Void... r15) {
        /*
        // Method dump skipped, instructions count: 443
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.screenshot.SaveImageInBackgroundTask.doInBackground(java.lang.Void[]):java.lang.Void");
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(Void r10) {
        SaveImageInBackgroundData saveImageInBackgroundData = this.mParams;
        int i = saveImageInBackgroundData.errorMsgResId;
        if (i != 0) {
            GlobalScreenshot.notifyScreenshotError(saveImageInBackgroundData.context, this.mNotificationManager, i);
        } else {
            Context context = saveImageInBackgroundData.context;
            Resources resources = context.getResources();
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType(this.mParams.imageUri, "image/png");
            intent.setFlags(268435457);
            long currentTimeMillis = System.currentTimeMillis();
            this.mPublicNotificationBuilder.setContentTitle(resources.getString(C0014R$string.screenshot_saved_title)).setContentText(resources.getString(C0014R$string.screenshot_saved_text)).setContentIntent(PendingIntent.getActivity(this.mParams.context, 0, intent, 0)).setWhen(currentTimeMillis).setAutoCancel(true).setColor(context.getColor(17170460));
            this.mNotificationBuilder.setContentTitle(resources.getString(C0014R$string.screenshot_saved_title)).setContentText(resources.getString(C0014R$string.screenshot_saved_text)).setContentIntent(PendingIntent.getActivity(this.mParams.context, 0, intent, 0)).setWhen(currentTimeMillis).setAutoCancel(true).setColor(context.getColor(17170460)).setPublicVersion(this.mPublicNotificationBuilder.build()).setFlag(32, false);
            this.mNotificationManager.notify(1, this.mNotificationBuilder.build());
        }
        this.mParams.finisher.run();
        this.mParams.clearContext();
    }

    /* access modifiers changed from: protected */
    public void onCancelled(Void r1) {
        this.mParams.finisher.run();
        this.mParams.clearImage();
        this.mParams.clearContext();
        this.mNotificationManager.cancel(1);
    }
}
