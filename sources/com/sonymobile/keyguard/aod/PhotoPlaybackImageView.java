package com.sonymobile.keyguard.aod;

import android.app.ActivityManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import com.android.systemui.statusbar.phone.DoubleTapHelper;
import java.io.IOException;

public class PhotoPlaybackImageView extends ImageView {
    private Bitmap mBitmap;
    private DoubleTapHelper mDoubleTapHelper;
    private FingerPrintFeedBackView mFingerPrintFeedBackView;
    private int mHeight;
    private onDoubleTapListener mListener = null;
    private Uri mUri = null;
    private int mWidth;
    private Path pathCircle = new Path();

    public interface onDoubleTapListener {
        void onDoubleTap();
    }

    private int getRotation(int i) {
        switch (i) {
            case 3:
            case 4:
                return 180;
            case 5:
            case 6:
                return 90;
            case 7:
            case 8:
                return 270;
            default:
                return 0;
        }
    }

    static /* synthetic */ void lambda$new$0(boolean z) {
    }

    public PhotoPlaybackImageView(Context context, FingerPrintFeedBackView fingerPrintFeedBackView) {
        super(context, null);
        this.mFingerPrintFeedBackView = fingerPrintFeedBackView;
        this.mDoubleTapHelper = new DoubleTapHelper(this, $$Lambda$PhotoPlaybackImageView$9yFSfQmXju9ajFpuJGycz_UsMLI.INSTANCE, new DoubleTapHelper.DoubleTapListener() {
            /* class com.sonymobile.keyguard.aod.$$Lambda$PhotoPlaybackImageView$ivUhIhZybFsD3ml4VBwDv0eXTY */

            @Override // com.android.systemui.statusbar.phone.DoubleTapHelper.DoubleTapListener
            public final boolean onDoubleTap() {
                return PhotoPlaybackImageView.this.lambda$new$1$PhotoPlaybackImageView();
            }
        }, null, null);
    }

    public /* synthetic */ boolean lambda$new$1$PhotoPlaybackImageView() {
        if (this.mUri == null || PhotoPlaybackProviderUtils.isAlbumApplicationEnabled(getContext())) {
            Log.e("PhotoPlaybackImageView", "call performDoubleTap!!!!!");
            this.mListener.onDoubleTap();
            performDoubleTap();
            return false;
        }
        FingerPrintFeedBackView fingerPrintFeedBackView = this.mFingerPrintFeedBackView;
        if (fingerPrintFeedBackView == null) {
            return false;
        }
        fingerPrintFeedBackView.showAlbumDisabledMessage(true);
        return false;
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mWidth = i;
        this.mHeight = i2;
        Log.d("PhotoPlaybackImageView", ".onSizeChanged: w = " + i + ", h = " + i2);
        super.onSizeChanged(i, i2, i3, i4);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        Log.d("PhotoPlaybackImageView", ".onDraw: mWidth = " + this.mWidth + ", mHeight = " + this.mHeight);
        int i = this.mHeight;
        this.pathCircle.addCircle((float) (this.mWidth / 2), (float) (i / 2), (float) (i / 2), Path.Direction.CW);
        canvas.clipPath(this.pathCircle);
        super.onDraw(canvas);
    }

    public boolean setUri(Uri uri) {
        this.mUri = uri;
        return createBitmapFromUri(uri);
    }

