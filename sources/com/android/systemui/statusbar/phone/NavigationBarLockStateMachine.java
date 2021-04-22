package com.android.systemui.statusbar.phone;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

/* access modifiers changed from: package-private */
public class NavigationBarLockStateMachine extends StateMachine {
    static final boolean DEBUG = Log.isLoggable("NavBarLockStateMachine", 3);
    private DefaultState mDefaultState = new DefaultState();
    private LockedState mLockedState = new LockedState();
    private PausedState mPausedState = new PausedState();
    private long mReLockDelay;
    private UnlockedState mUnlockedState = new UnlockedState();
    private final Runnable mUpdater;

    NavigationBarLockStateMachine(Handler handler, Runnable runnable) {
        super("NavBarLockStateMachine", handler);
        setDbg(DEBUG);
        this.mUpdater = runnable;
        addState(this.mDefaultState);
        addState(this.mLockedState);
        addState(this.mUnlockedState);
        addState(this.mPausedState);
        setInitialState(this.mDefaultState);
    }

    /* access modifiers changed from: protected */
    public void onQuitting() {
        this.mUpdater.run();
    }

    /* access modifiers changed from: protected */
    public void unhandledMessage(Message message) {
        int i = message.what;
        if (i != 1) {
            if (i == 7) {
                this.mUpdater.run();
            } else if (DEBUG) {
                log("Unhandled message " + message);
            }
        } else if (message.arg1 == 0) {
            transitionTo(this.mDefaultState);
        }
    }

    /* access modifiers changed from: package-private */
    public void setReLockDelay(long j) {
        this.mReLockDelay = j;
    }

    /* access modifiers changed from: package-private */
    public void unlock() {
        sendMessage(2);
    }

    /* access modifiers changed from: package-private */
    public void windowHidden() {
        sendMessage(3);
    }

    /* access modifiers changed from: package-private */
    public void preRequisitesChanged(boolean z) {
        sendMessage(1, z ? 1 : 0);
    }

    /* access modifiers changed from: package-private */
    public void touchPressed() {
        sendMessage(5);
    }

    /* access modifiers changed from: package-private */
    public void touchReleased() {
        sendMessage(6);
    }

    /* access modifiers changed from: package-private */
    public boolean isLocked() {
        return getCurrentState() == this.mLockedState;
    }

    class DefaultState extends State {
        private boolean mPreReqMet;
        private boolean mTouchPressed;

        DefaultState() {
        }

        public void enter() {
            this.mPreReqMet = false;
            this.mTouchPressed = false;
            NavigationBarLockStateMachine.this.sendMessage(7);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            boolean z = false;
            if (i == 1) {
                if (message.arg1 == 1) {
                    z = true;
                }
                this.mPreReqMet = z;
            } else if (i == 5) {
                this.mTouchPressed = true;
            } else if (i != 6) {
                return false;
            } else {
                this.mTouchPressed = false;
            }
            if (this.mPreReqMet && !this.mTouchPressed) {
                NavigationBarLockStateMachine navigationBarLockStateMachine = NavigationBarLockStateMachine.this;
                navigationBarLockStateMachine.transitionTo(navigationBarLockStateMachine.mLockedState);
            }
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class LockedState extends State {
        LockedState() {
        }

        public void enter() {
            NavigationBarLockStateMachine.this.sendMessage(7);
        }

        public void exit() {
            NavigationBarLockStateMachine.this.sendMessage(7);
        }

        public boolean processMessage(Message message) {
            if (message.what != 2) {
                return false;
            }
            NavigationBarLockStateMachine navigationBarLockStateMachine = NavigationBarLockStateMachine.this;
            navigationBarLockStateMachine.transitionTo(navigationBarLockStateMachine.mUnlockedState);
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class UnlockedState extends State {
        UnlockedState() {
        }

        public void enter() {
            NavigationBarLockStateMachine navigationBarLockStateMachine = NavigationBarLockStateMachine.this;
            navigationBarLockStateMachine.sendMessageDelayed(4, navigationBarLockStateMachine.mReLockDelay);
        }

        public void exit() {
            NavigationBarLockStateMachine.this.removeMessages(4);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 5) {
                NavigationBarLockStateMachine navigationBarLockStateMachine = NavigationBarLockStateMachine.this;
                navigationBarLockStateMachine.transitionTo(navigationBarLockStateMachine.mPausedState);
                return true;
            } else if (i == 4) {
                NavigationBarLockStateMachine.this.sendMessage(3);
                return true;
            } else if (i != 3) {
                return false;
            } else {
                NavigationBarLockStateMachine navigationBarLockStateMachine2 = NavigationBarLockStateMachine.this;
                navigationBarLockStateMachine2.transitionTo(navigationBarLockStateMachine2.mLockedState);
                return true;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public class PausedState extends State {
        PausedState() {
        }

        public boolean processMessage(Message message) {
            if (message.what != 6) {
                return false;
            }
            NavigationBarLockStateMachine navigationBarLockStateMachine = NavigationBarLockStateMachine.this;
            navigationBarLockStateMachine.transitionTo(navigationBarLockStateMachine.mUnlockedState);
            return true;
        }
    }
}
