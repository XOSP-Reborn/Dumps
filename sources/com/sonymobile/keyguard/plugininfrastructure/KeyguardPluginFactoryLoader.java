package com.sonymobile.keyguard.plugininfrastructure;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import com.android.systemui.C0003R$bool;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class KeyguardPluginFactoryLoader {
    private final ClockPluginUserSelectionHandler mClockPluginUserSelectionHandler;
    private final Context mContext;
    private KeyguardComponentFactory mKeyguardComponentFactory;
    private final DefaultKeyguardFactoryProvider mKeyguardFactoryProvider;
    private ComponentName mLastComponentName;

    public KeyguardPluginFactoryLoader(Context context, DefaultKeyguardFactoryProvider defaultKeyguardFactoryProvider, ClockPluginUserSelectionHandler clockPluginUserSelectionHandler) {
        if (context == null) {
            throw new IllegalArgumentException("Context can not be null");
        } else if (defaultKeyguardFactoryProvider == null) {
            throw new IllegalArgumentException("DefaultKeyguardFactoryProvider can not be null");
        } else if (clockPluginUserSelectionHandler != null) {
            this.mContext = context;
            this.mKeyguardFactoryProvider = defaultKeyguardFactoryProvider;
            this.mClockPluginUserSelectionHandler = clockPluginUserSelectionHandler;
        } else {
            throw new IllegalArgumentException("ClockPluginUserSelectionHandler can not be null");
        }
    }

    public final ViewGroup createKeyguardClockView(ViewGroup viewGroup) {
        KeyguardComponentFactory keyguardComponentFactory = this.mKeyguardComponentFactory;
        ViewGroup createKeyguardClockView = keyguardComponentFactory != null ? keyguardComponentFactory.createKeyguardClockView(this.mContext, viewGroup) : null;
        if (!(createKeyguardClockView == null || createKeyguardClockView.getParent() == null)) {
            Log.e("KeyguardPluginFactoryLoader", "Clock plugin should not assume where it shall end up. view = " + createKeyguardClockView);
        }
        return createKeyguardClockView;
    }

    public final boolean refreshLoader() {
        KeyguardComponentFactory keyguardComponentFactory = this.mKeyguardComponentFactory;
        this.mKeyguardComponentFactory = instantiateKeyguardComponentFactory();
        return keyguardComponentFactory != this.mKeyguardComponentFactory;
    }

    public final KeyguardComponentFactory createComponentFactoryFromFactoryEntry(KeyguardComponentFactoryEntry keyguardComponentFactoryEntry) {
        if (keyguardComponentFactoryEntry == null || keyguardComponentFactoryEntry.getFullyQualifiedClassName() == null) {
            return null;
        }
        return instantiateKeyguardComponentFactory(new ComponentName(this.mContext, keyguardComponentFactoryEntry.getFullyQualifiedClassName()));
    }

    private Class<KeyguardComponentFactory> loadKeyguardComponentFactoryClassFromComponentName(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        try {
            return (Class) loadUncheckedClass(KeyguardComponentFactory.class, componentName.getClassName(), this.mContext.getClassLoader());
        } catch (ClassNotFoundException e) {
            Log.w("KeyguardPluginFactoryLoader", "Keyguard plugin factory class " + componentName.getClassName() + " could not be found: " + e);
        } catch (SecurityException e2) {
            Log.w("KeyguardPluginFactoryLoader", e2);
        } catch (Throwable th) {
            Log.w("KeyguardPluginFactoryLoader", th);
        }
        return null;
    }

    private <T> T loadUncheckedClass(T t, String str, ClassLoader classLoader) throws ClassNotFoundException, ClassCastException {
        return (T) classLoader.loadClass(str);
    }

    private ComponentName getActiveKeyguardComponentFactoryComponentName() {
        String activeFullPluginClassName = getActiveFullPluginClassName();
        if (activeFullPluginClassName != null) {
            return new ComponentName(this.mContext, activeFullPluginClassName);
        }
        return null;
    }

    public String getActiveFullPluginClassName() {
        if (this.mContext.getResources().getBoolean(C0003R$bool.somc_keyguard_use_default_clock)) {
            return "com.sonymobile.keyguard.plugin.digitalclock.DigitalClockKeyguardComponentFactory";
        }
        String presentableUserSelection = this.mClockPluginUserSelectionHandler.getPresentableUserSelection();
        return presentableUserSelection == null ? this.mKeyguardFactoryProvider.getDefaultKeyguardFactoryClassName() : presentableUserSelection;
    }

    private KeyguardComponentFactory instantiateKeyguardComponentFactory() {
        KeyguardComponentFactory keyguardComponentFactory;
        ComponentName activeKeyguardComponentFactoryComponentName = getActiveKeyguardComponentFactoryComponentName();
        if (activeKeyguardComponentFactoryComponentName == null || activeKeyguardComponentFactoryComponentName.equals(this.mLastComponentName)) {
            keyguardComponentFactory = activeKeyguardComponentFactoryComponentName != null ? this.mKeyguardComponentFactory : null;
        } else {
            keyguardComponentFactory = instantiateKeyguardComponentFactory(activeKeyguardComponentFactoryComponentName);
        }
        this.mLastComponentName = activeKeyguardComponentFactoryComponentName;
        return keyguardComponentFactory;
    }

    private KeyguardComponentFactory instantiateKeyguardComponentFactory(ComponentName componentName) {
        Constructor<KeyguardComponentFactory> constructor;
        try {
            Class<KeyguardComponentFactory> loadKeyguardComponentFactoryClassFromComponentName = loadKeyguardComponentFactoryClassFromComponentName(componentName);
            if (loadKeyguardComponentFactoryClassFromComponentName == null || (constructor = loadKeyguardComponentFactoryClassFromComponentName.getConstructor(new Class[0])) == null) {
                return null;
            }
            return constructor.newInstance(new Object[0]);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            Log.w("KeyguardPluginFactoryLoader", e);
            return null;
        }
    }
}
