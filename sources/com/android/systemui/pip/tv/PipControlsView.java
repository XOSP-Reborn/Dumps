package com.android.systemui.pip.tv;

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.pip.tv.PipManager;
import java.util.ArrayList;
import java.util.List;

public class PipControlsView extends LinearLayout {
    private static final String TAG = "PipControlsView";
    private PipControlButtonView mCloseButtonView;
    private List<RemoteAction> mCustomActions;
    private ArrayList<PipControlButtonView> mCustomButtonViews;
    private final View.OnFocusChangeListener mFocusChangeListener;
    private PipControlButtonView mFocusedChild;
    private PipControlButtonView mFullButtonView;
    private final Handler mHandler;
    private final LayoutInflater mLayoutInflater;
    private Listener mListener;
    private MediaController mMediaController;
    private MediaController.Callback mMediaControllerCallback;
    private final PipManager mPipManager;
    private final PipManager.MediaListener mPipMediaListener;
    private PipControlButtonView mPlayPauseButtonView;

    public interface Listener {
        void onClosed();
    }

    public PipControlsView(Context context) {
        this(context, null, 0, 0);
    }

    public PipControlsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public PipControlsView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public PipControlsView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mPipManager = PipManager.getInstance();
        this.mCustomButtonViews = new ArrayList<>();
        this.mCustomActions = new ArrayList();
        this.mMediaControllerCallback = new MediaController.Callback() {
            /* class com.android.systemui.pip.tv.PipControlsView.AnonymousClass1 */

            public void onPlaybackStateChanged(PlaybackState playbackState) {
                PipControlsView.this.updateUserActions();
            }
        };
        this.mPipMediaListener = new PipManager.MediaListener() {
            /* class com.android.systemui.pip.tv.PipControlsView.AnonymousClass2 */

            @Override // com.android.systemui.pip.tv.PipManager.MediaListener
            public void onMediaControllerChanged() {
                PipControlsView.this.updateMediaController();
            }
        };
        this.mFocusChangeListener = new View.OnFocusChangeListener() {
            /* class com.android.systemui.pip.tv.PipControlsView.AnonymousClass3 */

            public void onFocusChange(View view, boolean z) {
                if (z) {
                    PipControlsView.this.mFocusedChild = (PipControlButtonView) view;
                } else if (PipControlsView.this.mFocusedChild == view) {
                    PipControlsView.this.mFocusedChild = null;
                }
            }
        };
        this.mLayoutInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mLayoutInflater.inflate(C0010R$layout.tv_pip_controls, this);
        this.mHandler = new Handler();
        setOrientation(0);
        setGravity(49);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mFullButtonView = (PipControlButtonView) findViewById(C0007R$id.full_button);
        this.mFullButtonView.setOnFocusChangeListener(this.mFocusChangeListener);
        this.mFullButtonView.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.pip.tv.PipControlsView.AnonymousClass4 */

