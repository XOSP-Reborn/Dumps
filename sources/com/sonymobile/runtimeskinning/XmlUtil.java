package com.sonymobile.runtimeskinning;

import android.content.res.XmlResourceParser;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class XmlUtil {
    static void skipCurrentTag(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (xmlPullParser != null) {
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                if (next == 1) {
                    return;
                }
                if (next == 3 && depth == xmlPullParser.getDepth()) {
                    return;
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    static Boolean parseBoolean(XmlPullParser xmlPullParser, String str) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue != null) {
            return Boolean.valueOf(Boolean.parseBoolean(attributeValue));
        }
        return null;
    }

    static Integer parseInteger(XmlPullParser xmlPullParser, String str) throws NumberFormatException {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue != null) {
            return Integer.valueOf(Integer.parseInt(attributeValue));
        }
        return null;
    }

    static boolean findTag(XmlResourceParser xmlResourceParser, String str, int i) throws XmlPullParserException, IOException {
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 1) {
                return false;
            }
            if (next == 3 && xmlResourceParser.getDepth() < i) {
                return false;
            }
            if (!(next == 3 || next == 4)) {
                if (xmlResourceParser.getName().equals(str) && xmlResourceParser.getDepth() == i) {
                    return true;
                }
                skipCurrentTag(xmlResourceParser);
            }
        }
    }

    static boolean startAtElement(XmlResourceParser xmlResourceParser, String str) throws XmlPullParserException, IOException {
        int eventType = xmlResourceParser.getEventType();
        while (eventType != 1 && (eventType != 2 || !xmlResourceParser.getName().equals(str))) {
            eventType = xmlResourceParser.next();
        }
        if (eventType != 1) {
            return true;
        }
        return false;
    }
}
