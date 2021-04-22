package com.google.android.material.textfield;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.DrawableUtils;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.widget.TextViewCompat;
import androidx.customview.view.AbsSavedState;
import com.google.android.material.R$attr;
import com.google.android.material.R$color;
import com.google.android.material.R$dimen;
import com.google.android.material.R$drawable;
import com.google.android.material.R$id;
import com.google.android.material.R$layout;
import com.google.android.material.R$string;
import com.google.android.material.R$style;
import com.google.android.material.R$styleable;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.CheckableImageButton;
import com.google.android.material.internal.CollapsingTextHelper;
import com.google.android.material.internal.DescendantOffsetUtils;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.resources.MaterialResources;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class TextInputLayout extends LinearLayout {
    private static final int DEF_STYLE_RES = R$style.Widget_Design_TextInputLayout;
    private ValueAnimator animator;
    private MaterialShapeDrawable boxBackground;
    private int boxBackgroundColor;
    private int boxBackgroundMode;
    private final int boxCollapsedPaddingTopPx;
    private final int boxLabelCutoutPaddingPx;
    private int boxStrokeColor;
    private final int boxStrokeWidthDefaultPx;
    private final int boxStrokeWidthFocusedPx;
    private int boxStrokeWidthPx;
    private MaterialShapeDrawable boxUnderline;
    private final TextWatcher clearTextEndIconTextWatcher;
    private final OnEditTextAttachedListener clearTextOnEditTextAttachedListener;
    final CollapsingTextHelper collapsingTextHelper;
    private final ShapeAppearanceModel cornerAdjustedShapeAppearanceModel;
    boolean counterEnabled;
    private int counterMaxLength;
    private int counterOverflowTextAppearance;
    private ColorStateList counterOverflowTextColor;
    private boolean counterOverflowed;
    private int counterTextAppearance;
    private ColorStateList counterTextColor;
    private TextView counterView;
    private int defaultFilledBackgroundColor;
    private ColorStateList defaultHintTextColor;
    private final int defaultStrokeColor;
    private final int disabledColor;
    private final int disabledFilledBackgroundColor;
    EditText editText;
    private final LinkedHashSet<OnEditTextAttachedListener> editTextAttachedListeners;
    private final LinkedHashSet<OnEndIconChangedListener> endIconChangedListeners;
    private Drawable endIconDummyDrawable;
    private int endIconMode;
    private ColorStateList endIconTintList;
    private PorterDuff.Mode endIconTintMode;
    private final CheckableImageButton endIconView;
    private int focusedStrokeColor;
    private ColorStateList focusedTextColor;
    private boolean hasEndIconTintList;
    private boolean hasEndIconTintMode;
    private boolean hasStartIconTintList;
    private boolean hasStartIconTintMode;
    private CharSequence hint;
    private boolean hintAnimationEnabled;
    private boolean hintEnabled;
    private boolean hintExpanded;
    private final int hoveredFilledBackgroundColor;
    private final int hoveredStrokeColor;
    private boolean inDrawableStateChanged;
    private final IndicatorViewController indicatorViewController;
    private final FrameLayout inputFrame;
    private boolean isProvidingHint;
    private Drawable originalEditTextEndDrawable;
    private CharSequence originalHint;
    private final OnEndIconChangedListener passwordToggleEndIconChangedListener;
    private final OnEditTextAttachedListener passwordToggleOnEditTextAttachedListener;
    private boolean restoringSavedState;
    private final ShapeAppearanceModel shapeAppearanceModel;
    private Drawable startIconDummyDrawable;
    private ColorStateList startIconTintList;
    private PorterDuff.Mode startIconTintMode;
    private final CheckableImageButton startIconView;
    private final Rect tmpBoundsRect;
    private final Rect tmpRect;
    private final RectF tmpRectF;
    private Typeface typeface;

    public interface OnEditTextAttachedListener {
        void onEditTextAttached();
    }

    public interface OnEndIconChangedListener {
        void onEndIconChanged(int i);
    }

    public TextInputLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.textInputStyle);
    }

    public TextInputLayout(Context context, AttributeSet attributeSet, int i) {
        super(ThemeEnforcement.createThemedContext(context, attributeSet, i, DEF_STYLE_RES), attributeSet, i);
        this.indicatorViewController = new IndicatorViewController(this);
        this.tmpRect = new Rect();
        this.tmpBoundsRect = new Rect();
        this.tmpRectF = new RectF();
        this.editTextAttachedListeners = new LinkedHashSet<>();
        this.endIconMode = 0;
        this.endIconChangedListeners = new LinkedHashSet<>();
        this.passwordToggleOnEditTextAttachedListener = new OnEditTextAttachedListener() {
            /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass1 */

            @Override // com.google.android.material.textfield.TextInputLayout.OnEditTextAttachedListener
            public void onEditTextAttached() {
                TextInputLayout textInputLayout = TextInputLayout.this;
                textInputLayout.setEndIconVisible(textInputLayout.hasPasswordTransformation());
            }
        };
        this.passwordToggleEndIconChangedListener = new OnEndIconChangedListener() {
            /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass2 */

            @Override // com.google.android.material.textfield.TextInputLayout.OnEndIconChangedListener
            public void onEndIconChanged(int i) {
                EditText editText = TextInputLayout.this.editText;
                if (editText != null && i == 1) {
                    editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        };
        this.clearTextEndIconTextWatcher = new TextWatcher() {
            /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass3 */

            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            public void afterTextChanged(Editable editable) {
                TextInputLayout.this.setEndIconVisible(editable.length() > 0);
            }
        };
        this.clearTextOnEditTextAttachedListener = new OnEditTextAttachedListener() {
            /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass4 */

            @Override // com.google.android.material.textfield.TextInputLayout.OnEditTextAttachedListener
            public void onEditTextAttached() {
                TextInputLayout textInputLayout = TextInputLayout.this;
                textInputLayout.setEndIconVisible(!TextUtils.isEmpty(textInputLayout.editText.getText()));
                TextInputLayout textInputLayout2 = TextInputLayout.this;
                textInputLayout2.editText.removeTextChangedListener(textInputLayout2.clearTextEndIconTextWatcher);
                TextInputLayout textInputLayout3 = TextInputLayout.this;
                textInputLayout3.editText.addTextChangedListener(textInputLayout3.clearTextEndIconTextWatcher);
            }
        };
        this.collapsingTextHelper = new CollapsingTextHelper(this);
        Context context2 = getContext();
        setOrientation(1);
        setWillNotDraw(false);
        setAddStatesFromChildren(true);
        this.inputFrame = new FrameLayout(context2);
        this.inputFrame.setAddStatesFromChildren(true);
        addView(this.inputFrame);
        this.collapsingTextHelper.setTextSizeInterpolator(AnimationUtils.LINEAR_INTERPOLATOR);
        this.collapsingTextHelper.setPositionInterpolator(AnimationUtils.LINEAR_INTERPOLATOR);
        this.collapsingTextHelper.setCollapsedTextGravity(8388659);
        TintTypedArray obtainTintedStyledAttributes = ThemeEnforcement.obtainTintedStyledAttributes(context2, attributeSet, R$styleable.TextInputLayout, i, DEF_STYLE_RES, R$styleable.TextInputLayout_counterTextAppearance, R$styleable.TextInputLayout_counterOverflowTextAppearance, R$styleable.TextInputLayout_errorTextAppearance, R$styleable.TextInputLayout_helperTextTextAppearance, R$styleable.TextInputLayout_hintTextAppearance);
        this.hintEnabled = obtainTintedStyledAttributes.getBoolean(R$styleable.TextInputLayout_hintEnabled, true);
        setHint(obtainTintedStyledAttributes.getText(R$styleable.TextInputLayout_android_hint));
        this.hintAnimationEnabled = obtainTintedStyledAttributes.getBoolean(R$styleable.TextInputLayout_hintAnimationEnabled, true);
        this.shapeAppearanceModel = new ShapeAppearanceModel(context2, attributeSet, i, DEF_STYLE_RES);
        this.cornerAdjustedShapeAppearanceModel = new ShapeAppearanceModel(this.shapeAppearanceModel);
        this.boxLabelCutoutPaddingPx = context2.getResources().getDimensionPixelOffset(R$dimen.mtrl_textinput_box_label_cutout_padding);
        this.boxCollapsedPaddingTopPx = obtainTintedStyledAttributes.getDimensionPixelOffset(R$styleable.TextInputLayout_boxCollapsedPaddingTop, 0);
        this.boxStrokeWidthDefaultPx = context2.getResources().getDimensionPixelSize(R$dimen.mtrl_textinput_box_stroke_width_default);
        this.boxStrokeWidthFocusedPx = context2.getResources().getDimensionPixelSize(R$dimen.mtrl_textinput_box_stroke_width_focused);
        this.boxStrokeWidthPx = this.boxStrokeWidthDefaultPx;
        float dimension = obtainTintedStyledAttributes.getDimension(R$styleable.TextInputLayout_boxCornerRadiusTopStart, -1.0f);
        float dimension2 = obtainTintedStyledAttributes.getDimension(R$styleable.TextInputLayout_boxCornerRadiusTopEnd, -1.0f);
        float dimension3 = obtainTintedStyledAttributes.getDimension(R$styleable.TextInputLayout_boxCornerRadiusBottomEnd, -1.0f);
        float dimension4 = obtainTintedStyledAttributes.getDimension(R$styleable.TextInputLayout_boxCornerRadiusBottomStart, -1.0f);
        if (dimension >= 0.0f) {
            this.shapeAppearanceModel.getTopLeftCorner().setCornerSize(dimension);
        }
        if (dimension2 >= 0.0f) {
            this.shapeAppearanceModel.getTopRightCorner().setCornerSize(dimension2);
        }
        if (dimension3 >= 0.0f) {
            this.shapeAppearanceModel.getBottomRightCorner().setCornerSize(dimension3);
        }
        if (dimension4 >= 0.0f) {
            this.shapeAppearanceModel.getBottomLeftCorner().setCornerSize(dimension4);
        }
        adjustCornerSizeForStrokeWidth();
        ColorStateList colorStateList = MaterialResources.getColorStateList(context2, obtainTintedStyledAttributes, R$styleable.TextInputLayout_boxBackgroundColor);
        if (colorStateList != null) {
            this.defaultFilledBackgroundColor = colorStateList.getDefaultColor();
            this.boxBackgroundColor = this.defaultFilledBackgroundColor;
            if (colorStateList.isStateful()) {
                this.disabledFilledBackgroundColor = colorStateList.getColorForState(new int[]{-16842910}, -1);
                this.hoveredFilledBackgroundColor = colorStateList.getColorForState(new int[]{16843623}, -1);
            } else {
                ColorStateList colorStateList2 = AppCompatResources.getColorStateList(context2, R$color.mtrl_filled_background_color);
                this.disabledFilledBackgroundColor = colorStateList2.getColorForState(new int[]{-16842910}, -1);
                this.hoveredFilledBackgroundColor = colorStateList2.getColorForState(new int[]{16843623}, -1);
            }
        } else {
            this.boxBackgroundColor = 0;
            this.defaultFilledBackgroundColor = 0;
            this.disabledFilledBackgroundColor = 0;
            this.hoveredFilledBackgroundColor = 0;
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_android_textColorHint)) {
            ColorStateList colorStateList3 = obtainTintedStyledAttributes.getColorStateList(R$styleable.TextInputLayout_android_textColorHint);
            this.focusedTextColor = colorStateList3;
            this.defaultHintTextColor = colorStateList3;
        }
        ColorStateList colorStateList4 = MaterialResources.getColorStateList(context2, obtainTintedStyledAttributes, R$styleable.TextInputLayout_boxStrokeColor);
        if (colorStateList4 == null || !colorStateList4.isStateful()) {
            this.focusedStrokeColor = obtainTintedStyledAttributes.getColor(R$styleable.TextInputLayout_boxStrokeColor, 0);
            this.defaultStrokeColor = ContextCompat.getColor(context2, R$color.mtrl_textinput_default_box_stroke_color);
            this.disabledColor = ContextCompat.getColor(context2, R$color.mtrl_textinput_disabled_color);
            this.hoveredStrokeColor = ContextCompat.getColor(context2, R$color.mtrl_textinput_hovered_box_stroke_color);
        } else {
            this.defaultStrokeColor = colorStateList4.getDefaultColor();
            this.disabledColor = colorStateList4.getColorForState(new int[]{-16842910}, -1);
            this.hoveredStrokeColor = colorStateList4.getColorForState(new int[]{16843623}, -1);
            this.focusedStrokeColor = colorStateList4.getColorForState(new int[]{16842908}, -1);
        }
        if (obtainTintedStyledAttributes.getResourceId(R$styleable.TextInputLayout_hintTextAppearance, -1) != -1) {
            setHintTextAppearance(obtainTintedStyledAttributes.getResourceId(R$styleable.TextInputLayout_hintTextAppearance, 0));
        }
        int resourceId = obtainTintedStyledAttributes.getResourceId(R$styleable.TextInputLayout_errorTextAppearance, 0);
        boolean z = obtainTintedStyledAttributes.getBoolean(R$styleable.TextInputLayout_errorEnabled, false);
        int resourceId2 = obtainTintedStyledAttributes.getResourceId(R$styleable.TextInputLayout_helperTextTextAppearance, 0);
        boolean z2 = obtainTintedStyledAttributes.getBoolean(R$styleable.TextInputLayout_helperTextEnabled, false);
        CharSequence text = obtainTintedStyledAttributes.getText(R$styleable.TextInputLayout_helperText);
        boolean z3 = obtainTintedStyledAttributes.getBoolean(R$styleable.TextInputLayout_counterEnabled, false);
        setCounterMaxLength(obtainTintedStyledAttributes.getInt(R$styleable.TextInputLayout_counterMaxLength, -1));
        this.counterTextAppearance = obtainTintedStyledAttributes.getResourceId(R$styleable.TextInputLayout_counterTextAppearance, 0);
        this.counterOverflowTextAppearance = obtainTintedStyledAttributes.getResourceId(R$styleable.TextInputLayout_counterOverflowTextAppearance, 0);
        this.startIconView = (CheckableImageButton) LayoutInflater.from(getContext()).inflate(R$layout.design_text_input_start_icon, (ViewGroup) this.inputFrame, false);
        this.inputFrame.addView(this.startIconView);
        this.startIconView.setVisibility(8);
        setStartIconOnClickListener(null);
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_startIconDrawable)) {
            setStartIconDrawable(obtainTintedStyledAttributes.getDrawable(R$styleable.TextInputLayout_startIconDrawable));
            if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_startIconContentDescription)) {
                setStartIconContentDescription(obtainTintedStyledAttributes.getText(R$styleable.TextInputLayout_startIconContentDescription));
            }
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_startIconTint)) {
            setStartIconTintList(MaterialResources.getColorStateList(context2, obtainTintedStyledAttributes, R$styleable.TextInputLayout_startIconTint));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_startIconTintMode)) {
            setStartIconTintMode(ViewUtils.parseTintMode(obtainTintedStyledAttributes.getInt(R$styleable.TextInputLayout_startIconTintMode, -1), null));
        }
        this.endIconView = (CheckableImageButton) LayoutInflater.from(getContext()).inflate(R$layout.design_text_input_end_icon, (ViewGroup) this.inputFrame, false);
        this.inputFrame.addView(this.endIconView);
        this.endIconView.setVisibility(8);
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_endIconMode)) {
            setEndIconMode(obtainTintedStyledAttributes.getInt(R$styleable.TextInputLayout_endIconMode, 0));
            if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_endIconDrawable)) {
                setEndIconDrawable(obtainTintedStyledAttributes.getDrawable(R$styleable.TextInputLayout_endIconDrawable));
            }
            if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_endIconContentDescription)) {
                setEndIconContentDescription(obtainTintedStyledAttributes.getText(R$styleable.TextInputLayout_endIconContentDescription));
            }
        } else if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_passwordToggleEnabled)) {
            setEndIconMode(1);
            setEndIconDrawable(obtainTintedStyledAttributes.getDrawable(R$styleable.TextInputLayout_passwordToggleDrawable));
            setEndIconContentDescription(obtainTintedStyledAttributes.getText(R$styleable.TextInputLayout_passwordToggleContentDescription));
            if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_passwordToggleTint)) {
                setEndIconTintList(MaterialResources.getColorStateList(context2, obtainTintedStyledAttributes, R$styleable.TextInputLayout_passwordToggleTint));
            }
            if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_passwordToggleTintMode)) {
                setEndIconTintMode(ViewUtils.parseTintMode(obtainTintedStyledAttributes.getInt(R$styleable.TextInputLayout_passwordToggleTintMode, -1), null));
            }
        }
        if (!obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_passwordToggleEnabled)) {
            if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_endIconTint)) {
                setEndIconTintList(MaterialResources.getColorStateList(context2, obtainTintedStyledAttributes, R$styleable.TextInputLayout_endIconTint));
            }
            if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_endIconTintMode)) {
                setEndIconTintMode(ViewUtils.parseTintMode(obtainTintedStyledAttributes.getInt(R$styleable.TextInputLayout_endIconTintMode, -1), null));
            }
        }
        setHelperTextEnabled(z2);
        setHelperText(text);
        setHelperTextTextAppearance(resourceId2);
        setErrorEnabled(z);
        setErrorTextAppearance(resourceId);
        setCounterTextAppearance(this.counterTextAppearance);
        setCounterOverflowTextAppearance(this.counterOverflowTextAppearance);
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_errorTextColor)) {
            setErrorTextColor(obtainTintedStyledAttributes.getColorStateList(R$styleable.TextInputLayout_errorTextColor));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_helperTextTextColor)) {
            setHelperTextColor(obtainTintedStyledAttributes.getColorStateList(R$styleable.TextInputLayout_helperTextTextColor));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_hintTextColor)) {
            setHintTextColor(obtainTintedStyledAttributes.getColorStateList(R$styleable.TextInputLayout_hintTextColor));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_counterTextColor)) {
            setCounterTextColor(obtainTintedStyledAttributes.getColorStateList(R$styleable.TextInputLayout_counterTextColor));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.TextInputLayout_counterOverflowTextColor)) {
            setCounterOverflowTextColor(obtainTintedStyledAttributes.getColorStateList(R$styleable.TextInputLayout_counterOverflowTextColor));
        }
        setCounterEnabled(z3);
        setBoxBackgroundMode(obtainTintedStyledAttributes.getInt(R$styleable.TextInputLayout_boxBackgroundMode, 0));
        obtainTintedStyledAttributes.recycle();
        ViewCompat.setImportantForAccessibility(this, 2);
    }

    @Override // android.view.ViewGroup
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        if (view instanceof EditText) {
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(layoutParams);
            layoutParams2.gravity = (layoutParams2.gravity & -113) | 16;
            this.inputFrame.addView(view, layoutParams2);
            this.inputFrame.setLayoutParams(layoutParams);
            updateInputLayoutMargins();
            setEditText((EditText) view);
            return;
        }
        super.addView(view, i, layoutParams);
    }

    private Drawable getBoxBackground() {
        int i = this.boxBackgroundMode;
        if (i == 1 || i == 2) {
            return this.boxBackground;
        }
        throw new IllegalStateException();
    }

    public void setBoxBackgroundMode(int i) {
        if (i != this.boxBackgroundMode) {
            this.boxBackgroundMode = i;
            if (this.editText != null) {
                onApplyBoxBackgroundMode();
            }
        }
    }

    private void onApplyBoxBackgroundMode() {
        assignBoxBackgroundByMode();
        setEditTextBoxBackground();
        updateTextInputBoxState();
        if (this.boxBackgroundMode != 0) {
            updateInputLayoutMargins();
        }
    }

    private void assignBoxBackgroundByMode() {
        int i = this.boxBackgroundMode;
        if (i == 0) {
            this.boxBackground = null;
            this.boxUnderline = null;
        } else if (i == 1) {
            this.boxBackground = new MaterialShapeDrawable(this.shapeAppearanceModel);
            this.boxUnderline = new MaterialShapeDrawable();
        } else if (i == 2) {
            if (!this.hintEnabled || (this.boxBackground instanceof CutoutDrawable)) {
                this.boxBackground = new MaterialShapeDrawable(this.shapeAppearanceModel);
            } else {
                this.boxBackground = new CutoutDrawable(this.shapeAppearanceModel);
            }
            this.boxUnderline = null;
        } else {
            throw new IllegalArgumentException(this.boxBackgroundMode + " is illegal; only @BoxBackgroundMode constants are supported.");
        }
    }

    private void setEditTextBoxBackground() {
        if (shouldUseEditTextBackgroundForBoxBackground()) {
            ViewCompat.setBackground(this.editText, this.boxBackground);
        }
    }

    private boolean shouldUseEditTextBackgroundForBoxBackground() {
        EditText editText2 = this.editText;
        return (editText2 == null || this.boxBackground == null || editText2.getBackground() != null || this.boxBackgroundMode == 0) ? false : true;
    }

    private void adjustCornerSizeForStrokeWidth() {
        float f = this.boxBackgroundMode == 2 ? ((float) this.boxStrokeWidthPx) / 2.0f : 0.0f;
        if (f > 0.0f) {
            this.cornerAdjustedShapeAppearanceModel.getTopLeftCorner().setCornerSize(this.shapeAppearanceModel.getTopLeftCorner().getCornerSize() + f);
            this.cornerAdjustedShapeAppearanceModel.getTopRightCorner().setCornerSize(this.shapeAppearanceModel.getTopRightCorner().getCornerSize() + f);
            this.cornerAdjustedShapeAppearanceModel.getBottomRightCorner().setCornerSize(this.shapeAppearanceModel.getBottomRightCorner().getCornerSize() + f);
            this.cornerAdjustedShapeAppearanceModel.getBottomLeftCorner().setCornerSize(this.shapeAppearanceModel.getBottomLeftCorner().getCornerSize() + f);
            ensureCornerAdjustedShapeAppearanceModel();
        }
    }

    private void ensureCornerAdjustedShapeAppearanceModel() {
        if (this.boxBackgroundMode != 0 && (getBoxBackground() instanceof MaterialShapeDrawable)) {
            ((MaterialShapeDrawable) getBoxBackground()).setShapeAppearanceModel(this.cornerAdjustedShapeAppearanceModel);
        }
    }

    public void dispatchProvideAutofillStructure(ViewStructure viewStructure, int i) {
        EditText editText2;
        if (this.originalHint == null || (editText2 = this.editText) == null) {
            super.dispatchProvideAutofillStructure(viewStructure, i);
            return;
        }
        boolean z = this.isProvidingHint;
        this.isProvidingHint = false;
        CharSequence hint2 = editText2.getHint();
        this.editText.setHint(this.originalHint);
        try {
            super.dispatchProvideAutofillStructure(viewStructure, i);
        } finally {
            this.editText.setHint(hint2);
            this.isProvidingHint = z;
        }
    }

    private void setEditText(EditText editText2) {
        if (this.editText == null) {
            if (!(editText2 instanceof TextInputEditText)) {
                Log.i("TextInputLayout", "EditText added is not a TextInputEditText. Please switch to using that class instead.");
            }
            this.editText = editText2;
            onApplyBoxBackgroundMode();
            setTextInputAccessibilityDelegate(new AccessibilityDelegate(this));
            this.collapsingTextHelper.setTypefaces(this.editText.getTypeface());
            this.collapsingTextHelper.setExpandedTextSize(this.editText.getTextSize());
            int gravity = this.editText.getGravity();
            this.collapsingTextHelper.setCollapsedTextGravity((gravity & -113) | 48);
            this.collapsingTextHelper.setExpandedTextGravity(gravity);
            this.editText.addTextChangedListener(new TextWatcher() {
                /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass5 */

                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                }

                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                }

                public void afterTextChanged(Editable editable) {
                    TextInputLayout textInputLayout = TextInputLayout.this;
                    textInputLayout.updateLabelState(!textInputLayout.restoringSavedState);
                    TextInputLayout textInputLayout2 = TextInputLayout.this;
                    if (textInputLayout2.counterEnabled) {
                        textInputLayout2.updateCounter(editable.length());
                    }
                }
            });
            if (this.defaultHintTextColor == null) {
                this.defaultHintTextColor = this.editText.getHintTextColors();
            }
            if (this.hintEnabled) {
                if (TextUtils.isEmpty(this.hint)) {
                    this.originalHint = this.editText.getHint();
                    setHint(this.originalHint);
                    this.editText.setHint((CharSequence) null);
                }
                this.isProvidingHint = true;
            }
            if (this.counterView != null) {
                updateCounter(this.editText.getText().length());
            }
            updateEditTextBackground();
            this.indicatorViewController.adjustIndicatorPadding();
            updateIconViewOnEditTextAttached(this.startIconView, R$dimen.mtrl_textinput_start_icon_padding_start, R$dimen.mtrl_textinput_start_icon_padding_end);
            updateIconViewOnEditTextAttached(this.endIconView, R$dimen.mtrl_textinput_end_icon_padding_start, R$dimen.mtrl_textinput_end_icon_padding_end);
            dispatchOnEditTextAttached();
            updateLabelState(false, true);
            return;
        }
        throw new IllegalArgumentException("We already have an EditText, can only have one");
    }

    private void updateInputLayoutMargins() {
        if (this.boxBackgroundMode != 1) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.inputFrame.getLayoutParams();
            int calculateLabelMarginTop = calculateLabelMarginTop();
            if (calculateLabelMarginTop != layoutParams.topMargin) {
                layoutParams.topMargin = calculateLabelMarginTop;
                this.inputFrame.requestLayout();
            }
        }
    }

    public int getBaseline() {
        EditText editText2 = this.editText;
        if (editText2 != null) {
            return editText2.getBaseline() + getPaddingTop() + calculateLabelMarginTop();
        }
        return super.getBaseline();
    }

    /* access modifiers changed from: package-private */
    public void updateLabelState(boolean z) {
        updateLabelState(z, false);
    }

    private void updateLabelState(boolean z, boolean z2) {
        ColorStateList colorStateList;
        TextView textView;
        boolean isEnabled = isEnabled();
        EditText editText2 = this.editText;
        boolean z3 = true;
        boolean z4 = editText2 != null && !TextUtils.isEmpty(editText2.getText());
        EditText editText3 = this.editText;
        if (editText3 == null || !editText3.hasFocus()) {
            z3 = false;
        }
        boolean errorShouldBeShown = this.indicatorViewController.errorShouldBeShown();
        ColorStateList colorStateList2 = this.defaultHintTextColor;
        if (colorStateList2 != null) {
            this.collapsingTextHelper.setCollapsedTextColor(colorStateList2);
            this.collapsingTextHelper.setExpandedTextColor(this.defaultHintTextColor);
        }
        if (!isEnabled) {
            this.collapsingTextHelper.setCollapsedTextColor(ColorStateList.valueOf(this.disabledColor));
            this.collapsingTextHelper.setExpandedTextColor(ColorStateList.valueOf(this.disabledColor));
        } else if (errorShouldBeShown) {
            this.collapsingTextHelper.setCollapsedTextColor(this.indicatorViewController.getErrorViewTextColors());
        } else if (this.counterOverflowed && (textView = this.counterView) != null) {
            this.collapsingTextHelper.setCollapsedTextColor(textView.getTextColors());
        } else if (z3 && (colorStateList = this.focusedTextColor) != null) {
            this.collapsingTextHelper.setCollapsedTextColor(colorStateList);
        }
        if (z4 || (isEnabled() && (z3 || errorShouldBeShown))) {
            if (z2 || this.hintExpanded) {
                collapseHint(z);
            }
        } else if (z2 || !this.hintExpanded) {
            expandHint(z);
        }
    }

    public EditText getEditText() {
        return this.editText;
    }

    public void setHint(CharSequence charSequence) {
        if (this.hintEnabled) {
            setHintInternal(charSequence);
            sendAccessibilityEvent(2048);
        }
    }

    private void setHintInternal(CharSequence charSequence) {
        if (!TextUtils.equals(charSequence, this.hint)) {
            this.hint = charSequence;
            this.collapsingTextHelper.setText(charSequence);
            if (!this.hintExpanded) {
                openCutout();
            }
        }
    }

    public CharSequence getHint() {
        if (this.hintEnabled) {
            return this.hint;
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public boolean isProvidingHint() {
        return this.isProvidingHint;
    }

    public void setHintTextAppearance(int i) {
        this.collapsingTextHelper.setCollapsedTextAppearance(i);
        this.focusedTextColor = this.collapsingTextHelper.getCollapsedTextColor();
        if (this.editText != null) {
            updateLabelState(false);
            updateInputLayoutMargins();
        }
    }

    public void setHintTextColor(ColorStateList colorStateList) {
        if (this.collapsingTextHelper.getCollapsedTextColor() != colorStateList) {
            this.collapsingTextHelper.setCollapsedTextColor(colorStateList);
            this.focusedTextColor = colorStateList;
            if (this.editText != null) {
                updateLabelState(false);
            }
        }
    }

    public void setErrorEnabled(boolean z) {
        this.indicatorViewController.setErrorEnabled(z);
    }

    public void setErrorTextAppearance(int i) {
        this.indicatorViewController.setErrorTextAppearance(i);
    }

    public void setErrorTextColor(ColorStateList colorStateList) {
        this.indicatorViewController.setErrorViewTextColor(colorStateList);
    }

    public void setHelperTextTextAppearance(int i) {
        this.indicatorViewController.setHelperTextAppearance(i);
    }

    public void setHelperTextColor(ColorStateList colorStateList) {
        this.indicatorViewController.setHelperTextViewTextColor(colorStateList);
    }

    public void setHelperTextEnabled(boolean z) {
        this.indicatorViewController.setHelperTextEnabled(z);
    }

    public void setHelperText(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            if (!isHelperTextEnabled()) {
                setHelperTextEnabled(true);
            }
            this.indicatorViewController.showHelper(charSequence);
        } else if (isHelperTextEnabled()) {
            setHelperTextEnabled(false);
        }
    }

    public boolean isHelperTextEnabled() {
        return this.indicatorViewController.isHelperTextEnabled();
    }

    public void setError(CharSequence charSequence) {
        if (!this.indicatorViewController.isErrorEnabled()) {
            if (!TextUtils.isEmpty(charSequence)) {
                setErrorEnabled(true);
            } else {
                return;
            }
        }
        if (!TextUtils.isEmpty(charSequence)) {
            this.indicatorViewController.showError(charSequence);
        } else {
            this.indicatorViewController.hideError();
        }
    }

    public void setCounterEnabled(boolean z) {
        if (this.counterEnabled != z) {
            if (z) {
                this.counterView = new AppCompatTextView(getContext());
                this.counterView.setId(R$id.textinput_counter);
                Typeface typeface2 = this.typeface;
                if (typeface2 != null) {
                    this.counterView.setTypeface(typeface2);
                }
                this.counterView.setMaxLines(1);
                this.indicatorViewController.addIndicator(this.counterView, 2);
                updateCounterTextAppearanceAndColor();
                updateCounter();
            } else {
                this.indicatorViewController.removeIndicator(this.counterView, 2);
                this.counterView = null;
            }
            this.counterEnabled = z;
        }
    }

    public void setCounterTextAppearance(int i) {
        if (this.counterTextAppearance != i) {
            this.counterTextAppearance = i;
            updateCounterTextAppearanceAndColor();
        }
    }

    public void setCounterTextColor(ColorStateList colorStateList) {
        if (this.counterTextColor != colorStateList) {
            this.counterTextColor = colorStateList;
            updateCounterTextAppearanceAndColor();
        }
    }

    public void setCounterOverflowTextAppearance(int i) {
        if (this.counterOverflowTextAppearance != i) {
            this.counterOverflowTextAppearance = i;
            updateCounterTextAppearanceAndColor();
        }
    }

    public void setCounterOverflowTextColor(ColorStateList colorStateList) {
        if (this.counterOverflowTextColor != colorStateList) {
            this.counterOverflowTextColor = colorStateList;
            updateCounterTextAppearanceAndColor();
        }
    }

    public void setCounterMaxLength(int i) {
        if (this.counterMaxLength != i) {
            if (i > 0) {
                this.counterMaxLength = i;
            } else {
                this.counterMaxLength = -1;
            }
            if (this.counterEnabled) {
                updateCounter();
            }
        }
    }

    private void updateCounter() {
        if (this.counterView != null) {
            EditText editText2 = this.editText;
            updateCounter(editText2 == null ? 0 : editText2.getText().length());
        }
    }

    /* access modifiers changed from: package-private */
    public void updateCounter(int i) {
        boolean z = this.counterOverflowed;
        if (this.counterMaxLength == -1) {
            this.counterView.setText(String.valueOf(i));
            this.counterView.setContentDescription(null);
            this.counterOverflowed = false;
        } else {
            if (ViewCompat.getAccessibilityLiveRegion(this.counterView) == 1) {
                ViewCompat.setAccessibilityLiveRegion(this.counterView, 0);
            }
            this.counterOverflowed = i > this.counterMaxLength;
            updateCounterContentDescription(getContext(), this.counterView, i, this.counterMaxLength, this.counterOverflowed);
            if (z != this.counterOverflowed) {
                updateCounterTextAppearanceAndColor();
                if (this.counterOverflowed) {
                    ViewCompat.setAccessibilityLiveRegion(this.counterView, 1);
                }
            }
            this.counterView.setText(getContext().getString(R$string.character_counter_pattern, Integer.valueOf(i), Integer.valueOf(this.counterMaxLength)));
        }
        if (this.editText != null && z != this.counterOverflowed) {
            updateLabelState(false);
            updateTextInputBoxState();
            updateEditTextBackground();
        }
    }

    private static void updateCounterContentDescription(Context context, TextView textView, int i, int i2, boolean z) {
        textView.setContentDescription(context.getString(z ? R$string.character_counter_overflowed_content_description : R$string.character_counter_content_description, Integer.valueOf(i), Integer.valueOf(i2)));
    }

    public void setEnabled(boolean z) {
        recursiveSetEnabled(this, z);
        super.setEnabled(z);
    }

    private static void recursiveSetEnabled(ViewGroup viewGroup, boolean z) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            childAt.setEnabled(z);
            if (childAt instanceof ViewGroup) {
                recursiveSetEnabled((ViewGroup) childAt, z);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public CharSequence getCounterOverflowDescription() {
        TextView textView;
        if (!this.counterEnabled || !this.counterOverflowed || (textView = this.counterView) == null) {
            return null;
        }
        return textView.getContentDescription();
    }

    private void updateCounterTextAppearanceAndColor() {
        ColorStateList colorStateList;
        ColorStateList colorStateList2;
        TextView textView = this.counterView;
        if (textView != null) {
            setTextAppearanceCompatWithErrorFallback(textView, this.counterOverflowed ? this.counterOverflowTextAppearance : this.counterTextAppearance);
            if (!this.counterOverflowed && (colorStateList2 = this.counterTextColor) != null) {
                this.counterView.setTextColor(colorStateList2);
            }
            if (this.counterOverflowed && (colorStateList = this.counterOverflowTextColor) != null) {
                this.counterView.setTextColor(colorStateList);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setTextAppearanceCompatWithErrorFallback(TextView textView, int i) {
        boolean z = true;
        try {
            TextViewCompat.setTextAppearance(textView, i);
            if (Build.VERSION.SDK_INT < 23 || textView.getTextColors().getDefaultColor() != -65281) {
                z = false;
            }
        } catch (Exception unused) {
        }
        if (z) {
            TextViewCompat.setTextAppearance(textView, R$style.TextAppearance_AppCompat_Caption);
            textView.setTextColor(ContextCompat.getColor(getContext(), R$color.design_error));
        }
    }

    private int calculateLabelMarginTop() {
        float collapsedTextHeight;
        if (!this.hintEnabled) {
            return 0;
        }
        int i = this.boxBackgroundMode;
        if (i == 0 || i == 1) {
            collapsedTextHeight = this.collapsingTextHelper.getCollapsedTextHeight();
        } else if (i != 2) {
            return 0;
        } else {
            collapsedTextHeight = this.collapsingTextHelper.getCollapsedTextHeight() / 2.0f;
        }
        return (int) collapsedTextHeight;
    }

    private Rect calculateCollapsedTextBounds(Rect rect) {
        EditText editText2 = this.editText;
        if (editText2 != null) {
            Rect rect2 = this.tmpBoundsRect;
            rect2.bottom = rect.bottom;
            int i = this.boxBackgroundMode;
            if (i == 1) {
                rect2.left = rect.left + editText2.getCompoundPaddingLeft();
                rect2.top = rect.top + this.boxCollapsedPaddingTopPx;
                rect2.right = rect.right - this.editText.getCompoundPaddingRight();
                return rect2;
            } else if (i != 2) {
                rect2.left = rect.left + editText2.getCompoundPaddingLeft();
                rect2.top = getPaddingTop();
                rect2.right = rect.right - this.editText.getCompoundPaddingRight();
                return rect2;
            } else {
                rect2.left = rect.left + editText2.getPaddingLeft();
                rect2.top = rect.top - calculateLabelMarginTop();
                rect2.right = rect.right - this.editText.getPaddingRight();
                return rect2;
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private Rect calculateExpandedTextBounds(Rect rect) {
        EditText editText2 = this.editText;
        if (editText2 != null) {
            Rect rect2 = this.tmpBoundsRect;
            rect2.left = rect.left + editText2.getCompoundPaddingLeft();
            rect2.top = rect.top + this.editText.getCompoundPaddingTop();
            rect2.right = rect.right - this.editText.getCompoundPaddingRight();
            rect2.bottom = rect.bottom - this.editText.getCompoundPaddingBottom();
            return rect2;
        }
        throw new IllegalStateException();
    }

    private int calculateBoxBackgroundColor() {
        return this.boxBackgroundMode == 1 ? MaterialColors.layer(MaterialColors.getColor(this, R$attr.colorSurface, 0), this.boxBackgroundColor) : this.boxBackgroundColor;
    }

    private void applyBoxAttributes() {
        if (this.boxBackground != null) {
            if (canDrawOutlineStroke()) {
                this.boxBackground.setStroke((float) this.boxStrokeWidthPx, this.boxStrokeColor);
            }
            this.boxBackground.setFillColor(ColorStateList.valueOf(calculateBoxBackgroundColor()));
            applyBoxUnderlineAttributes();
            invalidate();
        }
    }

    private void applyBoxUnderlineAttributes() {
        if (this.boxUnderline != null) {
            if (canDrawStroke()) {
                this.boxUnderline.setFillColor(ColorStateList.valueOf(this.boxStrokeColor));
            }
            invalidate();
        }
    }

    private boolean canDrawOutlineStroke() {
        return this.boxBackgroundMode == 2 && canDrawStroke();
    }

    private boolean canDrawStroke() {
        return this.boxStrokeWidthPx > -1 && this.boxStrokeColor != 0;
    }

    /* access modifiers changed from: package-private */
    public void updateEditTextBackground() {
        Drawable background;
        TextView textView;
        EditText editText2 = this.editText;
        if (editText2 != null && this.boxBackgroundMode == 0 && (background = editText2.getBackground()) != null) {
            if (DrawableUtils.canSafelyMutateDrawable(background)) {
                background = background.mutate();
            }
            if (this.indicatorViewController.errorShouldBeShown()) {
                background.setColorFilter(AppCompatDrawableManager.getPorterDuffColorFilter(this.indicatorViewController.getErrorViewCurrentTextColor(), PorterDuff.Mode.SRC_IN));
            } else if (!this.counterOverflowed || (textView = this.counterView) == null) {
                DrawableCompat.clearColorFilter(background);
                this.editText.refreshDrawableState();
            } else {
                background.setColorFilter(AppCompatDrawableManager.getPorterDuffColorFilter(textView.getCurrentTextColor(), PorterDuff.Mode.SRC_IN));
            }
        }
    }

    /* access modifiers changed from: package-private */
    public static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {
            /* class com.google.android.material.textfield.TextInputLayout.SavedState.AnonymousClass1 */

            @Override // android.os.Parcelable.ClassLoaderCreator
            public SavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new SavedState(parcel, classLoader);
            }

            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel, null);
            }

            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        CharSequence error;
        boolean isEndIconChecked;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        SavedState(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.error = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.isEndIconChecked = parcel.readInt() != 1 ? false : true;
        }

        @Override // androidx.customview.view.AbsSavedState
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            TextUtils.writeToParcel(this.error, parcel, i);
            parcel.writeInt(this.isEndIconChecked ? 1 : 0);
        }

        public String toString() {
            return "TextInputLayout.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " error=" + ((Object) this.error) + "}";
        }
    }

    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        if (this.indicatorViewController.errorShouldBeShown()) {
            savedState.error = getError();
        }
        savedState.isEndIconChecked = hasEndIcon() && this.endIconView.isChecked();
        return savedState;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setError(savedState.error);
        if (savedState.isEndIconChecked) {
            this.endIconView.performClick();
            this.endIconView.jumpDrawablesToCurrentState();
        }
        requestLayout();
    }

    /* access modifiers changed from: protected */
    @Override // android.view.View, android.view.ViewGroup
    public void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        this.restoringSavedState = true;
        super.dispatchRestoreInstanceState(sparseArray);
        this.restoringSavedState = false;
    }

    public CharSequence getError() {
        if (this.indicatorViewController.isErrorEnabled()) {
            return this.indicatorViewController.getErrorText();
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        setEditTextHeightAndDummyDrawables();
    }

    private void setEditTextHeightAndDummyDrawables() {
        if (this.editText != null) {
            int max = Math.max(this.endIconView.getMeasuredHeight(), this.startIconView.getMeasuredHeight());
            if (this.editText.getMeasuredHeight() < max) {
                this.editText.setMinimumHeight(max);
                this.editText.post(new Runnable() {
                    /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass6 */

                    public void run() {
                        TextInputLayout.this.editText.requestLayout();
                    }
                });
            }
            updateIconDummyDrawables();
        }
    }

    public void setStartIconDrawable(Drawable drawable) {
        this.startIconView.setImageDrawable(drawable);
        if (drawable != null) {
            setStartIconVisible(true);
            applyStartIconTint();
            return;
        }
        setStartIconVisible(false);
        setStartIconOnClickListener(null);
        setStartIconContentDescription(null);
    }

    public Drawable getStartIconDrawable() {
        return this.startIconView.getDrawable();
    }

    public void setStartIconOnClickListener(View.OnClickListener onClickListener) {
        setIconOnClickListener(this.startIconView, onClickListener);
    }

    public void setStartIconVisible(boolean z) {
        if (isStartIconVisible() != z) {
            this.startIconView.setVisibility(z ? 0 : 8);
            updateIconDummyDrawables();
        }
    }

    public boolean isStartIconVisible() {
        return this.startIconView.getVisibility() == 0;
    }

    public void setStartIconContentDescription(CharSequence charSequence) {
        if (getStartIconContentDescription() != charSequence) {
            this.startIconView.setContentDescription(charSequence);
        }
    }

    public CharSequence getStartIconContentDescription() {
        return this.startIconView.getContentDescription();
    }

    public void setStartIconTintList(ColorStateList colorStateList) {
        if (this.startIconTintList != colorStateList) {
            this.startIconTintList = colorStateList;
            this.hasStartIconTintList = true;
            applyStartIconTint();
        }
    }

    public void setStartIconTintMode(PorterDuff.Mode mode) {
        if (this.startIconTintMode != mode) {
            this.startIconTintMode = mode;
            this.hasStartIconTintMode = true;
            applyStartIconTint();
        }
    }

    public void setEndIconMode(int i) {
        int i2 = this.endIconMode;
        this.endIconMode = i;
        setEndIconVisible(i != 0);
        if (i == -1) {
            setEndIconOnClickListener(null);
        } else if (i == 1) {
            setEndIconPasswordToggleDefaults();
        } else if (i != 2) {
            setEndIconOnClickListener(null);
            setEndIconDrawable(null);
            setEndIconContentDescription(null);
        } else {
            setEndIconClearTextDefaults();
        }
        applyEndIconTint();
        dispatchOnEndIconChanged(i2);
    }

    public void setEndIconOnClickListener(View.OnClickListener onClickListener) {
        setIconOnClickListener(this.endIconView, onClickListener);
    }

    public void setEndIconVisible(boolean z) {
        if (isEndIconVisible() != z) {
            this.endIconView.setVisibility(z ? 0 : 4);
            updateIconDummyDrawables();
        }
    }

    public boolean isEndIconVisible() {
        return this.endIconView.getVisibility() == 0;
    }

    public void setEndIconDrawable(Drawable drawable) {
        this.endIconView.setImageDrawable(drawable);
    }

    public void setEndIconContentDescription(CharSequence charSequence) {
        if (getEndIconContentDescription() != charSequence) {
            this.endIconView.setContentDescription(charSequence);
        }
    }

    public CharSequence getEndIconContentDescription() {
        return this.endIconView.getContentDescription();
    }

    public void setEndIconTintList(ColorStateList colorStateList) {
        if (this.endIconTintList != colorStateList) {
            this.endIconTintList = colorStateList;
            this.hasEndIconTintList = true;
            applyEndIconTint();
        }
    }

    public void setEndIconTintMode(PorterDuff.Mode mode) {
        if (this.endIconTintMode != mode) {
            this.endIconTintMode = mode;
            this.hasEndIconTintMode = true;
            applyEndIconTint();
        }
    }

    public void addOnEndIconChangedListener(OnEndIconChangedListener onEndIconChangedListener) {
        this.endIconChangedListeners.add(onEndIconChangedListener);
    }

    public void addOnEditTextAttachedListener(OnEditTextAttachedListener onEditTextAttachedListener) {
        this.editTextAttachedListeners.add(onEditTextAttachedListener);
        if (this.editText != null) {
            onEditTextAttachedListener.onEditTextAttached();
        }
    }

    private void setEndIconPasswordToggleDefaults() {
        setEndIconDrawable(AppCompatResources.getDrawable(getContext(), R$drawable.design_password_eye));
        setEndIconContentDescription(getResources().getText(R$string.password_toggle_content_description));
        setEndIconOnClickListener(new View.OnClickListener() {
            /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass7 */

            public void onClick(View view) {
                EditText editText = TextInputLayout.this.editText;
                if (editText != null) {
                    int selectionEnd = editText.getSelectionEnd();
                    if (TextInputLayout.this.hasPasswordTransformation()) {
                        TextInputLayout.this.editText.setTransformationMethod(null);
                        TextInputLayout.this.endIconView.setChecked(true);
                    } else {
                        TextInputLayout.this.editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        TextInputLayout.this.endIconView.setChecked(false);
                    }
                    TextInputLayout.this.editText.setSelection(selectionEnd);
                }
            }
        });
        addOnEditTextAttachedListener(this.passwordToggleOnEditTextAttachedListener);
        addOnEndIconChangedListener(this.passwordToggleEndIconChangedListener);
    }

    private void setEndIconClearTextDefaults() {
        setEndIconDrawable(AppCompatResources.getDrawable(getContext(), R$drawable.mtrl_clear_text_button));
        setEndIconContentDescription(getResources().getText(R$string.clear_text_end_icon_content_description));
        setEndIconOnClickListener(new View.OnClickListener() {
            /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass8 */

            public void onClick(View view) {
                TextInputLayout.this.editText.setText((CharSequence) null);
            }
        });
        addOnEditTextAttachedListener(this.clearTextOnEditTextAttachedListener);
    }

    public void setTextInputAccessibilityDelegate(AccessibilityDelegate accessibilityDelegate) {
        EditText editText2 = this.editText;
        if (editText2 != null) {
            ViewCompat.setAccessibilityDelegate(editText2, accessibilityDelegate);
        }
    }

    private void dispatchOnEditTextAttached() {
        Iterator<OnEditTextAttachedListener> it = this.editTextAttachedListeners.iterator();
        while (it.hasNext()) {
            it.next().onEditTextAttached();
        }
    }

    private boolean hasStartIcon() {
        return getStartIconDrawable() != null;
    }

    private void applyStartIconTint() {
        applyIconTint(this.startIconView, this.hasStartIconTintList, this.startIconTintList, this.hasStartIconTintMode, this.startIconTintMode);
    }

    private boolean hasEndIcon() {
        return this.endIconMode != 0;
    }

    private void dispatchOnEndIconChanged(int i) {
        Iterator<OnEndIconChangedListener> it = this.endIconChangedListeners.iterator();
        while (it.hasNext()) {
            it.next().onEndIconChanged(i);
        }
    }

    private void applyEndIconTint() {
        applyIconTint(this.endIconView, this.hasEndIconTintList, this.endIconTintList, this.hasEndIconTintMode, this.endIconTintMode);
    }

    private void updateIconDummyDrawables() {
        if (this.editText != null) {
            if (hasStartIcon() && isStartIconVisible()) {
                this.startIconDummyDrawable = new ColorDrawable();
                this.startIconDummyDrawable.setBounds(0, 0, this.startIconView.getMeasuredWidth() - this.startIconView.getPaddingRight(), 1);
                Drawable[] compoundDrawablesRelative = TextViewCompat.getCompoundDrawablesRelative(this.editText);
                TextViewCompat.setCompoundDrawablesRelative(this.editText, this.startIconDummyDrawable, compoundDrawablesRelative[1], compoundDrawablesRelative[2], compoundDrawablesRelative[3]);
            } else if (this.startIconDummyDrawable != null) {
                Drawable[] compoundDrawablesRelative2 = TextViewCompat.getCompoundDrawablesRelative(this.editText);
                TextViewCompat.setCompoundDrawablesRelative(this.editText, null, compoundDrawablesRelative2[1], compoundDrawablesRelative2[2], compoundDrawablesRelative2[3]);
                this.startIconDummyDrawable = null;
            }
            if (hasEndIcon() && isEndIconVisible()) {
                if (this.endIconDummyDrawable == null) {
                    this.endIconDummyDrawable = new ColorDrawable();
                    this.endIconDummyDrawable.setBounds(0, 0, this.endIconView.getMeasuredWidth() - this.endIconView.getPaddingLeft(), 1);
                }
                Drawable[] compoundDrawablesRelative3 = TextViewCompat.getCompoundDrawablesRelative(this.editText);
                if (compoundDrawablesRelative3[2] != this.endIconDummyDrawable) {
                    this.originalEditTextEndDrawable = compoundDrawablesRelative3[2];
                }
                TextViewCompat.setCompoundDrawablesRelative(this.editText, compoundDrawablesRelative3[0], compoundDrawablesRelative3[1], this.endIconDummyDrawable, compoundDrawablesRelative3[3]);
            } else if (this.endIconDummyDrawable != null) {
                Drawable[] compoundDrawablesRelative4 = TextViewCompat.getCompoundDrawablesRelative(this.editText);
                if (compoundDrawablesRelative4[2] == this.endIconDummyDrawable) {
                    TextViewCompat.setCompoundDrawablesRelative(this.editText, compoundDrawablesRelative4[0], compoundDrawablesRelative4[1], this.originalEditTextEndDrawable, compoundDrawablesRelative4[3]);
                }
                this.endIconDummyDrawable = null;
            }
        }
    }

    private void applyIconTint(CheckableImageButton checkableImageButton, boolean z, ColorStateList colorStateList, boolean z2, PorterDuff.Mode mode) {
        Drawable drawable = checkableImageButton.getDrawable();
        if (drawable != null && (z || z2)) {
            drawable = DrawableCompat.wrap(drawable).mutate();
            if (z) {
                DrawableCompat.setTintList(drawable, colorStateList);
            }
            if (z2) {
                DrawableCompat.setTintMode(drawable, mode);
            }
        }
        if (checkableImageButton.getDrawable() != drawable) {
            checkableImageButton.setImageDrawable(drawable);
        }
    }

    private void setIconOnClickListener(View view, View.OnClickListener onClickListener) {
        view.setOnClickListener(onClickListener);
        boolean z = true;
        view.setFocusable(onClickListener != null);
        if (onClickListener == null) {
            z = false;
        }
        view.setClickable(z);
    }

    private void updateIconViewOnEditTextAttached(View view, int i, int i2) {
        ViewCompat.setPaddingRelative(view, getResources().getDimensionPixelSize(i), this.editText.getPaddingTop(), getResources().getDimensionPixelSize(i2), this.editText.getPaddingBottom());
        view.bringToFront();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean hasPasswordTransformation() {
        EditText editText2 = this.editText;
        return editText2 != null && (editText2.getTransformationMethod() instanceof PasswordTransformationMethod);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        EditText editText2 = this.editText;
        if (editText2 != null) {
            Rect rect = this.tmpRect;
            DescendantOffsetUtils.getDescendantRect(this, editText2, rect);
            updateBoxUnderlineBounds(rect);
            if (this.hintEnabled) {
                this.collapsingTextHelper.setCollapsedBounds(calculateCollapsedTextBounds(rect));
                this.collapsingTextHelper.setExpandedBounds(calculateExpandedTextBounds(rect));
                this.collapsingTextHelper.recalculate();
                if (cutoutEnabled() && !this.hintExpanded) {
                    openCutout();
                }
            }
        }
    }

    private void updateBoxUnderlineBounds(Rect rect) {
        MaterialShapeDrawable materialShapeDrawable = this.boxUnderline;
        if (materialShapeDrawable != null) {
            int i = rect.bottom;
            materialShapeDrawable.setBounds(rect.left, i - this.boxStrokeWidthFocusedPx, rect.right, i);
        }
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawHint(canvas);
        drawBoxUnderline(canvas);
    }

    private void drawHint(Canvas canvas) {
        if (this.hintEnabled) {
            this.collapsingTextHelper.draw(canvas);
        }
    }

    private void drawBoxUnderline(Canvas canvas) {
        MaterialShapeDrawable materialShapeDrawable = this.boxUnderline;
        if (materialShapeDrawable != null) {
            Rect bounds = materialShapeDrawable.getBounds();
            bounds.top = bounds.bottom - this.boxStrokeWidthPx;
            this.boxUnderline.draw(canvas);
        }
    }

    private void collapseHint(boolean z) {
        ValueAnimator valueAnimator = this.animator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.animator.cancel();
        }
        if (!z || !this.hintAnimationEnabled) {
            this.collapsingTextHelper.setExpansionFraction(1.0f);
        } else {
            animateToExpansionFraction(1.0f);
        }
        this.hintExpanded = false;
        if (cutoutEnabled()) {
            openCutout();
        }
    }

    private boolean cutoutEnabled() {
        return this.hintEnabled && !TextUtils.isEmpty(this.hint) && (this.boxBackground instanceof CutoutDrawable);
    }

    private void openCutout() {
        if (cutoutEnabled()) {
            RectF rectF = this.tmpRectF;
            this.collapsingTextHelper.getCollapsedTextActualBounds(rectF);
            applyCutoutPadding(rectF);
            rectF.offset((float) (-getPaddingLeft()), 0.0f);
            ((CutoutDrawable) this.boxBackground).setCutout(rectF);
        }
    }

    private void closeCutout() {
        if (cutoutEnabled()) {
            ((CutoutDrawable) this.boxBackground).removeCutout();
        }
    }

    private void applyCutoutPadding(RectF rectF) {
        float f = rectF.left;
        int i = this.boxLabelCutoutPaddingPx;
        rectF.left = f - ((float) i);
        rectF.top -= (float) i;
        rectF.right += (float) i;
        rectF.bottom += (float) i;
    }

    /* access modifiers changed from: package-private */
    public boolean cutoutIsOpen() {
        return cutoutEnabled() && ((CutoutDrawable) this.boxBackground).hasCutout();
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        if (!this.inDrawableStateChanged) {
            boolean z = true;
            this.inDrawableStateChanged = true;
            super.drawableStateChanged();
            int[] drawableState = getDrawableState();
            CollapsingTextHelper collapsingTextHelper2 = this.collapsingTextHelper;
            boolean state = collapsingTextHelper2 != null ? collapsingTextHelper2.setState(drawableState) | false : false;
            if (!ViewCompat.isLaidOut(this) || !isEnabled()) {
                z = false;
            }
            updateLabelState(z);
            updateEditTextBackground();
            updateTextInputBoxState();
            if (state) {
                invalidate();
            }
            this.inDrawableStateChanged = false;
        }
    }

    /* access modifiers changed from: package-private */
    public void updateTextInputBoxState() {
        TextView textView;
        EditText editText2;
        EditText editText3;
        if (this.boxBackground != null && this.boxBackgroundMode != 0) {
            boolean z = false;
            boolean z2 = isFocused() || ((editText3 = this.editText) != null && editText3.hasFocus());
            if (isHovered() || ((editText2 = this.editText) != null && editText2.isHovered())) {
                z = true;
            }
            if (!isEnabled()) {
                this.boxStrokeColor = this.disabledColor;
            } else if (this.indicatorViewController.errorShouldBeShown()) {
                this.boxStrokeColor = this.indicatorViewController.getErrorViewCurrentTextColor();
            } else if (this.counterOverflowed && (textView = this.counterView) != null) {
                this.boxStrokeColor = textView.getCurrentTextColor();
            } else if (z2) {
                this.boxStrokeColor = this.focusedStrokeColor;
            } else if (z) {
                this.boxStrokeColor = this.hoveredStrokeColor;
            } else {
                this.boxStrokeColor = this.defaultStrokeColor;
            }
            if ((z || z2) && isEnabled()) {
                this.boxStrokeWidthPx = this.boxStrokeWidthFocusedPx;
                adjustCornerSizeForStrokeWidth();
            } else {
                this.boxStrokeWidthPx = this.boxStrokeWidthDefaultPx;
                adjustCornerSizeForStrokeWidth();
            }
            if (this.boxBackgroundMode == 1) {
                if (!isEnabled()) {
                    this.boxBackgroundColor = this.disabledFilledBackgroundColor;
                } else if (z) {
                    this.boxBackgroundColor = this.hoveredFilledBackgroundColor;
                } else {
                    this.boxBackgroundColor = this.defaultFilledBackgroundColor;
                }
            }
            applyBoxAttributes();
        }
    }

    private void expandHint(boolean z) {
        ValueAnimator valueAnimator = this.animator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.animator.cancel();
        }
        if (!z || !this.hintAnimationEnabled) {
            this.collapsingTextHelper.setExpansionFraction(0.0f);
        } else {
            animateToExpansionFraction(0.0f);
        }
        if (cutoutEnabled() && ((CutoutDrawable) this.boxBackground).hasCutout()) {
            closeCutout();
        }
        this.hintExpanded = true;
    }

    /* access modifiers changed from: package-private */
    public void animateToExpansionFraction(float f) {
        if (this.collapsingTextHelper.getExpansionFraction() != f) {
            if (this.animator == null) {
                this.animator = new ValueAnimator();
                this.animator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
                this.animator.setDuration(167L);
                this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    /* class com.google.android.material.textfield.TextInputLayout.AnonymousClass9 */

                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        TextInputLayout.this.collapsingTextHelper.setExpansionFraction(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
            this.animator.setFloatValues(this.collapsingTextHelper.getExpansionFraction(), f);
            this.animator.start();
        }
    }

    /* access modifiers changed from: package-private */
    public final boolean isHintExpanded() {
        return this.hintExpanded;
    }

    /* access modifiers changed from: package-private */
    public final boolean isHelperTextDisplayed() {
        return this.indicatorViewController.helperTextIsDisplayed();
    }

    /* access modifiers changed from: package-private */
    public final int getHintCurrentCollapsedTextColor() {
        return this.collapsingTextHelper.getCurrentCollapsedTextColor();
    }

    /* access modifiers changed from: package-private */
    public final float getHintCollapsedTextHeight() {
        return this.collapsingTextHelper.getCollapsedTextHeight();
    }

    /* access modifiers changed from: package-private */
    public final int getErrorTextCurrentColor() {
        return this.indicatorViewController.getErrorViewCurrentTextColor();
    }

    public static class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final TextInputLayout layout;

        public AccessibilityDelegate(TextInputLayout textInputLayout) {
            this.layout = textInputLayout;
        }

        @Override // androidx.core.view.AccessibilityDelegateCompat
        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfoCompat);
            EditText editText = this.layout.getEditText();
            Editable text = editText != null ? editText.getText() : null;
            CharSequence hint = this.layout.getHint();
            CharSequence error = this.layout.getError();
            CharSequence counterOverflowDescription = this.layout.getCounterOverflowDescription();
            boolean z = !TextUtils.isEmpty(text);
            boolean z2 = !TextUtils.isEmpty(hint);
            boolean z3 = !TextUtils.isEmpty(error);
            boolean z4 = false;
            boolean z5 = z3 || !TextUtils.isEmpty(counterOverflowDescription);
            if (z) {
                accessibilityNodeInfoCompat.setText(text);
            } else if (z2) {
                accessibilityNodeInfoCompat.setText(hint);
            }
            if (z2) {
                accessibilityNodeInfoCompat.setHintText(hint);
                if (!z && z2) {
                    z4 = true;
                }
                accessibilityNodeInfoCompat.setShowingHintText(z4);
            }
            if (z5) {
                if (z3) {
                    counterOverflowDescription = error;
                }
                accessibilityNodeInfoCompat.setError(counterOverflowDescription);
                accessibilityNodeInfoCompat.setContentInvalid(true);
            }
        }

        @Override // androidx.core.view.AccessibilityDelegateCompat
        public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            super.onPopulateAccessibilityEvent(view, accessibilityEvent);
            EditText editText = this.layout.getEditText();
            CharSequence text = editText != null ? editText.getText() : null;
            if (TextUtils.isEmpty(text)) {
                text = this.layout.getHint();
            }
            if (!TextUtils.isEmpty(text)) {
                accessibilityEvent.getText().add(text);
            }
        }
    }
}
