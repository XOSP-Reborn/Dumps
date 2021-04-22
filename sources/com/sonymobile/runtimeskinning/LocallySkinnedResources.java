package com.sonymobile.runtimeskinning;

import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;

public class LocallySkinnedResources extends SkinnedResources {
    private final Map<Integer, Pair<Integer, Resources>> mIdMap;
    private final Resources mOriginalResources;
    private final HashSet<Resources> mResources = new HashSet<>();

    LocallySkinnedResources(String str, Resources resources, List<Pair<String, Resources>> list, Map<String, String> map) {
        super(resources);
        this.mOriginalResources = resources;
        this.mIdMap = createIdMap(str, list, map, this.mResources);
    }

    protected LocallySkinnedResources(Resources resources, Map<Integer, Pair<Integer, Resources>> map) {
        super(resources);
        this.mOriginalResources = resources;
        this.mIdMap = map;
    }

    private Map<Integer, Pair<Integer, Resources>> createIdMap(String str, List<Pair<String, Resources>> list, Map<String, String> map, HashSet<Resources> hashSet) {
        ArrayMap arrayMap = new ArrayMap();
        if (!map.isEmpty() && !list.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                int identifier = this.mOriginalResources.getIdentifier(key, value, str);
                if (identifier != 0) {
                    int size = list.size() - 1;
                    while (true) {
                        if (size < 0) {
                            break;
                        }
                        Pair<String, Resources> pair = list.get(size);
                        Resources resources = (Resources) pair.second;
                        int identifier2 = resources.getIdentifier(key, value, (String) pair.first);
                        if (identifier2 != 0) {
                            hashSet.add(resources);
                            arrayMap.put(Integer.valueOf(identifier), new Pair(Integer.valueOf(identifier2), resources));
                            break;
                        }
                        size--;
                    }
                }
            }
        }
        return arrayMap;
    }

    @Override // android.content.res.Resources
    public CharSequence getText(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getText(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getText(resolveReference);
    }

    @Override // android.content.res.Resources
    public CharSequence getQuantityText(int i, int i2) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getQuantityText(((Integer) pair.first).intValue(), i2);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getQuantityText(resolveReference, i2);
    }

    @Override // android.content.res.Resources
    public String getString(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getString(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getString(resolveReference);
    }

    @Override // android.content.res.Resources
    public String getString(int i, Object... objArr) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getString(((Integer) pair.first).intValue(), objArr);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getString(resolveReference, objArr);
    }

    @Override // android.content.res.Resources
    public String getQuantityString(int i, int i2, Object... objArr) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getQuantityString(((Integer) pair.first).intValue(), i2, objArr);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getQuantityString(resolveReference, i2, objArr);
    }

    @Override // android.content.res.Resources
    public String getQuantityString(int i, int i2) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getQuantityString(((Integer) pair.first).intValue(), i2);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getQuantityString(resolveReference, i2);
    }

    public CharSequence getText(int i, CharSequence charSequence) {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getText(((Integer) pair.first).intValue(), charSequence);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getText(resolveReference, charSequence);
    }

    @Override // android.content.res.Resources
    public CharSequence[] getTextArray(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.getTextArray(i);
    }

    @Override // android.content.res.Resources
    public String[] getStringArray(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.getStringArray(i);
    }

    @Override // android.content.res.Resources
    public int[] getIntArray(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.getIntArray(i);
    }

    @Override // android.content.res.Resources
    public TypedArray obtainTypedArray(int i) throws Resources.NotFoundException {
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(i));
        if (pair != null) {
            try {
                return ((Resources) pair.second).obtainTypedArray(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.obtainTypedArray(i);
    }

    @Override // android.content.res.Resources
    public float getDimension(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getDimension(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getDimension(resolveReference);
    }

    @Override // android.content.res.Resources
    public int getDimensionPixelOffset(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getDimensionPixelOffset(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getDimensionPixelOffset(resolveReference);
    }

    @Override // android.content.res.Resources
    public int getDimensionPixelSize(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getDimensionPixelSize(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getDimensionPixelSize(resolveReference);
    }

    public float getFraction(int i, int i2, int i3) {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getFraction(((Integer) pair.first).intValue(), i2, i3);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getFraction(resolveReference, i2, i3);
    }

    @Override // android.content.res.Resources
    public Drawable getDrawable(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getDrawable(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getDrawable(resolveReference);
    }

    @Override // android.content.res.Resources
    public Drawable getDrawable(int i, Resources.Theme theme) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getDrawable(((Integer) pair.first).intValue(), theme);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getDrawable(resolveReference, theme);
    }

    @Override // android.content.res.Resources
    public Drawable getDrawableForDensity(int i, int i2) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getDrawableForDensity(((Integer) pair.first).intValue(), i2);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getDrawableForDensity(resolveReference, i2);
    }

    public Drawable getDrawableForDensity(int i, int i2, Resources.Theme theme) {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getDrawableForDensity(((Integer) pair.first).intValue(), i2, theme);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getDrawableForDensity(resolveReference, i2, theme);
    }

    @Override // android.content.res.Resources
    public Movie getMovie(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getMovie(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getMovie(resolveReference);
    }

    @Override // android.content.res.Resources
    public int getColor(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getColor(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getColor(resolveReference);
    }

    @Override // android.content.res.Resources
    public int getColor(int i, Resources.Theme theme) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getColor(((Integer) pair.first).intValue(), theme);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getColor(resolveReference, theme);
    }

    @Override // android.content.res.Resources
    public ColorStateList getColorStateList(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getColorStateList(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getColorStateList(resolveReference);
    }

    @Override // android.content.res.Resources
    public ColorStateList getColorStateList(int i, Resources.Theme theme) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getColorStateList(((Integer) pair.first).intValue(), theme);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getColorStateList(resolveReference, theme);
    }

    @Override // android.content.res.Resources
    public boolean getBoolean(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getBoolean(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getBoolean(resolveReference);
    }

    @Override // android.content.res.Resources
    public int getInteger(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getInteger(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getInteger(resolveReference);
    }

    @Override // android.content.res.Resources
    public XmlResourceParser getLayout(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getLayout(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getLayout(resolveReference);
    }

    @Override // android.content.res.Resources
    public XmlResourceParser getAnimation(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getAnimation(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getAnimation(resolveReference);
    }

    @Override // android.content.res.Resources
    public XmlResourceParser getXml(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).getXml(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.getXml(resolveReference);
    }

    @Override // android.content.res.Resources
    public InputStream openRawResource(int i) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).openRawResource(((Integer) pair.first).intValue());
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.openRawResource(resolveReference);
    }

    @Override // android.content.res.Resources
    public InputStream openRawResource(int i, TypedValue typedValue) throws Resources.NotFoundException {
        int resolveReference = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(resolveReference));
        if (pair != null) {
            try {
                return ((Resources) pair.second).openRawResource(((Integer) pair.first).intValue(), typedValue);
            } catch (Throwable unused) {
            }
        }
        return this.mOriginalResources.openRawResource(resolveReference, typedValue);
    }

    @Override // android.content.res.Resources
    public AssetFileDescriptor openRawResourceFd(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.openRawResourceFd(i);
    }

    @Override // android.content.res.Resources
    public void getValue(int i, TypedValue typedValue, boolean z) throws Resources.NotFoundException {
        if (z) {
            i = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        }
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(i));
        if (pair != null) {
            try {
                ((Resources) pair.second).getValue(((Integer) pair.first).intValue(), typedValue, z);
                return;
            } catch (Throwable unused) {
            }
        }
        this.mOriginalResources.getValue(i, typedValue, z);
    }

    @Override // android.content.res.Resources
    public void getValueForDensity(int i, int i2, TypedValue typedValue, boolean z) throws Resources.NotFoundException {
        if (z) {
            i = ResolverUtil.resolveReference(this.mOriginalResources, this.mIdMap, i);
        }
        Pair<Integer, Resources> pair = this.mIdMap.get(Integer.valueOf(i));
        if (pair != null) {
            try {
                ((Resources) pair.second).getValueForDensity(((Integer) pair.first).intValue(), i2, typedValue, z);
                return;
            } catch (Throwable unused) {
            }
        }
        this.mOriginalResources.getValueForDensity(i, i2, typedValue, z);
    }

    @Override // android.content.res.Resources
    public void getValue(String str, TypedValue typedValue, boolean z) throws Resources.NotFoundException {
        getValue(getIdentifier(str, "string", null), typedValue, z);
    }

    public TypedArray obtainAttributes(AttributeSet attributeSet, int[] iArr) {
        return this.mOriginalResources.obtainAttributes(attributeSet, iArr);
    }

    public void updateConfiguration(Configuration configuration, DisplayMetrics displayMetrics) {
        Iterator<Resources> it = this.mResources.iterator();
        while (it.hasNext()) {
            it.next().updateConfiguration(configuration, displayMetrics);
        }
        this.mOriginalResources.updateConfiguration(configuration, displayMetrics);
    }

    public DisplayMetrics getDisplayMetrics() {
        return this.mOriginalResources.getDisplayMetrics();
    }

    public Configuration getConfiguration() {
        return this.mOriginalResources.getConfiguration();
    }

    public int getIdentifier(String str, String str2, String str3) {
        return this.mOriginalResources.getIdentifier(str, str2, str3);
    }

    @Override // android.content.res.Resources
    public String getResourceName(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.getResourceName(i);
    }

    @Override // android.content.res.Resources
    public String getResourcePackageName(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.getResourcePackageName(i);
    }

    @Override // android.content.res.Resources
    public String getResourceTypeName(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.getResourceTypeName(i);
    }

    @Override // android.content.res.Resources
    public String getResourceEntryName(int i) throws Resources.NotFoundException {
        return this.mOriginalResources.getResourceEntryName(i);
    }

    @Override // android.content.res.Resources
    public void parseBundleExtras(XmlResourceParser xmlResourceParser, Bundle bundle) throws XmlPullParserException, IOException {
        this.mOriginalResources.parseBundleExtras(xmlResourceParser, bundle);
    }

    @Override // android.content.res.Resources
    public void parseBundleExtra(String str, AttributeSet attributeSet, Bundle bundle) throws XmlPullParserException {
        this.mOriginalResources.parseBundleExtra(str, attributeSet, bundle);
    }
}
