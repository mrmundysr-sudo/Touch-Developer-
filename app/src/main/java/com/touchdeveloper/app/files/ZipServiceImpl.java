package com.touchdeveloper.app.files;

import com.touchdeveloper.app.util.Formats;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP creation using the JDK archive classes. This service contains no network
 * access and never adds credential files: only the explicit source roots, assets,
 * APK, and text sections supplied by the caller are written.
 */
public class ZipServiceImpl implements ZipService {

    @Override
    public List<String> zipDirectories(String rootName, List<File> roots, File outputFile)
            throws IOException {
        List<String> written = new ArrayList<>();
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            for (File root : roots) {
                if (root == null || !root.exists()) {
                    continue;
                }
                if (root.isFile()) {
                    addFile(zos, root, rootName + "/" + root.getName(), written);
                } else {
                    addTree(zos, root, rootName + "/" + root.getName(), written);
                }
            }
        }
        return written;
    }

    private void addTree(ZipOutputStream zos, File dir, String prefix, List<String> written)
            throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String name = prefix + "/" + child.getName();
            if (child.isDirectory()) {
                addTree(zos, child, name, written);
            } else {
                addFile(zos, child, name, written);
            }
        }
    }

    private void addFile(ZipOutputStream zos, File file, String entryName, List<String> written)
            throws IOException {
        byte[] buffer = new byte[8192];
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            ZipEntry entry = new ZipEntry(entryName);
            entry.setTime(file.lastModified());
            zos.putNextEntry(entry);
            int read;
            while ((read = in.read(buffer)) != -1) {
                zos.write(buffer, 0, read);
            }
            zos.closeEntry();
        }
        written.add(entryName);
    }

    @Override
    public HandoffResult createHandoffZip(HandoffRequest request, File outputDir) throws IOException {
        String fileName = handoffFileName(request.getProjectName(), 1);
        File out = new File(outputDir, fileName);
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        HandoffResult result = new HandoffResult(out);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            // Full Android source project.
            List<File> roots = request.getSourceRoots();
            if (!roots.isEmpty()) {
                for (File root : roots) {
                    if (root == null || !root.exists()) {
                        result.getMissing().add("source: " + (root == null ? "null" : root.getPath()));
                        continue;
                    }
                    String name = "source/" + root.getName();
                    if (root.isDirectory()) {
                        addTreeRecording(zos, root, name, result.getContents());
                    } else {
                        addFileRecording(zos, root, name, result.getContents());
                    }
                }
            } else {
                result.getMissing().add("source project tree (none supplied)");
            }

            // Assets.
            for (File asset : request.getAssets()) {
                if (asset == null || !asset.exists()) {
                    result.getMissing().add("asset: " + (asset == null ? "null" : asset.getPath()));
                    continue;
                }
                if (asset.isDirectory()) {
                    addTreeRecording(zos, asset, "assets/" + asset.getName(), result.getContents());
                } else {
                    addFileRecording(zos, asset, "assets/" + asset.getName(), result.getContents());
                }
            }

            // APK.
            if (request.getApkFile() != null && request.getApkFile().exists()) {
                addFileRecording(zos, request.getApkFile(),
                        "apk/" + request.getApkFile().getName(), result.getContents());
            } else {
                result.getMissing().add("APK (no successful build artifact available yet)");
            }

            writeText(zos, "README.md", request.getReadme(), result);
            writeText(zos, "AI-INSTRUCTIONS.md", request.getAiInstructions(), result);
            writeText(zos, "SOURCE-NOTE.md", request.getSourceCode(), result);
            writeText(zos, "build-log.txt", request.getBuildLog(), result);
            writeText(zos, "error-log.txt", request.getErrorLog(), result);
            writeText(zos, "test-results.txt", request.getTestResults(), result);
            writeText(zos, "version-info.txt", request.getVersionInfo(), result);
            writeText(zos, "commit-info.txt", request.getCommitInfo(), result);
        }

        result.getChecksums().add(sha256(out));
        return result;
    }

    private void writeText(ZipOutputStream zos, String name, String content, HandoffResult result)
            throws IOException {
        if (content == null || content.trim().isEmpty()) {
            result.getMissing().add(name + " (empty)");
            return;
        }
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(System.currentTimeMillis());
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        result.getContents().add(name);
    }

    private void addTreeRecording(ZipOutputStream zos, File dir, String prefix, List<String> written)
            throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String name = prefix + "/" + child.getName();
            if (child.isDirectory()) {
                addTreeRecording(zos, child, name, written);
            } else {
                addFileRecording(zos, child, name, written);
            }
        }
    }

    private void addFileRecording(ZipOutputStream zos, File file, String entryName,
                                  List<String> written) throws IOException {
        byte[] buffer = new byte[8192];
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            ZipEntry entry = new ZipEntry(entryName);
            entry.setTime(file.lastModified());
            zos.putNextEntry(entry);
            int read;
            while ((read = in.read(buffer)) != -1) {
                zos.write(buffer, 0, read);
            }
            zos.closeEntry();
        }
        written.add(entryName);
    }

    @Override
    public String sha256(File file) {
        if (file == null || !file.exists()) {
            return "unavailable";
        }
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unavailable";
        }
    }

    @Override
    public String handoffFileName(String projectName, int version) {
        return "TouchDeveloper-" + Formats.sanitizeFileName(projectName)
                + "-handoff-v" + String.format(Locale.US, "%03d", version) + ".zip";
    }
}
