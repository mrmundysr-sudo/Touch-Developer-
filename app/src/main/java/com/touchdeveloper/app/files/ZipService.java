package com.touchdeveloper.app.files;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Creates source and handoff ZIP archives and computes checksums.
 */
public interface ZipService {

    /**
     * Zips the given directory trees into {@code outputFile}.
     *
     * @return list of relative entry names written.
     */
    List<String> zipDirectories(String rootName, List<File> roots, File outputFile) throws IOException;

    /** Builds a complete handoff ZIP and returns the created file. */
    HandoffResult createHandoffZip(HandoffRequest request, File outputDir) throws IOException;

    /** Computes the SHA-256 of a file, or "unavailable" when it cannot be read. */
    String sha256(File file);

    /** Suggested handoff file name, e.g. {@code TouchDeveloper-myapp-handoff-v001.zip}. */
    String handoffFileName(String projectName, int version);
}
