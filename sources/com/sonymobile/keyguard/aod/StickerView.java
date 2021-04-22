package com.sonymobile.keyguard.aod;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController;

public class StickerView extends ImageView {
    private static final String TAG = "StickerView";
    private Context mContext;
    private String mStickerUri = ((LockscreenAmbientDisplayController) Dependency.get(LockscreenAmbientDisplayController.class)).getStickerUri();

    public StickerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    private boolean hasSticker() {
        String str = this.mStickerUri;
        return str != null && !str.equals("");
    }

    public void setDozing(boolean z) {
        if (z) {
            refresh();
        }
    }

    private void refresh() {
        Drawable presetSticker;
        this.mStickerUri = ((LockscreenAmbientDisplayController) Dependency.get(LockscreenAmbientDisplayController.class)).getStickerUri();
        setImageDrawable(null);
        if (hasSticker()) {
            Uri parse = Uri.parse(this.mStickerUri);
            if (parse.getScheme().equals("file")) {
                setImageURI(parse);
            } else if (parse.getScheme().equals("resource") && (presetSticker = getPresetSticker(parse)) != null) {
                setImageDrawable(presetSticker);
            }
        }
    }

    private Drawable getPresetSticker(Uri uri) {
        try {
            Resources resourcesForApplicationAsUser = this.mContext.getPackageManager().getResourcesForApplicationAsUser(uri.getAuthority(), KeyguardUpdateMonitor.getCurrentUser());
            try {
                return resourcesForApplicationAsUser.getDrawable(resourcesForApplicationAsUser.getIdentifier(uri.getPath().substring(1), "drawable", uri.getAuthority()), null);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                return null;
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e(TAG, e2.toString());
            return null;
        }
    }
}
