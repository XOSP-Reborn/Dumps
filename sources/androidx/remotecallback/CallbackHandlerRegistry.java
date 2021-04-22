package androidx.remotecallback;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.collection.ArrayMap;
import java.lang.reflect.InvocationTargetException;

public class CallbackHandlerRegistry {
    public static final CallbackHandlerRegistry sInstance = new CallbackHandlerRegistry();
    private final ArrayMap<Class<? extends CallbackReceiver>, ClsHandler> mClsLookup = new ArrayMap<>();

    public interface CallbackHandler<T extends CallbackReceiver> {
        void executeCallback(Context context, T t, Bundle bundle);
    }

    /* access modifiers changed from: package-private */
    public <T extends CallbackReceiver> void ensureInitialized(Class<T> cls) {
        synchronized (this) {
            if (!this.mClsLookup.containsKey(cls)) {
                runInit(cls);
            }
        }
    }

    public <T extends CallbackReceiver> void invokeCallback(Context context, T t, Bundle bundle) {
        Class<?> cls = t.getClass();
        ensureInitialized(cls);
        ClsHandler findMap = findMap(cls);
        if (findMap == null) {
            Log.e("CallbackHandlerRegistry", "No map found for " + cls.getName());
            return;
        }
        String string = bundle.getString("remotecallback.method");
        CallbackHandler<? extends CallbackReceiver> callbackHandler = findMap.mHandlers.get(string);
        if (callbackHandler == null) {
            Log.e("CallbackHandlerRegistry", "No handler found for " + string + " on " + cls.getName());
            return;
        }
        callbackHandler.executeCallback(context, t, bundle);
    }

    private ClsHandler findMap(Class<?> cls) {
        ClsHandler clsHandler;
        synchronized (this) {
            clsHandler = this.mClsLookup.get(cls);
        }
        if (clsHandler != null) {
            return clsHandler;
        }
        if (cls.getSuperclass() != null) {
            return findMap(cls.getSuperclass());
        }
        return null;
    }

    private <T extends CallbackReceiver> void runInit(Class<T> cls) {
        try {
            ClsHandler clsHandler = new ClsHandler();
            this.mClsLookup.put(cls, clsHandler);
            clsHandler.mCallStub = (CallbackReceiver) findInitClass(cls).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (InstantiationException e) {
            Log.e("CallbackHandlerRegistry", "Unable to initialize " + cls.getName(), e);
        } catch (IllegalAccessException e2) {
            Log.e("CallbackHandlerRegistry", "Unable to initialize " + cls.getName(), e2);
        } catch (InvocationTargetException e3) {
            Log.e("CallbackHandlerRegistry", "Unable to initialize " + cls.getName(), e3);
        } catch (NoSuchMethodException e4) {
            Log.e("CallbackHandlerRegistry", "Unable to initialize " + cls.getName(), e4);
        } catch (ClassNotFoundException e5) {
            Log.e("CallbackHandlerRegistry", "Unable to initialize " + cls.getName(), e5);
        }
    }

    /* JADX DEBUG: Type inference failed for r4v2. Raw type applied. Possible types: java.lang.Class<?>, java.lang.Class<? extends java.lang.Runnable> */
    private static Class<? extends Runnable> findInitClass(Class<? extends CallbackReceiver> cls) throws ClassNotFoundException {
        return Class.forName(String.format("%s.%sInitializer", cls.getPackage().getName(), cls.getSimpleName()), false, cls.getClassLoader());
    }

    /* access modifiers changed from: package-private */
    public static class ClsHandler {
        CallbackReceiver mCallStub;
        final ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> mHandlers = new ArrayMap<>();

        ClsHandler() {
        }
    }
}
