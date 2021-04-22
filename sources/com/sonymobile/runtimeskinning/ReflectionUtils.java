package com.sonymobile.runtimeskinning;

import android.util.Log;
import java.lang.Thread;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ReflectionUtils {
    public static <T> T invokeMethod(Method method, Object obj, Class<T> cls, Object... objArr) {
        return (T) invokeMethod(method, obj, null, cls, objArr);
    }

    public static <T> T invokeMethod(Method method, Object obj, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, Class<T> cls, Object... objArr) {
        Object obj2 = null;
        if (method == null) {
            return null;
        }
        try {
            obj2 = method.invoke(obj, objArr);
        } catch (IllegalArgumentException e) {
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
            }
            Log.w("runtime-skinning-lib", "Error invoking " + method.getName(), e);
        } catch (IllegalAccessException e2) {
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e2);
            }
            Log.w("runtime-skinning-lib", "Error invoking " + method.getName(), e2);
        } catch (InvocationTargetException e3) {
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e3);
            }
            Log.w("runtime-skinning-lib", "Error invoking " + method.getName(), e3);
        }
        return cls.cast(obj2);
    }

    public static <T> Constructor<T> getConstructor(Class<T> cls, ExceptionHandler exceptionHandler) {
        try {
            return cls.getConstructor(new Class[0]);
        } catch (NoSuchMethodException e) {
            if (exceptionHandler == null) {
                return null;
            }
            exceptionHandler.uncaughtException(Thread.currentThread(), e);
            return null;
        }
    }

    public static Method getMethod(String str, String str2, Class<?> cls, Class<?>... clsArr) {
        try {
            return getMethod(Class.forName(str), str2, cls, clsArr);
        } catch (ClassNotFoundException unused) {
            return null;
        }
    }

    public static Method getMethod(Class<?> cls, String str, Class<?> cls2, Class<?>... clsArr) {
        return getMethod(null, cls, str, cls2, clsArr);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:5:0x0010, code lost:
        if (r4.isAssignableFrom(r2.getReturnType()) == false) goto L_0x0012;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.reflect.Method getMethod(com.sonymobile.runtimeskinning.ExceptionHandler r1, java.lang.Class<?> r2, java.lang.String r3, java.lang.Class<?> r4, java.lang.Class<?>... r5) {
        /*
            r0 = 0
            java.lang.reflect.Method r2 = r2.getDeclaredMethod(r3, r5)     // Catch:{ NoSuchMethodException -> 0x0012 }
            makeAccessible(r2)     // Catch:{ NoSuchMethodException -> 0x0013 }
            java.lang.Class r3 = r2.getReturnType()     // Catch:{ NoSuchMethodException -> 0x0013 }
            boolean r3 = r4.isAssignableFrom(r3)     // Catch:{ NoSuchMethodException -> 0x0013 }
            if (r3 != 0) goto L_0x0013
        L_0x0012:
            r2 = r0
        L_0x0013:
            if (r2 != 0) goto L_0x0023
            if (r1 == 0) goto L_0x0023
            java.lang.Thread r3 = java.lang.Thread.currentThread()
            java.util.NoSuchElementException r4 = new java.util.NoSuchElementException
            r4.<init>()
            r1.uncaughtException(r3, r4)
        L_0x0023:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.runtimeskinning.ReflectionUtils.getMethod(com.sonymobile.runtimeskinning.ExceptionHandler, java.lang.Class, java.lang.String, java.lang.Class, java.lang.Class[]):java.lang.reflect.Method");
    }

    public static <T> T readField(Field field, Object obj, Class<T> cls) {
        Object obj2;
        try {
            obj2 = field.get(obj);
        } catch (IllegalArgumentException e) {
            Log.w("runtime-skinning-lib", "Error reading " + field.getName(), e);
        } catch (IllegalAccessException e2) {
            Log.w("runtime-skinning-lib", "Error reading " + field.getName(), e2);
        }
        return cls.cast(obj2);
        obj2 = null;
        return cls.cast(obj2);
    }

    public static Field getField(Class<?> cls, String str, Class<?> cls2) {
        try {
            Field declaredField = cls.getDeclaredField(str);
            try {
                makeAccessible(declaredField);
                if (cls2.isAssignableFrom(declaredField.getType())) {
                    return declaredField;
                }
                return null;
            } catch (NoSuchFieldException unused) {
                return declaredField;
            }
        } catch (NoSuchFieldException unused2) {
        }
    }

    private static void makeAccessible(AccessibleObject accessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }
    }
}
