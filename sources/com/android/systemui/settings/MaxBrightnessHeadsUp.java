package com.android.systemui.settings;

public interface MaxBrightnessHeadsUp {

    public interface Listener {
        void onUserResponded();
    }

    void setOnUserRespondedListener(Listener listener);

    void show();

    void showWithGameEnhancer();
}
