package com.android.systemui.pip.phone;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.Interpolators;
import com.android.systemui.shared.system.WindowManagerWrapper;

public class PipDismissViewController {
    private Context mContext;
    private View mDismissView;
    private int[] mLoc = new int[2];
    private int mTargetSlop;
    private View mTargetView;
    private Vibrator mVibe;
    private WindowManager mWindowManager;
    private Point mWindowSize;

    public PipDismissViewController(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mVibe = (Vibrator) context.getSystemService("vibrator");
    }

    public void createDismissTarget() {
        if (this.mDismissView == null) {
            Rect rect = new Rect();
            WindowManagerWrapper.getInstance().getStableInsets(rect);
            this.mWindowSize = new Point();
            this.mWindowManager.getDefaultDisplay().getRealSize(this.mWindowSize);
            int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.pip_dismiss_gradient_height);
            int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.pip_dismiss_text_bottom_margin);
            this.mTargetSlop = this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.bubble_dismiss_slop);
            this.mDismissView = LayoutInflater.from(this.mContext).inflate(C0010R$layout.pip_dismiss_view, (ViewGroup) null);
            this.mDismissView.setSystemUiVisibility(256);
            this.mDismissView.forceHasOverlappingRendering(false);
            Drawable drawable = this.mContext.getResources().getDrawable(C0006R$drawable.pip_dismiss_scrim);
            drawable.setAlpha(216);
            this.mDismissView.setBackground(drawable);
            this.mTargetView = this.mDismissView.findViewById(C0007R$id.pip_dismiss_text);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mTargetView.getLayoutParams();
            layoutParams.bottomMargin = rect.bottom + dimensionPixelSize2;
            this.mTargetView.setLayoutParams(layoutParams);
            WindowManager.LayoutParams layoutParams2 = new WindowManager.LayoutParams(-1, dimensionPixelSize, 0, this.mWindowSize.y - dimensionPixelSize, 2024, 280, -3);
            layoutParams2.setTitle("pip-dismiss-overlay");
            layoutParams2.privateFlags |= 16;
            layoutParams2.gravity = 49;
            this.mWindowManager.addView(this.mDismissView, layoutParams2);
        }
        this.mDismissView.animate().cancel();
    }

    public void showDismissTarget() {
        this.mDismissView.animate().alpha(1.0f).setInterpolator(Interpolators.LINEAR).setStartDelay(100).setDuration(350).start();
    }

    public void destroyDismissTarget() {
        View view = this.mDismissView;
        if (view != null) {
            view.animate().alpha(0.0f).setInterpolator(Interpolators.LINEAR).setStartDelay(0).setDuration(225).withEndAction(new Runnable() {
                /* class com.android.systemui.pip.phone.PipDismissViewController.AnonymousClass1 */

                public void run() {
                    PipDismissViewController.this.mWindowManager.removeViewImmediate(PipDismissViewController.this.mDismissView);
                    PipDismissViewController.this.mDismissView = null;
                }
            }).start();
        }
    }
}
