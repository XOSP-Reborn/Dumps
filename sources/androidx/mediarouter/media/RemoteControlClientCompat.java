package androidx.mediarouter.media;

/* access modifiers changed from: package-private */
public abstract class RemoteControlClientCompat {

    public static final class PlaybackInfo {
        public int playbackStream = 3;
        public int playbackType = 1;
        public int volume;
        public int volumeHandling = 0;
        public int volumeMax;
    }

    public interface VolumeCallback {
    }

    public abstract void setPlaybackInfo(PlaybackInfo playbackInfo);
}
