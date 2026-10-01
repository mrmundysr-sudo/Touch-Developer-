package com.touchdeveloper.app.github;

import android.util.Base64;

import com.touchdeveloper.app.model.CommitChange;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.model.RepoFile;
import com.touchdeveloper.app.util.Http;
import com.touchdeveloper.app.util.Json;
import com.touchdeveloper.app.util.Result;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real GitHub REST API integration.
 *
 * Every write returns success only when GitHub confirms it with a 2xx response.
 * The token is held in memory for this object's lifetime only; it is never
 * logged, written to a file, or included in a ZIP.
 */
public class GitHubApiService implements GitHubService {

    private static final String API = "https://api.github.com";
    private static final String ACCEPT = "application/vnd.github+json";

    private final String token;
    private final StagingArea staging = new StagingArea();
    private final List<String> committedLocal = new ArrayList<>();

    public GitHubApiService(String token) {
        this.token = token == null ? "" : token.trim();
    }

    @Override
    public boolean isConfigured() {
        return !token.isEmpty();
    }

    @Override
    public String modeDescription() {
        return isConfigured() ? "GitHub REST API (live)" : "GitHub REST API (no token)";
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    private String contentsUrl(Repo repo, String path) {
        String url = API + "/repos/" + repo.getOwner() + "/" + repo.getName() + "/contents";
        if (path != null && !path.isEmpty()) {
            url += "/" + path.replace(" ", "%20");
        }
        return url;
    }

    @Override
    public Result<List<Repo>> listRepositories() {
        Http.Response response = Http.get(API + "/user/repos?per_page=50&sort=updated&affiliation=owner,collaborator", token);
        if (!response.ok()) {
            return Result.failure("GitHub could not list repositories (" + Http.statusText(response) + "): "
                    + describeError(response));
        }
        List<Repo> repos = new ArrayList<>();
        for (String object : Json.objects(response.body)) {
            String name = Http.stringField(object, "name");
            String owner = ownerFrom(object);
            String branch = Http.stringField(object, "default_branch");
            if (name == null || owner == null) {
                continue;
            }
            Repo repo = new Repo(owner, name, branch == null ? "main" : branch, false);
            repos.add(repo);
        }
        return Result.success("Loaded " + repos.size() + " repositories from GitHub.", repos);
    }

    private String ownerFrom(String repoObject) {
        int ownerIndex = repoObject.indexOf("\"owner\"");
        if (ownerIndex < 0) {
            return Http.stringField(repoObject, "login");
        }
        String ownerObject = repoObject.substring(ownerIndex);
        String login = Http.stringField(ownerObject, "login");
        if (login != null) {
            return login;
        }
        return Http.stringField(repoObject, "login");
    }

    @Override
    public Result<List<String>> listBranches(Repo repo) {
        Http.Response response = Http.get(API + "/repos/" + repo.getOwner() + "/" + repo.getName()
                + "/branches?per_page=100", token);
        if (!response.ok()) {
            return Result.failure("Could not list branches (" + Http.statusText(response) + "): "
                    + describeError(response));
        }
        List<String> branches = new ArrayList<>();
        for (String object : Json.objects(response.body)) {
            String name = Http.stringField(object, "name");
            if (name != null) {
                branches.add(name);
            }
        }
        return Result.success("Loaded " + branches.size() + " branches.", branches);
    }

    @Override
    public Result<Repo> refresh(Repo repo) {
        Http.Response mavenResponse = Http.get(API + "/repos/" + repo.getOwner() + "/" + repo.getName(), token);
        if (!mavenResponse.ok()) {
            return Result.failure("Could not refresh repository (" + Http.statusText(mavenResponse) + "): "
                    + describeError(mavenResponse));
        }
        String defaultBranch = Http.stringField(mavenResponse.body, "default_branch");
        if (defaultBranch != null) {
            repo.addBranch(defaultBranch);
        }
        refreshCommit(repo);
        return Result.success("Refreshed " + repo.getFullName() + " from GitHub.", repo);
    }

    private void refreshCommit(Repo repo) {
        Http.Response commits = Http.get(API + "/repos/" + repo.getOwner() + "/" + repo.getName()
                + "/commits/" + encode(repo.getCurrentBranch()) + "?per_page=1", token);
        if (commits.ok()) {
            String sha = Http.stringField(commits.body, "sha");
            if (sha != null) {
                repo.setLatestCommitSha(sha);
            }
            int commitIndex = commits.body.indexOf("\"commit\"");
            if (commitIndex >= 0) {
                String message = Http.stringField(commits.body.substring(commitIndex), "message");
                if (message != null) {
                    repo.setLatestCommitMessage(message.split("\n")[0]);
                }
            }
        }
    }

    @Override
    public Result<String> pullLatest(Repo repo) {
        Http.Response commits = Http.get(API + "/repos/" + repo.getOwner() + "/" + repo.getName()
                + "/commits/" + encode(repo.getCurrentBranch()) + "?per_page=1", token);
        if (!commits.ok()) {
            return Result.failure("Pull failed (" + Http.statusText(commits) + "): " + describeError(commits));
        }
        refreshCommit(repo);
        return Result.success("Pulled latest commit " + repo.shortCommit() + " on "
                + repo.getCurrentBranch() + ".", repo.getLatestCommitSha());
    }

    @Override
    public Result<List<RepoFile>> listFiles(Repo repo, String path) {
        String url = contentsUrl(repo, path) + (path == null || path.isEmpty() ? "?" : "&")
                + "ref=" + encode(repo.getCurrentBranch());
        Http.Response response = Http.get(url, token);
        if (!response.ok()) {
            return Result.failure("Could not list files (" + Http.statusText(response) + "): "
                    + describeError(response));
        }
        List<RepoFile> files = new ArrayList<>();
        for (String object : Json.objects(response.body)) {
            String name = Http.stringField(object, "name");
            String objectPath = Http.stringField(object, "path");
            String type = Http.stringField(object, "type");
            String sha = Http.stringField(object, "sha");
            long size = Json.numericField(object, "size");
            if (name == null || objectPath == null) {
                continue;
            }
            boolean directory = "dir".equals(type);
            files.add(new RepoFile(objectPath, name, directory, size, sha));
        }
        return Result.success("Loaded " + files.size() + " entries in "
                + (path == null || path.isEmpty() ? "repository root" : path) + ".",
                files);
    }

    @Override
    public Result<String> readFile(Repo repo, RepoFile file) {
        Result<byte[]> downloaded = downloadFile(repo, file);
        if (downloaded.data == null) {
            return Result.failure(downloaded.message);
        }
        String text = new String(downloaded.data, StandardCharsets.UTF_8);
        if (text.length() > 200_000) {
            text = text.substring(0, 200_000) + "\n... [truncated for preview]";
        }
        file.setTextContent(text);
        return Result.success("Loaded " + file.getPath() + " from GitHub.", text);
    }

    @Override
    public Result<byte[]> downloadFile(Repo repo, RepoFile file) {
        Http.Response response = Http.get(contentsUrl(repo, file.getPath())
                + "?ref=" + encode(repo.getCurrentBranch()), token);
        if (!response.ok()) {
            return Result.failure("Could not download file (" + Http.statusText(response) + "): "
                    + describeError(response));
        }
        String encoded = Http.stringField(response.body, "content");
        if (encoded == null) {
            return Result.failure("GitHub did not return file content for " + file.getPath()
                    + ". Files larger than 1 MB must be downloaded through the Git blob or raw endpoint, "
                    + "which is not implemented in version 1.");
        }
        try {
            byte[] decoded = Base64.decode(encoded.replaceAll("\\s", ""), Base64.DEFAULT);
            return Result.success("Downloaded " + file.getPath() + " (" + decoded.length + " bytes).",
                    decoded);
        } catch (Exception e) {
            return Result.failure("Could not decode file content: " + e.getMessage());
        }
    }

    @Override
    public Result<String> stageFile(Repo repo, String path, String content) {
        staging.stage(path, content);
        return Result.success("Staged " + path + " locally. Nothing has been sent to GitHub yet.", path);
    }

    @Override
    public Result<String> stageDelete(Repo repo, RepoFile file) {
        staging.stageDelete(file.getPath(), "staged for deletion");
        return Result.success("Staged deletion of " + file.getPath()
                + " locally. Nothing has been sent to GitHub yet.", file.getPath());
    }

    @Override
    public Result<String> stageRename(Repo repo, RepoFile file, String newPath) {
        staging.stageRename(file.getPath(), newPath);
        return Result.success("Staged rename " + file.getPath() + " -> " + newPath
                + " locally. Nothing has been sent to GitHub yet.", newPath);
    }

    @Override
    public List<CommitChange> previewChanges(Repo repo) {
        return staging.changes();
    }

    @Override
    public String stagedContent(String path) {
        return staging.getUpserts().get(path);
    }

    @Override
    public Result<String> commitStaged(Repo repo, String message) {
        if (staging.isEmpty()) {
            return Result.failure("Nothing is staged, so there is nothing to commit.");
        }
        int count = staging.changes().size();
        String localSha = "local-" + Long.toHexString(System.currentTimeMillis());
        committedLocal.add(localSha);
        repo.setLatestCommitSha(localSha);
        repo.setLatestCommitMessage(message);
        return Result.success("Recorded a local commit for " + count + " change(s) on "
                + repo.getCurrentBranch() + ". This commit is not on GitHub until you push.", localSha);
    }

    @Override
    public Result<String> pushStaged(Repo repo, String message) {
        if (!isConfigured()) {
            return Result.failure("Pushing requires a stored GitHub token.");
        }
        if (staging.isEmpty()) {
            return Result.failure("Nothing is staged, so there is nothing to push.");
        }
        int succeeded = 0;
        List<String> problems = new ArrayList<>();

        for (Map.Entry<String, String> entry : staging.getUpserts().entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue() == null ? "" : entry.getValue();
            String existingSha = existingSha(repo, path);
            StringBuilder json = new StringBuilder();
            json.append("{\"message\":\"").append(Json.escape(message)).append("\",");
            json.append("\"content\":\"").append(Base64.encodeToString(
                    content.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)).append("\",");
            json.append("\"branch\":\"").append(Json.escape(repo.getCurrentBranch())).append("\"");
            if (existingSha != null) {
                json.append(",\"sha\":\"").append(existingSha).append("\"");
            }
            json.append("}");
            Http.Response response = Http.put(contentsUrl(repo, path), token, json.toString());
            if (response.ok()) {
                succeeded++;
            } else {
                problems.add(path + " (" + Http.statusText(response) + ": " + describeError(response) + ")");
            }
        }

        for (Map.Entry<String, String> entry : staging.getDeletes().entrySet()) {
            String path = entry.getKey();
            String sha = existingSha(repo, path);
            if (sha == null) {
                problems.add(path + " (could not read the current file revision)");
                continue;
            }
            String body = "{\"message\":\"" + Json.escape(message) + "\",\"sha\":\"" + sha
                    + "\",\"branch\":\"" + Json.escape(repo.getCurrentBranch()) + "\"}";
            Http.Response response = Http.delete(contentsUrl(repo, path), token, body);
            if (response.ok()) {
                succeeded++;
            } else {
                problems.add(path + " (" + Http.statusText(response) + ": " + describeError(response) + ")");
            }
        }

        for (Map.Entry<String, String> entry : staging.getRenames().entrySet()) {
            problems.add(entry.getKey() + " rename is staged but rename publishing is not implemented "
                    + "in version 1; re-upload the file under the new name instead.");
        }

        if (!problems.isEmpty()) {
            return Result.failure("Push partially completed. Confirmed " + succeeded
                    + " change(s). Problems: " + String.join("; ", problems));
        }
        staging.clear();
        refreshCommit(repo);
        return Result.success("GitHub confirmed " + succeeded + " published change(s) on "
                + repo.getCurrentBranch() + ".", repo.getLatestCommitSha());
    }

    private String existingSha(Repo repo, String path) {
        Http.Response response = Http.get(contentsUrl(repo, path) + "?ref="
                + encode(repo.getCurrentBranch()), token);
        if (!response.ok()) {
            return null;
        }
        return Http.stringField(response.body, "sha");
    }

    @Override
    public Result<String> createBranch(Repo repo, String newBranch) {
        Http.Response ref = Http.get(API + "/repos/" + repo.getOwner() + "/" + repo.getName()
                + "/git/ref/heads/" + encode(repo.getCurrentBranch()), token);
        if (!ref.ok()) {
            return Result.failure("Could not read the current branch reference (" + Http.statusText(ref)
                    + "): " + describeError(ref));
        }
        String sha = Http.stringField(ref.body, "sha");
        if (sha == null) {
            return Result.failure("GitHub did not return a commit for " + repo.getCurrentBranch() + ".");
        }
        String body = "{\"ref\":\"refs/heads/" + Json.escape(newBranch) + "\",\"sha\":\"" + sha + "\"}";
        Http.Response response = Http.post(API + "/repos/" + repo.getOwner() + "/" + repo.getName()
                + "/git/refs", token, body);
        if (!response.ok()) {
            return Result.failure("Branch creation failed (" + Http.statusText(response) + "): "
                    + describeError(response));
        }
        repo.addBranch(newBranch);
        return Result.success("GitHub confirmed branch " + newBranch + " at " + sha.substring(0, 7) + ".",
                newBranch);
    }

    @Override
    public Result<String> deleteRepository(Repo repo) {
        Http.Response response = Http.delete(API + "/repos/" + repo.getOwner() + "/" + repo.getName(),
                token, null);
        if (!response.ok()) {
            return Result.failure("Repository deletion failed (" + Http.statusText(response) + "): "
                    + describeError(response));
        }
        return Result.success("GitHub confirmed deletion of " + repo.getFullName() + ".", repo.getFullName());
    }

    private String describeError(Http.Response response) {
        // A transport failure carries a human-readable reason (host, timeout, TLS),
        // not a JSON error object. Do not replace it with "no message".
        if (response.transportError) {
            return response.body == null || response.body.isEmpty()
                    ? "the request could not reach GitHub." : response.body;
        }
        String message = Json.errorMessage(response.body);
        return message == null ? "no message" : message;
    }
}
