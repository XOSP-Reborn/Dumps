package com.android.systemui.statusbar.policy;

import com.android.systemui.C0006R$drawable;

public class WifiIcons {
    public static final int[][] QS_WIFI_4_SIGNAL_STRENGTH = {WIFI_4_NO_INTERNET_ICONS, WIFI_4_FULL_ICONS};
    public static final int[][] QS_WIFI_5_SIGNAL_STRENGTH = {WIFI_5_NO_INTERNET_ICONS, WIFI_5_FULL_ICONS};
    public static final int[][] QS_WIFI_6_SIGNAL_STRENGTH = {WIFI_6_NO_INTERNET_ICONS, WIFI_6_FULL_ICONS};
    public static final int[][] QS_WIFI_SIGNAL_STRENGTH = {WIFI_NO_INTERNET_ICONS, WIFI_FULL_ICONS};
    static final int[] WIFI_4_FULL_ICONS = {17302849, 17302850, 17302851, 17302852, 17302853};
    private static final int[] WIFI_4_NO_INTERNET_ICONS = {C0006R$drawable.ic_qs_wifi_4_0, C0006R$drawable.ic_qs_wifi_4_1, C0006R$drawable.ic_qs_wifi_4_2, C0006R$drawable.ic_qs_wifi_4_3, C0006R$drawable.ic_qs_wifi_4_4};
    static final int[][] WIFI_4_SIGNAL_STRENGTH = QS_WIFI_4_SIGNAL_STRENGTH;
    static final int[] WIFI_5_FULL_ICONS = {17302854, 17302855, 17302856, 17302857, 17302858};
    private static final int[] WIFI_5_NO_INTERNET_ICONS = {C0006R$drawable.ic_qs_wifi_5_0, C0006R$drawable.ic_qs_wifi_5_1, C0006R$drawable.ic_qs_wifi_5_2, C0006R$drawable.ic_qs_wifi_5_3, C0006R$drawable.ic_qs_wifi_5_4};
    static final int[][] WIFI_5_SIGNAL_STRENGTH = QS_WIFI_5_SIGNAL_STRENGTH;
    static final int[] WIFI_6_FULL_ICONS = {17302859, 17302860, 17302861, 17302862, 17302863};
    private static final int[] WIFI_6_NO_INTERNET_ICONS = {C0006R$drawable.ic_qs_wifi_6_0, C0006R$drawable.ic_qs_wifi_6_1, C0006R$drawable.ic_qs_wifi_6_2, C0006R$drawable.ic_qs_wifi_6_3, C0006R$drawable.ic_qs_wifi_6_4};
    static final int[][] WIFI_6_SIGNAL_STRENGTH = QS_WIFI_6_SIGNAL_STRENGTH;
    static final int[] WIFI_FULL_ICONS = {17302865, 17302866, 17302867, 17302868, 17302869};
    static final int WIFI_LEVEL_COUNT = WIFI_SIGNAL_STRENGTH[0].length;
    private static final int[] WIFI_NO_INTERNET_ICONS = {C0006R$drawable.ic_qs_wifi_0, C0006R$drawable.ic_qs_wifi_1, C0006R$drawable.ic_qs_wifi_2, C0006R$drawable.ic_qs_wifi_3, C0006R$drawable.ic_qs_wifi_4};
    static final int[][] WIFI_SIGNAL_STRENGTH = QS_WIFI_SIGNAL_STRENGTH;
}
