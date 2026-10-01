package com.touchdeveloper.app.build;

import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.model.StatusLabel;
import com.touchdeveloper.app.openhands.OpenHandsService;
import com.touchdeveloper.app.util.Result;

/**
 * Routes builds through a configured {@link OpenHandsService}.
 *
 * Starting a build only proves OpenHands accepted the request. The record stays in
 * {@link StatusLabel#BUILDING} until a status poll reports success; this service
 * never marks a build successful on the strength of the start response alone.
 */
public class OpenHandsBuildService implements BuildService {

    private final OpenHandsService openHands;

    public OpenHandsBuildService(OpenHandsService openHands) {
        this.openHands = openHands;
    }

    @Override
    public boolean isConfigured() {
        return openHands != null && openHands.isConfigured();
    }

    @Override
    public String modeDescription() {
        if (!isConfigured()) {
            return "OpenHands build route is not configured";
        }
        return "Builds routed through the configured OpenHands endpoint (endpoint contract is unverified).";
    }

    @Override
    public BuildRecord startBuild(BuildRequest request) {
        BuildRecord record = new BuildRecord("openhands-" + System.currentTimeMillis(),
                request.getProjectName(), request.getRepoFullName(), request.getBranch());
        record.setDemo(false);
        record.setStartTime(System.currentTimeMillis());
        record.setStatus(StatusLabel.BUILDING);
        record.appendProgress("Requesting a build through OpenHands for " + request.getRepoFullName()
                + " (" + request.getBranch() + ").");

        if (!isConfigured()) {
            record.setStatus(StatusLabel.FAILED);
            record.setEndTime(System.currentTimeMillis());
            record.appendError("No OpenHands endpoint is configured, so no build was started.");
            return record;
        }
        if (request.getRepoFullName().isEmpty()) {
            record.setStatus(StatusLabel.FAILED);
            record.setEndTime(System.currentTimeMillis());
            record.appendError("No repository is selected, so no build was started.");
            return record;
        }

        Result<String> result = openHands.startBuild(request);
        record.appendProgress("OpenHands: " + result.display());
        if (!result.ok) {
            record.setStatus(StatusLabel.FAILED);
            record.setEndTime(System.currentTimeMillis());
            record.appendError(result.display());
            return record;
        }

        record.appendProgress("Job reference: " + (result.data == null ? "unknown" : result.data));
        record.appendProgress("OpenHands accepted the request. Success is only shown once a status "
                + "poll confirms it.");
        return record;
    }
}
