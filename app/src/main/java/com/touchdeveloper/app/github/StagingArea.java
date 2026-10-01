package com.touchdeveloper.app.github;

import com.touchdeveloper.app.model.CommitChange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local staging area shared by the real and demo GitHub services.
 *
 * Nothing here is published. It only records what the user asked to change so the
 * UI can show exactly what a commit or push would contain.
 */
public class StagingArea {

    private final Map<String, String> upserts = new LinkedHashMap<>();
    private final Map<String, String> deletes = new LinkedHashMap<>();
    private final Map<String, String> renames = new LinkedHashMap<>();

    public void stage(String path, String content) {
        upserts.put(path, content);
        deletes.remove(path);
    }

    public void stageDelete(String path, String reason) {
        deletes.put(path, reason);
        upserts.remove(path);
    }

    public void stageRename(String from, String to) {
        renames.put(from, to);
    }

    public boolean isEmpty() {
        return upserts.isEmpty() && deletes.isEmpty() && renames.isEmpty();
    }

    public Map<String, String> getUpserts() {
        return upserts;
    }

    public Map<String, String> getDeletes() {
        return deletes;
    }

    public Map<String, String> getRenames() {
        return renames;
    }

    public List<CommitChange> changes() {
        List<CommitChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> e : upserts.entrySet()) {
            changes.add(new CommitChange(CommitChange.Kind.MODIFY, e.getKey(),
                    "new content, " + (e.getValue() == null ? 0 : e.getValue().length()) + " chars"));
        }
        for (Map.Entry<String, String> e : deletes.entrySet()) {
            changes.add(new CommitChange(CommitChange.Kind.DELETE, e.getKey(), e.getValue()));
        }
        for (Map.Entry<String, String> e : renames.entrySet()) {
            changes.add(new CommitChange(CommitChange.Kind.RENAME, e.getKey(), "to " + e.getValue()));
        }
        return changes;
    }

    public List<String> paths() {
        List<String> paths = new ArrayList<>();
        for (CommitChange c : changes()) {
            paths.add(c.display());
        }
        return paths;
    }

    public void clear() {
        upserts.clear();
        deletes.clear();
        renames.clear();
    }
}
