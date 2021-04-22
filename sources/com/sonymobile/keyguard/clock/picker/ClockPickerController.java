package com.sonymobile.keyguard.clock.picker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.C0000R$anim;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.keyguard.plugininfrastructure.ClockPluginUserSelectionHandler;
import com.sonymobile.keyguard.plugininfrastructure.DefaultKeyguardFactoryProvider;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactory;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardComponentFactoryEntry;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginConstants$ClockSelectionSource;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginFactoryLoader;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginMetaDataLoader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

public class ClockPickerController {
    private ClockPickerView mClockPicker;
    private final ClockPluginUserSelectionHandler mClockPluginUserSelectionHandler;
    private LinkedList<ClockPlugin> mClockPlugins = new LinkedList<>();
    private final Context mContext;
    private final DefaultKeyguardFactoryProvider mDefaultKeyguardFactoryProvider;
    private View mDismissView;
    private final KeyguardPluginFactoryLoader mKeyguardPluginFactoryLoader;
    private final KeyguardPluginMetaDataLoader mKeyguardPluginMetaDataLoader;

    public ClockPickerController(Context context, KeyguardPluginMetaDataLoader keyguardPluginMetaDataLoader, KeyguardPluginFactoryLoader keyguardPluginFactoryLoader, DefaultKeyguardFactoryProvider defaultKeyguardFactoryProvider, ClockPluginUserSelectionHandler clockPluginUserSelectionHandler) {
        this.mContext = context;
        this.mKeyguardPluginMetaDataLoader = keyguardPluginMetaDataLoader;
        this.mKeyguardPluginFactoryLoader = keyguardPluginFactoryLoader;
        this.mDefaultKeyguardFactoryProvider = defaultKeyguardFactoryProvider;
        this.mClockPluginUserSelectionHandler = clockPluginUserSelectionHandler;
    }

    public final void startClockPicker(ViewGroup viewGroup) {
        if (viewGroup != null && this.mClockPicker == null) {
            displayClockPickerView(viewGroup);
        }
    }

    private void createClickDismissingView(View view) {
        ViewParent parent = view != null ? view.getParent() : null;
        if (parent != null) {
            FrameLayout frameLayout = (FrameLayout) parent;
            this.mDismissView = new View(this.mContext);
            this.mDismissView.setLayoutParams(new FrameLayout.LayoutParams(-1, frameLayout.getHeight() - (view.getHeight() + Math.round(view.getY())), 80));
            this.mDismissView.setOnClickListener(new View.OnClickListener() {
                /* class com.sonymobile.keyguard.clock.picker.ClockPickerController.AnonymousClass1 */

                public void onClick(View view) {
                    ClockPickerController.this.exitClockPicker(null, false);
                    ClockPickerController.removeViewFromItsParent(view);
                }
            });
            frameLayout.addView(this.mDismissView);
        }
    }

    public final void resizeDismissView(View view) {
        View view2 = this.mDismissView;
        if (view2 != null && view2.getParent() != null && view != null) {
            this.mDismissView.setLayoutParams(new FrameLayout.LayoutParams(-1, ((ViewGroup) this.mDismissView.getParent()).getHeight() - (view.getHeight() + Math.round(view.getY())), 80));
        }
    }

    public final void exitClockPicker(KeyguardComponentFactoryEntry keyguardComponentFactoryEntry, boolean z) {
        ClockPickerView clockPickerView = this.mClockPicker;
        if (clockPickerView != null) {
            clockPickerView.clearSelectionTimeout();
            stopAllClocks();
            setClockForUser(keyguardComponentFactoryEntry);
            restoreGUI(z);
        }
    }

    private void restoreGUI(boolean z) {
        ViewGroup viewGroup = (ViewGroup) this.mClockPicker.getParent();
        if (viewGroup != null) {
            removeDismissView();
            if (!z) {
                hideClockPicker(viewGroup);
                showCurrentClock(viewGroup);
                return;
            }
            animateOutClockPicker(viewGroup);
        }
    }

