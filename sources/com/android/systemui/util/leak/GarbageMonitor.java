package com.android.systemui.util.leak;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.LongSparseArray;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dumpable;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.util.leak.GarbageMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class GarbageMonitor implements Dumpable {
    private static final boolean DEBUG = Log.isLoggable("GarbageMonitor", 3);
    private static final boolean ENABLE_AM_HEAP_LIMIT;
    private static final boolean HEAP_TRACKING_ENABLED;
    private static final boolean LEAK_REPORTING_ENABLED;
    private final ActivityManager mAm;
    private final Context mContext;
    private final LongSparseArray<ProcessMemInfo> mData = new LongSparseArray<>();
    private DumpTruck mDumpTruck;
    private final Handler mHandler;
    private long mHeapLimit;
    private final LeakReporter mLeakReporter;
    private final ArrayList<Long> mPids = new ArrayList<>();
    private int[] mPidsArray = new int[1];
    private MemoryTile mQSTile;
    private final TrackedGarbage mTrackedGarbage;

    static {
        boolean z = false;
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("debug.enable_leak_reporting", false)) {
            z = true;
        }
        LEAK_REPORTING_ENABLED = z;
        boolean z2 = Build.IS_DEBUGGABLE;
        HEAP_TRACKING_ENABLED = z2;
        ENABLE_AM_HEAP_LIMIT = z2;
    }

    public GarbageMonitor(Context context, Looper looper, LeakDetector leakDetector, LeakReporter leakReporter) {
        this.mContext = context.getApplicationContext();
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mHandler = new BackgroundHeapCheckHandler(looper);
        this.mTrackedGarbage = leakDetector.getTrackedGarbage();
        this.mLeakReporter = leakReporter;
        this.mDumpTruck = new DumpTruck(this.mContext);
        if (ENABLE_AM_HEAP_LIMIT) {
            this.mHeapLimit = (long) Settings.Global.getInt(context.getContentResolver(), "systemui_am_heap_limit", this.mContext.getResources().getInteger(C0008R$integer.watch_heap_limit));
        }
    }

    public void startLeakMonitor() {
        if (this.mTrackedGarbage != null) {
            this.mHandler.sendEmptyMessage(1000);
        }
    }

    public void startHeapTracking() {
        startTrackingProcess((long) Process.myPid(), this.mContext.getPackageName(), System.currentTimeMillis());
        this.mHandler.sendEmptyMessage(3000);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean gcAndCheckGarbage() {
        if (this.mTrackedGarbage.countOldGarbage() <= 5) {
            return false;
        }
        Runtime.getRuntime().gc();
        return true;
    }

    /* access modifiers changed from: package-private */
    public void reinspectGarbageAfterGc() {
        int countOldGarbage = this.mTrackedGarbage.countOldGarbage();
        if (countOldGarbage > 5) {
            this.mLeakReporter.dumpLeak(countOldGarbage);
        }
    }

    public ProcessMemInfo getMemInfo(int i) {
        return this.mData.get((long) i);
    }

    public int[] getTrackedProcesses() {
        return this.mPidsArray;
    }

    public void startTrackingProcess(long j, String str, long j2) {
        synchronized (this.mPids) {
            if (!this.mPids.contains(Long.valueOf(j))) {
                this.mPids.add(Long.valueOf(j));
                updatePidsArrayL();
                this.mData.put(j, new ProcessMemInfo(j, str, j2));
            }
        }
    }

    private void updatePidsArrayL() {
        int size = this.mPids.size();
        this.mPidsArray = new int[size];
        StringBuffer stringBuffer = new StringBuffer("Now tracking processes: ");
        for (int i = 0; i < size; i++) {
            int intValue = this.mPids.get(i).intValue();
            this.mPidsArray[i] = intValue;
            stringBuffer.append(intValue);
            stringBuffer.append(" ");
        }
        if (DEBUG) {
            Log.v("GarbageMonitor", stringBuffer.toString());
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void update() {
        synchronized (this.mPids) {
            Debug.MemoryInfo[] processMemoryInfo = this.mAm.getProcessMemoryInfo(this.mPidsArray);
            int i = 0;
            while (true) {
                if (i >= processMemoryInfo.length) {
                    break;
                }
                Debug.MemoryInfo memoryInfo = processMemoryInfo[i];
                if (i <= this.mPids.size()) {
                    long intValue = (long) this.mPids.get(i).intValue();
                    ProcessMemInfo processMemInfo = this.mData.get(intValue);
                    long[] jArr = processMemInfo.pss;
                    int i2 = processMemInfo.head;
                    long totalPss = (long) memoryInfo.getTotalPss();
                    processMemInfo.currentPss = totalPss;
                    jArr[i2] = totalPss;
                    long[] jArr2 = processMemInfo.uss;
                    int i3 = processMemInfo.head;
                    long totalPrivateDirty = (long) memoryInfo.getTotalPrivateDirty();
                    processMemInfo.currentUss = totalPrivateDirty;
                    jArr2[i3] = totalPrivateDirty;
                    processMemInfo.head = (processMemInfo.head + 1) % processMemInfo.pss.length;
                    if (processMemInfo.currentPss > processMemInfo.max) {
                        processMemInfo.max = processMemInfo.currentPss;
                    }
                    if (processMemInfo.currentUss > processMemInfo.max) {
                        processMemInfo.max = processMemInfo.currentUss;
                    }
                    if (processMemInfo.currentPss == 0) {
                        if (DEBUG) {
                            Log.v("GarbageMonitor", "update: pid " + intValue + " has pss=0, it probably died");
                        }
                        this.mData.remove(intValue);
                    }
                    i++;
                } else if (DEBUG) {
                    Log.e("GarbageMonitor", "update: unknown process info received: " + memoryInfo);
                }
            }
            for (int size = this.mPids.size() - 1; size >= 0; size--) {
                if (this.mData.get((long) this.mPids.get(size).intValue()) == null) {
                    this.mPids.remove(size);
                    updatePidsArrayL();
                }
            }
        }
        MemoryTile memoryTile = this.mQSTile;
        if (memoryTile != null) {
            memoryTile.update();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setTile(MemoryTile memoryTile) {
        this.mQSTile = memoryTile;
        if (memoryTile != null) {
            memoryTile.update();
        }
    }

    /* access modifiers changed from: private */
    public static String formatBytes(long j) {
        String[] strArr = {"B", "K", "M", "G", "T"};
        int i = 0;
        while (i < strArr.length && j >= 1024) {
            j /= 1024;
            i++;
        }
        return j + strArr[i];
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Intent dumpHprofAndGetShareIntent() {
        DumpTruck dumpTruck = this.mDumpTruck;
        dumpTruck.captureHeaps(getTrackedProcesses());
        return dumpTruck.createShareIntent();
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("GarbageMonitor params:");
        printWriter.println(String.format("   mHeapLimit=%d KB", Long.valueOf(this.mHeapLimit)));
        printWriter.println(String.format("   GARBAGE_INSPECTION_INTERVAL=%d (%.1f mins)", 900000L, Float.valueOf(15.0f)));
        printWriter.println(String.format("   HEAP_TRACK_INTERVAL=%d (%.1f mins)", 60000L, Float.valueOf(1.0f)));
        printWriter.println(String.format("   HEAP_TRACK_HISTORY_LEN=%d (%.1f hr total)", 720, Float.valueOf(12.0f)));
        printWriter.println("GarbageMonitor tracked processes:");
        Iterator<Long> it = this.mPids.iterator();
        while (it.hasNext()) {
            ProcessMemInfo processMemInfo = this.mData.get(it.next().longValue());
            if (processMemInfo != null) {
                processMemInfo.dump(fileDescriptor, printWriter, strArr);
            }
        }
    }

    private static class MemoryIconDrawable extends Drawable {
        final Drawable baseIcon;
        final float dp;
        long limit;
        final Paint paint = new Paint();
        long pss;

        public int getOpacity() {
            return -3;
        }

        MemoryIconDrawable(Context context) {
            this.baseIcon = context.getDrawable(C0006R$drawable.ic_memory).mutate();
            this.dp = context.getResources().getDisplayMetrics().density;
            this.paint.setColor(QSTileImpl.getColorForState(context, 2));
        }

        public void setPss(long j) {
            if (j != this.pss) {
                this.pss = j;
                invalidateSelf();
            }
        }

        public void setLimit(long j) {
            if (j != this.limit) {
                this.limit = j;
                invalidateSelf();
            }
        }

        public void draw(Canvas canvas) {
            this.baseIcon.draw(canvas);
            long j = this.limit;
            if (j > 0) {
                long j2 = this.pss;
                if (j2 > 0) {
                    float min = Math.min(1.0f, ((float) j2) / ((float) j));
                    Rect bounds = getBounds();
                    float f = this.dp;
                    canvas.translate(((float) bounds.left) + (f * 8.0f), ((float) bounds.top) + (f * 5.0f));
                    float f2 = this.dp;
                    canvas.drawRect(0.0f, f2 * 14.0f * (1.0f - min), (8.0f * f2) + 1.0f, (f2 * 14.0f) + 1.0f, this.paint);
                }
            }
        }

        public void setBounds(int i, int i2, int i3, int i4) {
            super.setBounds(i, i2, i3, i4);
            this.baseIcon.setBounds(i, i2, i3, i4);
        }

        public int getIntrinsicHeight() {
            return this.baseIcon.getIntrinsicHeight();
        }

        public int getIntrinsicWidth() {
            return this.baseIcon.getIntrinsicWidth();
        }

        public void setAlpha(int i) {
            this.baseIcon.setAlpha(i);
        }

        public void setColorFilter(ColorFilter colorFilter) {
            this.baseIcon.setColorFilter(colorFilter);
            this.paint.setColorFilter(colorFilter);
        }

        public void setTint(int i) {
            super.setTint(i);
            this.baseIcon.setTint(i);
        }

        public void setTintList(ColorStateList colorStateList) {
            super.setTintList(colorStateList);
            this.baseIcon.setTintList(colorStateList);
        }

        public void setTintMode(PorterDuff.Mode mode) {
            super.setTintMode(mode);
            this.baseIcon.setTintMode(mode);
        }
    }

    private static class MemoryGraphIcon extends QSTile.Icon {
        long limit;
        long pss;

        private MemoryGraphIcon() {
        }

        public void setPss(long j) {
            this.pss = j;
        }

        public void setHeapLimit(long j) {
            this.limit = j;
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            MemoryIconDrawable memoryIconDrawable = new MemoryIconDrawable(context);
            memoryIconDrawable.setPss(this.pss);
            memoryIconDrawable.setLimit(this.limit);
            return memoryIconDrawable;
        }
    }

    public static class MemoryTile extends QSTileImpl<QSTile.State> {
        private boolean dumpInProgress;
        private final GarbageMonitor gm = SystemUIFactory.getInstance().getRootComponent().createGarbageMonitor();
        private ProcessMemInfo pmi;

        @Override // com.android.systemui.plugins.qs.QSTile, com.android.systemui.qs.tileimpl.QSTileImpl
        public int getMetricsCategory() {
            return 0;
        }

        public MemoryTile(QSHost qSHost) {
            super(qSHost);
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public QSTile.State newTileState() {
            return new QSTile.State();
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public Intent getLongClickIntent() {
            return new Intent();
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public void handleClick() {
            if (!this.dumpInProgress) {
                this.dumpInProgress = true;
                refreshState();
                new Thread("HeapDumpThread") {
                    /* class com.android.systemui.util.leak.GarbageMonitor.MemoryTile.AnonymousClass1 */

                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException unused) {
                        }
                        ((QSTileImpl) MemoryTile.this).mHandler.post(new Runnable(MemoryTile.this.gm.dumpHprofAndGetShareIntent()) {
                            /* class com.android.systemui.util.leak.$$Lambda$GarbageMonitor$MemoryTile$1$cmBeuqKr1b9hrY1trlao7X6pfIc */
                            private final /* synthetic */ Intent f$1;

                            {
                                this.f$1 = r2;
                            }

                            public final void run() {
                                GarbageMonitor.MemoryTile.AnonymousClass1.this.lambda$run$0$GarbageMonitor$MemoryTile$1(this.f$1);
                            }
                        });
                    }

                    public /* synthetic */ void lambda$run$0$GarbageMonitor$MemoryTile$1(Intent intent) {
                        MemoryTile.this.dumpInProgress = false;
                        MemoryTile.this.refreshState();
                        MemoryTile.this.getHost().collapsePanels();
                        ((QSTileImpl) MemoryTile.this).mContext.startActivity(intent);
                    }
                }.start();
            }
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public void handleSetListening(boolean z) {
            GarbageMonitor garbageMonitor = this.gm;
            if (garbageMonitor != null) {
                garbageMonitor.setTile(z ? this : null);
            }
            ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(ActivityManager.class);
            if (!z || this.gm.mHeapLimit <= 0) {
                activityManager.clearWatchHeapLimit();
            } else {
                activityManager.setWatchHeapLimit(this.gm.mHeapLimit * 1024);
            }
        }

        @Override // com.android.systemui.plugins.qs.QSTile
        public CharSequence getTileLabel() {
            return getState().label;
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public void handleUpdateState(QSTile.State state, Object obj) {
            String str;
            this.pmi = this.gm.getMemInfo(Process.myPid());
            MemoryGraphIcon memoryGraphIcon = new MemoryGraphIcon();
            memoryGraphIcon.setHeapLimit(this.gm.mHeapLimit);
            state.state = this.dumpInProgress ? 0 : 2;
            if (this.dumpInProgress) {
                str = "Dumping...";
            } else {
                str = this.mContext.getString(C0014R$string.heap_dump_tile_name);
            }
            state.label = str;
            ProcessMemInfo processMemInfo = this.pmi;
            if (processMemInfo != null) {
                memoryGraphIcon.setPss(processMemInfo.currentPss);
                state.secondaryLabel = String.format("pss: %s / %s", GarbageMonitor.formatBytes(this.pmi.currentPss * 1024), GarbageMonitor.formatBytes(this.gm.mHeapLimit * 1024));
            } else {
                memoryGraphIcon.setPss(0);
                state.secondaryLabel = null;
            }
            state.icon = memoryGraphIcon;
        }

        public void update() {
            refreshState();
        }
    }

    public static class ProcessMemInfo implements Dumpable {
        public long currentPss;
        public long currentUss;
        public int head = 0;
        public long max = 1;
        public String name;
        public long pid;
        public long[] pss = new long[720];
        public long startTime;
        public long[] uss = new long[720];

        public ProcessMemInfo(long j, String str, long j2) {
            this.pid = j;
            this.name = str;
            this.startTime = j2;
        }

        public long getUptime() {
            return System.currentTimeMillis() - this.startTime;
        }

        @Override // com.android.systemui.Dumpable
        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.print("{ \"pid\": ");
            printWriter.print(this.pid);
            printWriter.print(", \"name\": \"");
            printWriter.print(this.name.replace('\"', '-'));
            printWriter.print("\", \"start\": ");
            printWriter.print(this.startTime);
            printWriter.print(", \"pss\": [");
            for (int i = 0; i < this.pss.length; i++) {
                if (i > 0) {
                    printWriter.print(",");
                }
                long[] jArr = this.pss;
                printWriter.print(jArr[(this.head + i) % jArr.length]);
            }
            printWriter.print("], \"uss\": [");
            for (int i2 = 0; i2 < this.uss.length; i2++) {
                if (i2 > 0) {
                    printWriter.print(",");
                }
                long[] jArr2 = this.uss;
                printWriter.print(jArr2[(this.head + i2) % jArr2.length]);
            }
            printWriter.println("] }");
        }
    }

    public static class Service extends SystemUI implements Dumpable {
        private GarbageMonitor mGarbageMonitor;

        @Override // com.android.systemui.SystemUI
        public void start() {
            boolean z = false;
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "sysui_force_enable_leak_reporting", 0) != 0) {
                z = true;
            }
            this.mGarbageMonitor = SystemUIFactory.getInstance().getRootComponent().createGarbageMonitor();
            if (GarbageMonitor.LEAK_REPORTING_ENABLED || z) {
                this.mGarbageMonitor.startLeakMonitor();
            }
            if (GarbageMonitor.HEAP_TRACKING_ENABLED || z) {
                this.mGarbageMonitor.startHeapTracking();
            }
        }

        @Override // com.android.systemui.SystemUI, com.android.systemui.Dumpable
        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            GarbageMonitor garbageMonitor = this.mGarbageMonitor;
            if (garbageMonitor != null) {
                garbageMonitor.dump(fileDescriptor, printWriter, strArr);
            }
        }
    }

    private class BackgroundHeapCheckHandler extends Handler {
        BackgroundHeapCheckHandler(Looper looper) {
            super(looper);
            if (Looper.getMainLooper().equals(looper)) {
                throw new RuntimeException("BackgroundHeapCheckHandler may not run on the ui thread");
            }
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1000) {
                if (GarbageMonitor.this.gcAndCheckGarbage()) {
                    postDelayed(new Runnable() {
                        /* class com.android.systemui.util.leak.$$Lambda$XMHjUeThvUDRPlJmBo9djG71pM8 */

                        public final void run() {
                            GarbageMonitor.this.reinspectGarbageAfterGc();
                        }
                    }, 100);
                }
                removeMessages(1000);
                sendEmptyMessageDelayed(1000, 900000);
            } else if (i == 3000) {
                GarbageMonitor.this.update();
                removeMessages(3000);
                sendEmptyMessageDelayed(3000, 60000);
            }
        }
    }
}
