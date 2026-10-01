package com.touchdeveloper.app.openhands;

import com.touchdeveloper.app.build.BuildRequest;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.util.Http;
import com.touchdeveloper.app.util.Json;
import com.touchdeveloper.app.util.Result;

/**
 * OpenHands integration against a user-configured endpoint.
 *
 * The endpoint and token come from secure storage at call time. Because no live
 * OpenHands endpoint contract has been verified for this project, every call
 * reports exactly what it sent and states that the endpoint contract is
 * unverified; nothing is reported as successful unless the endpoint returns 2xx.
 */
public class OpenHandsApiService implements OpenHandsService {

    private final String endpoint;
    private final String token;

    public OpenHandsApiService(String endpoint, String token) {
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.token = token == null ? "" : token.trim();
    }

    @Override
    public boolean isConfigured() {
        return !endpoint.isEmpty();
    }

    @Override
    public String modeDescription() {
        if (!isConfigured()) {
            return "OpenHands endpoint not configured";
        }
        return "OpenHands endpoint: " + endpoint + (token.isEmpty() ? " (no token)" : " (token stored)");
    }

    @Override
    public Result<String> openRepositoryLink(Repo repo) {
        return Result.success("OpenHands web link prepared for " + repo.getFullName() + ".",
                "https://app.all-hands.dev/");
    }

    @Override
    public Result<String> sendInstructions(BuildRequest request) {
        if (!isConfigured()) {
            return Result.failure("No OpenHands endpoint is configured. Store an endpoint in Settings first. "
                    + "Nothing was sent.");
        }
        String body = "{\"instructions\":\"" + Json.escape(request.getAiInstructions())
                + "\",\"repository\":\"" + Json.escape(request.getRepoFullName())
                + "\",\"branch\":\"" + Json.escape(request.getBranch()) + "\"}";
        Http.Response response = Http.post(endpoint + "/instructions", token, body);
        if (!response.ok()) {
            return Result.failure("OpenHands rejected the request (" + Http.statusText(response) + "): "
                    + safeMessage(response) + ". The endpoint contract is unverified.");
        }
        return Result.success("OpenHands returned HTTP " + response.code
                + ", which confirms it accepted the request. Build success is not implied.",
                response.body);
    }

    @Override
    public Result<String> startBuild(BuildRequest request) {
        if (!isConfigured()) {
            return Result.failure("No OpenHands endpoint is configured, so no build was started. "
                    + "Use the demo build to preview the flow.");
        }
        String body = "{\"repository\":\"" + Json.escape(request.getRepoFullName())
                + "\",\"branch\":\"" + Json.escape(request.getBranch())
                + "\",\"task\":\"assembleDebug\"}";
        Http.Response response = Http.post(endpoint + "/builds", token, body);
        if (!response.ok()) {
            return Result.failure("OpenHands did not accept the build (" + Http.statusText(response) + "): "
                    + safeMessage(response) + ". The endpoint contract is unverified.");
        }
        String reference = Http.stringField(response.body, "id");
        if (reference == null) {
            reference = Http.stringField(response.body, "job");
        }
        if (reference == null) {
            reference = "openhands-" + System.currentTimeMillis();
        }
        return Result.success("OpenHands returned HTTP " + response.code
                + " and a job reference. Use Refresh status to poll it.", reference);
    }

    @Override
    public Result<String> buildStatus(String jobReference) {
        if (!isConfigured()) {
            return Result.failure("No OpenHands endpoint is configured, so there is no remote job to poll.");
        }
        if (jobReference == null || jobReference.isEmpty()) {
            return Result.failure("No build job reference is available to poll.");
        }
        Http.Response response = Http.get(endpoint + "/builds/" + jobReference, token);
        if (!response.ok()) {
            return Result.failure("Could not read build status (" + Http.statusText(response) + "): "
                    + safeMessage(response) + ". The endpoint contract is unverified.");
        }
        String status = Http.stringField(response.body, "status");
        return Result.success("OpenHands status: " + (status == null ? "unknown" : status), response.body);
    }

    private String safeMessage(Http.Response response) {
        // A transport failure carries a human-readable reason, not a JSON error object.
        if (response.transportError) {
            return response.body == null || response.body.isEmpty()
                    ? "the request could not reach the endpoint." : response.body;
        }
        String message = Json.errorMessage(response.body);
        return message == null ? "no message" : message;
    }
}
