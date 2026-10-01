package com.touchdeveloper.app.github;

import com.touchdeveloper.app.model.CommitChange;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.model.RepoFile;
import com.touchdeveloper.app.util.Result;

import java.util.List;

/**
 * GitHub operations used by the dashboard, file screen, and action menu.
 *
 * Implementations must return {@code Result.failure} or a labelled demo result
 * when an integration is not configured, and must never report success without
 * a confirming API response.
 *
 * Commit model: file edits are staged locally first. {@code commitStaged} records
 * a commit, and {@code pushStaged} publishes it to GitHub after a separate
 * confirmation from the UI.
 */
public interface GitHubService {

    boolean isConfigured();

    /** Human-readable description of the active mode, e.g. "GitHub REST API" or "[DEMO]". */
    String modeDescription();

    Result<List<Repo>> listRepositories();

    Result<List<String>> listBranches(Repo repo);

    /** Refreshes branch and latest-commit information for the repository. */
    Result<Repo> refresh(Repo repo);

    Result<String> pullLatest(Repo repo);

    Result<List<RepoFile>> listFiles(Repo repo, String path);

    Result<String> readFile(Repo repo, RepoFile file);

    /** Downloads raw file bytes, or reports that the file cannot be downloaded. */
    Result<byte[]> downloadFile(Repo repo, RepoFile file);

    /** Stages a new or replaced file locally. Does not publish anything. */
    Result<String> stageFile(Repo repo, String path, String content);

    /** Stages a deletion locally. */
    Result<String> stageDelete(Repo repo, RepoFile file);

    /** Stages a rename locally. */
    Result<String> stageRename(Repo repo, RepoFile file, String newPath);

    List<CommitChange> previewChanges(Repo repo);

    /** Returns the staged content for a path, or null when nothing is staged for it. */
    String stagedContent(String path);

    /** Records the staged changes as a local commit. */
    Result<String> commitStaged(Repo repo, String message);

    /** Publishes the staged changes to GitHub (a real, confirmed API write). */
    Result<String> pushStaged(Repo repo, String message);

    Result<String> createBranch(Repo repo, String newBranch);

    Result<String> deleteRepository(Repo repo);
}
