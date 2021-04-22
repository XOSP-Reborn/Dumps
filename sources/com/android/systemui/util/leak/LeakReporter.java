package com.android.systemui.util.leak;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.os.SystemProperties;
import androidx.core.content.FileProvider;
import com.google.android.collect.Lists;
import java.io.File;
import java.util.ArrayList;

public class LeakReporter {
    private final Context mContext;
    private final LeakDetector mLeakDetector;
    private final String mLeakReportEmail;

    public LeakReporter(Context context, LeakDetector leakDetector, String str) {
        this.mContext = context;
        this.mLeakDetector = leakDetector;
        this.mLeakReportEmail = str;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x00c3, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:?, code lost:
        r3.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x00c9, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x00ca, code lost:
        r0.addSuppressed(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x00ce, code lost:
        throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dumpLeak(int r17) {
        /*
        // Method dump skipped, instructions count: 214
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.util.leak.LeakReporter.dumpLeak(int):void");
    }

    private Intent getIntent(File file, File file2) {
        Uri uriForFile = FileProvider.getUriForFile(this.mContext, "com.android.systemui.fileprovider", file2);
        Uri uriForFile2 = FileProvider.getUriForFile(this.mContext, "com.android.systemui.fileprovider", file);
        Intent intent = new Intent("android.intent.action.SEND_MULTIPLE");
        intent.addFlags(1);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("application/vnd.android.leakreport");
        intent.putExtra("android.intent.extra.SUBJECT", "SystemUI leak report");
        intent.putExtra("android.intent.extra.TEXT", "Build info: " + SystemProperties.get("ro.build.description"));
        ClipData clipData = new ClipData(null, new String[]{"application/vnd.android.leakreport"}, new ClipData.Item(null, null, null, uriForFile));
        ArrayList<? extends Parcelable> newArrayList = Lists.newArrayList(new Uri[]{uriForFile});
        clipData.addItem(new ClipData.Item(null, null, null, uriForFile2));
        newArrayList.add(uriForFile2);
        intent.setClipData(clipData);
        intent.putParcelableArrayListExtra("android.intent.extra.STREAM", newArrayList);
        String str = this.mLeakReportEmail;
        if (str != null) {
            intent.putExtra("android.intent.extra.EMAIL", new String[]{str});
        }
        return intent;
    }
}
