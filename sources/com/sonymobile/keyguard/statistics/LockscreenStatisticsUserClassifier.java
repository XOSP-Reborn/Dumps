package com.sonymobile.keyguard.statistics;

import android.content.pm.UserInfo;

public class LockscreenStatisticsUserClassifier {

    public enum UserType {
        Owner("Owner"),
        GuestUser("Guest-User"),
        RestrictedUser("Restricted-User"),
        SecondaryUser("Secondary-User"),
        PrimaryUser("Primary-User"),
        AdminUser("Admin-User"),
        Unknown("Unknown");
        
        public final String label;

        private UserType(String str) {
            this.label = str;
        }
    }

    public String getUserType(UserInfo userInfo) {
        String str;
        if (userInfo == null) {
            return UserType.Unknown.label;
        }
        if (userInfo.id == 0) {
            str = UserType.Owner.label;
        } else if (userInfo.isPrimary()) {
            str = UserType.PrimaryUser.label;
        } else if (userInfo.isAdmin()) {
            str = UserType.AdminUser.label;
        } else if (userInfo.isRestricted()) {
            str = UserType.RestrictedUser.label;
        } else if (userInfo.isGuest()) {
            str = UserType.GuestUser.label;
        } else {
            str = UserType.SecondaryUser.label;
        }
        return str + "_" + userInfo.id;
    }
}
