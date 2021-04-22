package com.sonymobile.runtimeskinning;

public class RuntimeSkinningException extends Exception {
    public RuntimeSkinningException(Throwable th) {
        super(th);
    }

    public RuntimeSkinningException(String str) {
        super(new Throwable(str));
    }
}
