package com.touchdeveloper.app.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Source, AI instructions, and optional attachments for one build request.
 *
 * Setters are fluent so a request can be built and passed to a
 * {@link BuildService} without extra statements. Text fields are never null.
 */
public class BuildRequest {

    private String sourceCode = "";
    private String aiInstructions = "";
    private String repoFullName = "";
    private String branch = "main";
    private String projectName = "";
    private File projectZip;
    private final List<File> assets = new ArrayList<>();

    public String getSourceCode() {
        return sourceCode;
    }

    public BuildRequest setSourceCode(String sourceCode) {
        this.sourceCode = nullToEmpty(sourceCode);
        return this;
    }

    public String getAiInstructions() {
        return aiInstructions;
    }

    public BuildRequest setAiInstructions(String aiInstructions) {
        this.aiInstructions = nullToEmpty(aiInstructions);
        return this;
    }

    public String getRepoFullName() {
        return repoFullName;
    }

    public BuildRequest setRepoFullName(String repoFullName) {
        this.repoFullName = nullToEmpty(repoFullName);
        return this;
    }

    public String getBranch() {
        return branch;
    }

    public BuildRequest setBranch(String branch) {
        this.branch = nullToEmpty(branch);
        return this;
    }

    public String getProjectName() {
        return projectName;
    }

    public BuildRequest setProjectName(String projectName) {
        this.projectName = nullToEmpty(projectName);
        return this;
    }

    public File getProjectZip() {
        return projectZip;
    }

    public BuildRequest setProjectZip(File projectZip) {
        this.projectZip = projectZip;
        return this;
    }

    /** Mutable list of optional asset files; never null. */
    public List<File> getAssets() {
        return assets;
    }

    /** True when any source code or instruction text is present. */
    public boolean hasContent() {
        return !sourceCode.trim().isEmpty() || !aiInstructions.trim().isEmpty()
                || projectZip != null || !assets.isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
