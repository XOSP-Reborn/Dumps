package com.sonymobile.keyguard.aod;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;

public class PhotoPlaybackProviderUtils {
    private static final String[] mOsvDeviceNames = {"H82", "H92", "H81", "H91", "H83", "H93"};

    public static Cursor queryForPhoto(Context context) {
        return getContextForUser(context).getContentResolver().query(PhotoPlaybackProviderContract$Uris.PHOTO, null, null, null, null);
    }

    public static Cursor queryForStatus(Context context) {
        return getContextForUser(context).getContentResolver().query(PhotoPlaybackProviderContract$Uris.STATUS, null, null, null, null);
    }

    public static boolean isPhotoPlaybackContentProviderAvailable(Context context) {
        ContentProviderClient acquireContentProviderClient = getContextForUser(context).getContentResolver().acquireContentProviderClient(PhotoPlaybackProviderContract$Uris.PHOTO);
        if (acquireContentProviderClient != null) {
            acquireContentProviderClient.close();
            return true;
        }
        Log.d("PhotoPlaybackProviderUtils", "Photo playback Content Provider is not available");
        return false;
    }

    private static Context getContextForUser(Context context) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, Binder.getCallingUserHandle());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isOwner() {
        return KeyguardUpdateMonitor.getCurrentUser() == 0;
    }

    public static boolean isPhotoPlaybackApplicationEnabled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        try {
            return packageManager.getApplicationInfoAsUser("com.sonymobile.recallplaybackphotos", 0, currentUser).enabled;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.v("PhotoPlaybackProviderUtils", "com.sonymobile.recallplaybackphotos not found as user:" + currentUser);
            return false;
        }
    }

    public static boolean isAlbumApplicationEnabled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        try {
            return packageManager.getApplicationInfoAsUser("com.sonyericsson.album", 0, currentUser).enabled;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.v("PhotoPlaybackProviderUtils", "com.sonyericsson.album not found as user:" + currentUser);
            return false;
        }
    }

    public static String getAlbumApplicationName(Context context) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo("com.sonyericsson.album", 0);
        } catch (PackageManager.NameNotFoundException unused) {
            applicationInfo = null;
        }
        return (String) (applicationInfo != null ? context.getPackageManager().getApplicationLabel(applicationInfo) : "");
    }

    public static boolean hasContentScheme(Uri uri) {
        return uri.getScheme().equals("content");
    }

    public static boolean hasDemoScheme(Uri uri) {
        return uri.getScheme().equals("demo");
    }

    private static boolean isOsvDeviceVersion() {
        String str = SystemProperties.get("ro.semc.product.device", "default");
        for (String str2 : mOsvDeviceNames) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPhotoPlaybackEnabled(Context context) {
        if (Settings.Secure.getInt(getContextForUser(context).getContentResolver(), "somc.lockscreen.key_photoplayback_enabled", !isOsvDeviceVersion() ? 1 : 0) != 0) {
            return true;
        }
        return false;
    }

    public static int getPhotoPlaybackMode(Context context) {
        return Settings.Secure.getInt(getContextForUser(context).getContentResolver(), "somc.lockscreen.key_photoplayback_mode", 0);
    }

    public static boolean isPhotoPlaybackApplicationInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.sonymobile.recallplaybackphotos", 1);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.v("PhotoPlaybackProviderUtils", "com.sonymobile.recallplaybackphotos not found");
            return false;
        }
    }

    public static boolean isIdiInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.sonymobile.indeviceintelligence", 1);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.v("PhotoPlaybackProviderUtils", "com.sonymobile.indeviceintelligence not found");
            return false;
        }
    }
}
