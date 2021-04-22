package com.sonymobile.keyguard.aod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadata;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.Dependency;
import com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController;

public class MusicInfoView extends LinearLayout {
    private Context mContext;

    public MusicInfoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    public boolean updateMediaMetaData(MediaMetadata mediaMetadata) {
        removeAllViews();
        if (mediaMetadata == null || !((LockscreenAmbientDisplayController) Dependency.get(LockscreenAmbientDisplayController.class)).shouldShowMusicInfo()) {
            return false;
        }
        BitmapDrawable bitmapDrawable = null;
        Bitmap bitmap = mediaMetadata.getBitmap("android.media.metadata.ART");
        if (bitmap == null) {
            bitmap = mediaMetadata.getBitmap("android.media.metadata.ALBUM_ART");
        }
        if (bitmap != null) {
            bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        }
        int dimensionPixelSize = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_music_textsize);
        int dimensionPixelSize2 = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_music_text_margin);
        int dimensionPixelSize3 = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_music_artwork_size);
        int dimensionPixelSize4 = getResources().getDimensionPixelSize(C0005R$dimen.somc_aod_music_artwork_margin);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.setMargins(0, dimensionPixelSize2, 0, dimensionPixelSize2);
        layoutParams.gravity = 1;
        String string = mediaMetadata.getString("android.media.metadata.ARTIST");
        if (string == null) {
            string = mediaMetadata.getString("android.media.metadata.ALBUM_ARTIST");
        }
        String string2 = mediaMetadata.getString("android.media.metadata.TITLE");
        TextView textView = new TextView(this.mContext);
        textView.setText(string2);
        textView.setTextColor(-1);
        float f = (float) dimensionPixelSize;
        textView.setTextSize(0, f);
        addView(textView, layoutParams);
        TextView textView2 = new TextView(this.mContext);
        textView2.setText(string);
        textView2.setTextColor(-1);
        textView2.setTextSize(0, f);
        addView(textView2, layoutParams);
        ImageView imageView = new ImageView(this.mContext);
        if (bitmapDrawable != null) {
            imageView.setImageDrawable(bitmapDrawable);
        }
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dimensionPixelSize3, dimensionPixelSize3);
        layoutParams2.setMargins(0, dimensionPixelSize4, 0, 0);
        layoutParams2.gravity = 1;
        addView(imageView, layoutParams2);
        return true;
    }
}
