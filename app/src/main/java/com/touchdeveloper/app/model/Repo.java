package com.touchdeveloper.app.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A connected repository as shown on the Repository Dashboard.
 */
public class Repo {

    private final String owner;
    private final String name;
    private String currentBranch;
    private String latestCommitSha;
    private String latestCommitMessage;
    private StatusLabel buildStatus;
    private String latestApkName;
    private String latestSourceZipName;
    private final boolean demo;
    private final List<String> branches = new ArrayList<>();

    public Repo(String owner, String name, String currentBranch, boolean demo) {
        this.owner = owner;
        this.name = name;
        this.currentBranch = currentBranch;
        this.demo = demo;
        this.buildStatus = demo ? StatusLabel.DEMO : StatusLabel.NOT_CONFIGURED;
        this.branches.add(currentBranch);
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return owner + "/" + name;
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }

    public String getLatestCommitSha() {
        return latestCommitSha;
    }

    public void setLatestCommitSha(String latestCommitSha) {
        this.latestCommitSha = latestCommitSha;
    }

    public String getLatestCommitMessage() {
        return latestCommitMessage;
    }

    public void setLatestCommitMessage(String latestCommitMessage) {
        this.latestCommitMessage = latestCommitMessage;
    }

    public StatusLabel getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(StatusLabel buildStatus) {
        this.buildStatus = buildStatus;
    }

    public String getLatestApkName() {
        return latestApkName;
    }

    public void setLatestApkName(String latestApkName) {
        this.latestApkName = latestApkName;
    }

    public String getLatestSourceZipName() {
        return latestSourceZipName;
    }

    public void setLatestSourceZipName(String latestSourceZipName) {
        this.latestSourceZipName = latestSourceZipName;
    }

    public boolean isDemo() {
        return demo;
    }

    public List<String> getBranches() {
        return branches;
    }

    public void addBranch(String branch) {
        if (!branches.contains(branch)) {
            branches.add(branch);
        }
    }

    /** Short commit id for display, or "none" when unknown. */
    public String shortCommit() {
        if (latestCommitSha == null || latestCommitSha.isEmpty()) {
            return "none";
        }
        return latestCommitSha.length() > 7 ? latestCommitSha.substring(0, 7) : latestCommitSha;
    }
}
