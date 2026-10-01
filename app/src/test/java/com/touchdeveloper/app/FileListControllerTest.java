package com.touchdeveloper.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.touchdeveloper.app.model.RepoFile;
import com.touchdeveloper.app.ui.FileListController;
import com.touchdeveloper.app.util.Result;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Regression tests for the file-screen reload loop.
 *
 * The screen previously replaced itself after every load, which re-ran its
 * automatic load and flashed "Loading files" forever. These tests pin the two
 * invariants that fix it: a bounded number of requests, and a single stable error.
 */
public class FileListControllerTest {

    private static Result<List<RepoFile>> files(String... names) {
        List<RepoFile> list = new ArrayList<>();
        for (String name : names) {
            list.add(new RepoFile(name, name, false, 1, "sha-" + name));
        }
        return Result.success("Loaded " + list.size() + " entries.", list);
    }

    private static Result<List<RepoFile>> failure(String message) {
        return Result.failure(message);
    }

    private static Result<List<RepoFile>> demo(String... names) {
        List<RepoFile> list = new ArrayList<>();
        for (String name : names) {
            list.add(new RepoFile(name, name, false, 1, "sha-" + name));
        }
        return Result.demo("Demo listing.", list);
    }

    @Test
    public void autoLoadHappensOnceThenStopsAfterSuccess() {
        FileListController controller = new FileListController();
        assertTrue("first show may auto-load", controller.shouldAutoLoad());

        assertTrue(controller.beginRequest());
        controller.onResult(files("README.md"));

        assertFalse("a completed load must not auto-load again", controller.shouldAutoLoad());
        assertEquals(1, controller.entries().size());
        assertEquals("", controller.error());
    }

    @Test
    public void autoLoadHappensOnceThenStopsAfterFailure() {
        FileListController controller = new FileListController();
        assertTrue(controller.shouldAutoLoad());

        assertTrue(controller.beginRequest());
        controller.onResult(failure("Could not list files (connection failed): timed out."));

        assertFalse("a failed load must not retry automatically", controller.shouldAutoLoad());
        assertTrue(controller.isLoaded());
        assertFalse(controller.isLoading());
        assertTrue(controller.error().contains("connection failed"));
    }

    @Test
    public void onlyOneRequestCanBeInFlight() {
        FileListController controller = new FileListController();
        assertTrue("the first request is admitted", controller.beginRequest());
        assertFalse("a second concurrent request is refused", controller.beginRequest());
        assertTrue(controller.isLoading());

        controller.onResult(files("a"));

        assertFalse(controller.isLoading());
        assertTrue("after completion a new request is allowed", controller.beginRequest());
    }

    @Test
    public void failureKeepsThePreviousListAndDoesNotDemo() {
        FileListController controller = new FileListController();
        controller.beginRequest();
        controller.onResult(files("README.md", "templates"));

        controller.beginRequest();
        controller.onResult(failure("Could not list files (connection failed): host not resolved."));

        assertEquals("a failure must not clear a previously loaded list", 2, controller.entries().size());
        assertEquals("README.md", controller.entries().get(0).getName());
        assertTrue(controller.error().contains("host not resolved"));
    }

    @Test
    public void emptySuccessIsReportedAsEmptyWithoutError() {
        FileListController controller = new FileListController();
        controller.beginRequest();
        controller.onResult(files());

        assertTrue(controller.isLoaded());
        assertTrue(controller.entries().isEmpty());
        assertEquals("an empty branch is not an error", "", controller.error());
        assertFalse(controller.shouldAutoLoad());
    }

    @Test
    public void demoResultIsNotAnErrorAndKeepsData() {
        FileListController controller = new FileListController();
        controller.beginRequest();
        controller.onResult(demo("demo-file"));

        assertEquals("", controller.error());
        assertEquals(1, controller.entries().size());
        assertFalse("demo data must never auto-load again", controller.shouldAutoLoad());
    }

    @Test
    public void repeatedResultsProduceNoExtraRequests() {
        FileListController controller = new FileListController();
        controller.beginRequest();
        controller.onResult(failure("Could not list files (connection failed): timed out."));

        // Simulate the old loop: many re-render passes.
        for (int i = 0; i < 100; i++) {
            assertFalse(controller.shouldAutoLoad());
        }
        assertTrue(controller.beginRequest());
        controller.onResult(files("README.md"));
        assertEquals(1, controller.entries().size());
    }

    @Test
    public void entriesViewIsUnmodifiable() {
        FileListController controller = new FileListController();
        controller.beginRequest();
        controller.onResult(files("x"));
        List<RepoFile> view = controller.entries();
        assertEquals(1, view.size());
        try {
            view.add(new RepoFile("y", "y", false, 1, "sha-y"));
            org.junit.Assert.fail("expected an unmodifiable view");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }
}
