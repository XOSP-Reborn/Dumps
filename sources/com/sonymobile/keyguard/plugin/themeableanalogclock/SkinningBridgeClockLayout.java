package com.sonymobile.keyguard.plugin.themeableanalogclock;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.widget.FrameLayout;
import com.android.systemui.C0007R$id;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.runtimeskinning.SkinningBridge;

public class SkinningBridgeClockLayout extends FrameLayout implements ClockPlugin, TextureView.SurfaceTextureListener {
    private String mPlace = null;
    private TextureView mTextureView = null;

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setNextAlarmText(String str) {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void startClockTicking() {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void stopClockTicking() {
    }

    public SkinningBridgeClockLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setPlace(String str) {
        this.mPlace = str;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        TextureView textureView = (TextureView) findViewById(C0007R$id.somc_customizable_texture_view);
        if (textureView != null) {
            this.mTextureView = textureView;
            this.mTextureView.setSurfaceTextureListener(this);
            this.mTextureView.setOpaque(false);
            return;
        }
        throw new RuntimeException("Layout must contain a TextureView");
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        SkinningBridge.onSurfaceAvailable(surfaceTexture);
        String str = this.mPlace;
        if (str != null) {
            SkinningBridge.onSurfaceChanged(surfaceTexture, str, this.mTextureView.getLeft(), this.mTextureView.getTop(), this.mTextureView.getRight(), this.mTextureView.getBottom());
        }
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        SkinningBridge.onSurfaceDestroyed(surfaceTexture);
        return true;
    }
}
