package com.android.systemui.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.R$styleable;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;

public class ToggleSliderView extends RelativeLayout implements ToggleSlider {
    private final CompoundButton.OnCheckedChangeListener mCheckListener;
    private TextView mLabel;
    private ToggleSlider.Listener mListener;
    private ToggleSliderView mMirror;
    private BrightnessMirrorController mMirrorController;
    private ToggleSlider.OnSliderMaximizedListener mOnSliderMaximizedListener;
    private final SeekBar.OnSeekBarChangeListener mSeekListener;
    private ToggleSeekBar mSlider;
    private CompoundButton mToggle;
    private boolean mTracking;

    public ToggleSliderView(Context context) {
        this(context, null);
    }

    public ToggleSliderView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ToggleSliderView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mCheckListener = new CompoundButton.OnCheckedChangeListener() {
            /* class com.android.systemui.settings.ToggleSliderView.AnonymousClass1 */

            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ToggleSliderView.this.mSlider.setEnabled(!z);
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSlider.Listener listener = ToggleSliderView.this.mListener;
                    ToggleSliderView toggleSliderView = ToggleSliderView.this;
                    listener.onChanged(toggleSliderView, toggleSliderView.mTracking, z, ToggleSliderView.this.mSlider.getProgress(), false);
                }
                if (ToggleSliderView.this.mMirror != null) {
                    ToggleSliderView.this.mMirror.mToggle.setChecked(z);
                }
            }
        };
        this.mSeekListener = new SeekBar.OnSeekBarChangeListener() {
            /* class com.android.systemui.settings.ToggleSliderView.AnonymousClass2 */

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSlider.Listener listener = ToggleSliderView.this.mListener;
                    ToggleSliderView toggleSliderView = ToggleSliderView.this;
                    listener.onChanged(toggleSliderView, toggleSliderView.mTracking, ToggleSliderView.this.mToggle.isChecked(), i, false);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                ToggleSliderView.this.mTracking = true;
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSlider.Listener listener = ToggleSliderView.this.mListener;
                    ToggleSliderView toggleSliderView = ToggleSliderView.this;
                    listener.onChanged(toggleSliderView, toggleSliderView.mTracking, ToggleSliderView.this.mToggle.isChecked(), ToggleSliderView.this.mSlider.getProgress(), false);
                }
                ToggleSliderView.this.mToggle.setChecked(false);
                if (ToggleSliderView.this.mMirrorController != null) {
                    ToggleSliderView.this.mMirrorController.showMirror();
                    ToggleSliderView.this.mMirrorController.setLocation((View) ToggleSliderView.this.getParent());
                }
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                ToggleSliderView.this.mTracking = false;
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSlider.Listener listener = ToggleSliderView.this.mListener;
                    ToggleSliderView toggleSliderView = ToggleSliderView.this;
                    listener.onChanged(toggleSliderView, toggleSliderView.mTracking, ToggleSliderView.this.mToggle.isChecked(), ToggleSliderView.this.mSlider.getProgress(), true);
                }
                if (ToggleSliderView.this.mMirrorController != null) {
                    ToggleSliderView.this.mMirrorController.hideMirror();
                }
                if (ToggleSliderView.this.mOnSliderMaximizedListener != null && ToggleSliderView.this.mSlider.getProgress() == ToggleSliderView.this.mSlider.getMax()) {
                    ToggleSliderView.this.mOnSliderMaximizedListener.onSliderMaximized();
                }
            }
        };
        View.inflate(context, C0010R$layout.status_bar_toggle_slider, this);
        context.getResources();
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.ToggleSliderView, i, 0);
        this.mToggle = (CompoundButton) findViewById(C0007R$id.toggle);
        this.mToggle.setOnCheckedChangeListener(this.mCheckListener);
        this.mSlider = (ToggleSeekBar) findViewById(C0007R$id.slider);
        this.mSlider.setOnSeekBarChangeListener(this.mSeekListener);
        this.mLabel = (TextView) findViewById(C0007R$id.label);
        this.mLabel.setText(obtainStyledAttributes.getString(R$styleable.ToggleSliderView_text));
        this.mSlider.setAccessibilityLabel(getContentDescription().toString());
        obtainStyledAttributes.recycle();
    }

    public void setMirror(ToggleSliderView toggleSliderView) {
        this.mMirror = toggleSliderView;
        ToggleSliderView toggleSliderView2 = this.mMirror;
        if (toggleSliderView2 != null) {
            toggleSliderView2.setChecked(this.mToggle.isChecked());
            this.mMirror.setMax(this.mSlider.getMax());
            this.mMirror.setValue(this.mSlider.getProgress());
        }
    }

    public void setMirrorController(BrightnessMirrorController brightnessMirrorController) {
        this.mMirrorController = brightnessMirrorController;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ToggleSlider.Listener listener = this.mListener;
        if (listener != null) {
            listener.onInit(this);
        }
    }

    public void setEnforcedAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        boolean z = true;
        this.mToggle.setEnabled(enforcedAdmin == null);
        ToggleSeekBar toggleSeekBar = this.mSlider;
        if (enforcedAdmin != null) {
            z = false;
        }
        toggleSeekBar.setEnabled(z);
        this.mSlider.setEnforcedAdmin(enforcedAdmin);
    }

    @Override // com.android.systemui.settings.ToggleSlider
    public void setOnChangedListener(ToggleSlider.Listener listener) {
        this.mListener = listener;
    }

    @Override // com.android.systemui.settings.ToggleSlider
    public void setOnSliderMaximizedListener(ToggleSlider.OnSliderMaximizedListener onSliderMaximizedListener) {
        this.mOnSliderMaximizedListener = onSliderMaximizedListener;
    }

    @Override // com.android.systemui.settings.ToggleSlider
    public void setChecked(boolean z) {
        this.mToggle.setChecked(z);
    }

    @Override // com.android.systemui.settings.ToggleSlider
    public void setMax(int i) {
        this.mSlider.setMax(i);
        ToggleSliderView toggleSliderView = this.mMirror;
        if (toggleSliderView != null) {
            toggleSliderView.setMax(i);
        }
    }

    @Override // com.android.systemui.settings.ToggleSlider
    public void setValue(int i) {
        this.mSlider.setProgress(i);
        ToggleSliderView toggleSliderView = this.mMirror;
        if (toggleSliderView != null) {
            toggleSliderView.setValue(i);
        }
    }

    @Override // com.android.systemui.settings.ToggleSlider
    public int getValue() {
        return this.mSlider.getProgress();
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.mMirror != null) {
            MotionEvent copy = motionEvent.copy();
            this.mMirror.dispatchTouchEvent(copy);
            copy.recycle();
        }
        return super.dispatchTouchEvent(motionEvent);
    }
}
