package com.touchdeveloper.app.util;

/**
 * Outcome of a service call.
 *
 * {@code ok} is true only when the underlying integration confirmed the action.
 * {@code demo} is true when the outcome came from mock/demo data, so callers can
 * label it and must not present it as a real result.
 */
public class Result<T> {

    public final boolean ok;
    public final String message;
    public final T data;
    public final boolean demo;

    private Result(boolean ok, String message, T data, boolean demo) {
        this.ok = ok;
        this.message = message;
        this.data = data;
        this.demo = demo;
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data, false);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(false, message, null, false);
    }

    public static <T> Result<T> demo(String message, T data) {
        return new Result<>(false, message, data, true);
    }

    public static <T> Result<T> notImplemented(String message) {
        return new Result<>(false, message, null, false);
    }

    public String display() {
        return demo ? "[DEMO] " + message : message;
    }
}
