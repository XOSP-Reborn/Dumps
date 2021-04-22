package com.android.keyguard.clock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import androidx.lifecycle.Observer;
import com.android.keyguard.clock.ClockInfo;
import com.android.keyguard.clock.ClockManager;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.dock.DockManager;
import com.android.systemui.plugins.ClockPlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.settings.CurrentUserObservable;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.util.InjectionInflationController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ClockManager {
    private final List<Supplier<ClockPlugin>> mBuiltinClocks;
    private final ContentObserver mContentObserver;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final CurrentUserObservable mCurrentUserObservable;
    private final Observer<Integer> mCurrentUserObserver;
    private final DockManager.DockEventListener mDockEventListener;
    private final DockManager mDockManager;
    private final int mHeight;
    private boolean mIsDocked;
    private final Map<ClockChangedListener, AvailableClocks> mListeners;
    private final Handler mMainHandler;
    private final PluginManager mPluginManager;
    private final AvailableClocks mPreviewClocks;
    private final SettingsWrapper mSettingsWrapper;
    private final int mWidth;

    public interface ClockChangedListener {
        void onClockChanged(ClockPlugin clockPlugin);
    }

    public /* synthetic */ void lambda$new$0$ClockManager(Integer num) {
        reload();
    }

    public ClockManager(Context context, InjectionInflationController injectionInflationController, PluginManager pluginManager, SysuiColorExtractor sysuiColorExtractor, DockManager dockManager) {
        this(context, injectionInflationController, pluginManager, sysuiColorExtractor, context.getContentResolver(), new CurrentUserObservable(context), new SettingsWrapper(context.getContentResolver()), dockManager);
    }

    ClockManager(Context context, InjectionInflationController injectionInflationController, PluginManager pluginManager, SysuiColorExtractor sysuiColorExtractor, ContentResolver contentResolver, CurrentUserObservable currentUserObservable, SettingsWrapper settingsWrapper, DockManager dockManager) {
        this.mBuiltinClocks = new ArrayList();
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mContentObserver = new ContentObserver(this.mMainHandler) {
            /* class com.android.keyguard.clock.ClockManager.AnonymousClass1 */

            public void onChange(boolean z, Uri uri, int i) {
                super.onChange(z, uri, i);
                if (Objects.equals(Integer.valueOf(i), ClockManager.this.mCurrentUserObservable.getCurrentUser().getValue())) {
                    ClockManager.this.reload();
                }
            }
        };
        this.mCurrentUserObserver = new Observer() {
            /* class com.android.keyguard.clock.$$Lambda$ClockManager$hg7TNpAa_jeQQKjwxI39ao59w9U */

            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                ClockManager.this.lambda$new$0$ClockManager((Integer) obj);
            }
        };
        this.mDockEventListener = new DockManager.DockEventListener() {
            /* class com.android.keyguard.clock.ClockManager.AnonymousClass2 */
        };
        this.mListeners = new ArrayMap();
        this.mContext = context;
        this.mPluginManager = pluginManager;
        this.mContentResolver = contentResolver;
        this.mSettingsWrapper = settingsWrapper;
        this.mCurrentUserObservable = currentUserObservable;
        this.mDockManager = dockManager;
        this.mPreviewClocks = new AvailableClocks();
        Resources resources = context.getResources();
        LayoutInflater injectable = injectionInflationController.injectable(LayoutInflater.from(context));
        addBuiltinClock(new Supplier(resources, injectable, sysuiColorExtractor) {
            /* class com.android.keyguard.clock.$$Lambda$ClockManager$qcpjSm9nfcenHjNSU7lKVTGsX4 */
            private final /* synthetic */ Resources f$0;
            private final /* synthetic */ LayoutInflater f$1;
            private final /* synthetic */ SysuiColorExtractor f$2;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return ClockManager.lambda$new$1(this.f$0, this.f$1, this.f$2);
            }
        });
        addBuiltinClock(new Supplier(resources, injectable, sysuiColorExtractor) {
            /* class com.android.keyguard.clock.$$Lambda$ClockManager$mCJuewhSbfqGAUXaP_8PWw4nqZs */
            private final /* synthetic */ Resources f$0;
            private final /* synthetic */ LayoutInflater f$1;
            private final /* synthetic */ SysuiColorExtractor f$2;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return ClockManager.lambda$new$2(this.f$0, this.f$1, this.f$2);
            }
        });
        addBuiltinClock(new Supplier(resources, injectable, sysuiColorExtractor) {
            /* class com.android.keyguard.clock.$$Lambda$ClockManager$KuKx3QjFfullqZu9O8YrysFYdRw */
            private final /* synthetic */ Resources f$0;
            private final /* synthetic */ LayoutInflater f$1;
            private final /* synthetic */ SysuiColorExtractor f$2;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
            }

            @Override // java.util.function.Supplier
            public final Object get() {
                return ClockManager.lambda$new$3(this.f$0, this.f$1, this.f$2);
            }
        });
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        this.mWidth = displayMetrics.widthPixels;
        this.mHeight = displayMetrics.heightPixels;
    }

    static /* synthetic */ ClockPlugin lambda$new$1(Resources resources, LayoutInflater layoutInflater, SysuiColorExtractor sysuiColorExtractor) {
        return new DefaultClockController(resources, layoutInflater, sysuiColorExtractor);
    }

    static /* synthetic */ ClockPlugin lambda$new$2(Resources resources, LayoutInflater layoutInflater, SysuiColorExtractor sysuiColorExtractor) {
        return new BubbleClockController(resources, layoutInflater, sysuiColorExtractor);
    }

    static /* synthetic */ ClockPlugin lambda$new$3(Resources resources, LayoutInflater layoutInflater, SysuiColorExtractor sysuiColorExtractor) {
        return new AnalogClockController(resources, layoutInflater, sysuiColorExtractor);
    }

    /* access modifiers changed from: package-private */
    public List<ClockInfo> getClockInfos() {
        return this.mPreviewClocks.getInfo();
    }

    /* access modifiers changed from: package-private */
    public boolean isDocked() {
        return this.mIsDocked;
    }

    /* access modifiers changed from: package-private */
    public ContentObserver getContentObserver() {
        return this.mContentObserver;
    }

    private void addBuiltinClock(Supplier<ClockPlugin> supplier) {
        this.mPreviewClocks.addClockPlugin(supplier.get());
        this.mBuiltinClocks.add(supplier);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void reload() {
        this.mPreviewClocks.reload();
        this.mListeners.forEach($$Lambda$ClockManager$i436KHmxBKLRfCOA6rL_7pJbxgc.INSTANCE);
    }

    static /* synthetic */ void lambda$reload$4(ClockChangedListener clockChangedListener, AvailableClocks availableClocks) {
        availableClocks.reload();
        ClockPlugin currentClock = availableClocks.getCurrentClock();
        if (currentClock instanceof DefaultClockController) {
            clockChangedListener.onClockChanged(null);
        } else {
            clockChangedListener.onClockChanged(currentClock);
        }
    }

    /* access modifiers changed from: private */
    public final class AvailableClocks implements PluginListener<ClockPlugin> {
        private final List<ClockInfo> mClockInfo;
        private final Map<String, ClockPlugin> mClocks;
        private ClockPlugin mCurrentClock;

        private AvailableClocks() {
            this.mClocks = new ArrayMap();
            this.mClockInfo = new ArrayList();
        }

        public void onPluginConnected(ClockPlugin clockPlugin, Context context) {
            addClockPlugin(clockPlugin);
            reload();
            if (clockPlugin == this.mCurrentClock) {
                ClockManager.this.reload();
            }
        }

        public void onPluginDisconnected(ClockPlugin clockPlugin) {
            boolean z = clockPlugin == this.mCurrentClock;
            removeClockPlugin(clockPlugin);
            reload();
            if (z) {
                ClockManager.this.reload();
            }
        }

        /* access modifiers changed from: package-private */
        public ClockPlugin getCurrentClock() {
            return this.mCurrentClock;
        }

        /* access modifiers changed from: package-private */
        public List<ClockInfo> getInfo() {
            return this.mClockInfo;
        }

        /* access modifiers changed from: package-private */
        public void addClockPlugin(ClockPlugin clockPlugin) {
            String name = clockPlugin.getClass().getName();
            this.mClocks.put(clockPlugin.getClass().getName(), clockPlugin);
            List<ClockInfo> list = this.mClockInfo;
            ClockInfo.Builder builder = ClockInfo.builder();
            builder.setName(clockPlugin.getName());
            builder.setTitle(clockPlugin.getTitle());
            builder.setId(name);
            Objects.requireNonNull(clockPlugin);
            builder.setThumbnail(new Supplier() {
                /* class com.android.keyguard.clock.$$Lambda$d3U4wCuqsezzeLGogc1fLHnUj0 */

                @Override // java.util.function.Supplier
                public final Object get() {
                    return ClockPlugin.this.getThumbnail();
                }
            });
            builder.setPreview(new Supplier(clockPlugin) {
                /* class com.android.keyguard.clock.$$Lambda$ClockManager$AvailableClocks$3xFQeynnnUMh38fqZ7v9xTaqzmA */
                private final /* synthetic */ ClockPlugin f$1;

                {
                    this.f$1 = r2;
                }

                @Override // java.util.function.Supplier
                public final Object get() {
                    return ClockManager.AvailableClocks.this.lambda$addClockPlugin$0$ClockManager$AvailableClocks(this.f$1);
                }
            });
            list.add(builder.build());
        }

        public /* synthetic */ Bitmap lambda$addClockPlugin$0$ClockManager$AvailableClocks(ClockPlugin clockPlugin) {
            return clockPlugin.getPreview(ClockManager.this.mWidth, ClockManager.this.mHeight);
        }

        private void removeClockPlugin(ClockPlugin clockPlugin) {
            String name = clockPlugin.getClass().getName();
            this.mClocks.remove(name);
            for (int i = 0; i < this.mClockInfo.size(); i++) {
                if (name.equals(this.mClockInfo.get(i).getId())) {
                    this.mClockInfo.remove(i);
                    return;
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void reload() {
            this.mCurrentClock = getClockPlugin();
        }

        private ClockPlugin getClockPlugin() {
            ClockPlugin clockPlugin;
            String dockedClockFace;
            if (!ClockManager.this.isDocked() || (dockedClockFace = ClockManager.this.mSettingsWrapper.getDockedClockFace(ClockManager.this.mCurrentUserObservable.getCurrentUser().getValue().intValue())) == null) {
                clockPlugin = null;
            } else {
                clockPlugin = this.mClocks.get(dockedClockFace);
                if (clockPlugin != null) {
                    return clockPlugin;
                }
            }
            String lockScreenCustomClockFace = ClockManager.this.mSettingsWrapper.getLockScreenCustomClockFace(ClockManager.this.mCurrentUserObservable.getCurrentUser().getValue().intValue());
            return lockScreenCustomClockFace != null ? this.mClocks.get(lockScreenCustomClockFace) : clockPlugin;
        }
    }
}
