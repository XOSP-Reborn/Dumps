package com.sonymobile.runtimeskinning;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class OverlayExtractor {
    public static String getPackageName(Resources resources, int i) throws IOException, XmlPullParserException {
        XmlResourceParser createManifestParser = createManifestParser(resources, i);
        if (XmlUtil.startAtElement(createManifestParser, "manifest")) {
            return createManifestParser.getAttributeValue(null, "package");
        }
        throw new XmlPullParserException("No <manifest> elements defined or referenced in the xml");
    }

    public static Map<String, String> getResourcesAllowedToOverlay(Resources resources, ApplicationInfo applicationInfo) throws IOException, XmlPullParserException {
        ArrayMap arrayMap = new ArrayMap();
        Bundle bundle = applicationInfo.metaData;
        int i = bundle != null ? bundle.getInt("com.sonymobile.runtimeskinning.SKIN_PERMISSIONS", 0) : 0;
        if (i == 0) {
            return arrayMap;
        }
        XmlResourceParser xml = resources.getXml(i);
        if (XmlUtil.startAtElement(xml, "overlay-resources")) {
            int depth = xml.getDepth() + 1;
            while (XmlUtil.findTag(xml, "overlay", depth)) {
                int attributeResourceValue = xml.getAttributeResourceValue(null, "id", 0);
                if (attributeResourceValue != 0) {
                    try {
                        arrayMap.put(resources.getResourceEntryName(attributeResourceValue), resources.getResourceTypeName(attributeResourceValue));
                    } catch (Resources.NotFoundException unused) {
                    }
                } else {
                    throw new XmlPullParserException("overlay does not specify an id");
                }
            }
            if (!XmlUtil.startAtElement(xml, "overlay-resources")) {
                return arrayMap;
            }
            throw new XmlPullParserException("Multiple overlay-resources elements defined or referenced in xml");
        }
        throw new XmlPullParserException("No overlay-resources elements defined or referenced in the xml of " + applicationInfo.packageName);
    }

    public static List<String> getSkinOverlayPaths(Context context, String str, String str2, String str3, int[] iArr) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 128);
        if (str3 == null) {
            str3 = getPackageAlias(packageManager, str2);
        }
        return parseOverlays(context, applicationInfo, str2, str3, iArr);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:72:0x00f9, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x00fb, code lost:
        if (r6 != null) goto L_0x00fd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:?, code lost:
        r6.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x0101, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x0102, code lost:
        r0.addSuppressed(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0106, code lost:
        throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static java.util.List<java.lang.String> parseOverlays(android.content.Context r17, android.content.pm.ApplicationInfo r18, java.lang.String r19, java.lang.String r20, int[] r21) throws android.content.pm.PackageManager.NameNotFoundException {
        /*
        // Method dump skipped, instructions count: 264
        */
        throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.runtimeskinning.OverlayExtractor.parseOverlays(android.content.Context, android.content.pm.ApplicationInfo, java.lang.String, java.lang.String, int[]):java.util.List");
    }

    private static int convertGroup(String str) throws XmlPullParserException {
        try {
            int parseInt = Integer.parseInt(str);
            if (parseInt > 0) {
                return parseInt;
            }
            throw new XmlPullParserException("Bad group value: " + parseInt + ", must be larger than: " + 0);
        } catch (NumberFormatException unused) {
            throw new XmlPullParserException("Bad group value: " + str + ", must be an integer");
        }
    }

    private static XmlResourceParser getParser(PackageManager packageManager, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        Bundle bundle = applicationInfo.metaData;
        int i = 0;
        if (bundle != null) {
            if (bundle.containsKey("com.sonymobile.runtimeskinning.SKIN_DEFINITION_V2")) {
                i = bundle.getInt("com.sonymobile.runtimeskinning.SKIN_DEFINITION_V2", 0);
            } else if (bundle.containsKey("com.sonymobile.runtimeskinning.SKIN_DEFINITION")) {
                i = bundle.getInt("com.sonymobile.runtimeskinning.SKIN_DEFINITION", 0);
            }
        }
        try {
            Resources resourcesForApplication = packageManager.getResourcesForApplication(applicationInfo);
            if (i != 0) {
                return resourcesForApplication.getXml(i);
            }
            return createManifestParser(applicationInfo, resourcesForApplication);
        } catch (IOException e) {
            Log.e("runtime-skinning-lib", "Failed to retrieve a xml resource parser", e);
            return null;
        }
    }

    private static XmlResourceParser createManifestParser(ApplicationInfo applicationInfo, Resources resources) throws IOException {
        String str;
        AssetManager assets = resources.getAssets();
        int firstCookieValue = ResourcesUtil.getFirstCookieValue(resources);
        Method method = ReflectionUtils.getMethod(AssetManager.class, "getCookieName", String.class, Integer.TYPE);
        if (method != null) {
            do {
                str = (String) ReflectionUtils.invokeMethod(method, assets, String.class, Integer.valueOf(firstCookieValue));
                if (applicationInfo.publicSourceDir.equals(str)) {
                    break;
                }
                firstCookieValue++;
            } while (str != null);
            if (str == null) {
                throw new IOException("Failed to find a XmlResourceParser");
            }
        } else {
            String str2 = null;
            do {
                try {
                    str2 = getPackageName(resources, firstCookieValue);
                } catch (XmlPullParserException e) {
                    Log.e("runtime-skinning-lib", String.format("Failed to parse manifest %s for cookie=%d", applicationInfo.packageName, Integer.valueOf(firstCookieValue)), e);
                }
                if (applicationInfo.packageName.equals(str2)) {
                    break;
                }
                firstCookieValue++;
            } while (str2 != null);
            if (str2 == null) {
                throw new IOException("Failed to find a XmlResourceParser");
            }
        }
        return assets.openXmlResourceParser(firstCookieValue, "AndroidManifest.xml");
    }

    private static XmlResourceParser createManifestParser(Resources resources, int i) throws IOException {
        return resources.getAssets().openXmlResourceParser(i, "AndroidManifest.xml");
    }

    private static String getVersion(ApplicationInfo applicationInfo) {
        Bundle bundle = applicationInfo.metaData;
        return (bundle == null || !bundle.containsKey("com.sonymobile.runtimeskinning.SKIN_DEFINITION_V2")) ? "1" : "2";
    }

    private static String getApplicationSkinInternalPath(String str, String str2) {
        return str + "?entry=assets/" + str2;
    }

    private static String getPackageAlias(PackageManager packageManager, String str) {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 128);
            if (applicationInfo.metaData != null) {
                return applicationInfo.metaData.getString("com.sonymobile.runtimeskinning.PACKAGE_ALIAS", str);
            }
        } catch (PackageManager.NameNotFoundException unused) {
        }
        return str;
    }

    private static boolean evaluateFilters(PackageManager packageManager, String str, XmlPullParser xmlPullParser) throws IOException, XmlPullParserException {
        int depth = xmlPullParser.getDepth() + 1;
        boolean z = true;
        boolean z2 = false;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() < depth)) {
                break;
            } else if (!(next == 3 || next == 4)) {
                String name = xmlPullParser.getName();
                if (xmlPullParser.getDepth() == depth) {
                    if (!name.equals("version-filter")) {
                        if (name.equals("laf-version-filter") && !(z2 = evaluateLafVersionFilter(packageManager, str, xmlPullParser))) {
                            break;
                        }
                    } else {
                        z = evaluateVersionCodeFilter(packageManager, str, xmlPullParser);
                        if (!z) {
                            break;
                        }
                    }
                } else {
                    XmlUtil.skipCurrentTag(xmlPullParser);
                }
            }
        }
        return z2 && z;
    }

    private static boolean evaluateVersionCodeFilter(PackageManager packageManager, String str, XmlPullParser xmlPullParser) throws IOException, XmlPullParserException {
        try {
            Integer parseInteger = XmlUtil.parseInteger(xmlPullParser, "from");
            Integer parseInteger2 = XmlUtil.parseInteger(xmlPullParser, "to");
            if (parseInteger2 == null) {
                if (parseInteger == null) {
                    throw new XmlPullParserException("<version-filter> requires to or from");
                }
            }
            try {
                return isInRange(parseInteger, parseInteger2, packageManager.getPackageInfo(str, 128).versionCode);
            } catch (PackageManager.NameNotFoundException unused) {
                return false;
            }
        } catch (NumberFormatException unused2) {
            throw new XmlPullParserException("Invalid from or to in <version-filter>");
        }
    }

    private static boolean evaluateLafVersionFilter(PackageManager packageManager, String str, XmlPullParser xmlPullParser) throws IOException, XmlPullParserException {
        try {
            Integer parseInteger = XmlUtil.parseInteger(xmlPullParser, "from");
            Integer parseInteger2 = XmlUtil.parseInteger(xmlPullParser, "to");
            if (parseInteger2 == null) {
                if (parseInteger == null) {
                    throw new XmlPullParserException("<laf-version-filter> requires to or from");
                }
            }
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(str, 128);
                Object obj = null;
                if (packageInfo.applicationInfo.metaData != null && packageInfo.applicationInfo.metaData.containsKey("com.sonymobile.runtimeskinning.LAF_VERSION")) {
                    obj = packageInfo.applicationInfo.metaData.get("com.sonymobile.runtimeskinning.LAF_VERSION");
                }
                if (obj == null) {
                    return false;
                }
                return isInRange(parseInteger, parseInteger2, Integer.parseInt(String.valueOf(obj)));
            } catch (PackageManager.NameNotFoundException | NumberFormatException unused) {
                return false;
            }
        } catch (NumberFormatException unused2) {
            throw new XmlPullParserException("Invalid from or to in <laf-version-filter>");
        }
    }

    private static boolean isInRange(Integer num, Integer num2, int i) {
        return (num == null || i >= num.intValue()) && (num2 == null || i <= num2.intValue());
    }
}
