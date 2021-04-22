package com.sonymobile.runtimeskinning;

import android.content.Context;
import android.os.RemoteException;

public class RuntimeSkinningHelper {
    private static final SkinGlueFactory[] FACTORIES = {new Rev6SkinGlueFactory(), new Rev5aSkinGlueFactory(), new Rev5SkinGlueFactory(), new Rev4SkinGlueFactory(), new Rev3aSkinGlueFactory(), new Rev3SkinGlueFactory()};
    private static Context sContext;
    private SkinGlue mSkinGlue;

    public synchronized void init(Context context) {
        if (context != null) {
            Context applicationContext = context.getApplicationContext();
            if (sContext != null) {
                if (sContext != applicationContext) {
                    throw new IllegalArgumentException("The context doesn't belong to the first app that made the call");
                }
            }
            sContext = applicationContext;
            if (this.mSkinGlue == null) {
                SkinGlueFactory[] skinGlueFactoryArr = FACTORIES;
                for (SkinGlueFactory skinGlueFactory : skinGlueFactoryArr) {
                    if (skinGlueFactory.isApplicable(sContext)) {
                        this.mSkinGlue = skinGlueFactory.produceInstance(sContext);
                        return;
                    }
                }
                this.mSkinGlue = SkinGlue.DISABLED;
            }
            return;
        }
        throw new NullPointerException();
    }

    public synchronized void reset() {
        if (this.mSkinGlue != null) {
            this.mSkinGlue.reset();
            this.mSkinGlue = null;
        }
    }

    /* access modifiers changed from: package-private */
    public final SkinGlue getSkinGlue(Context context) {
        init(context);
        return this.mSkinGlue;
    }

    public SkinnedResources getSkinnedResourcesForCurrentSkin(String str, String str2, Context context) throws RuntimeSkinningException, RemoteException {
        try {
            return getSkinGlue(context).getSkinnedResourcesForCurrentSkin(str, str2, context);
        } catch (Throwable th) {
            if (!(th instanceof RuntimeSkinningException)) {
                ExceptionHandler.reThrow(th);
                return null;
            }
            throw th;
        }
    }
}
