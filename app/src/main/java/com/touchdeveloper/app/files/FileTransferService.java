package com.touchdeveloper.app.files;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.touchdeveloper.app.util.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handles file and ZIP upload/download inside the app's private storage.
 *
 * Downloads written here are staged copies for preview and handoff building;
 * they only reach GitHub when a configured GitHubService push confirms success.
 */
public class FileTransferService {

    private final Context context;

    public FileTransferService(Context context) {
        this.context = context.getApplicationContext();
    }

    public File stagedDir() {
        File dir = new File(context.getFilesDir(), "staged");
        dir.mkdirs();
        return dir;
    }

    public File exportsDir() {
        File dir = new File(context.getFilesDir(), "exports");
        dir.mkdirs();
        return dir;
    }

    public File uploadsDir() {
        File dir = new File(context.getFilesDir(), "uploads");
        dir.mkdirs();
        return dir;
    }

    /** Copies a user-picked URI into private storage. */
    public Result<File> importUri(Uri uri, String fallbackName) {
        if (uri == null) {
            return Result.failure("No file was selected.");
        }
        String name = displayName(uri);
        if (name == null || name.isEmpty()) {
            name = fallbackName;
        }
        File target = new File(uploadsDir(), System.currentTimeMillis() + "-" + name);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) {
                return Result.failure("Could not open the selected file.");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return Result.success("Imported " + target.getName() + " into private storage.", target);
        } catch (Exception e) {
            return Result.failure("Import failed: " + e.getMessage());
        }
    }

    /**
     * Reads a staged text file for preview. ZIP and binary files are reported as
     * not previewable rather than decoded.
     */
    public Result<String> readText(File file) {
        if (file == null || !file.exists()) {
            return Result.failure("File is not available in local storage.");
        }
        String lower = file.getName().toLowerCase(java.util.Locale.US);
        if (lower.endsWith(".zip") || lower.endsWith(".apk") || lower.endsWith(".png")
                || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return Result.failure("Preview is not available for binary or archive files ("
                    + file.getName() + ").");
        }
        try (InputStream in = new java.io.FileInputStream(file)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            String text = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            if (text.length() > 200_000) {
                text = text.substring(0, 200_000) + "\n... [truncated for preview]";
            }
            return Result.success("Previewing " + file.getName(), text);
        } catch (Exception e) {
            return Result.failure("Preview failed: " + e.getMessage());
        }
    }

    /** Writes text to a file in the exports directory. */
    public Result<File> writeExport(String name, String content) {
        File target = new File(exportsDir(), name);
        try (OutputStream out = new FileOutputStream(target)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            return Result.success("Saved " + name + " to app storage.", target);
        } catch (Exception e) {
            return Result.failure("Could not save " + name + ": " + e.getMessage());
        }
    }

    /** Writes raw bytes to a file in the exports directory. */
    public Result<File> writeExportBytes(String name, byte[] data) {
        File target = new File(exportsDir(), name);
        try (OutputStream out = new FileOutputStream(target)) {
            out.write(data);
            return Result.success("Saved " + name + " (" + data.length + " bytes) to app storage.", target);
        } catch (Exception e) {
            return Result.failure("Could not save " + name + ": " + e.getMessage());
        }
    }

    public String displayName(Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
            // Fall through to the last path segment.
        }
        String path = uri.getLastPathSegment();
        return path == null ? null : path.substring(path.lastIndexOf('/') + 1);
    }
}
