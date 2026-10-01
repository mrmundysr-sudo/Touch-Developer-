package com.touchdeveloper.app.files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Inputs for a complete handoff ZIP.
 *
 * Every field is optional; the ZIP service writes whichever sections are present
 * and always writes a manifest that lists what was included.
 */
public class HandoffRequest {

    private String projectName = "project";
    private String versionInfo = "";
    private String commitInfo = "";
    private String aiInstructions = "";
    private String sourceCode = "";
    private String buildLog = "";
    private String errorLog = "";
    private String testResults = "";
    private String readme = "";
    private final List<File> sourceRoots = new ArrayList<>();
    private final List<File> assets = new ArrayList<>();
    private File apkFile;

    public String getProjectName() {
        return projectName;
    }

    public HandoffRequest setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    public HandoffRequest setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
        return this;
    }

    public String getCommitInfo() {
        return commitInfo;
    }

    public HandoffRequest setCommitInfo(String commitInfo) {
        this.commitInfo = commitInfo;
        return this;
    }

    public String getAiInstructions() {
        return aiInstructions;
    }

    public HandoffRequest setAiInstructions(String aiInstructions) {
        this.aiInstructions = aiInstructions;
        return this;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public HandoffRequest setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
        return this;
    }

    public String getBuildLog() {
        return buildLog;
    }

    public HandoffRequest setBuildLog(String buildLog) {
        this.buildLog = buildLog;
        return this;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public HandoffRequest setErrorLog(String errorLog) {
        this.errorLog = errorLog;
        return this;
    }

    public String getTestResults() {
        return testResults;
    }

    public HandoffRequest setTestResults(String testResults) {
        this.testResults = testResults;
        return this;
    }

    public String getReadme() {
        return readme;
    }

    public HandoffRequest setReadme(String readme) {
        this.readme = readme;
        return this;
    }

    public List<File> getSourceRoots() {
        return sourceRoots;
    }

    public List<File> getAssets() {
        return assets;
    }

    public File getApkFile() {
        return apkFile;
    }

    public HandoffRequest setApkFile(File apkFile) {
        this.apkFile = apkFile;
        return this;
    }
}
