package com.android.systemui.media;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.IAudioService;
import android.media.IRingtonePlayer;
import android.media.Ringtone;
import android.media.VolumeShaper;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.SystemUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class RingtonePlayer extends SystemUI {
    private final NotificationPlayer mAsyncPlayer = new NotificationPlayer("RingtonePlayer");
    private IAudioService mAudioService;
    private IRingtonePlayer mCallback = new IRingtonePlayer.Stub() {
        /* class com.android.systemui.media.RingtonePlayer.AnonymousClass1 */

        public void play(IBinder iBinder, Uri uri, AudioAttributes audioAttributes, float f, boolean z) throws RemoteException {
            playWithVolumeShaping(iBinder, uri, audioAttributes, f, z, null);
        }

        public void playWithVolumeShaping(IBinder iBinder, Uri uri, AudioAttributes audioAttributes, float f, boolean z, VolumeShaper.Configuration configuration) throws RemoteException {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(iBinder);
                if (client == null) {
                    client = new Client(iBinder, uri, Binder.getCallingUserHandle(), audioAttributes, configuration);
                    iBinder.linkToDeath(client, 0);
                    RingtonePlayer.this.mClients.put(iBinder, client);
                }
            }
            client.mRingtone.setLooping(z);
            client.mRingtone.setVolume(f);
            client.mRingtone.play();
        }

        public void stop(IBinder iBinder) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.remove(iBinder);
            }
            if (client != null) {
                client.mToken.unlinkToDeath(client, 0);
                client.mRingtone.stop();
            }
        }

        public boolean isPlaying(IBinder iBinder) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(iBinder);
            }
            if (client != null) {
                return client.mRingtone.isPlaying();
            }
            return false;
        }

        public void setPlaybackProperties(IBinder iBinder, float f, boolean z) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(iBinder);
            }
            if (client != null) {
                client.mRingtone.setVolume(f);
                client.mRingtone.setLooping(z);
            }
        }

        public void playAsync(Uri uri, UserHandle userHandle, boolean z, AudioAttributes audioAttributes) {
            if (Binder.getCallingUid() == 1000) {
                if (UserHandle.ALL.equals(userHandle)) {
                    userHandle = UserHandle.SYSTEM;
                }
                RingtonePlayer.this.mAsyncPlayer.play(RingtonePlayer.this.getContextForUser(userHandle), uri, z, audioAttributes);
                return;
            }
            throw new SecurityException("Async playback only available from system UID.");
        }

        public void stopAsync() {
            if (Binder.getCallingUid() == 1000) {
                RingtonePlayer.this.mAsyncPlayer.stop();
                return;
            }
            throw new SecurityException("Async playback only available from system UID.");
        }

        public String getTitle(Uri uri) {
            return Ringtone.getTitle(RingtonePlayer.this.getContextForUser(Binder.getCallingUserHandle()), uri, false, false);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:26:0x0067, code lost:
            r7 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:27:0x0068, code lost:
            if (r0 != null) goto L_0x006a;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:29:?, code lost:
            r0.close();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:30:0x006e, code lost:
            r0 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:0x006f, code lost:
            r6.addSuppressed(r0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x0072, code lost:
            throw r7;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public android.os.ParcelFileDescriptor openRingtone(android.net.Uri r7) {
            /*
            // Method dump skipped, instructions count: 138
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.media.RingtonePlayer.AnonymousClass1.openRingtone(android.net.Uri):android.os.ParcelFileDescriptor");
        }
    };
    private final HashMap<IBinder, Client> mClients = new HashMap<>();

    @Override // com.android.systemui.SystemUI
    public void start() {
        this.mAsyncPlayer.setUsesWakeLock(this.mContext);
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        try {
            this.mAudioService.setRingtonePlayer(this.mCallback);
        } catch (RemoteException e) {
            Log.e("RingtonePlayer", "Problem registering RingtonePlayer: " + e);
        }
    }

    /* access modifiers changed from: private */
    public class Client implements IBinder.DeathRecipient {
        private final Ringtone mRingtone;
        private final IBinder mToken;

        Client(IBinder iBinder, Uri uri, UserHandle userHandle, AudioAttributes audioAttributes, VolumeShaper.Configuration configuration) {
            this.mToken = iBinder;
            this.mRingtone = new Ringtone(RingtonePlayer.this.getContextForUser(userHandle), false);
            this.mRingtone.setAudioAttributes(audioAttributes);
            this.mRingtone.setUri(uri, configuration);
        }

        public void binderDied() {
            synchronized (RingtonePlayer.this.mClients) {
                RingtonePlayer.this.mClients.remove(this.mToken);
            }
            this.mRingtone.stop();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Context getContextForUser(UserHandle userHandle) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // com.android.systemui.SystemUI
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Clients:");
        synchronized (this.mClients) {
            for (Client client : this.mClients.values()) {
                printWriter.print("  mToken=");
                printWriter.print(client.mToken);
                printWriter.print(" mUri=");
                printWriter.println(client.mRingtone.getUri());
            }
        }
    }
}
