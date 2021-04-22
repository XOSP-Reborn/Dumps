package com.android.systemui.qs.customize;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.C0015R$style;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSDetailClipper;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.sonymobile.settingslib.logging.LoggingManager;
import java.util.ArrayList;

public class QSCustomizer extends LinearLayout implements Toolbar.OnMenuItemClickListener {
    private boolean isShown;
    private final QSDetailClipper mClipper;
    private final Animator.AnimatorListener mCollapseAnimationListener = new AnimatorListenerAdapter() {
        /* class com.android.systemui.qs.customize.QSCustomizer.AnonymousClass4 */

        public void onAnimationEnd(Animator animator) {
            if (!QSCustomizer.this.isShown) {
                QSCustomizer.this.setVisibility(8);
            }
            QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            QSCustomizer.this.mRecyclerView.setAdapter(QSCustomizer.this.mTileAdapter);
        }

        public void onAnimationCancel(Animator animator) {
            if (!QSCustomizer.this.isShown) {
                QSCustomizer.this.setVisibility(8);
            }
            QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
        }
    };
    private boolean mCustomizing;
    private final Animator.AnimatorListener mExpandAnimationListener = new AnimatorListenerAdapter() {
        /* class com.android.systemui.qs.customize.QSCustomizer.AnonymousClass3 */

        public void onAnimationEnd(Animator animator) {
            if (QSCustomizer.this.isShown) {
                QSCustomizer.this.setCustomizing(true);
            }
            QSCustomizer.this.mOpening = false;
            QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
        }

        public void onAnimationCancel(Animator animator) {
            QSCustomizer.this.mOpening = false;
            QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
        }
    };
    private QSTileHost mHost;
    private boolean mIsShowingNavBackdrop;
    private final KeyguardMonitor.Callback mKeyguardCallback = new KeyguardMonitor.Callback() {
        /* class com.android.systemui.qs.customize.$$Lambda$QSCustomizer$jyG9W7OYQzaSrDHqhU5p9dAeqes */

        @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
        public final void onKeyguardShowingChanged() {
            QSCustomizer.this.lambda$new$0$QSCustomizer();
        }
    };
    private final LightBarController mLightBarController;
    private NotificationsQuickSettingsContainer mNotifQsContainer;
    private boolean mOpening;
    private QS mQs;
    private RecyclerView mRecyclerView;
    private TileAdapter mTileAdapter;
    private final TileQueryHelper mTileQueryHelper;
    private Toolbar mToolbar;
    private final View mTransparentView;
    private int mX;
    private int mY;

