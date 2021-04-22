package com.sonymobile.systemui.gameservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.android.internal.util.FunctionalUtils;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.phone.GameButton;
import com.android.systemui.statusbar.phone.NavigationBarLockController;
import com.sonymobile.systemui.gameservice.GameService;
import com.sonymobile.systemui.gameservice.IGameService;
import java.util.Objects;

public class GameService extends Service {
    private final IGameService.Stub mBinder = new IGameService.Stub() {
        /* class com.sonymobile.systemui.gameservice.GameService.AnonymousClass3 */
        private Context mContext = GameService.this;

        private String getCallingPackage() {
            return this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        }

        @Override // com.sonymobile.systemui.gameservice.IGameService
        public void lockSetReLockDelay(IBinder iBinder, int i, long j) {
            GameService.this.mHandler.post(new Runnable(iBinder, i, j) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$3$Piqmg4XlAazQqbLMOZD4WydiIhg */
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ long f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    GameService.AnonymousClass3.this.lambda$lockSetReLockDelay$0$GameService$3(this.f$1, this.f$2, this.f$3);
                }
            });
        }

        public /* synthetic */ void lambda$lockSetReLockDelay$0$GameService$3(IBinder iBinder, int i, long j) {
            if (GameService.this.mLockClients.check(iBinder, i)) {
                GameService.this.getLockController(i).setReLockDelay(j);
            }
        }

        @Override // com.sonymobile.systemui.gameservice.IGameService
        public void lockSetEnabled(IBinder iBinder, int i, boolean z) {
            GameService.this.mHandler.post(new Runnable(z, iBinder, i) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$3$21GgSDne98B9bzwNaIlzrxqXc */
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ IBinder f$2;
                private final /* synthetic */ int f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    GameService.AnonymousClass3.this.lambda$lockSetEnabled$1$GameService$3(this.f$1, this.f$2, this.f$3);
                }
            });
        }

        public /* synthetic */ void lambda$lockSetEnabled$1$GameService$3(boolean z, IBinder iBinder, int i) {
            if (!z) {
                GameService.this.mLockClients.remove(iBinder, i);
            } else if (GameService.this.mLockClients.put((ClientList) iBinder, (IBinder) i)) {
                GameService.this.getLockController(i).setEnabled(true);
            }
        }

        @Override // com.sonymobile.systemui.gameservice.IGameService
        public void gameSetIcon(IBinder iBinder, int i, int i2) {
            GameService.this.mHandler.post(new Runnable(iBinder, i, getCallingPackage(), i2) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$3$rGadQN2PCMWgFWeRzfIO_DA93Xg */
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ String f$3;
                private final /* synthetic */ int f$4;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                }

                public final void run() {
                    GameService.AnonymousClass3.this.lambda$gameSetIcon$2$GameService$3(this.f$1, this.f$2, this.f$3, this.f$4);
                }
            });
        }

        public /* synthetic */ void lambda$gameSetIcon$2$GameService$3(IBinder iBinder, int i, String str, int i2) {
            if (GameService.this.mGameClients.check(iBinder, i) && GameService.this.mGameClients.put((ClientList) iBinder, (IBinder) i)) {
                GameService.this.getGameButton(i).setDrawable(str, i2, 0, 0, false);
            }
        }

        @Override // com.sonymobile.systemui.gameservice.IGameService
        public void gameSetColoredIcon(IBinder iBinder, int i, int i2, int i3, int i4) {
            GameService.this.mHandler.post(new Runnable(iBinder, i, getCallingPackage(), i2, i3, i4) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$3$Z_TRwRAbTGj1Y3mELpJI5PZUDxk */
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ String f$3;
                private final /* synthetic */ int f$4;
                private final /* synthetic */ int f$5;
                private final /* synthetic */ int f$6;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                    this.f$5 = r6;
                    this.f$6 = r7;
                }

                public final void run() {
                    GameService.AnonymousClass3.this.lambda$gameSetColoredIcon$3$GameService$3(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
                }
            });
        }

        public /* synthetic */ void lambda$gameSetColoredIcon$3$GameService$3(IBinder iBinder, int i, String str, int i2, int i3, int i4) {
            if (GameService.this.mGameClients.check(iBinder, i) && GameService.this.mGameClients.put((ClientList) iBinder, (IBinder) i)) {
                GameService.this.getGameButton(i).setDrawable(str, i2, i3, i4, true);
            }
        }

        @Override // com.sonymobile.systemui.gameservice.IGameService
        public void gameUnsetIcon(IBinder iBinder, int i) {
            GameService.this.mHandler.post(new Runnable(iBinder, i) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$3$GPLklYgMP5z36Q_RCqkzbuehrdk */
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    GameService.AnonymousClass3.this.lambda$gameUnsetIcon$4$GameService$3(this.f$1, this.f$2);
                }
            });
        }

        public /* synthetic */ void lambda$gameUnsetIcon$4$GameService$3(IBinder iBinder, int i) {
            GameService.this.mGameClients.remove(iBinder, i);
        }

        @Override // com.sonymobile.systemui.gameservice.IGameService
        public void gameSetOnGestureListener(IBinder iBinder, int i, IGameServiceOnGestureListener iGameServiceOnGestureListener) {
            GameService.this.mHandler.post(new Runnable(iBinder, i, iGameServiceOnGestureListener) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$3$eOW6RyNdN7dgHm_sYNbNll20Sk0 */
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ IGameServiceOnGestureListener f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    GameService.AnonymousClass3.this.lambda$gameSetOnGestureListener$5$GameService$3(this.f$1, this.f$2, this.f$3);
                }
            });
        }

        public /* synthetic */ void lambda$gameSetOnGestureListener$5$GameService$3(IBinder iBinder, int i, IGameServiceOnGestureListener iGameServiceOnGestureListener) {
            if (GameService.this.mGameClients.check(iBinder, i)) {
                GameButton gameButton = GameService.this.getGameButton(i);
                GameService gameService = GameService.this;
                gameButton.setOnTouchListener(new MyGestureDetector(this.mContext, new MyGestureListener(i, iGameServiceOnGestureListener)));
            }
        }

        @Override // com.sonymobile.systemui.gameservice.IGameService
        public void gameUnsetOnGestureListener(IBinder iBinder, int i) {
            GameService.this.mHandler.post(new Runnable(iBinder, i) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$3$rkx607LmT96JtoeYIJWLwlTD0hk */
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    GameService.AnonymousClass3.this.lambda$gameUnsetOnGestureListener$6$GameService$3(this.f$1, this.f$2);
                }
            });
        }

        public /* synthetic */ void lambda$gameUnsetOnGestureListener$6$GameService$3(IBinder iBinder, int i) {
            if (GameService.this.mGameClients.check(iBinder, i)) {
                GameService.this.getGameButton(i).setOnTouchListener(null);
            }
        }
    };
    private final ClientList mGameClients = new ClientList() {
        /* class com.sonymobile.systemui.gameservice.GameService.AnonymousClass2 */

        /* access modifiers changed from: protected */
        @Override // com.sonymobile.systemui.gameservice.GameService.ClientList
        public void onClientRemoved(int i) {
            GameService.this.getGameButton(i).setDrawable(null, 0, 0, 0, false);
            GameService.this.getGameButton(i).setOnTouchListener(null);
        }
    };
    private Handler mHandler;
    private final ClientList mLockClients = new ClientList() {
        /* class com.sonymobile.systemui.gameservice.GameService.AnonymousClass1 */

        /* access modifiers changed from: protected */
        @Override // com.sonymobile.systemui.gameservice.GameService.ClientList
        public void onClientRemoved(int i) {
            GameService.this.getLockController(i).setEnabled(false);
        }
    };
    private NavigationBarController mNavBarController;

    /* access modifiers changed from: private */
    public class Client implements IBinder.DeathRecipient {
        private final int mDisplayId;
        private final ClientList mList;
        private final IBinder mToken;

        Client(ClientList clientList, int i, IBinder iBinder) throws RemoteException {
            this.mList = clientList;
            this.mDisplayId = i;
            this.mToken = iBinder;
            this.mToken.linkToDeath(this, 0);
        }

        public void binderDied() {
            Log.w("GameService", "client died");
            GameService.this.mHandler.post(new Runnable() {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$Client$3Gjg6RIMCBjD7zEJyuhbOrdlbfc */

                public final void run() {
                    GameService.Client.this.lambda$binderDied$0$GameService$Client();
                }
            });
        }

        public /* synthetic */ void lambda$binderDied$0$GameService$Client() {
            this.mList.remove(this.mDisplayId);
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private void destroy() {
            this.mToken.unlinkToDeath(this, 0);
        }
    }

    /* access modifiers changed from: private */
    public abstract class ClientList extends SparseArray<Client> {
        /* access modifiers changed from: protected */
        public abstract void onClientRemoved(int i);

        private ClientList() {
        }

        public void clear() {
            for (int i = 0; i < size(); i++) {
                Client client = (Client) get(keyAt(i));
                client.destroy();
                onClientRemoved(client.mDisplayId);
            }
            super.clear();
        }

        public void remove(int i) {
            super.remove(i);
            onClientRemoved(i);
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean check(IBinder iBinder, int i) {
            if (indexOfKey(i) < 0 || ((Client) get(i)).mToken == iBinder) {
                return true;
            }
            Log.w("GameService", "only one client allowed");
            return false;
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean put(IBinder iBinder, int i) {
            if (iBinder == null) {
                Log.w("GameService", "null token is not allowed");
                return false;
            }
            try {
                super.put(i, new Client(this, i, iBinder));
                return true;
            } catch (RemoteException unused) {
                Log.w("GameService", "error adding client");
                return false;
            }
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private void remove(IBinder iBinder, int i) {
            Client client = (Client) get(i);
            if (client != null && client.mToken == iBinder) {
                client.destroy();
                remove(client.mDisplayId);
            }
        }
    }

    /* access modifiers changed from: private */
    public final class MyGestureDetector extends GestureDetector implements View.OnTouchListener {
        public MyGestureDetector(Context context, GestureDetector.OnGestureListener onGestureListener) {
            super(context, onGestureListener);
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            return onTouchEvent(motionEvent);
        }
    }

    /* access modifiers changed from: private */
    public final class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final int mDisplayId;
        private final IGameServiceOnGestureListener mListener;

        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        public MyGestureListener(int i, IGameServiceOnGestureListener iGameServiceOnGestureListener) {
            this.mDisplayId = i;
            this.mListener = iGameServiceOnGestureListener;
        }

        private void onGesture(FunctionalUtils.ThrowingRunnable throwingRunnable, int i) {
            GameService.this.getGameButton(this.mDisplayId).getCurrentView().performHapticFeedback(i);
            GameService.this.mHandler.post(new Runnable(throwingRunnable) {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$GameService$MyGestureListener$YFm7f3AKGpzLozXqIAjcAviD68U */
                private final /* synthetic */ FunctionalUtils.ThrowingRunnable f$0;

                {
                    this.f$0 = r1;
                }

                public final void run() {
                    GameService.MyGestureListener.lambda$onGesture$0(this.f$0);
                }
            });
        }

        static /* synthetic */ void lambda$onGesture$0(FunctionalUtils.ThrowingRunnable throwingRunnable) {
            try {
                throwingRunnable.runOrThrow();
            } catch (Exception e) {
                Log.w("GameService", "client gesture listener failed: " + e);
            }
        }

        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            IGameServiceOnGestureListener iGameServiceOnGestureListener = this.mListener;
            Objects.requireNonNull(iGameServiceOnGestureListener);
            onGesture(new FunctionalUtils.ThrowingRunnable() {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$AHbNOSMYlukXXck6QzZFMJpQmxk */

                public final void runOrThrow() {
                    IGameServiceOnGestureListener.this.onGameButtonSingleTap();
                }
            }, 1);
            return true;
        }

        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            if (motionEvent.getActionMasked() == 1) {
                IGameServiceOnGestureListener iGameServiceOnGestureListener = this.mListener;
                Objects.requireNonNull(iGameServiceOnGestureListener);
                onGesture(new FunctionalUtils.ThrowingRunnable() {
                    /* class com.sonymobile.systemui.gameservice.$$Lambda$kDl_8hIwBRFk2fYDGKIsNtj1PmQ */

                    public final void runOrThrow() {
                        IGameServiceOnGestureListener.this.onGameButtonDoubleTap();
                    }
                }, 1);
            }
            return true;
        }

        public void onLongPress(MotionEvent motionEvent) {
            IGameServiceOnGestureListener iGameServiceOnGestureListener = this.mListener;
            Objects.requireNonNull(iGameServiceOnGestureListener);
            onGesture(new FunctionalUtils.ThrowingRunnable() {
                /* class com.sonymobile.systemui.gameservice.$$Lambda$OiV5P6maFYtyjaMH6e7Ba2vMRNo */

                public final void runOrThrow() {
                    IGameServiceOnGestureListener.this.onGameButtonLongPress();
                }
            }, 0);
        }
    }

    public void onCreate() {
        this.mNavBarController = (NavigationBarController) Dependency.get(NavigationBarController.class);
        this.mHandler = new Handler();
    }

    public void onDestroy() {
        this.mLockClients.clear();
        this.mGameClients.clear();
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private NavigationBarLockController getLockController(int i) {
        return this.mNavBarController.getNavigationBarView(i).getLockController();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private GameButton getGameButton(int i) {
        return this.mNavBarController.getNavigationBarView(i).getGameButton();
    }
}
