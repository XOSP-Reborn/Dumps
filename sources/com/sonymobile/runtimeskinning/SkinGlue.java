package com.sonymobile.runtimeskinning;

import android.content.Context;
import android.os.RemoteException;

/* access modifiers changed from: package-private */
public interface SkinGlue {
    public static final SkinGlue DISABLED = new GlueStub();

    SkinnedResources getSkinnedResourcesForCurrentSkin(String str, String str2, Context context) throws RemoteException, RuntimeSkinningException;

    void reset();

    public static final class GlueStub implements SkinGlue {
        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public SkinnedResources getSkinnedResourcesForCurrentSkin(String str, String str2, Context context) throws RemoteException {
            return null;
        }

        @Override // com.sonymobile.runtimeskinning.SkinGlue
        public void reset() {
        }

        private GlueStub() {
        }
    }
}
