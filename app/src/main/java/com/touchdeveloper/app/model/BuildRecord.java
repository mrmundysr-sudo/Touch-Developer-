package com.touchdeveloper.app.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * One build attempt, including progress messages and Gradle output.
 */
public class BuildRecord {

    private static final SimpleDateFormat TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private final String id;
    private final String projectName;
    private final String repoFullName;
    private final String branch;
    private StatusLabel status;
    private long startTime;
    private long endTime;
    private String apkPath;
    private String sourceZipPath;
    private String handoffPath;
    private boolean demo;
    private final StringBuilder gradleOutput = new StringBuilder();
    private final StringBuilder errorOutput = new StringBuilder();
    private final List<String> progress = new ArrayList<>();

    public BuildRecord(String id, String projectName, String repoFullName, String branch) {
        this.id = id;
        this.projectName = projectName;
        this.repoFullName = repoFullName;
        this.branch = branch;
        this.status = StatusLabel.LOCAL;
    }

    public String getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getRepoFullName() {
        return repoFullName;
    }

    public String getBranch() {
        return branch;
    }

    public StatusLabel getStatus() {
        return status;
    }

    public void setStatus(StatusLabel status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String startText() {
        return startTime == 0 ? "not started" : TIME.format(new Date(startTime));
    }

    public String endText() {
        return endTime == 0 ? "in progress" : TIME.format(new Date(endTime));
    }

    public String getApkPath() {
        return apkPath;
    }

    public void setApkPath(String apkPath) {
        this.apkPath = apkPath;
    }

    public String getSourceZipPath() {
        return sourceZipPath;
    }

    public void setSourceZipPath(String sourceZipPath) {
        this.sourceZipPath = sourceZipPath;
    }

    public String getHandoffPath() {
        return handoffPath;
    }

    public void setHandoffPath(String handoffPath) {
        this.handoffPath = handoffPath;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public void appendProgress(String message) {
        progress.add(TIME.format(new Date()) + "  " + message);
    }

    public List<String> getProgress() {
        return progress;
    }

    public void appendGradle(String line) {
        gradleOutput.append(line).append('\n');
    }

    public String getGradleOutput() {
        return gradleOutput.toString();
    }

    public void appendError(String line) {
        errorOutput.append(line).append('\n');
    }

    public String getErrorOutput() {
        return errorOutput.toString();
    }

    public boolean hasErrors() {
        return errorOutput.length() > 0;
    }

    public String progressText() {
        StringBuilder sb = new StringBuilder();
        for (String p : progress) {
            sb.append(p).append('\n');
        }
        return sb.toString();
    }
}
