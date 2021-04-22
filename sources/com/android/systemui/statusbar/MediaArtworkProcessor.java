package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.MathUtils;
import androidx.palette.graphics.Palette;
import com.android.internal.graphics.ColorUtils;
import com.android.systemui.statusbar.notification.MediaNotificationProcessor;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: MediaArtworkProcessor.kt */
public final class MediaArtworkProcessor {
    private Bitmap mArtworkCache;
    private final Point mTmpSize = new Point();

    public final Bitmap processArtwork(Context context, Bitmap bitmap) {
        Intrinsics.checkParameterIsNotNull(context, "context");
        Intrinsics.checkParameterIsNotNull(bitmap, "artwork");
        Bitmap bitmap2 = this.mArtworkCache;
        if (bitmap2 == null) {
            context.getDisplay().getSize(this.mTmpSize);
            RenderScript create = RenderScript.create(context);
            Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            Point point = this.mTmpSize;
            MathUtils.fitRect(rect, Math.max(point.x / 6, point.y / 6));
            Bitmap createScaledBitmap = Bitmap.createScaledBitmap(bitmap, rect.width(), rect.height(), true);
            Intrinsics.checkExpressionValueIsNotNull(createScaledBitmap, "inBitmap");
            Bitmap.Config config = createScaledBitmap.getConfig();
            Bitmap.Config config2 = Bitmap.Config.ARGB_8888;
            if (config != config2) {
                Bitmap copy = createScaledBitmap.copy(config2, false);
                createScaledBitmap.recycle();
                createScaledBitmap = copy;
            }
            Allocation createFromBitmap = Allocation.createFromBitmap(create, createScaledBitmap, Allocation.MipmapControl.MIPMAP_NONE, 2);
            Intrinsics.checkExpressionValueIsNotNull(createScaledBitmap, "inBitmap");
            Bitmap createBitmap = Bitmap.createBitmap(createScaledBitmap.getWidth(), createScaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Allocation createFromBitmap2 = Allocation.createFromBitmap(create, createBitmap);
            ScriptIntrinsicBlur create2 = ScriptIntrinsicBlur.create(create, Element.U8_4(create));
            create2.setRadius(25.0f);
            create2.setInput(createFromBitmap);
            create2.forEach(createFromBitmap2);
            createFromBitmap2.copyTo(createBitmap);
            Palette.Swatch findBackgroundSwatch = MediaNotificationProcessor.findBackgroundSwatch(bitmap);
            createFromBitmap.destroy();
            createFromBitmap2.destroy();
            createScaledBitmap.recycle();
            create2.destroy();
            Canvas canvas = new Canvas(createBitmap);
            Intrinsics.checkExpressionValueIsNotNull(findBackgroundSwatch, "swatch");
            canvas.drawColor(ColorUtils.setAlphaComponent(findBackgroundSwatch.getRgb(), 178));
            Intrinsics.checkExpressionValueIsNotNull(createBitmap, "outBitmap");
            return createBitmap;
        } else if (bitmap2 != null) {
            return bitmap2;
        } else {
            Intrinsics.throwNpe();
            throw null;
        }
    }

    public final void clearCache() {
        Bitmap bitmap = this.mArtworkCache;
        if (bitmap != null) {
            bitmap.recycle();
        }
        this.mArtworkCache = null;
    }
}
