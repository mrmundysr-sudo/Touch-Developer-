package com.touchdeveloper.app.ui;

import android.view.View;

import com.touchdeveloper.app.MainActivity;

/** Base class for a single full-screen view. */
public abstract class Screen {

    protected final MainActivity main;

    protected Screen(MainActivity main) {
        this.main = main;
    }

    /** Short title shown in the activity header. */
    public abstract String title();

    /** Builds the screen content. */
    public abstract View create();

    /** Called after the view is shown, so screens can refresh data. */
    public void onShown() {
    }
}