    private void animateOutClockPicker(final ViewGroup viewGroup) {
        Animation loadAnimation = AnimationUtils.loadAnimation(this.mContext, C0000R$anim.somc_keyguard_clock_picker_hide_picker);
        loadAnimation.setAnimationListener(new Animation.AnimationListener() {
            /* class com.sonymobile.keyguard.clock.picker.ClockPickerController.AnonymousClass2 */

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                if (ClockPickerController.this.mClockPicker != null) {
                    ClockPickerController.this.mClockPicker.setAnimation(null);
                    ClockPickerController.this.hideClockPicker(viewGroup);
                    ClockPickerController.this.showCurrentClock(viewGroup);
                    ClockPickerController.this.animateInNewClock(viewGroup);
                }
            }
        });
        this.mClockPicker.startAnimation(loadAnimation);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void animateInNewClock(ViewGroup viewGroup) {
        final View findViewById = viewGroup.findViewById(C0007R$id.status_view_container);
        Animation loadAnimation = AnimationUtils.loadAnimation(this.mContext, C0000R$anim.somc_keyguard_clock_picker_show_current_clock);
        loadAnimation.setAnimationListener(new Animation.AnimationListener() {
            /* class com.sonymobile.keyguard.clock.picker.ClockPickerController.AnonymousClass3 */

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                findViewById.setAnimation(null);
            }
        });
        findViewById.startAnimation(loadAnimation);
    }

    private void removeDismissView() {
        View view = this.mDismissView;
        if (view != null) {
            removeViewFromItsParent(view);
            this.mDismissView = null;
        }
    }

    private void displayClockPickerView(ViewGroup viewGroup) {
        createClockPickerView(viewGroup);
        hideCurrentClock(viewGroup);
    }

    private void createClockPickerView(ViewGroup viewGroup) {
        this.mClockPicker = (ClockPickerView) LayoutInflater.from(this.mContext).inflate(C0010R$layout.somc_keyguard_clock_pager, viewGroup, false);
        this.mClockPicker.setController(this);
        this.mClockPicker.initPages();
        startAllClocks();
        this.mClockPicker.positionPicker(getCurrentClockFactoryClassName());
    }

    private void setClockForUser(KeyguardComponentFactoryEntry keyguardComponentFactoryEntry) {
        if (keyguardComponentFactoryEntry != null) {
            this.mClockPluginUserSelectionHandler.updateUserSelection(keyguardComponentFactoryEntry.getFullyQualifiedClassName(), KeyguardPluginConstants$ClockSelectionSource.LockscreenClockPicker);
        }
    }

    private String getCurrentClockFactoryClassName() {
        String presentableUserSelection = this.mClockPluginUserSelectionHandler.getPresentableUserSelection();
        return presentableUserSelection == null ? this.mDefaultKeyguardFactoryProvider.getDefaultKeyguardFactoryClassName() : presentableUserSelection;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void hideClockPicker(ViewGroup viewGroup) {
        ClockPickerView clockPickerView = this.mClockPicker;
        if (!(clockPickerView == null || viewGroup == null)) {
            viewGroup.removeView(clockPickerView);
            this.mClockPicker = null;
        }
        this.mClockPlugins.clear();
    }

    private void hideCurrentClock(final ViewGroup viewGroup) {
        final View findViewById = viewGroup.findViewById(C0007R$id.status_view_container);
        Animation loadAnimation = AnimationUtils.loadAnimation(this.mContext, C0000R$anim.somc_keyguard_clock_picker_hide_current_clock);
        loadAnimation.setAnimationListener(new Animation.AnimationListener() {
            /* class com.sonymobile.keyguard.clock.picker.ClockPickerController.AnonymousClass4 */

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                findViewById.setAnimation(null);
                if (ClockPickerController.this.mClockPicker != null) {
                    ClockPickerController.this.exchangeClockViewForPickerView(findViewById, viewGroup);
                }
            }
        });
        findViewById.startAnimation(loadAnimation);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void exchangeClockViewForPickerView(View view, final ViewGroup viewGroup) {
        view.setVisibility(8);
        viewGroup.addView(this.mClockPicker);
        createClickDismissingView(viewGroup);
        Animation loadAnimation = AnimationUtils.loadAnimation(this.mContext, C0000R$anim.somc_keyguard_clock_picker_show_picker);
        loadAnimation.setAnimationListener(new Animation.AnimationListener() {
            /* class com.sonymobile.keyguard.clock.picker.ClockPickerController.AnonymousClass5 */

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                viewGroup.setAnimation(null);
                if (ClockPickerController.this.mClockPicker != null) {
                    ClockPickerController.this.mClockPicker.createSelectionTimeoutForSelectedPage();
                    ClockPickerController.this.mClockPicker.enableScaleContainerScaling();
                    if (ClockPickerController.this.mDismissView != null) {
                        ClockPickerController clockPickerController = ClockPickerController.this;
                        clockPickerController.resizeDismissView((ViewGroup) clockPickerController.mClockPicker.getParent());
                    }
                }
            }
        });
        viewGroup.startAnimation(loadAnimation);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showCurrentClock(ViewGroup viewGroup) {
        if (viewGroup != null) {
            setVisibilityOnClockContainer(viewGroup, 0);
            if (viewGroup instanceof KeyguardStatusView) {
                ((KeyguardStatusView) viewGroup).loadClockPluginView();
            }
        }
    }

    private void setVisibilityOnClockContainer(ViewGroup viewGroup, int i) {
        View findViewById = viewGroup.findViewById(C0007R$id.status_view_container);
        if (findViewById != null) {
            findViewById.setVisibility(i);
        }
    }

    public final LinkedList<KeyguardComponentFactoryEntry> loadClockPlugins() {
        LinkedList<KeyguardComponentFactoryEntry> linkedList = null;
        try {
            linkedList = this.mKeyguardPluginMetaDataLoader.getAvailableKeyguardFactories();
            if (linkedList != null) {
                sortClockList(linkedList);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("ClockPickerController", e);
        }
        return linkedList != null ? linkedList : new LinkedList<>();
    }

    private void sortClockList(LinkedList<KeyguardComponentFactoryEntry> linkedList) {
        Collections.sort(linkedList, new Comparator<KeyguardComponentFactoryEntry>() {
            /* class com.sonymobile.keyguard.clock.picker.ClockPickerController.AnonymousClass6 */

            public int compare(KeyguardComponentFactoryEntry keyguardComponentFactoryEntry, KeyguardComponentFactoryEntry keyguardComponentFactoryEntry2) {
                if (keyguardComponentFactoryEntry == null || keyguardComponentFactoryEntry2 == null) {
                    return 0;
                }
                return keyguardComponentFactoryEntry2.getPriority() - keyguardComponentFactoryEntry.getPriority();
            }
        });
    }

    public final View createClockView(KeyguardComponentFactoryEntry keyguardComponentFactoryEntry) {
        KeyguardComponentFactory createComponentFactoryFromFactoryEntry = this.mKeyguardPluginFactoryLoader.createComponentFactoryFromFactoryEntry(keyguardComponentFactoryEntry);
        if (createComponentFactoryFromFactoryEntry == null) {
            return null;
        }
        ViewGroup createKeyguardClockPreviewView = createComponentFactoryFromFactoryEntry.createKeyguardClockPreviewView(this.mContext, null);
        if (createKeyguardClockPreviewView instanceof ClockPlugin) {
            this.mClockPlugins.add((ClockPlugin) createKeyguardClockPreviewView);
        }
        return createKeyguardClockPreviewView;
    }

    private void startAllClocks() {
        Iterator<ClockPlugin> it = this.mClockPlugins.iterator();
        while (it.hasNext()) {
            it.next().startClockTicking();
        }
    }

    private void stopAllClocks() {
        Iterator<ClockPlugin> it = this.mClockPlugins.iterator();
        while (it.hasNext()) {
            it.next().stopClockTicking();
        }
    }

    /* access modifiers changed from: private */
    public static void removeViewFromItsParent(View view) {
        ViewParent parent;
        if (view != null && (parent = view.getParent()) != null && (parent instanceof ViewGroup)) {
            ((ViewGroup) parent).removeView(view);
        }
    }
}
