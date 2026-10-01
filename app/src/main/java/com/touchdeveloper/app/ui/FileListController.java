package com.touchdeveloper.app.ui;

import com.touchdeveloper.app.model.RepoFile;
import com.touchdeveloper.app.util.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the file-list state for one file screen.
 *
 * Two invariants keep the screen stable:
 * exactly one request may be in flight at a time, and the automatic load happens
 * at most once. A completed load, whether it succeeded or failed, never repeats
 * on its own, so the screen settles instead of looping.
 *
 * This class is deliberately free of Android types so the behaviour can be tested
 * on the JVM.
 */
public final class FileListController {

    private final List<RepoFile> entries = new ArrayList<>();
    private boolean loading;
    private boolean loaded;
    private String error = "";

    /** True until the first load finishes; drives the single automatic load. */
    public boolean shouldAutoLoad() {
        return !loaded && !loading;
    }

    /** Marks a request in flight. Returns false when one is already running. */
    public boolean beginRequest() {
        if (loading) {
            return false;
        }
        loading = true;
        return true;
    }

    /** Records the outcome of the request that {@link #beginRequest()} admitted. */
    public void onResult(Result<List<RepoFile>> result) {
        loading = false;
        loaded = true;
        if (result.ok || result.demo) {
            entries.clear();
            if (result.data != null) {
                entries.addAll(result.data);
            }
            error = "";
        } else {
            // Keep the previous listing and record one stable error. A real failure
            // never substitutes demo data.
            error = result.display();
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** The current error message, or "" when there is none. */
    public String error() {
        return error;
    }

    public List<RepoFile> entries() {
        return Collections.unmodifiableList(entries);
    }
}
