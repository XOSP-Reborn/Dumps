package com.android.systemui.qs;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyEventLogger;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserManager;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.C0015R$style;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.SecurityController;

public class QSSecurityFooter implements View.OnClickListener, DialogInterface.OnClickListener {
    protected static final boolean DEBUG = Log.isLoggable("QSSecurityFooter", 3);
    private final ActivityStarter mActivityStarter;
    private final Callback mCallback = new Callback();
    private final Context mContext;
    private AlertDialog mDialog;
    private final View mDivider;
    private final ImageView mFooterIcon;
    private int mFooterIconId;
    private final TextView mFooterText;
    private CharSequence mFooterTextContent = null;
    protected H mHandler;
    private QSTileHost mHost;
    private boolean mIsVisible;
    private final Handler mMainHandler;
    private final View mRootView;
    private final SecurityController mSecurityController;
    private final UserManager mUm;
    private final Runnable mUpdateDisplayState = new Runnable() {
        /* class com.android.systemui.qs.QSSecurityFooter.AnonymousClass2 */

        public void run() {
            if (QSSecurityFooter.this.mFooterTextContent != null) {
                QSSecurityFooter.this.mFooterText.setText(QSSecurityFooter.this.mFooterTextContent);
            }
            int i = 0;
            QSSecurityFooter.this.mRootView.setVisibility(QSSecurityFooter.this.mIsVisible ? 0 : 8);
            if (QSSecurityFooter.this.mDivider != null) {
                View view = QSSecurityFooter.this.mDivider;
                if (QSSecurityFooter.this.mIsVisible) {
                    i = 8;
                }
                view.setVisibility(i);
            }
        }
    };
    private final Runnable mUpdateIcon = new Runnable() {
        /* class com.android.systemui.qs.QSSecurityFooter.AnonymousClass1 */

        public void run() {
            QSSecurityFooter.this.mFooterIcon.setImageResource(QSSecurityFooter.this.mFooterIconId);
        }
    };

