package com.android.settingslib.inputmethod;

import android.content.Context;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Locale;

public class InputMethodSubtypePreference extends SwitchWithNoTextPreference {
    private final boolean mIsSystemLanguage;
    private final boolean mIsSystemLocale;

    @VisibleForTesting
    InputMethodSubtypePreference(Context context, String str, CharSequence charSequence, Locale locale, Locale locale2) {
        super(context);
        boolean z = false;
        setPersistent(false);
        setKey(str);
        setTitle(charSequence);
        if (locale == null) {
            this.mIsSystemLocale = false;
            this.mIsSystemLanguage = false;
            return;
        }
        this.mIsSystemLocale = locale.equals(locale2);
        this.mIsSystemLanguage = (this.mIsSystemLocale || TextUtils.equals(locale.getLanguage(), locale2.getLanguage())) ? true : z;
    }
}
