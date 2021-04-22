package com.sonymobile.keyguard.aod;

import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import androidx.appcompat.R$styleable;
import com.android.systemui.C0008R$integer;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.AlarmTimeout;
import com.sonymobile.keyguard.aod.PhotoPlaybackImageView;
import com.sonymobile.keyguard.statistics.LockscreenStatisticsPhotoPlaybackPhotoShownReporter;
import com.sonymobile.keyguard.statistics.LockscreenStatisticsPhotoPlaybackPhotoTappedReporter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class PhotoPlaybackView extends FrameLayout {
    private AlarmManager mAlarmManager;
    private float mAlpha;
    private AsyncTask<Void, Void, Void> mAsyncTaskCheckConditions;
    private int mCenterX;
    private int mCenterY;
    private Context mContext;
    private float mDefaultAlpha;
    private int mDozeMode = -1;
    private boolean mDozing = false;
    private FingerPrintFeedBackView mFingerPrintFeedBackView;
    private final Handler mHandler = new Handler() {
        /* class com.sonymobile.keyguard.aod.PhotoPlaybackView.AnonymousClass1 */

        public void handleMessage(Message message) {
            switch (message.what) {
                case 100:
                    PhotoPlaybackView.this.handleShow();
                    return;
                case 101:
                    PhotoPlaybackView.this.QueryTimeout();
                    return;
                case 102:
                    PhotoPlaybackView.this.showMusicInfoOrSticker();
                    return;
                default:
                    return;
            }
        }
    };
    ArrayList<PhotoPlaybackImageView> mImageViews = new ArrayList<>();
    private boolean mIsCheckConditions = false;
    private boolean mIsQueryTimeout = false;
    private boolean mIsQueryTimerRunning = false;
    private boolean mIsScreenOn = false;
    private ArrayList<ArrayList<LayoutData>> mLayoutDatas = new ArrayList<>();
    private int mLayoutNumver = 0;
    private MusicInfoView mMusicInfoView;
    private ContentObserver mPhotoPlaybackObserver = new ContentObserver(new Handler()) {
        /* class com.sonymobile.keyguard.aod.PhotoPlaybackView.AnonymousClass6 */

        public void onChange(boolean z) {
            PhotoPlaybackView.this.updatephotoPlayback();
        }
    };
    private int mPhotoType = -1;
    private Random mRandom;
    private final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() {
        /* class com.sonymobile.keyguard.aod.PhotoPlaybackView.AnonymousClass2 */

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOn() {
            PhotoPlaybackView.this.mIsScreenOn = true;
            if (!PhotoPlaybackView.this.mUpdated && PhotoPlaybackView.this.mDozing && PhotoPlaybackView.this.mIsCheckConditions) {
                if (PhotoPlaybackView.this.mDozeMode == 1 || PhotoPlaybackView.this.mDozeMode == 2) {
                    PhotoPlaybackView.this.mUpdated = true;
                    PhotoPlaybackView.this.updatephotoPlayback();
                }
            }
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOff() {
            PhotoPlaybackView.this.mIsScreenOn = false;
        }
    };
    private boolean mShouldShowMusicInfo = false;
    private boolean mShouldShowSticker = false;
    private StatusBar mStatusBar;
    private StickerView mStickerView;
    private AlarmTimeout mTimeTicker;
    private boolean mUpdated = false;

    /* access modifiers changed from: private */
    public class LayoutData {
        int radius;
        int x;
        int y;

        LayoutData(int i, int i2, int i3) {
            this.x = i;
            this.y = i2;
            this.radius = i3;
        }
    }

    public PhotoPlaybackView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        this.mRandom = new Random();
        this.mDefaultAlpha = this.mContext.getResources().getFloat(C0008R$integer.somc_keyguard_photoplayback_default_alpha);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mTimeTicker = new AlarmTimeout(this.mAlarmManager, new AlarmManager.OnAlarmListener() {
            /* class com.sonymobile.keyguard.aod.$$Lambda$PhotoPlaybackView$mbeOwanoUeEmzhPOnMo_ZC1NOOg */

            public final void onAlarm() {
                PhotoPlaybackView.this.onDeletePhotoPlaybackTimeout();
            }
        }, "aod_photo_playback_delete", new Handler());
        ((ScreenLifecycle) Dependency.get(ScreenLifecycle.class)).addObserver(this.mScreenObserver);
        initLayoutData();
    }

    private void initLayoutData() {
        ArrayList<LayoutData> arrayList = new ArrayList<>();
        arrayList.add(new LayoutData(-102, 221, R$styleable.AppCompatTheme_windowFixedWidthMajor));
        arrayList.add(new LayoutData(129, -285, 150));
        arrayList.add(new LayoutData(156, 330, 100));
        arrayList.add(new LayoutData(-155, -313, 60));
        this.mLayoutDatas.add(arrayList);
        ArrayList<LayoutData> arrayList2 = new ArrayList<>();
        arrayList2.add(new LayoutData(142, 299, 140));
        arrayList2.add(new LayoutData(-97, -247, R$styleable.AppCompatTheme_windowFixedWidthMajor));
        arrayList2.add(new LayoutData(-160, 195, 60));
        arrayList2.add(new LayoutData(152, -222, 80));
        this.mLayoutDatas.add(arrayList2);
        ArrayList<LayoutData> arrayList3 = new ArrayList<>();
        arrayList3.add(new LayoutData(-74, 385, 170));
        arrayList3.add(new LayoutData(156, -202, R$styleable.AppCompatTheme_windowFixedWidthMajor));
        arrayList3.add(new LayoutData(-126, -290, 80));
        arrayList3.add(new LayoutData(164, 158, 60));
        this.mLayoutDatas.add(arrayList3);
        ArrayList<LayoutData> arrayList4 = new ArrayList<>();
        arrayList4.add(new LayoutData(-151, 259, 170));
        arrayList4.add(new LayoutData(156, -156, 80));
        arrayList4.add(new LayoutData(-54, -341, 80));
        arrayList4.add(new LayoutData(R$styleable.AppCompatTheme_viewInflaterClass, 316, 60));
        this.mLayoutDatas.add(arrayList4);
        ArrayList<LayoutData> arrayList5 = new ArrayList<>();
        arrayList5.add(new LayoutData(127, 278, 160));
        arrayList5.add(new LayoutData(-103, -253, R$styleable.AppCompatTheme_windowFixedWidthMajor));
        arrayList5.add(new LayoutData(-156, 260, 80));
        arrayList5.add(new LayoutData(R$styleable.AppCompatTheme_windowActionBar, -227, 60));
        this.mLayoutDatas.add(arrayList5);
        ArrayList<LayoutData> arrayList6 = new ArrayList<>();
        arrayList6.add(new LayoutData(-150, 239, 150));
        arrayList6.add(new LayoutData(130, -259, 100));
        arrayList6.add(new LayoutData(172, 354, R$styleable.AppCompatTheme_windowFixedWidthMajor));
        arrayList6.add(new LayoutData(-148, -351, 100));
        this.mLayoutDatas.add(arrayList6);
        ArrayList<LayoutData> arrayList7 = new ArrayList<>();
        arrayList7.add(new LayoutData(100, 269, R$styleable.AppCompatTheme_windowFixedWidthMajor));
        arrayList7.add(new LayoutData(-97, -301, R$styleable.AppCompatTheme_windowFixedWidthMajor));
        arrayList7.add(new LayoutData(152, -153, 80));
        arrayList7.add(new LayoutData(-101, 169, 60));
        this.mLayoutDatas.add(arrayList7);
        ArrayList<LayoutData> arrayList8 = new ArrayList<>();
        arrayList8.add(new LayoutData(-96, 370, 160));
        arrayList8.add(new LayoutData(162, -285, 140));
        arrayList8.add(new LayoutData(-150, -220, 80));
        arrayList8.add(new LayoutData(137, 158, 60));
        this.mLayoutDatas.add(arrayList8);
        ArrayList<LayoutData> arrayList9 = new ArrayList<>();
        arrayList9.add(new LayoutData(-151, 259, 170));
        arrayList9.add(new LayoutData(156, -194, 100));
        arrayList9.add(new LayoutData(-100, -261, 60));
        arrayList9.add(new LayoutData(164, 243, 60));
        this.mLayoutDatas.add(arrayList9);
        ArrayList<LayoutData> arrayList10 = new ArrayList<>();
        arrayList10.add(new LayoutData(141, 283, 100));
        arrayList10.add(new LayoutData(-142, -292, 140));
        arrayList10.add(new LayoutData(-156, 236, 80));
        arrayList10.add(new LayoutData(131, -262, 60));
        this.mLayoutDatas.add(arrayList10);
        Display defaultDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getRealSize(point);
        this.mCenterX = point.x / 2;
        this.mCenterY = point.y / 2;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setDozing(boolean z) {
        this.mDozing = z;
        resetFFMessage();
        if (this.mDozing) {
            calculateAlphaValue();
            this.mAsyncTaskCheckConditions = new AsyncTask<Void, Void, Void>() {
                /* class com.sonymobile.keyguard.aod.PhotoPlaybackView.AnonymousClass3 */

                /* access modifiers changed from: protected */
                public Void doInBackground(Void... voidArr) {
                    if (isCancelled()) {
                        return null;
                    }
                    PhotoPlaybackView photoPlaybackView = PhotoPlaybackView.this;
                    photoPlaybackView.mIsCheckConditions = photoPlaybackView.checkConditions();
                    return null;
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(Void r1) {
                    if (!isCancelled()) {
                        PhotoPlaybackView.this.screenUpdate();
                        PhotoPlaybackView.this.mAsyncTaskCheckConditions = null;
                    }
                }
            }.execute(new Void[0]);
            return;
        }
        AsyncTask<Void, Void, Void> asyncTask = this.mAsyncTaskCheckConditions;
        if (asyncTask != null) {
            asyncTask.cancel(false);
            this.mAsyncTaskCheckConditions = null;
        }
        this.mHandler.removeMessages(100);
        this.mHandler.removeMessages(101);
        this.mHandler.removeMessages(102);
        removeAllViews();
        this.mImageViews.clear();
        this.mMusicInfoView.clearAnimation();
        this.mStickerView.clearAnimation();
        this.mTimeTicker.cancel();
        unregisterPhotoPlaybackObserver();
        this.mDozeMode = -1;
        this.mIsCheckConditions = false;
        this.mIsQueryTimerRunning = false;
        this.mIsQueryTimeout = false;
        this.mShouldShowMusicInfo = false;
        this.mShouldShowSticker = false;
        this.mUpdated = false;
    }

    private boolean isUserdebugVariant() {
        return "userdebug".equals(Build.TYPE);
    }

    private void calculateAlphaValue() {
        int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "somc_photoplayback_dimmer", -1, -2);
        if (!isUserdebugVariant() || intForUser < 1 || intForUser > 100) {
            this.mAlpha = this.mDefaultAlpha;
        } else {
            this.mAlpha = ((float) (100 - intForUser)) / 100.0f;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void screenUpdate() {
        if (!this.mIsCheckConditions) {
            this.mHandler.sendEmptyMessage(102);
        } else if (this.mDozing) {
            removeAllViews();
            this.mImageViews.clear();
            this.mLayoutNumver = this.mRandom.nextInt(10);
            registerPhotoPlaybackObserver();
            DozeParameters instance = DozeParameters.getInstance(this.mContext);
            if (instance.getPickupOn()) {
                this.mDozeMode = 2;
            } else if (instance.getSmartOn()) {
                this.mDozeMode = 1;
            } else if (instance.getAlwaysOn()) {
                this.mDozeMode = 0;
            }
            if (this.mIsScreenOn || this.mDozeMode == 0) {
                updatephotoPlayback();
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean checkConditions() {
        if (!PhotoPlaybackProviderUtils.isOwner() || !PhotoPlaybackProviderUtils.isPhotoPlaybackContentProviderAvailable(this.mContext) || !PhotoPlaybackProviderUtils.isPhotoPlaybackApplicationEnabled(this.mContext) || !PhotoPlaybackProviderUtils.isPhotoPlaybackEnabled(this.mContext)) {
            return false;
        }
        if (PhotoPlaybackProviderUtils.getPhotoPlaybackMode(this.mContext) == 0 || (PhotoPlaybackProviderUtils.getPhotoPlaybackMode(this.mContext) == 1 && isPermissionAllowed())) {
            return true;
        }
        return false;
    }

    private boolean isPermissionAllowed() {
        Cursor queryForStatus = PhotoPlaybackProviderUtils.queryForStatus(this.mContext);
        boolean z = false;
        if (queryForStatus != null) {
            if (queryForStatus.getCount() > 0) {
                queryForStatus.moveToFirst();
                if (queryForStatus.getInt(1) == 1) {
                    z = true;
                }
            }
            queryForStatus.close();
        }
        return z;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updatephotoPlayback() {
        Cursor queryForPhoto = PhotoPlaybackProviderUtils.queryForPhoto(this.mContext);
        if (!this.mIsQueryTimerRunning) {
            QueryTimer();
        }
        if (this.mIsQueryTimeout) {
            if (queryForPhoto != null) {
                queryForPhoto.close();
            }
        } else if (queryForPhoto != null) {
            if (queryForPhoto.getCount() <= 0) {
                queryForPhoto.close();
                return;
            }
            this.mHandler.removeMessages(101);
            final int i = 0;
            final CountDownLatch countDownLatch = new CountDownLatch(queryForPhoto.getCount());
            unregisterPhotoPlaybackObserver();
            for (boolean moveToFirst = queryForPhoto.moveToFirst(); moveToFirst; moveToFirst = queryForPhoto.moveToNext()) {
                final Uri parse = Uri.parse(queryForPhoto.getString(1));
                try {
                    this.mPhotoType = queryForPhoto.getInt(2);
                } catch (Exception e) {
                    Log.e("PhotoPlaybackView", "get cursor.getInt(2) failed: " + e.toString());
                }
                new Thread() {
                    /* class com.sonymobile.keyguard.aod.PhotoPlaybackView.AnonymousClass4 */

                    public void run() {
                        PhotoPlaybackView.this.prepareImageView(parse, i);
                        countDownLatch.countDown();
                    }
                }.start();
                i++;
            }
            LockscreenStatisticsPhotoPlaybackPhotoShownReporter.sendEvent(this.mContext, this.mPhotoType);
            queryForPhoto.close();
            new Thread() {
                /* class com.sonymobile.keyguard.aod.PhotoPlaybackView.AnonymousClass5 */

                public void run() {
                    if (PhotoPlaybackView.this.mDozing) {
                        try {
                            Thread.sleep(1000);
                            countDownLatch.await();
                        } catch (InterruptedException e) {
                            Log.e("PhotoPlaybackView", e.toString());
                        }
                        PhotoPlaybackView.this.mHandler.sendEmptyMessage(100);
                    }
                }
            }.start();
        }
    }

    private void QueryTimer() {
        this.mHandler.sendEmptyMessageDelayed(101, 1000);
        this.mIsQueryTimerRunning = true;
        prepareMusicInfoView();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void QueryTimeout() {
        this.mIsQueryTimeout = true;
        showMusicInfoOrSticker();
    }

    private void prepareMusicInfoView() {
        if (this.mShouldShowMusicInfo) {
            this.mStickerView.setVisibility(8);
            this.mMusicInfoView.setVisibility(4);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showMusicInfoOrSticker() {
        if (this.mStickerView.getVisibility() != 0 && this.mMusicInfoView.getVisibility() != 0) {
            AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
            alphaAnimation.setDuration(1000);
            alphaAnimation.setInterpolator(this.mContext, 17432587);
            alphaAnimation.setFillAfter(true);
            if (this.mShouldShowMusicInfo) {
                this.mStickerView.setVisibility(8);
                this.mMusicInfoView.startAnimation(alphaAnimation);
            } else if (this.mShouldShowSticker) {
                this.mMusicInfoView.setVisibility(8);
                this.mStickerView.startAnimation(alphaAnimation);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void prepareImageView(Uri uri, int i) {
        boolean z;
        Drawable demoImage;
        PhotoPlaybackImageView photoPlaybackImageView = new PhotoPlaybackImageView(this.mContext, this.mFingerPrintFeedBackView);
        float convertDp2Px = convertDp2Px(this.mLayoutDatas.get(this.mLayoutNumver).get(i).x, this.mContext);
        float convertDp2Px2 = convertDp2Px(this.mLayoutDatas.get(this.mLayoutNumver).get(i).y, this.mContext);
        float convertDp2Px3 = convertDp2Px(this.mLayoutDatas.get(this.mLayoutNumver).get(i).radius, this.mContext);
        int i2 = (int) (2.0f * convertDp2Px3);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(i2, i2);
        layoutParams.leftMargin = this.mCenterX + ((int) (convertDp2Px - convertDp2Px3));
        layoutParams.topMargin = this.mCenterY + ((int) (convertDp2Px2 - convertDp2Px3));
        photoPlaybackImageView.setLayoutParams(layoutParams);
        if (PhotoPlaybackProviderUtils.hasContentScheme(uri)) {
            z = photoPlaybackImageView.setUri(uri);
        } else if (!PhotoPlaybackProviderUtils.hasDemoScheme(uri) || (demoImage = getDemoImage(uri)) == null) {
            z = false;
        } else {
            photoPlaybackImageView.setImageDrawable(demoImage);
            z = true;
            showDemoMessage();
        }
        if (z) {
            photoPlaybackImageView.setOnDoubleTapListener(new PhotoPlaybackImageView.onDoubleTapListener() {
                /* class com.sonymobile.keyguard.aod.$$Lambda$PhotoPlaybackView$Mkp29SmWFlPwlzxM8GiWLTwfQA */

                @Override // com.sonymobile.keyguard.aod.PhotoPlaybackImageView.onDoubleTapListener
                public final void onDoubleTap() {
                    PhotoPlaybackView.this.lambda$prepareImageView$0$PhotoPlaybackView();
                }
            });
            this.mImageViews.add(photoPlaybackImageView);
            return;
        }
        Log.d("PhotoPlaybackView", "prepareImageView failed to set uri:" + uri);
    }

    public /* synthetic */ void lambda$prepareImageView$0$PhotoPlaybackView() {
        this.mStatusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), this, "DOUBLE_TAP_TO_PHOTO");
        LockscreenStatisticsPhotoPlaybackPhotoTappedReporter.sendEvent(this.mContext, this.mPhotoType);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleShow() {
        if (this.mImageViews.size() != 0) {
            AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, this.mAlpha);
            alphaAnimation.setDuration(1000);
            alphaAnimation.setInterpolator(this.mContext, 17432587);
            alphaAnimation.setFillAfter(true);
            for (int i = 0; i < this.mImageViews.size(); i++) {
                if (this.mImageViews.get(i) != null) {
                    ViewGroup viewGroup = (ViewGroup) this.mImageViews.get(i).getParent();
                    if (viewGroup != null) {
                        viewGroup.removeView(this.mImageViews.get(i));
                    }
                    addView(this.mImageViews.get(i));
                    this.mImageViews.get(i).startAnimation(alphaAnimation);
                }
            }
            this.mTimeTicker.schedule(600000, 2);
            if (PhotoPlaybackProviderUtils.getPhotoPlaybackMode(this.mContext) == 0) {
                PhotoPlaybackSharedPreferences.setPhotoplaybackSharedPrefOobeShown(this.mContext);
            }
        }
    }

    private void registerPhotoPlaybackObserver() {
        this.mContext.getContentResolver().registerContentObserver(PhotoPlaybackProviderContract$Uris.PHOTO, false, this.mPhotoPlaybackObserver);
    }

    private void unregisterPhotoPlaybackObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mPhotoPlaybackObserver);
    }

    private static float convertDp2Px(int i, Context context) {
        return ((float) i) * context.getResources().getDisplayMetrics().density;
    }

    private Drawable getDemoImage(Uri uri) {
        try {
            Resources resourcesForApplication = this.mContext.getPackageManager().getResourcesForApplication(uri.getAuthority());
            try {
                return resourcesForApplication.getDrawable(resourcesForApplication.getIdentifier(uri.getPath().substring(1, 10), "drawable", uri.getAuthority()), null);
            } catch (Exception e) {
                Log.e("PhotoPlaybackView", "resouce name:" + uri.getPath().substring(1, 10) + " " + e.toString());
                return null;
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e("PhotoPlaybackView", e2.toString());
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void onDeletePhotoPlaybackTimeout() {
        AlphaAnimation alphaAnimation = new AlphaAnimation(this.mAlpha, 0.0f);
        alphaAnimation.setDuration(1000);
        alphaAnimation.setInterpolator(this.mContext, 17432587);
        for (int i = 0; i < this.mImageViews.size(); i++) {
            if (this.mImageViews.get(i) != null) {
                this.mImageViews.get(i).startAnimation(alphaAnimation);
            }
        }
        removeAllViews();
        this.mImageViews.clear();
        resetFFMessage();
        this.mHandler.sendEmptyMessageDelayed(102, 1000);
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }

    public void setFFView(FingerPrintFeedBackView fingerPrintFeedBackView) {
        this.mFingerPrintFeedBackView = fingerPrintFeedBackView;
    }

    public void setMusicInfoAndStickerView(MusicInfoView musicInfoView, StickerView stickerView) {
        this.mMusicInfoView = musicInfoView;
        this.mStickerView = stickerView;
    }

    public void setShouldShowMusicInfoOrSticker(boolean z, boolean z2) {
        this.mShouldShowMusicInfo = z;
        this.mShouldShowSticker = z2;
    }

    private void showDemoMessage() {
        FingerPrintFeedBackView fingerPrintFeedBackView = this.mFingerPrintFeedBackView;
        if (fingerPrintFeedBackView != null) {
            fingerPrintFeedBackView.setPhotoPlayBackDemoMode(true, 3000);
        }
    }

    private void resetFFMessage() {
        FingerPrintFeedBackView fingerPrintFeedBackView = this.mFingerPrintFeedBackView;
        if (fingerPrintFeedBackView != null) {
            fingerPrintFeedBackView.setPhotoPlayBackDemoMode(false, 0);
            this.mFingerPrintFeedBackView.showAlbumDisabledMessage(false);
        }
    }
}
