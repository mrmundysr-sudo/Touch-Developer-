package com.touchdeveloper.app.model;

/**
 * Status labels required by the Touch Developer V1 specification.
 */
public enum StatusLabel {
    LOCAL("Local"),
    COMMITTED("Committed"),
    PUSHED("Pushed"),
    BUILDING("Building"),
    SUCCESSFUL("Successful"),
    FAILED("Failed"),
    NOT_CONFIGURED("Not configured"),
    DEMO("Demo");

    private final String display;

    StatusLabel(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
