package com.sonymobile.runtimeskinning;

import android.content.res.AssetManager;
import android.os.RemoteException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class AssetManagerUtils {
    private static Method sAddAssetPath;
    private static Constructor<AssetManager> sAssetManagerConstructor;

    public static AssetManager getAssetManager() throws RemoteException {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        try {
            if (sAssetManagerConstructor == null) {
                sAssetManagerConstructor = ReflectionUtils.getConstructor(AssetManager.class, exceptionHandler);
            }
            if (sAssetManagerConstructor != null) {
                return sAssetManagerConstructor.newInstance(new Object[0]);
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            exceptionHandler.uncaughtException(Thread.currentThread(), e);
        }
        exceptionHandler.reThrow();
        return null;
    }

    public static int addAssetPath(AssetManager assetManager, String str, ExceptionHandler exceptionHandler) {
        if (sAddAssetPath == null) {
            sAddAssetPath = ReflectionUtils.getMethod(exceptionHandler, AssetManager.class, "addAssetPath", Integer.TYPE, String.class);
        }
        Integer num = (Integer) ReflectionUtils.invokeMethod(sAddAssetPath, assetManager, exceptionHandler, Integer.class, str);
        if (num != null) {
            return num.intValue();
        }
        return 0;
    }
}