    public QSCustomizer(Context context, AttributeSet attributeSet) {
        super(new ContextThemeWrapper(context, C0015R$style.edit_theme), attributeSet);
        LayoutInflater.from(getContext()).inflate(C0010R$layout.qs_customize_panel_content, this);
        this.mClipper = new QSDetailClipper(findViewById(C0007R$id.customize_container));
        this.mToolbar = (Toolbar) findViewById(16908682);
        TypedValue typedValue = new TypedValue();
        ((LinearLayout) this).mContext.getTheme().resolveAttribute(16843531, typedValue, true);
        this.mToolbar.setNavigationIcon(getResources().getDrawable(typedValue.resourceId, ((LinearLayout) this).mContext.getTheme()));
        this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.qs.customize.QSCustomizer.AnonymousClass1 */

            public void onClick(View view) {
                QSCustomizer.this.hide();
            }
        });
        this.mToolbar.setOnMenuItemClickListener(this);
        this.mToolbar.getMenu().add(0, 1, 0, ((LinearLayout) this).mContext.getString(17041241));
        this.mToolbar.setTitle(C0014R$string.qs_edit);
        this.mRecyclerView = (RecyclerView) findViewById(16908298);
        this.mTransparentView = findViewById(C0007R$id.customizer_transparent_view);
        this.mTileAdapter = new TileAdapter(getContext());
        this.mTileQueryHelper = new TileQueryHelper(context, this.mTileAdapter);
        this.mRecyclerView.setAdapter(this.mTileAdapter);
        this.mTileAdapter.getItemTouchHelper().attachToRecyclerView(this.mRecyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
        gridLayoutManager.setSpanSizeLookup(this.mTileAdapter.getSizeLookup());
        this.mRecyclerView.setLayoutManager(gridLayoutManager);
        this.mRecyclerView.addItemDecoration(this.mTileAdapter.getItemDecoration());
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        defaultItemAnimator.setMoveDuration(150);
        this.mRecyclerView.setItemAnimator(defaultItemAnimator);
        this.mLightBarController = (LightBarController) Dependency.get(LightBarController.class);
        updateNavBackDrop(getResources().getConfiguration());
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateNavBackDrop(configuration);
        updateResources();
    }

    private void updateResources() {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mTransparentView.getLayoutParams();
        layoutParams.height = ((LinearLayout) this).mContext.getResources().getDimensionPixelSize(17105397);
        this.mTransparentView.setLayoutParams(layoutParams);
    }

    private void updateNavBackDrop(Configuration configuration) {
        View findViewById = findViewById(C0007R$id.nav_bar_background);
        int i = 0;
        this.mIsShowingNavBackdrop = configuration.smallestScreenWidthDp >= 600 || configuration.orientation != 2;
        if (findViewById != null) {
            if (!this.mIsShowingNavBackdrop) {
                i = 8;
            }
            findViewById.setVisibility(i);
        }
        updateNavColors();
    }

    private void updateNavColors() {
        this.mLightBarController.setQsCustomizing(this.mIsShowingNavBackdrop && this.isShown);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
        this.mTileAdapter.setHost(qSTileHost);
    }

    public void setContainer(NotificationsQuickSettingsContainer notificationsQuickSettingsContainer) {
        this.mNotifQsContainer = notificationsQuickSettingsContainer;
    }

    public void setQs(QS qs) {
        this.mQs = qs;
    }

    public void show(int i, int i2) {
        if (!this.isShown) {
            int[] locationOnScreen = findViewById(C0007R$id.customize_container).getLocationOnScreen();
            this.mX = i - locationOnScreen[0];
            this.mY = i2 - locationOnScreen[1];
            MetricsLogger.visible(getContext(), 358);
            LoggingManager.logQSEvent(getContext(), "edit", "open", null);
            this.isShown = true;
            this.mOpening = true;
            setTileSpecs();
            setVisibility(0);
            this.mClipper.animateCircularClip(this.mX, this.mY, true, this.mExpandAnimationListener);
            queryTiles();
            this.mNotifQsContainer.setCustomizerAnimating(true);
            this.mNotifQsContainer.setCustomizerShowing(true);
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).addCallback(this.mKeyguardCallback);
            updateNavColors();
        }
    }

    public void showImmediately() {
        if (!this.isShown) {
            setVisibility(0);
            this.mClipper.showBackground();
            this.isShown = true;
            setTileSpecs();
            setCustomizing(true);
            queryTiles();
            this.mNotifQsContainer.setCustomizerAnimating(false);
            this.mNotifQsContainer.setCustomizerShowing(true);
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).addCallback(this.mKeyguardCallback);
            updateNavColors();
        }
    }

    private void queryTiles() {
        this.mTileQueryHelper.queryTiles(this.mHost);
    }

    public void hide() {
        if (this.isShown) {
            MetricsLogger.hidden(getContext(), 358);
            LoggingManager.logQSEvent(getContext(), "edit", "close", null);
            this.isShown = false;
            this.mToolbar.dismissPopupMenus();
            setCustomizing(false);
            save();
            this.mClipper.animateCircularClip(this.mX, this.mY, false, this.mCollapseAnimationListener);
            this.mNotifQsContainer.setCustomizerAnimating(true);
            this.mNotifQsContainer.setCustomizerShowing(false);
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(this.mKeyguardCallback);
            updateNavColors();
        }
    }

    public boolean isShown() {
        return this.isShown;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setCustomizing(boolean z) {
        this.mCustomizing = z;
        this.mQs.notifyCustomizeChanged();
    }

    public boolean isCustomizing() {
        return this.mCustomizing || this.mOpening;
    }

    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() != 1) {
            return false;
        }
        MetricsLogger.action(getContext(), 359);
        reset();
        return false;
    }

    private void reset() {
        ArrayList arrayList = new ArrayList();
        for (String str : ((LinearLayout) this).mContext.getString(C0014R$string.quick_settings_tiles_default).split(",")) {
            arrayList.add(str);
        }
        this.mTileAdapter.resetTileSpecs(this.mHost, arrayList);
    }

    private void setTileSpecs() {
        ArrayList arrayList = new ArrayList();
        for (QSTile qSTile : this.mHost.getTiles()) {
            arrayList.add(qSTile.getTileSpec());
        }
        this.mTileAdapter.setTileSpecs(arrayList);
        this.mRecyclerView.setAdapter(this.mTileAdapter);
    }

    private void save() {
        if (this.mTileQueryHelper.isFinished()) {
            this.mTileAdapter.saveSpecs(this.mHost);
        }
    }

    public void saveInstanceState(Bundle bundle) {
        if (this.isShown) {
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(this.mKeyguardCallback);
        }
        bundle.putBoolean("qs_customizing", this.mCustomizing);
    }

    public void restoreInstanceState(Bundle bundle) {
        if (bundle.getBoolean("qs_customizing")) {
            setVisibility(0);
            addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                /* class com.android.systemui.qs.customize.QSCustomizer.AnonymousClass2 */

                public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    QSCustomizer.this.removeOnLayoutChangeListener(this);
                    QSCustomizer.this.showImmediately();
                }
            });
        }
    }

    public void setEditLocation(int i, int i2) {
        int[] locationOnScreen = findViewById(C0007R$id.customize_container).getLocationOnScreen();
        this.mX = i - locationOnScreen[0];
        this.mY = i2 - locationOnScreen[1];
    }

    public /* synthetic */ void lambda$new$0$QSCustomizer() {
        if (isAttachedToWindow() && ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing() && !this.mOpening) {
            hide();
        }
    }
}