            public void onClick(View view) {
                PipControlsView.this.mPipManager.movePipToFullscreen();
            }
        });
        this.mCloseButtonView = (PipControlButtonView) findViewById(C0007R$id.close_button);
        this.mCloseButtonView.setOnFocusChangeListener(this.mFocusChangeListener);
        this.mCloseButtonView.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.pip.tv.PipControlsView.AnonymousClass5 */

            public void onClick(View view) {
                PipControlsView.this.mPipManager.closePip();
                if (PipControlsView.this.mListener != null) {
                    PipControlsView.this.mListener.onClosed();
                }
            }
        });
        this.mPlayPauseButtonView = (PipControlButtonView) findViewById(C0007R$id.play_pause_button);
        this.mPlayPauseButtonView.setOnFocusChangeListener(this.mFocusChangeListener);
        this.mPlayPauseButtonView.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.pip.tv.PipControlsView.AnonymousClass6 */

            public void onClick(View view) {
                if (PipControlsView.this.mMediaController != null && PipControlsView.this.mMediaController.getPlaybackState() != null) {
                    PipControlsView.this.mMediaController.getPlaybackState().getActions();
                    PipControlsView.this.mMediaController.getPlaybackState().getState();
                    if (PipControlsView.this.mPipManager.getPlaybackState() == 1) {
                        PipControlsView.this.mMediaController.getTransportControls().play();
                    } else if (PipControlsView.this.mPipManager.getPlaybackState() == 0) {
                        PipControlsView.this.mMediaController.getTransportControls().pause();
                    }
                }
            }
        });
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateMediaController();
        this.mPipManager.addMediaListener(this.mPipMediaListener);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mPipManager.removeMediaListener(this.mPipMediaListener);
        MediaController mediaController = this.mMediaController;
        if (mediaController != null) {
            mediaController.unregisterCallback(this.mMediaControllerCallback);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateMediaController() {
        MediaController mediaController = this.mPipManager.getMediaController();
        MediaController mediaController2 = this.mMediaController;
        if (mediaController2 != mediaController) {
            if (mediaController2 != null) {
                mediaController2.unregisterCallback(this.mMediaControllerCallback);
            }
            this.mMediaController = mediaController;
            MediaController mediaController3 = this.mMediaController;
            if (mediaController3 != null) {
                mediaController3.registerCallback(this.mMediaControllerCallback);
            }
            updateUserActions();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateUserActions() {
        int i = 0;
        if (!this.mCustomActions.isEmpty()) {
            while (this.mCustomButtonViews.size() < this.mCustomActions.size()) {
                PipControlButtonView pipControlButtonView = (PipControlButtonView) this.mLayoutInflater.inflate(C0010R$layout.tv_pip_custom_control, (ViewGroup) this, false);
                addView(pipControlButtonView);
                this.mCustomButtonViews.add(pipControlButtonView);
            }
            int i2 = 0;
            while (i2 < this.mCustomButtonViews.size()) {
                this.mCustomButtonViews.get(i2).setVisibility(i2 < this.mCustomActions.size() ? 0 : 8);
                i2++;
            }
            while (i < this.mCustomActions.size()) {
                RemoteAction remoteAction = this.mCustomActions.get(i);
                PipControlButtonView pipControlButtonView2 = this.mCustomButtonViews.get(i);
                remoteAction.getIcon().loadDrawableAsync(getContext(), new Icon.OnDrawableLoadedListener() {
                    /* class com.android.systemui.pip.tv.$$Lambda$PipControlsView$ZwQyQkGsN0bsRufZ6MVGwaQtJA8 */

                    public final void onDrawableLoaded(Drawable drawable) {
                        PipControlsView.lambda$updateUserActions$0(PipControlButtonView.this, drawable);
                    }
                }, this.mHandler);
                pipControlButtonView2.setText(remoteAction.getContentDescription());
                if (remoteAction.isEnabled()) {
                    pipControlButtonView2.setOnClickListener(new View.OnClickListener(remoteAction) {
                        /* class com.android.systemui.pip.tv.$$Lambda$PipControlsView$HMvSXxIxW1kpM7rGrVPgyskxY */
                        private final /* synthetic */ RemoteAction f$0;

                        {
                            this.f$0 = r1;
                        }

                        public final void onClick(View view) {
                            PipControlsView.lambda$updateUserActions$1(this.f$0, view);
                        }
                    });
                }
                pipControlButtonView2.setEnabled(remoteAction.isEnabled());
                pipControlButtonView2.setAlpha(remoteAction.isEnabled() ? 1.0f : 0.54f);
                i++;
            }
            this.mPlayPauseButtonView.setVisibility(8);
            return;
        }
        int playbackState = this.mPipManager.getPlaybackState();
        if (playbackState == 2) {
            this.mPlayPauseButtonView.setVisibility(8);
        } else {
            this.mPlayPauseButtonView.setVisibility(0);
            if (playbackState == 0) {
                this.mPlayPauseButtonView.setImageResource(C0006R$drawable.ic_pause_white);
                this.mPlayPauseButtonView.setText(C0014R$string.pip_pause);
            } else {
                this.mPlayPauseButtonView.setImageResource(C0006R$drawable.ic_play_arrow_white);
                this.mPlayPauseButtonView.setText(C0014R$string.pip_play);
            }
        }
        while (i < this.mCustomButtonViews.size()) {
            this.mCustomButtonViews.get(i).setVisibility(8);
            i++;
        }
    }

    static /* synthetic */ void lambda$updateUserActions$0(PipControlButtonView pipControlButtonView, Drawable drawable) {
        drawable.setTint(-1);
        pipControlButtonView.setImageDrawable(drawable);
    }

    static /* synthetic */ void lambda$updateUserActions$1(RemoteAction remoteAction, View view) {
        try {
            remoteAction.getActionIntent().send();
        } catch (PendingIntent.CanceledException e) {
            Log.w(TAG, "Failed to send action", e);
        }
    }

    public void setActions(List<RemoteAction> list) {
        this.mCustomActions.clear();
        this.mCustomActions.addAll(list);
        updateUserActions();
    }
}
