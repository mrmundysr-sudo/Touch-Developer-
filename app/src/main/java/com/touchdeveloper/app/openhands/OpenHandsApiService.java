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
        return "OpenHands Cloud V1: " + endpoint + (token.isEmpty() ? " (API key missing)" : " (API key stored)");
    }

    @Override
    public Result<String> openRepositoryLink(Repo repo) {
        return Result.success("OpenHands web link prepared for " + repo.getFullName() + ".",
                "https://app.all-hands.dev/");
    }

    /**
     * OpenHands Cloud V1 starts a repository-backed coding conversation.
     * The returned id is a START TASK id, not proof that the agent has finished.
     * This integration does not choose or charge for a model; the account profile
     * controls that, and users must confirm available credits separately.
     */
    @Override
    public Result<String> sendInstructions(BuildRequest request) {
        return startConversation(request);
    }

    @Override
    public Result<String> startBuild(BuildRequest request) {
        // The legacy build UI must never silently start a coding conversation.
        return Result.failure("OpenHands APK build endpoint is not configured. Use GitHub Actions for APK builds; use the DeepSeek workspace for coding sessions.");
    }

    private Result<String> startConversation(BuildRequest request) {
        if (!isConfigured() || token.isEmpty()) {
            return Result.failure("OpenHands Cloud endpoint and API key are required. Nothing was sent.");
        }
        if (request == null || request.getRepoFullName() == null
                || request.getRepoFullName().trim().isEmpty()) {
            return Result.failure("Select a GitHub repository before starting OpenHands.");
        }
        if (request.getAiInstructions() == null || request.getAiInstructions().trim().isEmpty()) {
            return Result.failure("Enter coding instructions before starting OpenHands.");
        }
        String instructions = "Work only on GitHub branch: " + request.getBranch()
                + ". Do not modify main/master. Commit or push source changes to that branch "
                + "only when authorized. Do not build APKs or run paid model calls without "
                + "authorization.\\n\\n" + request.getAiInstructions();
        String body = "{\"initial_message\":{\"content\":[{\"type\":\"text\",\"text\":\""
                + Json.escape(instructions) + "\"}]},\"selected_repository\":\""
                + Json.escape(request.getRepoFullName()) + "\"}";
        Http.Response response = Http.post(baseUrl() + "/api/v1/app-conversations", token, body);
        if (!response.ok()) {
            return Result.failure("OpenHands could not start a conversation ("
                    + Http.statusText(response) + "): " + safeMessage(response));
        }
        String taskId = Http.stringField(response.body, "id");
        if (taskId == null || taskId.isEmpty()) {
            return Result.failure("OpenHands returned HTTP " + response.code
                    + " without a start-task ID. Check the OpenHands dashboard.");
        }
        return Result.success("OpenHands accepted a conversation start request; "
                + "the agent has not been verified as running or finished. Start task: " + taskId,
                "task:" + taskId);
    }

    @Override
    public Result<String> buildStatus(String jobReference) {
        if (!isConfigured() || token.isEmpty()) {
            return Result.failure("OpenHands Cloud endpoint or API key is missing.");
        }
        if (jobReference == null || jobReference.trim().isEmpty()) {
            return Result.failure("No OpenHands start-task or conversation reference is available.");
        }
        if (jobReference.startsWith("task:")) {
            String taskId = jobReference.substring(5);
            Http.Response response = Http.get(baseUrl()
                    + "/api/v1/app-conversations/start-tasks?ids=" + taskId, token);
            if (!response.ok()) {
                return Result.failure("OpenHands start status failed ("
                        + Http.statusText(response) + "): " + safeMessage(response));
            }
            String status = Http.stringField(response.body, "status");
            String conversationId = Http.stringField(response.body, "app_conversation_id");
            if ("READY".equalsIgnoreCase(status) && conversationId != null
                    && !conversationId.isEmpty()) {
                return Result.success("OpenHands workspace ready. Conversation: " + conversationId
                        + ". Coding completion has not been verified. "
                        + baseUrl() + "/conversations/" + conversationId,
                        "conversation:" + conversationId);
            }
            return Result.success("OpenHands workspace startup: "
                    + (status == null ? "unknown" : status)
                    + ". Refresh to check again. This is not an APK build.", jobReference);
        }
        String id = jobReference.startsWith("conversation:")
                ? jobReference.substring("conversation:".length()) : jobReference;
        Http.Response response = Http.get(baseUrl()
                + "/api/v1/app-conversations?ids=" + id, token);
        if (!response.ok()) {
            return Result.failure("OpenHands conversation status failed ("
                    + Http.statusText(response) + "): " + safeMessage(response));
        }
        String status = Http.stringField(response.body, "execution_status");
        return Result.success("OpenHands coding execution: "
                + (status == null ? "unknown" : status)
                + ". GitHub commits and APK builds must be verified separately.",
                "conversation:" + id);
    }

    private String baseUrl() {
        String base = endpoint;
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
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
