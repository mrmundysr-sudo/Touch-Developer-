package com.touchdeveloper.app.build;

import com.touchdeveloper.app.model.BuildRecord;

/**
 * Starts Android builds and reports their status.
 *
 * A returned {@link BuildRecord} must reflect the real outcome: callers only show
 * a build as successful when this service confirmed success. Implementations that
 * cannot run a real build must say so and never claim one started.
 */
public interface BuildService {

    /** True when a real build backend is configured. */
    boolean isConfigured();

    /** Human-readable description of the current build route (real or demo). */
    String modeDescription();

    /**
     * Starts a build for the request and returns the record tracking it.
     *
     * The record's status, progress, and output must only claim success that the
     * backend actually confirmed.
     */
    BuildRecord startBuild(BuildRequest request);
}