    public QSSecurityFooter(QSPanel qSPanel, Context context) {
        View view = null;
        this.mRootView = LayoutInflater.from(context).inflate(C0010R$layout.quick_settings_footer, (ViewGroup) qSPanel, false);
        this.mRootView.setOnClickListener(this);
        this.mFooterText = (TextView) this.mRootView.findViewById(C0007R$id.footer_text);
        this.mFooterIcon = (ImageView) this.mRootView.findViewById(C0007R$id.footer_icon);
        this.mFooterIconId = C0006R$drawable.ic_info_outline;
        this.mContext = context;
        this.mMainHandler = new Handler(Looper.myLooper());
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mSecurityController = (SecurityController) Dependency.get(SecurityController.class);
        this.mHandler = new H((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mDivider = qSPanel != null ? qSPanel.getDivider() : view;
        this.mUm = (UserManager) this.mContext.getSystemService("user");
    }

    public void setHostEnvironment(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public void setListening(boolean z) {
        if (z) {
            this.mSecurityController.addCallback(this.mCallback);
            refreshState();
            return;
        }
        this.mSecurityController.removeCallback(this.mCallback);
    }

    public void onConfigurationChanged() {
        FontSizeUtils.updateFontSize(this.mFooterText, C0005R$dimen.qs_tile_text_size);
    }

    public View getView() {
        return this.mRootView;
    }

    public void onClick(View view) {
        this.mHandler.sendEmptyMessage(0);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleClick() {
        showDeviceMonitoringDialog();
        DevicePolicyEventLogger.createEvent(57).write();
    }

    public void showDeviceMonitoringDialog() {
        this.mHost.collapsePanels();
        createDialog();
    }

    public void refreshState() {
        this.mHandler.sendEmptyMessage(1);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleRefreshState() {
        boolean isDeviceManaged = this.mSecurityController.isDeviceManaged();
        UserInfo userInfo = this.mUm.getUserInfo(ActivityManager.getCurrentUser());
        boolean z = true;
        boolean z2 = UserManager.isDeviceInDemoMode(this.mContext) && userInfo != null && userInfo.isDemo();
        boolean hasWorkProfile = this.mSecurityController.hasWorkProfile();
        boolean hasCACertInCurrentUser = this.mSecurityController.hasCACertInCurrentUser();
        boolean hasCACertInWorkProfile = this.mSecurityController.hasCACertInWorkProfile();
        boolean isNetworkLoggingEnabled = this.mSecurityController.isNetworkLoggingEnabled();
        String primaryVpnName = this.mSecurityController.getPrimaryVpnName();
        String workProfileVpnName = this.mSecurityController.getWorkProfileVpnName();
        CharSequence deviceOwnerOrganizationName = this.mSecurityController.getDeviceOwnerOrganizationName();
        CharSequence workProfileOrganizationName = this.mSecurityController.getWorkProfileOrganizationName();
        if ((!isDeviceManaged || z2) && !hasCACertInCurrentUser && !hasCACertInWorkProfile && primaryVpnName == null && workProfileVpnName == null) {
            z = false;
        }
        this.mIsVisible = z;
        this.mFooterTextContent = getFooterText(isDeviceManaged, hasWorkProfile, hasCACertInCurrentUser, hasCACertInWorkProfile, isNetworkLoggingEnabled, primaryVpnName, workProfileVpnName, deviceOwnerOrganizationName, workProfileOrganizationName);
        int i = C0006R$drawable.ic_info_outline;
        if (!(primaryVpnName == null && workProfileVpnName == null)) {
            if (this.mSecurityController.isVpnBranded()) {
                i = C0006R$drawable.stat_sys_branded_vpn;
            } else {
                i = C0006R$drawable.stat_sys_vpn_ic;
            }
        }
        if (this.mFooterIconId != i) {
            this.mFooterIconId = i;
            this.mMainHandler.post(this.mUpdateIcon);
        }
        this.mMainHandler.post(this.mUpdateDisplayState);
    }

    /* access modifiers changed from: protected */
    public CharSequence getFooterText(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String str, String str2, CharSequence charSequence, CharSequence charSequence2) {
        if (z) {
            if (z3 || z4 || z5) {
                if (charSequence == null) {
                    return this.mContext.getString(C0014R$string.quick_settings_disclosure_management_monitoring);
                }
                return this.mContext.getString(C0014R$string.quick_settings_disclosure_named_management_monitoring, charSequence);
            } else if (str == null || str2 == null) {
                if (str == null && str2 == null) {
                    if (charSequence == null) {
                        return this.mContext.getString(C0014R$string.quick_settings_disclosure_management);
                    }
                    return this.mContext.getString(C0014R$string.quick_settings_disclosure_named_management, charSequence);
                } else if (charSequence == null) {
                    Context context = this.mContext;
                    int i = C0014R$string.quick_settings_disclosure_management_named_vpn;
                    Object[] objArr = new Object[1];
                    if (str == null) {
                        str = str2;
                    }
                    objArr[0] = str;
                    return context.getString(i, objArr);
                } else {
                    Context context2 = this.mContext;
                    int i2 = C0014R$string.quick_settings_disclosure_named_management_named_vpn;
                    Object[] objArr2 = new Object[2];
                    objArr2[0] = charSequence;
                    if (str == null) {
                        str = str2;
                    }
                    objArr2[1] = str;
                    return context2.getString(i2, objArr2);
                }
            } else if (charSequence == null) {
                return this.mContext.getString(C0014R$string.quick_settings_disclosure_management_vpns);
            } else {
                return this.mContext.getString(C0014R$string.quick_settings_disclosure_named_management_vpns, charSequence);
            }
        } else if (z4) {
            if (charSequence2 == null) {
                return this.mContext.getString(C0014R$string.quick_settings_disclosure_managed_profile_monitoring);
            }
            return this.mContext.getString(C0014R$string.quick_settings_disclosure_named_managed_profile_monitoring, charSequence2);
        } else if (z3) {
            return this.mContext.getString(C0014R$string.quick_settings_disclosure_monitoring);
        } else {
            if (str != null && str2 != null) {
                return this.mContext.getString(C0014R$string.quick_settings_disclosure_vpns);
            }
            if (str2 != null) {
                return this.mContext.getString(C0014R$string.quick_settings_disclosure_managed_profile_named_vpn, str2);
            } else if (str == null) {
                return null;
            } else {
                if (z2) {
                    return this.mContext.getString(C0014R$string.quick_settings_disclosure_personal_profile_named_vpn, str);
                }
                return this.mContext.getString(C0014R$string.quick_settings_disclosure_named_vpn, str);
            }
        }
    }

    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -2) {
            Intent intent = new Intent("android.settings.ENTERPRISE_PRIVACY_SETTINGS");
            this.mDialog.dismiss();
            this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        }
    }

    private void createDialog() {
        boolean isDeviceManaged = this.mSecurityController.isDeviceManaged();
        boolean hasWorkProfile = this.mSecurityController.hasWorkProfile();
        CharSequence deviceOwnerOrganizationName = this.mSecurityController.getDeviceOwnerOrganizationName();
        boolean hasCACertInCurrentUser = this.mSecurityController.hasCACertInCurrentUser();
        boolean hasCACertInWorkProfile = this.mSecurityController.hasCACertInWorkProfile();
        boolean isNetworkLoggingEnabled = this.mSecurityController.isNetworkLoggingEnabled();
        String primaryVpnName = this.mSecurityController.getPrimaryVpnName();
        String workProfileVpnName = this.mSecurityController.getWorkProfileVpnName();
        this.mDialog = new SystemUIDialog(this.mContext);
        this.mDialog.requestWindowFeature(1);
        View inflate = LayoutInflater.from(new ContextThemeWrapper(this.mContext, C0015R$style.Theme_SystemUI_Dialog)).inflate(C0010R$layout.quick_settings_footer_dialog, (ViewGroup) null, false);
        this.mDialog.setView(inflate);
        this.mDialog.setButton(-1, getPositiveButton(), this);
        CharSequence managementMessage = getManagementMessage(isDeviceManaged, deviceOwnerOrganizationName);
        if (managementMessage == null) {
            inflate.findViewById(C0007R$id.device_management_disclosures).setVisibility(8);
        } else {
            inflate.findViewById(C0007R$id.device_management_disclosures).setVisibility(0);
            ((TextView) inflate.findViewById(C0007R$id.device_management_warning)).setText(managementMessage);
            this.mDialog.setButton(-2, getSettingsButton(), this);
        }
        CharSequence caCertsMessage = getCaCertsMessage(isDeviceManaged, hasCACertInCurrentUser, hasCACertInWorkProfile);
        if (caCertsMessage == null) {
            inflate.findViewById(C0007R$id.ca_certs_disclosures).setVisibility(8);
        } else {
            inflate.findViewById(C0007R$id.ca_certs_disclosures).setVisibility(0);
            TextView textView = (TextView) inflate.findViewById(C0007R$id.ca_certs_warning);
            textView.setText(caCertsMessage);
            textView.setMovementMethod(new LinkMovementMethod());
        }
        CharSequence networkLoggingMessage = getNetworkLoggingMessage(isNetworkLoggingEnabled);
        if (networkLoggingMessage == null) {
            inflate.findViewById(C0007R$id.network_logging_disclosures).setVisibility(8);
        } else {
            inflate.findViewById(C0007R$id.network_logging_disclosures).setVisibility(0);
            ((TextView) inflate.findViewById(C0007R$id.network_logging_warning)).setText(networkLoggingMessage);
        }
        CharSequence vpnMessage = getVpnMessage(isDeviceManaged, hasWorkProfile, primaryVpnName, workProfileVpnName);
        if (vpnMessage == null) {
            inflate.findViewById(C0007R$id.vpn_disclosures).setVisibility(8);
        } else {
            inflate.findViewById(C0007R$id.vpn_disclosures).setVisibility(0);
            TextView textView2 = (TextView) inflate.findViewById(C0007R$id.vpn_warning);
            textView2.setText(vpnMessage);
            textView2.setMovementMethod(new LinkMovementMethod());
        }
        configSubtitleVisibility(managementMessage != null, caCertsMessage != null, networkLoggingMessage != null, vpnMessage != null, inflate);
        this.mDialog.show();
        this.mDialog.getWindow().setLayout(-1, -2);
    }

    /* access modifiers changed from: protected */
    public void configSubtitleVisibility(boolean z, boolean z2, boolean z3, boolean z4, View view) {
        if (!z) {
            int i = 0;
            if (z2) {
                i = 1;
            }
            if (z3) {
                i++;
            }
            if (z4) {
                i++;
            }
            if (i == 1) {
                if (z2) {
                    view.findViewById(C0007R$id.ca_certs_subtitle).setVisibility(8);
                }
                if (z3) {
                    view.findViewById(C0007R$id.network_logging_subtitle).setVisibility(8);
                }
                if (z4) {
                    view.findViewById(C0007R$id.vpn_subtitle).setVisibility(8);
                }
            }
        }
    }

    private String getSettingsButton() {
        return this.mContext.getString(C0014R$string.monitoring_button_view_policies);
    }

    private String getPositiveButton() {
        return this.mContext.getString(C0014R$string.ok);
    }

    /* access modifiers changed from: protected */
    public CharSequence getManagementMessage(boolean z, CharSequence charSequence) {
        if (!z) {
            return null;
        }
        if (charSequence == null) {
            return this.mContext.getString(C0014R$string.monitoring_description_management);
        }
        return this.mContext.getString(C0014R$string.monitoring_description_named_management, charSequence);
    }

    /* access modifiers changed from: protected */
    public CharSequence getCaCertsMessage(boolean z, boolean z2, boolean z3) {
        if (!z2 && !z3) {
            return null;
        }
        if (z) {
            return this.mContext.getString(C0014R$string.monitoring_description_management_ca_certificate);
        }
        if (z3) {
            return this.mContext.getString(C0014R$string.monitoring_description_managed_profile_ca_certificate);
        }
        return this.mContext.getString(C0014R$string.monitoring_description_ca_certificate);
    }

    /* access modifiers changed from: protected */
    public CharSequence getNetworkLoggingMessage(boolean z) {
        if (!z) {
            return null;
        }
        return this.mContext.getString(C0014R$string.monitoring_description_management_network_logging);
    }

    /* access modifiers changed from: protected */
    public CharSequence getVpnMessage(boolean z, boolean z2, String str, String str2) {
        if (str == null && str2 == null) {
            return null;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        if (z) {
            if (str == null || str2 == null) {
                Context context = this.mContext;
                int i = C0014R$string.monitoring_description_named_vpn;
                Object[] objArr = new Object[1];
                if (str == null) {
                    str = str2;
                }
                objArr[0] = str;
                spannableStringBuilder.append((CharSequence) context.getString(i, objArr));
            } else {
                spannableStringBuilder.append((CharSequence) this.mContext.getString(C0014R$string.monitoring_description_two_named_vpns, str, str2));
            }
        } else if (str != null && str2 != null) {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(C0014R$string.monitoring_description_two_named_vpns, str, str2));
        } else if (str2 != null) {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(C0014R$string.monitoring_description_managed_profile_named_vpn, str2));
        } else if (z2) {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(C0014R$string.monitoring_description_personal_profile_named_vpn, str));
        } else {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(C0014R$string.monitoring_description_named_vpn, str));
        }
        spannableStringBuilder.append((CharSequence) this.mContext.getString(C0014R$string.monitoring_description_vpn_settings_separator));
        spannableStringBuilder.append(this.mContext.getString(C0014R$string.monitoring_description_vpn_settings), new VpnSpan(), 0);
        return spannableStringBuilder;
    }

    /* access modifiers changed from: private */
    public class Callback implements SecurityController.SecurityControllerCallback {
        private Callback() {
        }

        @Override // com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback
        public void onStateChanged() {
            QSSecurityFooter.this.refreshState();
        }
    }

    /* access modifiers changed from: private */
    public class H extends Handler {
        private H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            try {
                if (message.what == 1) {
                    QSSecurityFooter.this.handleRefreshState();
                } else if (message.what == 0) {
                    QSSecurityFooter.this.handleClick();
                }
            } catch (Throwable th) {
                String str = "Error in " + ((String) null);
                Log.w("QSSecurityFooter", str, th);
                QSSecurityFooter.this.mHost.warn(str, th);
            }
        }
    }

    /* access modifiers changed from: protected */
    public class VpnSpan extends ClickableSpan {
        public int hashCode() {
            return 314159257;
        }

        protected VpnSpan() {
        }

        public void onClick(View view) {
            Intent intent = new Intent("android.settings.VPN_SETTINGS");
            QSSecurityFooter.this.mDialog.dismiss();
            QSSecurityFooter.this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        }

        public boolean equals(Object obj) {
            return obj instanceof VpnSpan;
        }
    }
}
