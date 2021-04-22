package com.sonymobile.systemui.lockscreen.theme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.ViewGroup;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0004R$color;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.Dependency;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.sonymobile.runtimeskinning.RuntimeSkinningException;
import com.sonymobile.runtimeskinning.RuntimeSkinningHelper;
import com.sonymobile.runtimeskinning.SkinnedResources;
import com.sonymobile.systemui.lockscreen.LockscreenLoopsController;

public class LockscreenSkinningController implements ConfigurationChangedReceiver {
    private static final String TAG = "LockscreenSkinningController";
    private final Configuration mConfiguration = new Configuration();
    private Context mContext;
    private boolean mIsSkinningEnabled;
    private KeyguardIndicationController mKeyguardIndicationController;
    private ThemeChangedReceiver mReceiver;
    private Resources mResources;
    private int mScrimColor = 0;
    private ViewGroup mStatusBarWindow;

    public LockscreenSkinningController(Context context) {
        this.mContext = context;
        this.mIsSkinningEnabled = context.getResources().getBoolean(C0003R$bool.somc_keyguard_theme_enabled);
        this.mConfiguration.setTo(this.mContext.getResources().getConfiguration());
    }

    public void init(ViewGroup viewGroup, KeyguardIndicationController keyguardIndicationController) {
        if (this.mIsSkinningEnabled) {
            this.mStatusBarWindow = viewGroup;
            this.mKeyguardIndicationController = keyguardIndicationController;
            initReceiverForCurrentUser(this.mContext);
            getSkinnedResourcesAsync(this.mContext);
        }
    }

    public final void onUserSwitched(int i) {
        if (this.mIsSkinningEnabled) {
            initReceiverForCurrentUser(this.mContext);
            getSkinnedResourcesAsync(this.mContext);
        }
    }

    private void initReceiverForCurrentUser(Context context) {
        BroadcastReceiver broadcastReceiver = this.mReceiver;
        if (broadcastReceiver != null) {
            context.unregisterReceiver(broadcastReceiver);
            this.mReceiver = null;
        }
        ThemeChangedReceiver themeChangedReceiver = new ThemeChangedReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.sonymobile.runtimeskinning.intent.SKIN_CHANGED");
        context.registerReceiverAsUser(themeChangedReceiver, UserHandle.CURRENT, intentFilter, "com.sonymobile.runtimeskinning.permission.SEND_SKIN_CHANGED", null);
        this.mReceiver = themeChangedReceiver;
    }

    /* access modifiers changed from: private */
    public static class ThemeChangedReceiver extends BroadcastReceiver {
        private ThemeChangedReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getSkinnedResourcesAsync(context);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void getSkinnedResourcesAsync(Context context) {
        new AsyncTaskResourcesLoader(context, this).execute(new Void[0]);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSkinnedResources(Resources resources) {
        if (resources == null) {
            resources = this.mContext.getResources();
        }
        this.mResources = resources;
        SomcLockscreenRuntimeThemeUpdater.newThemeConfiguration(this.mStatusBarWindow, this.mKeyguardIndicationController, this.mResources);
        int scrimColor = getScrimColor(this.mResources);
        if (scrimColor != this.mScrimColor) {
            this.mScrimColor = scrimColor;
            updateColorExtractor();
        }
        ((LockscreenLoopsController) Dependency.get(LockscreenLoopsController.class)).onThemeChanged();
    }

    private int getScrimColor(Resources resources) {
        return resources.getColor(C0004R$color.somc_keyguard_theme_color_scrim, null);
    }

    private void updateColorExtractor() {
        ColorExtractor.GradientColors gradientColors = new ColorExtractor.GradientColors();
        gradientColors.setMainColor(this.mScrimColor);
        gradientColors.setSecondaryColor(this.mScrimColor);
        gradientColors.setSupportsDarkText(false);
        ((SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class)).setSomcLockColors(gradientColors);
    }

    public Resources getResources() {
        if (this.mIsSkinningEnabled) {
            return this.mResources;
        }
        return null;
    }

    /* access modifiers changed from: private */
    public static class AsyncTaskResourcesLoader extends AsyncTask<Void, Void, Resources> {
        private final Context mContext;
        private final LockscreenSkinningController mController;

        AsyncTaskResourcesLoader(Context context, LockscreenSkinningController lockscreenSkinningController) {
            this.mContext = context;
            this.mController = lockscreenSkinningController;
        }

        /* access modifiers changed from: protected */
        public Resources doInBackground(Void... voidArr) {
            SkinnedResources skinnedResources = null;
            if (isCancelled()) {
                return null;
            }
            RuntimeSkinningHelper runtimeSkinningHelper = new RuntimeSkinningHelper();
            runtimeSkinningHelper.init(this.mContext);
            try {
                skinnedResources = runtimeSkinningHelper.getSkinnedResourcesForCurrentSkin("com.android.systemui", null, this.mContext);
            } catch (RemoteException e) {
                String str = LockscreenSkinningController.TAG;
                Log.e(str, "RemoteException e:" + e.toString(), e);
            } catch (RuntimeSkinningException e2) {
                String str2 = LockscreenSkinningController.TAG;
                Log.e(str2, "RuntimeSkinningException e:" + e2.toString(), e2);
            } catch (RuntimeException e3) {
                String str3 = LockscreenSkinningController.TAG;
                Log.e(str3, "RuntimeException e:" + e3.toString(), e3);
            }
            runtimeSkinningHelper.reset();
            return skinnedResources;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Resources resources) {
            if (!isCancelled()) {
                this.mController.updateSkinnedResources(resources);
            }
        }
    }

    @Override // com.android.systemui.ConfigurationChangedReceiver
    public void onConfigurationChanged(Configuration configuration) {
        int updateFrom = this.mConfiguration.updateFrom(configuration);
        if (this.mIsSkinningEnabled && (updateFrom & 4) != 0) {
            ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getSkinnedResourcesAsync(this.mContext);
        }
    }
}
