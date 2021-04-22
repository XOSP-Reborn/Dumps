package com.android.settingslib.net;

import android.content.Context;
import android.net.NetworkTemplate;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DataUsageUtils {
    public static NetworkTemplate getMobileTemplate(Context context, int i) {
        TelephonyManager createForSubscriptionId = ((TelephonyManager) context.getSystemService(TelephonyManager.class)).createForSubscriptionId(i);
        SubscriptionInfo activeSubscriptionInfo = ((SubscriptionManager) context.getSystemService(SubscriptionManager.class)).getActiveSubscriptionInfo(i);
        NetworkTemplate buildTemplateMobileAll = NetworkTemplate.buildTemplateMobileAll(createForSubscriptionId.getSubscriberId(i));
        if (activeSubscriptionInfo != null) {
            return NetworkTemplate.normalize(buildTemplateMobileAll, createForSubscriptionId.getMergedSubscriberIds());
        }
        Log.i("DataUsageUtils", "Subscription is not active: " + i);
        return buildTemplateMobileAll;
    }
}
