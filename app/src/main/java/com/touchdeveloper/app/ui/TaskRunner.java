package com.touchdeveloper.app.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;

/**
 * Runs a blocking service call off the main thread and delivers the result on the
 * main thread.
 *
 * Network calls must never run on the UI thread. A modal progress dialog blocks
 * further taps while a call is in flight (so a user cannot start a second call and
 * stack up ANRs), and the dialog is always dismissed before the callback runs.
 */
public final class TaskRunner {

    /** Receives the result of a background job on the main thread. */
    public interface Callback<T> {
        void onResult(T result);
    }

    /** A unit of work that may block; executed off the main thread. */
    public interface Job<T> {
        T run() throws Exception;
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private TaskRunner() {
    }

    /**
     * Runs {@code work} on a background thread, showing {@code message} in a modal
     * progress dialog, then delivers the result to {@code callback} on the main thread.
     */
    public static <T> void run(Activity activity, String title, String message,
                               final Job<T> work, final Callback<T> callback) {
        final ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        final Async<T> holder = new Async<>();
        new Thread(() -> {
            try {
                holder.value = work.run();
            } catch (Throwable t) {
                holder.error = t;
            }
            MAIN.post(() -> {
                dismissQuietly(dialog);
                if (holder.error != null) {
                    throw new RuntimeException("Background task failed", holder.error);
                }
                callback.onResult(holder.value);
            });
        }, "touch-task").start();
    }

    private static final class Async<T> {
        T value;
        Throwable error;
    }

    private static void dismissQuietly(ProgressDialog dialog) {
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (IllegalArgumentException ignored) {
            // The activity was destroyed while the call was in flight.
        }
    }
}
