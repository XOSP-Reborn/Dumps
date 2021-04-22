package com.android.systemui.statusbar.phone;

import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyCarrierArea;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.ActivityIntentHelper;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.doze.util.BurnInHelperKt;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.IntentButtonProvider;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.tuner.LockscreenFragment;
import com.android.systemui.tuner.TunerService;
import com.sonymobile.keyguard.statistics.LockscreenStatisticsShortcutReporter;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeyguardBottomAreaView extends FrameLayout implements View.OnClickListener, UnlockMethodCache.OnUnlockMethodChangedListener, AccessibilityController.AccessibilityStateChangedCallback {
    public static final Intent INSECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA");
    private static final Intent PHONE_INTENT = new Intent("android.intent.action.DIAL");
    private static final Intent SECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
    private AccessibilityController mAccessibilityController;
    private View.AccessibilityDelegate mAccessibilityDelegate;
    private ActivityIntentHelper mActivityIntentHelper;
    private ActivityStarter mActivityStarter;
    private KeyguardAffordanceHelper mAffordanceHelper;
    private AssistManager mAssistManager;
    private int mBurnInXOffset;
    private int mBurnInYOffset;
    private View mCameraPreview;
    private float mDarkAmount;
    private final BroadcastReceiver mDevicePolicyReceiver;
    private boolean mDozing;
    private EmergencyCarrierArea mEmergencyCarrierArea;
    private TextView mEnterpriseDisclosure;
    private FlashlightController mFlashlightController;
    private ViewGroup mIndicationArea;
    private int mIndicationBottomMargin;
    private TextView mIndicationText;
    private final boolean mIsUseDocomoDevice;
    private KeyguardAffordanceView mLeftAffordanceView;
    private Drawable mLeftAssistIcon;
    private IntentButtonProvider.IntentButton mLeftButton;
    private String mLeftButtonStr;
    private ExtensionController.Extension<IntentButtonProvider.IntentButton> mLeftExtension;
    private boolean mLeftIsVoiceAssist;
    private View mLeftPreview;
    private LockIcon mLockIcon;
    private LockPatternUtils mLockPatternUtils;
    private ViewGroup mOverlayContainer;
    private ViewGroup mPreviewContainer;
    private PreviewInflater mPreviewInflater;
    private boolean mPrewarmBound;
    private final ServiceConnection mPrewarmConnection;
    private Messenger mPrewarmMessenger;
    private KeyguardAffordanceView mRightAffordanceView;
    private IntentButtonProvider.IntentButton mRightButton;
    private String mRightButtonStr;
    private ExtensionController.Extension<IntentButtonProvider.IntentButton> mRightExtension;
    private StatusBar mStatusBar;
    private UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private boolean mUserSetupComplete;

    /* access modifiers changed from: private */
    public static boolean isSuccessfulLaunch(int i) {
        return i == 0 || i == 3 || i == 2;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public KeyguardBottomAreaView(Context context) {
        this(context, null);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mPrewarmConnection = new ServiceConnection() {
            /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass1 */

            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = new Messenger(iBinder);
            }

            public void onServiceDisconnected(ComponentName componentName) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = null;
            }
        };
        this.mRightButton = new DefaultRightButton();
        this.mLeftButton = new DefaultLeftButton();
        this.mAccessibilityDelegate = new View.AccessibilityDelegate() {
            /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass2 */

            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                String str;
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                if (view == KeyguardBottomAreaView.this.mRightAffordanceView) {
                    str = KeyguardBottomAreaView.this.getResources().getString(C0014R$string.camera_label);
                } else if (view == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                    str = KeyguardBottomAreaView.this.mLeftIsVoiceAssist ? KeyguardBottomAreaView.this.getResources().getString(C0014R$string.voice_assist_label) : KeyguardBottomAreaView.this.getResources().getString(C0014R$string.phone_label);
                } else {
                    str = null;
                }
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, str));
            }

            public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                if (i == 16) {
                    if (view == KeyguardBottomAreaView.this.mRightAffordanceView) {
                        KeyguardBottomAreaView.this.launchCamera(3);
                        return true;
                    } else if (view == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                        KeyguardBottomAreaView.this.launchLeftAffordance();
                        return true;
                    }
                }
                return super.performAccessibilityAction(view, i, bundle);
            }
        };
        this.mDevicePolicyReceiver = new BroadcastReceiver() {
            /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass8 */

            public void onReceive(Context context, Intent intent) {
                KeyguardBottomAreaView.this.post(new Runnable() {
                    /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass8.AnonymousClass1 */

                    public void run() {
                        KeyguardBottomAreaView.this.updateCameraVisibility();
                    }
                });
            }
        };
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass9 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onUserSwitchComplete(int i) {
                KeyguardBottomAreaView.this.updateCameraVisibility();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onUserUnlocked() {
                KeyguardBottomAreaView.this.inflateCameraPreview();
                KeyguardBottomAreaView.this.updateCameraVisibility();
                KeyguardBottomAreaView.this.updateLeftAffordance();
            }
        };
        this.mIsUseDocomoDevice = context.getResources().getBoolean(C0003R$bool.somc_keyguard_docomo_device);
    }

    public void initFrom(KeyguardBottomAreaView keyguardBottomAreaView) {
        setStatusBar(keyguardBottomAreaView.mStatusBar);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(((FrameLayout) this).mContext);
        this.mPreviewInflater = new PreviewInflater(((FrameLayout) this).mContext, new LockPatternUtils(((FrameLayout) this).mContext), new ActivityIntentHelper(((FrameLayout) this).mContext));
        this.mPreviewContainer = (ViewGroup) findViewById(C0007R$id.preview_container);
        this.mEmergencyCarrierArea = (EmergencyCarrierArea) findViewById(C0007R$id.keyguard_selector_fade_container);
        this.mOverlayContainer = (ViewGroup) findViewById(C0007R$id.overlay_container);
        this.mRightAffordanceView = (KeyguardAffordanceView) findViewById(C0007R$id.camera_button);
        this.mLeftAffordanceView = (KeyguardAffordanceView) findViewById(C0007R$id.left_button);
        this.mLockIcon = (LockIcon) findViewById(C0007R$id.lock_icon);
        this.mIndicationArea = (ViewGroup) findViewById(C0007R$id.keyguard_indication_area);
        this.mEnterpriseDisclosure = (TextView) findViewById(C0007R$id.keyguard_indication_enterprise_disclosure);
        this.mIndicationText = (TextView) findViewById(C0007R$id.keyguard_indication_text);
        this.mIndicationBottomMargin = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_indication_margin_bottom);
        this.mBurnInYOffset = getResources().getDimensionPixelSize(C0005R$dimen.default_burn_in_prevention_offset);
        updateCameraVisibility();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(getContext());
        this.mUnlockMethodCache.addListener(this);
        setClipChildren(false);
        setClipToPadding(false);
        inflateCameraPreview();
        this.mRightAffordanceView.setOnClickListener(this);
        this.mLeftAffordanceView.setOnClickListener(this);
        initAccessibility();
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mFlashlightController = (FlashlightController) Dependency.get(FlashlightController.class);
        this.mAccessibilityController = (AccessibilityController) Dependency.get(AccessibilityController.class);
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        this.mActivityIntentHelper = new ActivityIntentHelper(getContext());
        updateLeftAffordance();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAccessibilityController.addStateChangedCallback(this);
        ExtensionController.ExtensionBuilder newExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButtonProvider.IntentButton.class);
        newExtension.withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_RIGHT_BUTTON", $$Lambda$KeyguardBottomAreaView$g4KaNPI9kzVsHrOlMYmA_f9J2Y.INSTANCE);
        newExtension.withTunerFactory(new LockscreenFragment.LockButtonFactory(((FrameLayout) this).mContext, "sysui_keyguard_right"));
        newExtension.withDefault(new Supplier() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$KeyguardBottomAreaView$41MKD52m3LHIf9RRtKFf6LfUif0 */

            @Override // java.util.function.Supplier
            public final Object get() {
                return KeyguardBottomAreaView.this.lambda$onAttachedToWindow$1$KeyguardBottomAreaView();
            }
        });
        newExtension.withCallback(new Consumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$KeyguardBottomAreaView$Z_R5g5wpXUcfPYLHCfZHekG4xK0 */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                KeyguardBottomAreaView.this.lambda$onAttachedToWindow$2$KeyguardBottomAreaView((IntentButtonProvider.IntentButton) obj);
            }
        });
        this.mRightExtension = newExtension.build();
        ExtensionController.ExtensionBuilder newExtension2 = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButtonProvider.IntentButton.class);
        newExtension2.withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_LEFT_BUTTON", $$Lambda$KeyguardBottomAreaView$Eh9_ou4HbbT4H4ZFilpDDtanY4k.INSTANCE);
        newExtension2.withTunerFactory(new LockscreenFragment.LockButtonFactory(((FrameLayout) this).mContext, "sysui_keyguard_left"));
        newExtension2.withDefault(new Supplier() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$KeyguardBottomAreaView$WhTEBW5YZVW2MsKtz0LzBCynHY */

            @Override // java.util.function.Supplier
            public final Object get() {
                return KeyguardBottomAreaView.this.lambda$onAttachedToWindow$4$KeyguardBottomAreaView();
            }
        });
        newExtension2.withCallback(new Consumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$KeyguardBottomAreaView$owXxFBBnubMOAUdfyf5a48bfZo */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                KeyguardBottomAreaView.this.lambda$onAttachedToWindow$5$KeyguardBottomAreaView((IntentButtonProvider.IntentButton) obj);
            }
        });
        this.mLeftExtension = newExtension2.build();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        getContext().registerReceiverAsUser(this.mDevicePolicyReceiver, UserHandle.ALL, intentFilter, null, null);
        KeyguardUpdateMonitor.getInstance(((FrameLayout) this).mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    public /* synthetic */ IntentButtonProvider.IntentButton lambda$onAttachedToWindow$1$KeyguardBottomAreaView() {
        return new DefaultRightButton();
    }

    public /* synthetic */ IntentButtonProvider.IntentButton lambda$onAttachedToWindow$4$KeyguardBottomAreaView() {
        return new DefaultLeftButton();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAccessibilityController.removeStateChangedCallback(this);
        this.mRightExtension.destroy();
        this.mLeftExtension.destroy();
        getContext().unregisterReceiver(this.mDevicePolicyReceiver);
        KeyguardUpdateMonitor.getInstance(((FrameLayout) this).mContext).removeCallback(this.mUpdateMonitorCallback);
    }

    private void initAccessibility() {
        this.mLeftAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mRightAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mIndicationBottomMargin = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_indication_margin_bottom);
        this.mBurnInYOffset = getResources().getDimensionPixelSize(C0005R$dimen.default_burn_in_prevention_offset);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mIndicationArea.getLayoutParams();
        int i = marginLayoutParams.bottomMargin;
        int i2 = this.mIndicationBottomMargin;
        if (i != i2) {
            marginLayoutParams.bottomMargin = i2;
            this.mIndicationArea.setLayoutParams(marginLayoutParams);
        }
        this.mEnterpriseDisclosure.setTextSize(0, (float) getResources().getDimensionPixelSize(17105455));
        this.mIndicationText.setTextSize(0, (float) getResources().getDimensionPixelSize(17105455));
        ViewGroup.LayoutParams layoutParams = this.mRightAffordanceView.getLayoutParams();
        layoutParams.width = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_affordance_width);
        layoutParams.height = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_affordance_height);
        this.mRightAffordanceView.setLayoutParams(layoutParams);
        updateRightAffordanceIcon();
        ViewGroup.LayoutParams layoutParams2 = this.mLeftAffordanceView.getLayoutParams();
        layoutParams2.width = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_affordance_width);
        layoutParams2.height = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_affordance_height);
        this.mLeftAffordanceView.setLayoutParams(layoutParams2);
        updateLeftAffordanceIcon();
    }

    private void updateRightAffordanceIcon() {
        IntentButtonProvider.IntentButton.IconState icon = this.mRightButton.getIcon();
        this.mRightAffordanceView.setVisibility((this.mDozing || !icon.isVisible) ? 8 : 0);
        if (!(icon.drawable == this.mRightAffordanceView.getDrawable() && icon.tint == this.mRightAffordanceView.shouldTint())) {
            this.mRightAffordanceView.setImageDrawable(icon.drawable, icon.tint);
        }
        this.mRightAffordanceView.setContentDescription(icon.contentDescription);
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        updateCameraVisibility();
    }

    public void setAffordanceHelper(KeyguardAffordanceHelper keyguardAffordanceHelper) {
        this.mAffordanceHelper = keyguardAffordanceHelper;
    }

    public void setUserSetupComplete(boolean z) {
        this.mUserSetupComplete = z;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
    }

    private Intent getCameraIntent() {
        return this.mRightButton.getIntent();
    }

    private Intent getSonyCameraIntent() {
        Intent intent = new Intent(getCameraIntent());
        intent.setPackage("com.sonyericsson.android.camera");
        return intent;
    }

    public ResolveInfo resolveCameraIntent() {
        return ((FrameLayout) this).mContext.getPackageManager().resolveActivityAsUser(getCameraIntent(), 65536, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: package-private */
    public ResolveInfo resolveSonyCameraIntent() {
        return ((FrameLayout) this).mContext.getPackageManager().resolveActivityAsUser(getSonyCameraIntent(), 65536, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateCameraVisibility() {
        KeyguardAffordanceView keyguardAffordanceView = this.mRightAffordanceView;
        if (keyguardAffordanceView != null) {
            keyguardAffordanceView.setVisibility((this.mDozing || !this.mRightButton.getIcon().isVisible) ? 8 : 0);
        }
    }

    private void updateLeftAffordanceIcon() {
        IntentButtonProvider.IntentButton.IconState icon = this.mLeftButton.getIcon();
        this.mLeftAffordanceView.setVisibility((this.mDozing || !icon.isVisible) ? 8 : 0);
        if (!(icon.drawable == this.mLeftAffordanceView.getDrawable() && icon.tint == this.mLeftAffordanceView.shouldTint())) {
            this.mLeftAffordanceView.setImageDrawable(icon.drawable, icon.tint);
        }
        this.mLeftAffordanceView.setContentDescription(icon.contentDescription);
    }

    public boolean isLeftVoiceAssist() {
        return this.mLeftIsVoiceAssist;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isPhoneVisible() {
        PackageManager packageManager = ((FrameLayout) this).mContext.getPackageManager();
        if (!packageManager.hasSystemFeature("android.hardware.telephony") || packageManager.resolveActivity(PHONE_INTENT, 0) == null) {
            return false;
        }
        return true;
    }

    @Override // com.android.systemui.statusbar.policy.AccessibilityController.AccessibilityStateChangedCallback
    public void onStateChanged(boolean z, boolean z2) {
        this.mRightAffordanceView.setClickable(z2);
        this.mLeftAffordanceView.setClickable(z2);
        this.mRightAffordanceView.setFocusable(z);
        this.mLeftAffordanceView.setFocusable(z);
    }

    public void onClick(View view) {
        if (view == this.mRightAffordanceView) {
            launchCamera(3);
        } else if (view == this.mLeftAffordanceView) {
            launchLeftAffordance();
        }
    }

    public void bindCameraPrewarmService() {
        String string;
        ActivityInfo targetActivityInfo = this.mActivityIntentHelper.getTargetActivityInfo(getSonyCameraIntent(), KeyguardUpdateMonitor.getCurrentUser(), true);
        if (targetActivityInfo != null && targetActivityInfo.metaData != null && (string = targetActivityInfo.metaData.getString("android.media.still_image_camera_preview_service")) != null) {
            Intent intent = new Intent();
            intent.setClassName(targetActivityInfo.packageName, string);
            intent.setAction("android.service.media.CameraPrewarmService.ACTION_PREWARM");
            try {
                if (getContext().bindServiceAsUser(intent, this.mPrewarmConnection, 67108865, new UserHandle(-2))) {
                    this.mPrewarmBound = true;
                }
            } catch (SecurityException e) {
                Log.w("StatusBar/KeyguardBottomAreaView", "Unable to bind to prewarm service package=" + targetActivityInfo.packageName + " class=" + string, e);
            }
        }
    }

    public void unbindCameraPrewarmService(boolean z) {
        if (this.mPrewarmBound) {
            Messenger messenger = this.mPrewarmMessenger;
            if (messenger != null && z) {
                try {
                    messenger.send(Message.obtain((Handler) null, 1));
                } catch (RemoteException e) {
                    Log.w("StatusBar/KeyguardBottomAreaView", "Error sending camera fired message", e);
                }
            }
            ((FrameLayout) this).mContext.unbindService(this.mPrewarmConnection);
            this.mPrewarmBound = false;
        }
    }

    public void launchCamera(int i) {
        LockscreenStatisticsShortcutReporter.sendEvent(getContext(), LockscreenStatisticsShortcutReporter.Types.Camera);
        final Intent cameraIntent = getCameraIntent();
        cameraIntent.putExtra("com.android.systemui.camera_launch_source", this.mStatusBar.getCameraLaunchSourceString(i));
        cameraIntent.setPackage(this.mStatusBar.getCameraLaunchPackage(i));
        Log.i("StatusBar/KeyguardBottomAreaView", "launchCamera " + i);
        boolean wouldLaunchResolverActivity = this.mActivityIntentHelper.wouldLaunchResolverActivity(cameraIntent, KeyguardUpdateMonitor.getCurrentUser());
        if (!"android.media.action.STILL_IMAGE_CAMERA_SECURE".equals(cameraIntent.getAction()) || wouldLaunchResolverActivity) {
            this.mActivityStarter.startActivity(cameraIntent, false, (ActivityStarter.Callback) new ActivityStarter.Callback() {
                /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass4 */

                @Override // com.android.systemui.plugins.ActivityStarter.Callback
                public void onActivityStarted(int i) {
                    KeyguardBottomAreaView.this.unbindCameraPrewarmService(KeyguardBottomAreaView.isSuccessfulLaunch(i));
                    if (KeyguardBottomAreaView.isSuccessfulLaunch(i)) {
                        KeyguardBottomAreaView.this.mStatusBar.readyForKeyguardDone();
                    }
                }
            });
        } else {
            AsyncTask.execute(new Runnable() {
                /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass3 */

                public void run() {
                    int i;
                    ActivityOptions makeBasic = ActivityOptions.makeBasic();
                    makeBasic.setDisallowEnterPictureInPictureWhileLaunching(true);
                    makeBasic.setRotationAnimationHint(3);
                    try {
                        i = ActivityTaskManager.getService().startActivityAsUser((IApplicationThread) null, KeyguardBottomAreaView.this.getContext().getBasePackageName(), cameraIntent, cameraIntent.resolveTypeIfNeeded(KeyguardBottomAreaView.this.getContext().getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, makeBasic.toBundle(), UserHandle.CURRENT.getIdentifier());
                    } catch (RemoteException e) {
                        Log.w("StatusBar/KeyguardBottomAreaView", "Unable to start camera activity", e);
                        i = -96;
                    }
                    final boolean isSuccessfulLaunch = KeyguardBottomAreaView.isSuccessfulLaunch(i);
                    KeyguardBottomAreaView.this.post(new Runnable() {
                        /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass3.AnonymousClass1 */

                        public void run() {
                            KeyguardBottomAreaView.this.unbindCameraPrewarmService(isSuccessfulLaunch);
                        }
                    });
                }
            });
        }
    }

    public void setDarkAmount(float f) {
        if (f != this.mDarkAmount) {
            this.mDarkAmount = f;
            dozeTimeTick();
        }
    }

    public void launchLeftAffordance() {
        if (this.mLeftIsVoiceAssist) {
            launchVoiceAssist();
        } else {
            launchPhone();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void launchVoiceAssist() {
        LockscreenStatisticsShortcutReporter.sendEvent(getContext(), LockscreenStatisticsShortcutReporter.Types.VoiceAssist);
        AnonymousClass5 r3 = new Runnable() {
            /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass5 */

            public void run() {
                KeyguardBottomAreaView.this.mAssistManager.launchVoiceAssistFromKeyguard();
            }
        };
        if (this.mStatusBar.isKeyguardCurrentlySecure()) {
            AsyncTask.execute(r3);
        } else {
            this.mStatusBar.executeRunnableDismissingKeyguard(r3, null, !TextUtils.isEmpty(this.mRightButtonStr) && ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_keyguard_right_unlock", 1) != 0, false, true);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean canLaunchVoiceAssist() {
        return this.mAssistManager.canVoiceAssistBeLaunchedFromKeyguard();
    }

    private void launchPhone() {
        LockscreenStatisticsShortcutReporter.sendEvent(getContext(), LockscreenStatisticsShortcutReporter.Types.Phone);
        final TelecomManager from = TelecomManager.from(((FrameLayout) this).mContext);
        if (from.isInCall()) {
            AsyncTask.execute(new Runnable() {
                /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass6 */

                public void run() {
                    from.showInCallScreen(false);
                }
            });
            return;
        }
        boolean z = true;
        if (TextUtils.isEmpty(this.mLeftButtonStr) || ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_keyguard_left_unlock", 1) == 0) {
            z = false;
        }
        this.mActivityStarter.startActivity(this.mLeftButton.getIntent(), z, new ActivityStarter.Callback() {
            /* class com.android.systemui.statusbar.phone.KeyguardBottomAreaView.AnonymousClass7 */

            @Override // com.android.systemui.plugins.ActivityStarter.Callback
            public void onActivityStarted(int i) {
                if (KeyguardBottomAreaView.isSuccessfulLaunch(i)) {
                    KeyguardBottomAreaView.this.mStatusBar.readyForKeyguardDone();
                }
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (view == this && i == 0) {
            updateCameraVisibility();
        }
    }

    public KeyguardAffordanceView getLeftView() {
        return this.mLeftAffordanceView;
    }

    public KeyguardAffordanceView getRightView() {
        return this.mRightAffordanceView;
    }

    public View getLeftPreview() {
        return this.mLeftPreview;
    }

    public View getRightPreview() {
        return this.mCameraPreview;
    }

    public LockIcon getLockIcon() {
        return this.mLockIcon;
    }

    public View getIndicationArea() {
        return this.mIndicationArea;
    }

    public void updateThemeResources(Resources resources) {
        int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground);
        int color2 = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground_inverse);
        this.mIndicationText.setTextColor(color);
        this.mRightAffordanceView.setColors(color, color, color2);
        this.mLeftAffordanceView.setColors(color, color, color2);
        this.mLockIcon.setColors(color, color, color2);
        invalidate();
    }

    @Override // com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener
    public void onUnlockMethodStateChanged() {
        updateCameraVisibility();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0035  */
    /* JADX WARNING: Removed duplicated region for block: B:16:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0023  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void inflateCameraPreview() {
        /*
            r4 = this;
            android.view.View r0 = r4.mCameraPreview
            r1 = 0
            if (r0 == 0) goto L_0x0012
            android.view.ViewGroup r2 = r4.mPreviewContainer
            r2.removeView(r0)
            int r0 = r0.getVisibility()
            if (r0 != 0) goto L_0x0012
            r0 = 1
            goto L_0x0013
        L_0x0012:
            r0 = r1
        L_0x0013:
            com.android.systemui.statusbar.policy.PreviewInflater r2 = r4.mPreviewInflater
            android.content.Intent r3 = r4.getSonyCameraIntent()
            android.view.View r2 = r2.inflatePreview(r3)
            r4.mCameraPreview = r2
            android.view.View r2 = r4.mCameraPreview
            if (r2 == 0) goto L_0x0031
            android.view.ViewGroup r3 = r4.mPreviewContainer
            r3.addView(r2)
            android.view.View r2 = r4.mCameraPreview
            if (r0 == 0) goto L_0x002d
            goto L_0x002e
        L_0x002d:
            r1 = 4
        L_0x002e:
            r2.setVisibility(r1)
        L_0x0031:
            com.android.systemui.statusbar.phone.KeyguardAffordanceHelper r4 = r4.mAffordanceHelper
            if (r4 == 0) goto L_0x0038
            r4.updatePreviews()
        L_0x0038:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.KeyguardBottomAreaView.inflateCameraPreview():void");
    }

    private void updateLeftPreview() {
        View view = this.mLeftPreview;
        if (view != null) {
            this.mPreviewContainer.removeView(view);
        }
        if (!this.mLeftIsVoiceAssist || this.mAssistManager.getVoiceInteractorComponentName() == null) {
            this.mLeftPreview = this.mPreviewInflater.inflatePreview(this.mLeftButton.getIntent());
        } else {
            this.mLeftPreview = this.mPreviewInflater.inflatePreviewFromService(this.mAssistManager.getVoiceInteractorComponentName());
        }
        View view2 = this.mLeftPreview;
        if (view2 != null) {
            this.mPreviewContainer.addView(view2);
            this.mLeftPreview.setVisibility(4);
        }
        KeyguardAffordanceHelper keyguardAffordanceHelper = this.mAffordanceHelper;
        if (keyguardAffordanceHelper != null) {
            keyguardAffordanceHelper.updatePreviews();
        }
    }

    public void startFinishDozeAnimation() {
        long j = 0;
        if (this.mLeftAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mLeftAffordanceView, 0);
            j = 48;
        }
        if (this.mRightAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mRightAffordanceView, j);
        }
    }

    private void startFinishDozeAnimationElement(View view, long j) {
        view.setAlpha(0.0f);
        view.setTranslationY((float) (view.getHeight() / 2));
        view.animate().alpha(1.0f).translationY(0.0f).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(j).setDuration(250);
    }

    public void updateLeftAffordance() {
        updateLeftAffordanceIcon();
        updateLeftPreview();
    }

    /* access modifiers changed from: private */
    /* renamed from: setRightButton */
    public void lambda$onAttachedToWindow$2$KeyguardBottomAreaView(IntentButtonProvider.IntentButton intentButton) {
        this.mRightButton = intentButton;
        updateRightAffordanceIcon();
        updateCameraVisibility();
        inflateCameraPreview();
    }

    /* access modifiers changed from: private */
    /* renamed from: setLeftButton */
    public void lambda$onAttachedToWindow$5$KeyguardBottomAreaView(IntentButtonProvider.IntentButton intentButton) {
        this.mLeftButton = intentButton;
        if (!(this.mLeftButton instanceof DefaultLeftButton)) {
            this.mLeftIsVoiceAssist = false;
        }
        updateLeftAffordance();
    }

    public void setDozing(boolean z, boolean z2) {
        this.mDozing = z;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
        if (z) {
            this.mOverlayContainer.setVisibility(4);
            this.mEmergencyCarrierArea.setVisibility(8);
            return;
        }
        this.mOverlayContainer.setVisibility(0);
        this.mEmergencyCarrierArea.setVisibility(8);
        if (z2) {
            startFinishDozeAnimation();
        }
    }

    public void dozeTimeTick() {
        this.mIndicationArea.setTranslationY(((float) (BurnInHelperKt.getBurnInOffset(this.mBurnInYOffset * 2, false) - this.mBurnInYOffset)) * this.mDarkAmount);
    }

    public void setAntiBurnInOffsetX(int i) {
        if (this.mBurnInXOffset != i) {
            this.mBurnInXOffset = i;
            this.mIndicationArea.setTranslationX((float) i);
        }
    }

    public void setAffordanceAlpha(float f) {
        this.mLeftAffordanceView.setAlpha(f);
        this.mRightAffordanceView.setAlpha(f);
        this.mLockIcon.setIconAlpha(f);
        this.mIndicationArea.setAlpha(f);
        this.mEmergencyCarrierArea.setAlpha(f);
    }

    /* access modifiers changed from: private */
    public class DefaultLeftButton implements IntentButtonProvider.IntentButton {
        private IntentButtonProvider.IntentButton.IconState mIconState;

        private DefaultLeftButton() {
            this.mIconState = new IntentButtonProvider.IntentButton.IconState();
        }

        @Override // com.android.systemui.plugins.IntentButtonProvider.IntentButton
        public IntentButtonProvider.IntentButton.IconState getIcon() {
            KeyguardBottomAreaView keyguardBottomAreaView = KeyguardBottomAreaView.this;
            boolean z = false;
            keyguardBottomAreaView.mLeftIsVoiceAssist = keyguardBottomAreaView.mIsUseDocomoDevice ? false : KeyguardBottomAreaView.this.canLaunchVoiceAssist();
            boolean z2 = KeyguardBottomAreaView.this.getResources().getBoolean(C0003R$bool.config_keyguardShowLeftAffordance);
            if (KeyguardBottomAreaView.this.mLeftIsVoiceAssist) {
                IntentButtonProvider.IntentButton.IconState iconState = this.mIconState;
                if (KeyguardBottomAreaView.this.mUserSetupComplete && z2) {
                    z = true;
                }
                iconState.isVisible = z;
                if (KeyguardBottomAreaView.this.mLeftAssistIcon == null) {
                    this.mIconState.drawable = ((FrameLayout) KeyguardBottomAreaView.this).mContext.getDrawable(C0006R$drawable.ic_mic_26dp);
                } else {
                    this.mIconState.drawable = KeyguardBottomAreaView.this.mLeftAssistIcon;
                }
                this.mIconState.contentDescription = ((FrameLayout) KeyguardBottomAreaView.this).mContext.getString(C0014R$string.accessibility_voice_assist_button);
            } else {
                IntentButtonProvider.IntentButton.IconState iconState2 = this.mIconState;
                if (KeyguardBottomAreaView.this.mUserSetupComplete && z2 && KeyguardBottomAreaView.this.isPhoneVisible()) {
                    z = true;
                }
                iconState2.isVisible = z;
                this.mIconState.drawable = ((FrameLayout) KeyguardBottomAreaView.this).mContext.getDrawable(17302770);
                this.mIconState.contentDescription = ((FrameLayout) KeyguardBottomAreaView.this).mContext.getString(C0014R$string.accessibility_phone_button);
            }
            return this.mIconState;
        }

        @Override // com.android.systemui.plugins.IntentButtonProvider.IntentButton
        public Intent getIntent() {
            return KeyguardBottomAreaView.PHONE_INTENT;
        }
    }

    private class DefaultRightButton implements IntentButtonProvider.IntentButton {
        private IntentButtonProvider.IntentButton.IconState mIconState;

        private DefaultRightButton() {
            this.mIconState = new IntentButtonProvider.IntentButton.IconState();
        }

        @Override // com.android.systemui.plugins.IntentButtonProvider.IntentButton
        public IntentButtonProvider.IntentButton.IconState getIcon() {
            ResolveInfo resolveSonyCameraIntent = KeyguardBottomAreaView.this.resolveSonyCameraIntent();
            boolean z = true;
            boolean z2 = KeyguardBottomAreaView.this.mStatusBar != null && !KeyguardBottomAreaView.this.mStatusBar.isCameraAllowedByAdmin();
            IntentButtonProvider.IntentButton.IconState iconState = this.mIconState;
            if (z2 || resolveSonyCameraIntent == null || !KeyguardBottomAreaView.this.getResources().getBoolean(C0003R$bool.config_keyguardShowCameraAffordance) || !KeyguardBottomAreaView.this.mUserSetupComplete) {
                z = false;
            }
            iconState.isVisible = z;
            this.mIconState.drawable = ((FrameLayout) KeyguardBottomAreaView.this).mContext.getDrawable(C0006R$drawable.ic_camera_alt_24dp);
            this.mIconState.contentDescription = ((FrameLayout) KeyguardBottomAreaView.this).mContext.getString(C0014R$string.accessibility_camera_button);
            return this.mIconState;
        }

        @Override // com.android.systemui.plugins.IntentButtonProvider.IntentButton
        public Intent getIntent() {
            return (!KeyguardBottomAreaView.this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) || KeyguardUpdateMonitor.getInstance(((FrameLayout) KeyguardBottomAreaView.this).mContext).getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser())) ? KeyguardBottomAreaView.INSECURE_CAMERA_INTENT : KeyguardBottomAreaView.SECURE_CAMERA_INTENT;
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        int safeInsetBottom = windowInsets.getDisplayCutout() != null ? windowInsets.getDisplayCutout().getSafeInsetBottom() : 0;
        if (isPaddingRelative()) {
            setPaddingRelative(getPaddingStart(), getPaddingTop(), getPaddingEnd(), safeInsetBottom);
        } else {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), safeInsetBottom);
        }
        return windowInsets;
    }
}
