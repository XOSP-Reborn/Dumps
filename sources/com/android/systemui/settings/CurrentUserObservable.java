package com.android.systemui.settings;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class CurrentUserObservable {
    private final MutableLiveData<Integer> mCurrentUser = new MutableLiveData<Integer>() {
        /* class com.android.systemui.settings.CurrentUserObservable.AnonymousClass1 */

        /* access modifiers changed from: protected */
        @Override // androidx.lifecycle.LiveData
        public void onActive() {
            super.onActive();
            CurrentUserObservable.this.mTracker.startTracking();
        }

        /* access modifiers changed from: protected */
        @Override // androidx.lifecycle.LiveData
        public void onInactive() {
            super.onInactive();
            CurrentUserObservable.this.mTracker.startTracking();
        }
    };
    private final CurrentUserTracker mTracker;

    public CurrentUserObservable(Context context) {
        this.mTracker = new CurrentUserTracker(context) {
            /* class com.android.systemui.settings.CurrentUserObservable.AnonymousClass2 */

            @Override // com.android.systemui.settings.CurrentUserTracker
            public void onUserSwitched(int i) {
                CurrentUserObservable.this.mCurrentUser.setValue(Integer.valueOf(i));
            }
        };
    }

    public LiveData<Integer> getCurrentUser() {
        if (this.mCurrentUser.getValue() == null) {
            this.mCurrentUser.setValue(Integer.valueOf(this.mTracker.getCurrentUserId()));
        }
        return this.mCurrentUser;
    }
}
