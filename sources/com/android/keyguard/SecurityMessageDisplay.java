package com.android.keyguard;

import android.content.res.ColorStateList;

public interface SecurityMessageDisplay {
    void setDefaultMessage(int i);

    void setDefaultMessageColor(int i);

    void setMessage(int i);

    void setMessage(CharSequence charSequence);

    void setNextMessageColor(ColorStateList colorStateList);
}
