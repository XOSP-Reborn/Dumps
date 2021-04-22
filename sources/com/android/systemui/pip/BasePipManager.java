package com.android.systemui.pip;

import android.content.Context;
import android.content.res.Configuration;
import java.io.PrintWriter;

public interface BasePipManager {
    default void dump(PrintWriter printWriter) {
    }

    default void expandPip() {
    }

    default void hidePipMenu(Runnable runnable, Runnable runnable2) {
    }

    void initialize(Context context);

    void onConfigurationChanged(Configuration configuration);

    void showPictureInPictureMenu();
}
