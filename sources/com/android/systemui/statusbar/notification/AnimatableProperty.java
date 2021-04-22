package com.android.systemui.statusbar.notification;

import android.util.FloatProperty;
import android.util.Property;
import android.view.View;
import com.android.systemui.C0007R$id;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AnimatableProperty {
    public static final AnimatableProperty X = from(View.X, C0007R$id.x_animator_tag, C0007R$id.x_animator_tag_start_value, C0007R$id.x_animator_tag_end_value);
    public static final AnimatableProperty Y = from(View.Y, C0007R$id.y_animator_tag, C0007R$id.y_animator_tag_start_value, C0007R$id.y_animator_tag_end_value);

    public abstract int getAnimationEndTag();

    public abstract int getAnimationStartTag();

    public abstract int getAnimatorTag();

    public abstract Property getProperty();

    public static <T extends View> AnimatableProperty from(String str, final BiConsumer<T, Float> biConsumer, final Function<T, Float> function, final int i, final int i2, final int i3) {
        final AnonymousClass1 r0 = new FloatProperty<T>(str) {
            /* class com.android.systemui.statusbar.notification.AnimatableProperty.AnonymousClass1 */

            public Float get(T t) {
                return (Float) function.apply(t);
            }

            public void setValue(T t, float f) {
                biConsumer.accept(t, Float.valueOf(f));
            }
        };
        return new AnimatableProperty() {
            /* class com.android.systemui.statusbar.notification.AnimatableProperty.AnonymousClass2 */

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationStartTag() {
                return i2;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationEndTag() {
                return i3;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimatorTag() {
                return i;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public Property getProperty() {
                return r0;
            }
        };
    }

    public static <T extends View> AnimatableProperty from(final Property<T, Float> property, final int i, final int i2, final int i3) {
        return new AnimatableProperty() {
            /* class com.android.systemui.statusbar.notification.AnimatableProperty.AnonymousClass3 */

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationStartTag() {
                return i2;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationEndTag() {
                return i3;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimatorTag() {
                return i;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public Property getProperty() {
                return property;
            }
        };
    }
}
