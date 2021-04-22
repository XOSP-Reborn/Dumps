package com.sonymobile.keyguard.aod;

import android.content.Context;
import android.content.SharedPreferences;

public class PhotoPlaybackSharedPreferences {
    public static void setPhotoplaybackSharedPrefOobeShown(Context context) {
        SharedPreferences.Editor edit = context.getSharedPreferences("PhotoPlaybackSharedPref", 0).edit();
        edit.putBoolean("OOBE_SHOWN", true);
        edit.commit();
    }

    public static boolean getPhotoplaybackSharedPrefOobeShown(Context context) {
        return context.getSharedPreferences("PhotoPlaybackSharedPref", 0).getBoolean("OOBE_SHOWN", false);
    }

    public static void setPhotoplaybackSharedPrefIntroShown(Context context) {
        SharedPreferences.Editor edit = context.getSharedPreferences("PhotoPlaybackSharedPref", 0).edit();
        edit.putBoolean("INTRO_SHOWN", true);
        edit.commit();
    }

    public static boolean getPhotoplaybackSharedPrefIntroShown(Context context) {
        return context.getSharedPreferences("PhotoPlaybackSharedPref", 0).getBoolean("INTRO_SHOWN", false);
    }

    public static void setPhotoplaybackSharedPrefAlbumShown(Context context) {
        SharedPreferences.Editor edit = context.getSharedPreferences("PhotoPlaybackSharedPref", 0).edit();
        edit.putBoolean("ALBUM_SHOWN", true);
        edit.commit();
    }

    public static boolean getPhotoplaybackSharedPrefAlbumShown(Context context) {
        return context.getSharedPreferences("PhotoPlaybackSharedPref", 0).getBoolean("ALBUM_SHOWN", false);
    }
}
