package com.touchdeveloper.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.R;
import com.touchdeveloper.app.model.StatusLabel;

/** Small view helpers that keep the screens compact and consistent. */
public final class Ui {

    private Ui() {
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static TextView heading(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(20f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 4));
        return view;
    }

    public static TextView sectionLabel(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(13f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.parseColor("#5A6472"));
        view.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 2));
        return view;
    }

    public static TextView body(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(14f);
        view.setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4));
        return view;
    }

    public static TextView status(Context context, StatusLabel label) {
        TextView view = new TextView(context);
        view.setText(label.display());
        view.setTextSize(13f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(statusColor(label));
        view.setPadding(dp(context, 12), dp(context, 2), dp(context, 12), dp(context, 2));
        return view;
    }

    public static int statusColor(StatusLabel label) {
        switch (label) {
            case SUCCESSFUL:
                return Color.parseColor("#1E6B3A");
            case FAILED:
                return Color.parseColor("#A02020");
            case BUILDING:
                return Color.parseColor("#8A6D1F");
            case PUSHED:
                return Color.parseColor("#2E7D6B");
            case DEMO:
                return Color.parseColor("#8A6D1F");
            default:
                return Color.parseColor("#5A6472");
        }
    }

    public static Button button(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(context, 12), dp(context, 3), dp(context, 12), dp(context, 3));
        button.setLayoutParams(params);
        return button;
    }

    public static TextView mono(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(12f);
        view.setTypeface(Typeface.MONOSPACE);
        view.setBackgroundColor(Color.parseColor("#F1F3F5"));
        view.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4));
        view.setLayoutParams(params);
        return view;
    }

    /** Persistent error banner; red text on a light red panel. */
    public static TextView error(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(13f);
        view.setTextColor(Color.parseColor("#A02020"));
        view.setBackgroundColor(Color.parseColor("#FBE9E9"));
        view.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));
        view.setLayoutParams(params);
        return view;
    }

    public static TextView divider(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(14f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.parseColor("#1F3B57"));
        view.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        view.setGravity(Gravity.START);
        return view;
    }

    public static void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
