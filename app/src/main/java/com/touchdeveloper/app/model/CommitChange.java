package com.touchdeveloper.app.model;

/**
 * A pending change shown before committing, so the user can see exactly what will change.
 */
public class CommitChange {

    public enum Kind { ADD, MODIFY, DELETE, RENAME }

    private final Kind kind;
    private final String path;
    private final String detail;

    public CommitChange(Kind kind, String path, String detail) {
        this.kind = kind;
        this.path = path;
        this.detail = detail;
    }

    public Kind getKind() {
        return kind;
    }

    public String getPath() {
        return path;
    }

    public String getDetail() {
        return detail;
    }

    public String display() {
        return kind.name() + " " + path + (detail == null || detail.isEmpty() ? "" : " - " + detail);
    }
}
