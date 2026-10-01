package com.touchdeveloper.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.touchdeveloper.app.ui.UploadFlow;
import com.touchdeveloper.app.util.Result;

import org.junit.Test;

import java.io.File;

/**
 * Regression tests for the upload flow.
 *
 * The screen previously required a typed target path before the picker would open.
 * These tests pin the picker-first flow's decision logic: a picked file suggests a
 * sane path, bad paths are rejected with a useful message, and each import/read/stage
 * failure produces one clear outcome instead of an opaque failure.
 */
public class UploadFlowTest {

    private static Result<File> imported(String name) {
        return Result.success("Imported " + name + " into private storage.",
                new File("/data/uploads/123-" + name));
    }

    private static Result<String> text() {
        return Result.success("Previewing file", "file contents");
    }

    @Test
    public void suggestedPathUsesOnlyTheFileName() {
        assertEquals("notes.txt",
                UploadFlow.suggestedTargetPath("/storage/emulated/0/Download/notes.txt"));
        assertEquals("Main.java", UploadFlow.suggestedTargetPath("app/src/main/Main.java"));
        assertEquals("notes.txt", UploadFlow.suggestedTargetPath("notes.txt"));
        assertEquals("notes.txt", UploadFlow.suggestedTargetPath("C:\\Users\\me\\notes.txt"));
        assertEquals("", UploadFlow.suggestedTargetPath(null));
    }

    @Test
    public void validTargetPathIsAccepted() {
        assertNull(UploadFlow.validateTargetPath("app/src/main/java/Foo.java"));
        assertNull(UploadFlow.validateTargetPath("README.md"));
        assertNull(UploadFlow.validateTargetPath("  docs/notes.txt  "));
    }

    @Test
    public void badTargetPathsAreRejectedWithAUsefulMessage() {
        assertNotNull(UploadFlow.validateTargetPath(""));
        assertNotNull(UploadFlow.validateTargetPath("   "));
        assertTrue(UploadFlow.validateTargetPath("/etc/passwd").contains("relative"));
        assertTrue(UploadFlow.validateTargetPath("docs/").contains("file"));
        assertTrue(UploadFlow.validateTargetPath("../outside.txt").contains("inside"));
        assertTrue(UploadFlow.validateTargetPath("a/../../b.txt").contains("inside"));
    }

    @Test
    public void successfulUploadIsStagedAndNotDemo() {
        UploadFlow.Outcome outcome = UploadFlow.from("docs/notes.txt",
                imported("notes.txt"), text(), Result.success("Staged docs/notes.txt locally.", "docs/notes.txt"));
        assertEquals(UploadFlow.Outcome.Status.STAGED, outcome.status);
        assertTrue(outcome.ok());
        assertFalse(outcome.isError());
        assertFalse("a real stage must not be labelled demo", outcome.demo);
        assertEquals("123-notes.txt", outcome.sourceName);
        assertTrue(outcome.message.contains("Staged docs/notes.txt"));
    }

    @Test
    public void importFailureStopsTheChainWithTheImportMessage() {
        UploadFlow.Outcome outcome = UploadFlow.from("docs/notes.txt",
                Result.failure("Import failed: permission denied"), null, null);
        assertEquals(UploadFlow.Outcome.Status.FAILED, outcome.status);
        assertTrue(outcome.isError());
        assertTrue(outcome.message.contains("permission denied"));
    }

    @Test
    public void binaryFileIsSavedButReportedAsNotStaged() {
        UploadFlow.Outcome outcome = UploadFlow.from("logo.png", imported("logo.png"),
                Result.failure("Preview is not available for binary or archive files (logo.png)."), null);
        assertEquals(UploadFlow.Outcome.Status.BINARY_UNSUPPORTED, outcome.status);
        assertTrue(outcome.isError());
        assertTrue(outcome.message.contains("not implemented in version 1"));
        assertEquals("123-logo.png", outcome.sourceName);
    }

    @Test
    public void stageFailureIsReportedAsAFailedUpload() {
        UploadFlow.Outcome outcome = UploadFlow.from("docs/notes.txt", imported("notes.txt"), text(),
                Result.failure("Could not stage docs/notes.txt."));
        assertEquals(UploadFlow.Outcome.Status.FAILED, outcome.status);
        assertTrue(outcome.message.contains("Could not stage"));
    }

    @Test
    public void demoStagingIsStagedButClearlyLabelled() {
        UploadFlow.Outcome outcome = UploadFlow.from("docs/notes.txt", imported("notes.txt"), text(),
                Result.demo("Staged docs/notes.txt in the demo staging area only.", "docs/notes.txt"));
        assertEquals(UploadFlow.Outcome.Status.STAGED, outcome.status);
        assertTrue("demo data must be flagged so it is not shown as a real upload", outcome.demo);
    }
}
