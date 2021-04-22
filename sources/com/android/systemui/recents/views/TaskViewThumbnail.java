package com.android.systemui.recents.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.TaskSnapshotChangedEvent;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import java.io.PrintWriter;

public class TaskViewThumbnail extends View {
    private static final ColorMatrix TMP_BRIGHTNESS_COLOR_MATRIX = new ColorMatrix();
    private static final ColorMatrix TMP_FILTER_COLOR_MATRIX = new ColorMatrix();
    protected Paint mBgFillPaint;
    protected BitmapShader mBitmapShader;
    protected int mCornerRadius;
    @ViewDebug.ExportedProperty(category = "recents")
    private float mDimAlpha;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mDisabledInSafeMode;
    private int mDisplayOrientation;
    private Rect mDisplayRect;
    private Paint mDrawPaint;
    private float mFullscreenThumbnailScale;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mInvisible;
    private LightingColorFilter mLightingColorFilter;
    protected Paint mLockedPaint;
    private Matrix mMatrix;
    private boolean mOverlayHeaderOnThumbnailActionBar;
    private boolean mSizeToFit;
    private Task mTask;
    private View mTaskBar;
    @ViewDebug.ExportedProperty(category = "recents")
    protected Rect mTaskViewRect;
    private ThumbnailData mThumbnailData;
    @ViewDebug.ExportedProperty(category = "recents")
    protected Rect mThumbnailRect;
    @ViewDebug.ExportedProperty(category = "recents")
    protected float mThumbnailScale;
    private int mTitleBarHeight;
    protected boolean mUserLocked;

    public TaskViewThumbnail(Context context) {
        this(context, null);
    }

    public TaskViewThumbnail(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mDisplayOrientation = 0;
        this.mDisplayRect = new Rect();
        this.mTaskViewRect = new Rect();
        this.mThumbnailRect = new Rect();
        this.mFullscreenThumbnailScale = 1.0f;
        this.mSizeToFit = false;
        this.mOverlayHeaderOnThumbnailActionBar = true;
        this.mMatrix = new Matrix();
        this.mDrawPaint = new Paint();
        this.mLockedPaint = new Paint();
        this.mBgFillPaint = new Paint();
        this.mUserLocked = false;
        this.mLightingColorFilter = new LightingColorFilter(-1, 0);
        this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
        this.mDrawPaint.setFilterBitmap(true);
        this.mDrawPaint.setAntiAlias(true);
        Resources resources = getResources();
        this.mCornerRadius = resources.getDimensionPixelSize(2131166261);
        this.mBgFillPaint.setColor(-1);
        this.mLockedPaint.setColor(-1);
        this.mTitleBarHeight = resources.getDimensionPixelSize(2131166224);
    }

