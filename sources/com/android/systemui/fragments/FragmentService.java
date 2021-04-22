package com.android.systemui.fragments;

import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Handler;
import android.util.ArrayMap;
import android.view.View;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.Dumpable;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.fragments.FragmentService;
import com.android.systemui.qs.QSFragment;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;

public class FragmentService implements ConfigurationChangedReceiver, Dumpable {
    private final FragmentCreator mFragmentCreator;
    private final Handler mHandler = new Handler();
    private final ArrayMap<View, FragmentHostState> mHosts = new ArrayMap<>();
    private final ArrayMap<String, Method> mInjectionMap = new ArrayMap<>();

    public interface FragmentCreator {
        NavigationBarFragment createNavigationBarFragment();

        QSFragment createQSFragment();
    }

    public FragmentService(SystemUIFactory.SystemUIRootComponent systemUIRootComponent) {
        this.mFragmentCreator = systemUIRootComponent.createFragmentCreator();
        initInjectionMap();
    }

    /* access modifiers changed from: package-private */
    public ArrayMap<String, Method> getInjectionMap() {
        return this.mInjectionMap;
    }

    /* access modifiers changed from: package-private */
    public FragmentCreator getFragmentCreator() {
        return this.mFragmentCreator;
    }

    private void initInjectionMap() {
        Method[] declaredMethods = FragmentCreator.class.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (Fragment.class.isAssignableFrom(method.getReturnType()) && (method.getModifiers() & 1) != 0) {
                this.mInjectionMap.put(method.getReturnType().getName(), method);
            }
        }
    }

    public FragmentHostManager getFragmentHostManager(View view) {
        View rootView = view.getRootView();
        FragmentHostState fragmentHostState = this.mHosts.get(rootView);
        if (fragmentHostState == null) {
            fragmentHostState = new FragmentHostState(rootView);
            this.mHosts.put(rootView, fragmentHostState);
        }
        return fragmentHostState.getFragmentHostManager();
    }

    public void removeAndDestroy(View view) {
        FragmentHostState remove = this.mHosts.remove(view.getRootView());
        if (remove != null) {
            remove.mFragmentHostManager.destroy();
        }
    }

    public void destroyAll() {
        for (FragmentHostState fragmentHostState : this.mHosts.values()) {
            fragmentHostState.mFragmentHostManager.destroy();
        }
    }

    @Override // com.android.systemui.ConfigurationChangedReceiver
    public void onConfigurationChanged(Configuration configuration) {
        for (FragmentHostState fragmentHostState : this.mHosts.values()) {
            fragmentHostState.sendConfigurationChange(configuration);
        }
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Dumping fragments:");
        for (FragmentHostState fragmentHostState : this.mHosts.values()) {
            fragmentHostState.mFragmentHostManager.getFragmentManager().dump("  ", fileDescriptor, printWriter, strArr);
        }
    }

    /* access modifiers changed from: private */
    public class FragmentHostState {
        private FragmentHostManager mFragmentHostManager;
        private final View mView;

        public FragmentHostState(View view) {
            this.mView = view;
            this.mFragmentHostManager = new FragmentHostManager(FragmentService.this, this.mView);
        }

        public void sendConfigurationChange(Configuration configuration) {
            FragmentService.this.mHandler.post(new Runnable(configuration) {
                /* class com.android.systemui.fragments.$$Lambda$FragmentService$FragmentHostState$kEJEvu5Mq9Z5e9srOLcsFn7Glto */
                private final /* synthetic */ Configuration f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    FragmentService.FragmentHostState.this.lambda$sendConfigurationChange$0$FragmentService$FragmentHostState(this.f$1);
                }
            });
        }

        public FragmentHostManager getFragmentHostManager() {
            return this.mFragmentHostManager;
        }

        /* access modifiers changed from: private */
        /* renamed from: handleSendConfigurationChange */
        public void lambda$sendConfigurationChange$0$FragmentService$FragmentHostState(Configuration configuration) {
            this.mFragmentHostManager.onConfigurationChanged(configuration);
        }
    }
}
