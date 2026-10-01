package com.touchdeveloper.app.github;

import com.touchdeveloper.app.model.CommitChange;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.model.RepoFile;
import com.touchdeveloper.app.safety.DemoMode;
import com.touchdeveloper.app.util.Result;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clearly labelled demo GitHub data used when no token is stored.
 *
 * Every method returns a demo result. Demo results never report success, and the
 * UI must show the {@code [DEMO]} label so mock data is never mistaken for a real
 * GitHub response.
 */
public class GitHubDemoService implements GitHubService {

    private final Map<String, List<RepoFile>> trees = new LinkedHashMap<>();
    private final StagingArea staging = new StagingArea();

    public GitHubDemoService() {
        trees.put("", rootEntries());
        trees.put("app", appEntries());
        trees.put("app/src/main/java/com/touchdeveloper/app", javaEntries());
    }

    private List<RepoFile> rootEntries() {
        List<RepoFile> files = new ArrayList<>();
        files.add(new RepoFile("app", "app", true, 0, null));
        files.add(new RepoFile("README.md", "README.md", false, 1840, "demo-sha-readme"));
        files.add(new RepoFile("TOUCH-DEVELOPER-V1-BUILD-PROMPT.md",
                "TOUCH-DEVELOPER-V1-BUILD-PROMPT.md", false, 5420, "demo-sha-prompt"));
        files.add(new RepoFile("TouchDeveloper-sample-handoff-v001.zip",
                "TouchDeveloper-sample-handoff-v001.zip", false, 8_912_000, "demo-sha-handoff"));
        return files;
    }

    private List<RepoFile> appEntries() {
        List<RepoFile> files = new ArrayList<>();
        files.add(new RepoFile("app/build.gradle", "build.gradle", false, 900, "demo-sha-build"));
        files.add(new RepoFile("app/src", "src", true, 0, null));
        return files;
    }

    private List<RepoFile> javaEntries() {
        List<RepoFile> files = new ArrayList<>();
        files.add(new RepoFile("app/src/main/java/com/touchdeveloper/app/MainActivity.java",
                "MainActivity.java", false, 1200, "demo-sha-main"));
        files.add(new RepoFile("app/src/main/java/com/touchdeveloper/app/model/Repo.java",
                "Repo.java", false, 2100, "demo-sha-model"));
        return files;
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String modeDescription() {
        return DemoMode.banner("GitHub");
    }

    @Override
    public Result<List<Repo>> listRepositories() {
        List<Repo> repos = new ArrayList<>();
        Repo first = new Repo("demo-owner", "touch-demo-app", "main", true);
        first.setLatestCommitSha("demo1a2b3c4d");
        first.setLatestCommitMessage("Demo initial commit");
        first.setLatestApkName("app-debug.apk (demo)");
        first.setLatestSourceZipName("touch-demo-app-source.zip (demo)");
        first.addBranch("develop");
        repos.add(first);
        Repo second = new Repo("demo-owner", "touch-sandbox", "develop", true);
        second.setLatestCommitSha("demo5e6f7a8b");
        second.setLatestCommitMessage("Demo sandbox update");
        repos.add(second);
        return Result.demo("Loaded " + repos.size()
                + " demo repositories. Connect a GitHub token for real repositories.", repos);
    }

    @Override
    public Result<List<String>> listBranches(Repo repo) {
        return Result.demo("Demo branches for " + repo.getFullName() + ".", new ArrayList<>(repo.getBranches()));
    }

    @Override
    public Result<Repo> refresh(Repo repo) {
        return Result.demo("Demo refresh of " + repo.getFullName()
                + ". No request was sent to GitHub.", repo);
    }

    @Override
    public Result<String> pullLatest(Repo repo) {
        return Result.demo("Demo pull of the latest changes on " + repo.getCurrentBranch()
                + ". No real pull happened.", repo.getLatestCommitSha());
    }

    @Override
    public Result<List<RepoFile>> listFiles(Repo repo, String path) {
        String key = path == null ? "" : path;
        List<RepoFile> entries = trees.get(key);
        if (entries == null) {
            entries = new ArrayList<>();
            entries.add(new RepoFile(key + "/demo-file.txt", "demo-file.txt", false, 512, "demo-sha-file"));
        }
        return Result.demo("Demo file listing for " + (key.isEmpty() ? "repository root" : key) + ".",
                entries);
    }

    @Override
    public Result<String> readFile(Repo repo, RepoFile file) {
        String content = "Demo content for " + file.getPath()
                + "\n\nThis text comes from the built-in demo dataset.\n"
                + "Connect GitHub credentials to read the real file.";
        file.setTextContent(content);
        return Result.demo("Demo preview of " + file.getPath() + ".", content);
    }

    @Override
    public Result<byte[]> downloadFile(Repo repo, RepoFile file) {
        return Result.demo("Demo mode: no real download was performed for " + file.getPath()
                + ". Connect a GitHub token to download real files.", null);
    }

    @Override
    public Result<String> stageFile(Repo repo, String path, String content) {
        staging.stage(path, content);
        return Result.demo("Staged " + path + " in the demo staging area only.", path);
    }

    @Override
    public Result<String> stageDelete(Repo repo, RepoFile file) {
        staging.stageDelete(file.getPath(), "demo staged deletion");
        return Result.demo("Staged deletion of " + file.getPath() + " in the demo staging area only.",
                file.getPath());
    }

    @Override
    public Result<String> stageRename(Repo repo, RepoFile file, String newPath) {
        staging.stageRename(file.getPath(), newPath);
        return Result.demo("Staged rename " + file.getPath() + " -> " + newPath + " in demo mode.",
                newPath);
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
            return Result.demo("Nothing is staged. Demo commit skipped.", null);
        }
        return Result.demo("Demo commit recorded for " + staging.changes().size()
                + " change(s). No repository was modified.", null);
    }

    @Override
    public Result<String> pushStaged(Repo repo, String message) {
        if (!isConfigured()) {
            return Result.failure("Push is unavailable in demo mode. Store a GitHub token to push. "
                    + "No changes were sent anywhere.");
        }
        return Result.demo("Demo push. No changes were sent anywhere.", null);
    }

    @Override
    public Result<String> createBranch(Repo repo, String newBranch) {
        repo.addBranch(newBranch);
        return Result.demo("Added demo branch " + newBranch + " to the local demo list only.", newBranch);
    }

    @Override
    public Result<String> deleteRepository(Repo repo) {
        return Result.demo("Demo deletion of " + repo.getFullName()
                + ". Nothing was deleted on GitHub.", repo.getFullName());
    }
}
