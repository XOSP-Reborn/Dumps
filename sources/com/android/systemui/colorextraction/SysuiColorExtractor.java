package com.android.systemui.colorextraction;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.types.ExtractionType;
import com.android.internal.colorextraction.types.Tonal;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dumpable;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class SysuiColorExtractor extends ColorExtractor implements Dumpable, ConfigurationController.ConfigurationListener {
    private final ColorExtractor.GradientColors mBackdropColors;
    private boolean mHasMediaArtwork;
    private boolean mIsStyleCoverViewSelectedAndClosed;
    private final ColorExtractor.GradientColors mNeutralColorsLock;
    private ColorExtractor.GradientColors mSomcLockColors;
    private final Tonal mTonal;

    public SysuiColorExtractor(Context context, ConfigurationController configurationController) {
        this(context, new Tonal(context), configurationController, (WallpaperManager) context.getSystemService(WallpaperManager.class), false);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.systemui.colorextraction.SysuiColorExtractor */
    /* JADX WARN: Multi-variable type inference failed */
    @VisibleForTesting
    public SysuiColorExtractor(Context context, ExtractionType extractionType, ConfigurationController configurationController, WallpaperManager wallpaperManager, boolean z) {
        super(context, extractionType, z, wallpaperManager);
        this.mIsStyleCoverViewSelectedAndClosed = false;
        this.mTonal = extractionType instanceof Tonal ? (Tonal) extractionType : new Tonal(context);
        this.mNeutralColorsLock = new ColorExtractor.GradientColors();
        configurationController.addCallback(this);
        this.mBackdropColors = new ColorExtractor.GradientColors();
        this.mBackdropColors.setMainColor(-16777216);
        if (wallpaperManager != null) {
            wallpaperManager.removeOnColorsChangedListener(this);
            wallpaperManager.addOnColorsChangedListener(this, null, -1);
        }
    }

    /* access modifiers changed from: protected */
    public void extractWallpaperColors() {
        SysuiColorExtractor.super.extractWallpaperColors();
        Tonal tonal = this.mTonal;
        if (tonal != null) {
            WallpaperColors wallpaperColors = ((ColorExtractor) this).mLockColors;
            if (wallpaperColors == null) {
                wallpaperColors = ((ColorExtractor) this).mSystemColors;
            }
            tonal.applyFallback(wallpaperColors, this.mNeutralColorsLock);
        }
    }

    public void onColorsChanged(WallpaperColors wallpaperColors, int i, int i2) {
        if (i2 == KeyguardUpdateMonitor.getCurrentUser()) {
            if ((i & 2) != 0) {
                this.mTonal.applyFallback(wallpaperColors, this.mNeutralColorsLock);
            }
            SysuiColorExtractor.super.onColorsChanged(wallpaperColors, i);
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onUiModeChanged() {
        extractWallpaperColors();
        triggerColorsChanged(3);
    }

    public ColorExtractor.GradientColors getColors(int i, int i2) {
        ColorExtractor.GradientColors gradientColors;
        int i3 = i & 2;
        if (i3 != 0 && (gradientColors = this.mSomcLockColors) != null) {
            return gradientColors;
        }
        if (this.mHasMediaArtwork && i3 != 0) {
            return this.mBackdropColors;
        }
        if (this.mIsStyleCoverViewSelectedAndClosed) {
            return this.mBackdropColors;
        }
        return SysuiColorExtractor.super.getColors(i, i2);
    }

    public ColorExtractor.GradientColors getNeutralColors() {
        ColorExtractor.GradientColors gradientColors = this.mSomcLockColors;
        if (gradientColors != null) {
            return gradientColors;
        }
        if (this.mIsStyleCoverViewSelectedAndClosed) {
            return this.mBackdropColors;
        }
        return this.mHasMediaArtwork ? this.mBackdropColors : this.mNeutralColorsLock;
    }

    public void setHasMediaArtwork(boolean z) {
        if (this.mHasMediaArtwork != z) {
            this.mHasMediaArtwork = z;
            triggerColorsChanged(2);
        }
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("SysuiColorExtractor:");
        printWriter.println("  Current wallpaper colors:");
        printWriter.println("    system: " + ((ColorExtractor) this).mSystemColors);
        printWriter.println("    lock: " + ((ColorExtractor) this).mLockColors);
        printWriter.println("  Gradients:");
        printWriter.println("    system: " + Arrays.toString((ColorExtractor.GradientColors[]) ((ColorExtractor) this).mGradientColors.get(1)));
        printWriter.println("    lock: " + Arrays.toString((ColorExtractor.GradientColors[]) ((ColorExtractor) this).mGradientColors.get(2)));
        printWriter.println("  Neutral colors: " + this.mNeutralColorsLock);
        printWriter.println("  Has media backdrop: " + this.mHasMediaArtwork);
    }

    public void setSomcLockColors(ColorExtractor.GradientColors gradientColors) {
        this.mSomcLockColors = gradientColors;
        triggerColorsChanged(2);
    }

    public void updateLockColors(boolean z) {
        this.mIsStyleCoverViewSelectedAndClosed = z;
        triggerColorsChanged(2);
    }
}