    private boolean createBitmapFromUri(Uri uri) {
        this.mBitmap = null;
        try {
            this.mBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
            int rotation = getRotation(getOrientation(uri));
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate((float) rotation);
                rotateBitmap(matrix);
            }
        } catch (IOException unused) {
            this.mBitmap = null;
        }
        cropSquareBitmap();
        Bitmap bitmap = this.mBitmap;
        if (bitmap == null) {
            return false;
        }
        setImageBitmap(bitmap);
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x002b, code lost:
        if (r2 == null) goto L_0x002e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:3:0x001b, code lost:
        if (r2 != null) goto L_0x001d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:5:?, code lost:
        r2.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getOrientation(android.net.Uri r4) {
        /*
            r3 = this;
            java.lang.String r0 = "PhotoPlaybackImageView"
            r1 = 0
            r2 = 0
            android.content.Context r3 = r3.getContext()     // Catch:{ IOException -> 0x0023 }
            android.content.ContentResolver r3 = r3.getContentResolver()     // Catch:{ IOException -> 0x0023 }
            java.io.InputStream r2 = r3.openInputStream(r4)     // Catch:{ IOException -> 0x0023 }
            android.media.ExifInterface r3 = new android.media.ExifInterface     // Catch:{ IOException -> 0x0023 }
            r3.<init>(r2)     // Catch:{ IOException -> 0x0023 }
            java.lang.String r4 = "Orientation"
            int r1 = r3.getAttributeInt(r4, r1)     // Catch:{ IOException -> 0x0023 }
            if (r2 == 0) goto L_0x002e
        L_0x001d:
            r2.close()     // Catch:{ IOException -> 0x002e }
            goto L_0x002e
        L_0x0021:
            r3 = move-exception
            goto L_0x0043
        L_0x0023:
            r3 = move-exception
            java.lang.String r3 = r3.getMessage()     // Catch:{ all -> 0x0021 }
            android.util.Log.e(r0, r3)     // Catch:{ all -> 0x0021 }
            if (r2 == 0) goto L_0x002e
            goto L_0x001d
        L_0x002e:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = ".getOrientation orientation:"
            r3.append(r4)
            r3.append(r1)
            java.lang.String r3 = r3.toString()
            android.util.Log.d(r0, r3)
            return r1
        L_0x0043:
            if (r2 == 0) goto L_0x0048
            r2.close()     // Catch:{ IOException -> 0x0048 }
        L_0x0048:
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.keyguard.aod.PhotoPlaybackImageView.getOrientation(android.net.Uri):int");
    }

    private void rotateBitmap(Matrix matrix) {
        try {
            Bitmap createBitmap = Bitmap.createBitmap(this.mBitmap, 0, 0, this.mBitmap.getWidth(), this.mBitmap.getHeight(), matrix, true);
            if (!this.mBitmap.sameAs(createBitmap)) {
                this.mBitmap = createBitmap;
            }
        } catch (OutOfMemoryError e) {
            Log.e("PhotoPlaybackImageView", "rotateBitmap: ", e);
        }
    }

    private void cropSquareBitmap() {
        int i;
        int i2;
        Bitmap bitmap = this.mBitmap;
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = this.mBitmap.getHeight();
            int i3 = width < height ? width : height;
            if (width < height) {
                i = calculateStartYForPortrait(width, height);
                i2 = 0;
            } else {
                i2 = (width / 2) - (i3 / 2);
                i = 0;
            }
            if (i2 < 0 || i < 0 || width <= 0 || height <= 0 || i3 <= 0) {
                this.mBitmap = null;
                return;
            }
            try {
                this.mBitmap = Bitmap.createBitmap(this.mBitmap, i2, i, i3, i3, (Matrix) null, true);
            } catch (OutOfMemoryError e) {
                Log.e("PhotoPlaybackImageView", "cropSquareBitmap: ", e);
                this.mBitmap = null;
            }
        }
    }

    private int calculateStartYForPortrait(int i, int i2) {
        int i3 = i / 2;
        int i4 = i2 / 3;
        if (i4 > i3) {
            return i4 - i3;
        }
        return 0;
    }

    public void setOnDoubleTapListener(onDoubleTapListener ondoubletaplistener) {
        this.mListener = ondoubletaplistener;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        this.mDoubleTapHelper.onTouchEvent(motionEvent);
        return true;
    }

    private void performDoubleTap() {
        if (this.mUri != null) {
            PhotoPlaybackSharedPreferences.setPhotoplaybackSharedPrefAlbumShown(getContext());
            if (PhotoPlaybackProviderUtils.isAlbumApplicationEnabled(getContext())) {
                showOnAlbum();
                return;
            }
            return;
        }
        PhotoPlaybackSharedPreferences.setPhotoplaybackSharedPrefIntroShown(getContext());
        showIntroduction();
    }

    private void showOnAlbum() {
        Intent intent = new Intent();
        intent.setClass(getContext().getApplicationContext(), LaunchAlbumActivity.class);
        intent.putExtra("URI", this.mUri.toString());
        try {
            ActivityManager.getService().startActivity((IApplicationThread) null, getContext().getBasePackageName(), intent, intent.resolveTypeIfNeeded(getContext().getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, (Bundle) null);
        } catch (RemoteException unused) {
            Log.e("PhotoPlaybackImageView", "Unable to start LaunchAlbumActivity");
        }
    }

    private void showIntroduction() {
        Intent intent = new Intent();
        intent.setClass(getContext().getApplicationContext(), PhotoPlaybackIntroductionActivity.class);
        try {
            ActivityManager.getService().startActivity((IApplicationThread) null, getContext().getBasePackageName(), intent, intent.resolveTypeIfNeeded(getContext().getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, (Bundle) null);
        } catch (RemoteException unused) {
            Log.e("PhotoPlaybackImageView", "Unable to start PhotoPlaybackIntroductionActivity");
        }
    }
}
