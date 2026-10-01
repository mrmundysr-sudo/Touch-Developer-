package com.touchdeveloper.app.model;

/**
 * A file or folder entry on the Repository File Screen.
 */
public class RepoFile {

    private final String path;
    private final String name;
    private final boolean directory;
    private final long size;
    private final String sha;
    private String textContent;
    private boolean protectedArtifact;
    private boolean includedInBuild = true;
    private boolean stagedLocally;

    public RepoFile(String path, String name, boolean directory, long size, String sha) {
        this.path = path;
        this.name = name;
        this.directory = directory;
        this.size = size;
        this.sha = sha;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public long getSize() {
        return size;
    }

    public String getSha() {
        return sha;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public boolean isProtectedArtifact() {
        return protectedArtifact;
    }

    public void setProtectedArtifact(boolean protectedArtifact) {
        this.protectedArtifact = protectedArtifact;
    }

    public boolean isIncludedInBuild() {
        return includedInBuild;
    }

    public void setIncludedInBuild(boolean includedInBuild) {
        this.includedInBuild = includedInBuild;
    }

    public boolean isStagedLocally() {
        return stagedLocally;
    }

    public void setStagedLocally(boolean stagedLocally) {
        this.stagedLocally = stagedLocally;
    }

    public String displayName() {
        return directory ? name + "/" : name;
    }
}