    public void onTaskViewSizeChanged(int i, int i2) {
        if (this.mTaskViewRect.width() != i || this.mTaskViewRect.height() != i2) {
            this.mTaskViewRect.set(0, 0, i, i2);
            setLeftTopRightBottom(0, 0, i, i2);
            updateThumbnailMatrix();
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (!this.mInvisible) {
            int width = this.mTaskViewRect.width();
            int height = this.mTaskViewRect.height();
            int min = Math.min(width, (int) (((float) this.mThumbnailRect.width()) * this.mThumbnailScale));
            int min2 = Math.min(height, (int) (((float) this.mThumbnailRect.height()) * this.mThumbnailScale));
            if (this.mUserLocked) {
                int i = this.mCornerRadius;
                canvas.drawRoundRect(0.0f, 0.0f, (float) width, (float) height, (float) i, (float) i, this.mLockedPaint);
            } else if (this.mBitmapShader == null || min <= 0 || min2 <= 0) {
                int i2 = this.mCornerRadius;
                canvas.drawRoundRect(0.0f, 0.0f, (float) width, (float) height, (float) i2, (float) i2, this.mBgFillPaint);
            } else {
                View view = this.mTaskBar;
                int height2 = (view == null || !this.mOverlayHeaderOnThumbnailActionBar) ? 0 : view.getHeight() - this.mCornerRadius;
                if (min < width) {
                    int i3 = this.mCornerRadius;
                    canvas.drawRoundRect((float) Math.max(0, min - this.mCornerRadius), (float) height2, (float) width, (float) height, (float) i3, (float) i3, this.mBgFillPaint);
                }
                if (min2 < height) {
                    int i4 = this.mCornerRadius;
                    canvas.drawRoundRect(0.0f, (float) Math.max(height2, min2 - this.mCornerRadius), (float) width, (float) height, (float) i4, (float) i4, this.mBgFillPaint);
                }
                float f = (float) min;
                float f2 = (float) min2;
                int i5 = this.mCornerRadius;
                canvas.drawRoundRect(0.0f, (float) height2, f, f2, (float) i5, (float) i5, this.mDrawPaint);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setThumbnail(ThumbnailData thumbnailData) {
        Bitmap bitmap;
        if (thumbnailData == null || (bitmap = thumbnailData.thumbnail) == null) {
            this.mBitmapShader = null;
            this.mDrawPaint.setShader(null);
            this.mThumbnailRect.setEmpty();
            this.mThumbnailData = null;
            return;
        }
        bitmap.prepareToDraw();
        this.mFullscreenThumbnailScale = thumbnailData.scale;
        Shader.TileMode tileMode = Shader.TileMode.CLAMP;
        this.mBitmapShader = new BitmapShader(bitmap, tileMode, tileMode);
        this.mDrawPaint.setShader(this.mBitmapShader);
        Rect rect = this.mThumbnailRect;
        int width = bitmap.getWidth();
        Rect rect2 = thumbnailData.insets;
        int i = (width - rect2.left) - rect2.right;
        int height = bitmap.getHeight();
        Rect rect3 = thumbnailData.insets;
        rect.set(0, 0, i, (height - rect3.top) - rect3.bottom);
        this.mThumbnailData = thumbnailData;
        updateThumbnailMatrix();
        updateThumbnailPaintFilter();
    }

    /* access modifiers changed from: package-private */
    public void updateThumbnailPaintFilter() {
        if (!this.mInvisible) {
            int i = (int) ((1.0f - this.mDimAlpha) * 255.0f);
            if (this.mBitmapShader == null) {
                this.mDrawPaint.setColorFilter(null);
                this.mDrawPaint.setColor(Color.argb(255, i, i, i));
            } else if (this.mDisabledInSafeMode) {
                TMP_FILTER_COLOR_MATRIX.setSaturation(0.0f);
                float f = 1.0f - this.mDimAlpha;
                float[] array = TMP_BRIGHTNESS_COLOR_MATRIX.getArray();
                array[0] = f;
                array[6] = f;
                array[12] = f;
                float f2 = this.mDimAlpha;
                array[4] = f2 * 255.0f;
                array[9] = f2 * 255.0f;
                array[14] = f2 * 255.0f;
                TMP_FILTER_COLOR_MATRIX.preConcat(TMP_BRIGHTNESS_COLOR_MATRIX);
                ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(TMP_FILTER_COLOR_MATRIX);
                this.mDrawPaint.setColorFilter(colorMatrixColorFilter);
                this.mBgFillPaint.setColorFilter(colorMatrixColorFilter);
                this.mLockedPaint.setColorFilter(colorMatrixColorFilter);
            } else {
                this.mLightingColorFilter.setColorMultiply(Color.argb(255, i, i, i));
                this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
                this.mDrawPaint.setColor(-1);
                this.mBgFillPaint.setColorFilter(this.mLightingColorFilter);
                this.mLockedPaint.setColorFilter(this.mLightingColorFilter);
            }
            if (!this.mInvisible) {
                invalidate();
            }
        }
    }

    public void updateThumbnailMatrix() {
        this.mThumbnailScale = 1.0f;
        if (!(this.mBitmapShader == null || this.mThumbnailData == null)) {
            if (this.mTaskViewRect.isEmpty()) {
                this.mThumbnailScale = 0.0f;
            } else if (!this.mSizeToFit) {
                float f = 1.0f / this.mFullscreenThumbnailScale;
                if (this.mDisplayOrientation != 1) {
                    this.mThumbnailScale = f;
                } else if (this.mThumbnailData.orientation == 1) {
                    this.mThumbnailScale = ((float) this.mTaskViewRect.width()) / ((float) this.mThumbnailRect.width());
                } else {
                    this.mThumbnailScale = f * (((float) this.mTaskViewRect.width()) / ((float) this.mDisplayRect.width()));
                }
            } else if (((float) this.mTaskViewRect.width()) / ((float) (this.mTaskViewRect.height() - this.mTitleBarHeight)) > ((float) this.mThumbnailRect.width()) / ((float) this.mThumbnailRect.height())) {
                this.mThumbnailScale = ((float) this.mTaskViewRect.width()) / ((float) this.mThumbnailRect.width());
            } else {
                this.mThumbnailScale = ((float) (this.mTaskViewRect.height() - this.mTitleBarHeight)) / ((float) this.mThumbnailRect.height());
            }
            Matrix matrix = this.mMatrix;
            Rect rect = this.mThumbnailData.insets;
            float f2 = this.mFullscreenThumbnailScale;
            matrix.setTranslate(((float) (-rect.left)) * f2, ((float) (-rect.top)) * f2);
            Matrix matrix2 = this.mMatrix;
            float f3 = this.mThumbnailScale;
            matrix2.postScale(f3, f3);
            this.mBitmapShader.setLocalMatrix(this.mMatrix);
        }
        if (!this.mInvisible) {
            invalidate();
        }
    }

    public void setSizeToFit(boolean z) {
        this.mSizeToFit = z;
    }

    public void setOverlayHeaderOnThumbnailActionBar(boolean z) {
        this.mOverlayHeaderOnThumbnailActionBar = z;
    }

    /* access modifiers changed from: package-private */
    public void updateClipToTaskBar(View view) {
        this.mTaskBar = view;
        invalidate();
    }

    /* access modifiers changed from: package-private */
    public void updateThumbnailVisibility(int i) {
        boolean z = this.mTaskBar != null && getHeight() - i <= this.mTaskBar.getHeight();
        if (z != this.mInvisible) {
            this.mInvisible = z;
            if (!this.mInvisible) {
                updateThumbnailPaintFilter();
            }
        }
    }

    public void setDimAlpha(float f) {
        this.mDimAlpha = f;
        updateThumbnailPaintFilter();
    }

    /* access modifiers changed from: protected */
    public Paint getDrawPaint() {
        if (this.mUserLocked) {
            return this.mLockedPaint;
        }
        return this.mDrawPaint;
    }

    /* access modifiers changed from: package-private */
    public void bindToTask(Task task, boolean z, int i, Rect rect) {
        this.mTask = task;
        this.mDisabledInSafeMode = z;
        this.mDisplayOrientation = i;
        this.mDisplayRect.set(rect);
        int i2 = task.colorBackground;
        if (i2 != 0) {
            this.mBgFillPaint.setColor(i2);
        }
        int i3 = task.colorPrimary;
        if (i3 != 0) {
            this.mLockedPaint.setColor(i3);
        }
        this.mUserLocked = task.isLocked;
        EventBus.getDefault().register(this);
    }

    /* access modifiers changed from: package-private */
    public void onTaskDataLoaded(ThumbnailData thumbnailData) {
        setThumbnail(thumbnailData);
    }

    /* access modifiers changed from: package-private */
    public void unbindFromTask() {
        this.mTask = null;
        setThumbnail(null);
        EventBus.getDefault().unregister(this);
    }

    public final void onBusEvent(TaskSnapshotChangedEvent taskSnapshotChangedEvent) {
        ThumbnailData thumbnailData;
        Task task = this.mTask;
        if (task != null && taskSnapshotChangedEvent.taskId == task.key.id && (thumbnailData = taskSnapshotChangedEvent.thumbnailData) != null && thumbnailData.thumbnail != null) {
            setThumbnail(thumbnailData);
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("TaskViewThumbnail");
        printWriter.print(" mTaskViewRect=");
        printWriter.print(Utilities.dumpRect(this.mTaskViewRect));
        printWriter.print(" mThumbnailRect=");
        printWriter.print(Utilities.dumpRect(this.mThumbnailRect));
        printWriter.print(" mThumbnailScale=");
        printWriter.print(this.mThumbnailScale);
        printWriter.print(" mDimAlpha=");
        printWriter.print(this.mDimAlpha);
        printWriter.println();
    }
}
