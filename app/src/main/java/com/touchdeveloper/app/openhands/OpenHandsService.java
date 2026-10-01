package com.touchdeveloper.app.openhands;

import com.touchdeveloper.app.build.BuildRequest;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.util.Result;

/**
 * OpenHands / AI workspace integration.
 *
 * Implementations must clearly report when no endpoint or token is configured and
 * must never claim that a build or instruction was accepted without a confirming
 * response.
 */
public interface OpenHandsService {

    boolean isConfigured();

    String modeDescription();

    /** Returns a deep link that opens the repository in the OpenHands web UI. */
    Result<String> openRepositoryLink(Repo repo);

    /** Sends source code and AI instructions to the configured workspace. */
    Result<String> sendInstructions(BuildRequest request);

    /** Starts a build through the configured workspace and returns a job reference. */
    Result<String> startBuild(BuildRequest request);

    /** Polls the configured workspace for a build's current status text. */
    Result<String> buildStatus(String jobReference);
}
