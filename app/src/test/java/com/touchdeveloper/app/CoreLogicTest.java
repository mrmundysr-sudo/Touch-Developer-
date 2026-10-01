package com.touchdeveloper.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.touchdeveloper.app.build.BuildRequest;
import com.touchdeveloper.app.files.HandoffRequest;
import com.touchdeveloper.app.files.ZipServiceImpl;
import com.touchdeveloper.app.files.HandoffResult;
import com.touchdeveloper.app.github.GitHubDemoService;
import com.touchdeveloper.app.github.StagingArea;
import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.model.CommitChange;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.model.RepoFile;
import com.touchdeveloper.app.model.StatusLabel;
import com.touchdeveloper.app.openhands.OpenHandsDemoService;
import com.touchdeveloper.app.safety.Protection;
import com.touchdeveloper.app.util.Formats;
import com.touchdeveloper.app.util.Http;
import com.touchdeveloper.app.util.Json;
import com.touchdeveloper.app.util.Result;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Unit tests for the pure-Java logic that does not require an Android device.
 * These cover demo labelling, safety protections, ZIP creation, and JSON parsing.
 */
public class CoreLogicTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void demoResultsAreNeverReportedAsSuccess() {
        Result<String> demo = Result.demo("demo message", "data");
        assertFalse("demo results must not be ok", demo.ok);
        assertTrue(demo.demo);
        assertNotNull(demo.data);
        assertTrue(demo.display().startsWith("[DEMO]"));

        Result<String> failure = Result.failure("nope");
        assertFalse(failure.ok);
        assertFalse(failure.demo);
        assertNull(failure.data);
    }

    @Test
    public void demoGitHubServiceReportsFailureForPushAndIsNotConfigured() {
        GitHubDemoService service = new GitHubDemoService();
        assertFalse(service.isConfigured());

        Repo repo = new Repo("demo", "demo-repo", "main", true);
        Result<List<Repo>> repos = service.listRepositories();
        assertFalse(repos.ok);
        assertTrue(repos.demo);
        assertTrue(repos.data.size() >= 2);

        repo.addBranch("feature");
        service.stageFile(repo, "src/App.java", "class App {}");
        assertEquals(1, service.previewChanges(repo).size());
        assertEquals("class App {}", service.stagedContent("src/App.java"));

        Result<String> push = service.pushStaged(repo, "message");
        assertFalse("demo push must fail", push.ok);
        assertFalse(push.demo);
    }

    @Test
    public void protectionFlagsRiskyArtifacts() {
        assertTrue(Protection.isProtectedName("app-debug.apk"));
        assertTrue(Protection.isProtectedName("TouchDeveloper-handoff-v001.zip"));
        assertTrue(Protection.isProtectedName("README.md"));
        assertTrue(Protection.isProtectedName("TOUCH-DEVELOPER-V1-BUILD-PROMPT.md"));
        assertFalse(Protection.isProtectedName("MainActivity.java"));
        assertNotNull(Protection.reasonFor("app-debug.apk"));
    }

    @Test
    public void stagingAreaTracksUpsertsDeletesAndRenames() {
        StagingArea area = new StagingArea();
        assertTrue(area.isEmpty());
        area.stage("a.txt", "hello");
        area.stageDelete("b.txt", "gone");
        area.stageRename("c.txt", "d.txt");
        assertFalse(area.isEmpty());
        assertEquals(3, area.changes().size());

        boolean hasDelete = false;
        for (CommitChange change : area.changes()) {
            if (change.getKind() == CommitChange.Kind.DELETE) {
                hasDelete = true;
            }
        }
        assertTrue(hasDelete);
        area.clear();
        assertTrue(area.isEmpty());
    }

    @Test
    public void zipServiceCreatesHandoffPackageWithManifestAndChecksum() throws Exception {
        File sourceDir = temp.newFolder("source");
        Files.write(new File(sourceDir, "App.java").toPath(),
                "class App {}\n".getBytes(StandardCharsets.UTF_8));

        File assetsDir = temp.newFolder("assets");
        Files.write(new File(assetsDir, "logo.txt").toPath(),
                "asset\n".getBytes(StandardCharsets.UTF_8));

        File apk = temp.newFile("app-debug.apk");
        Files.write(apk.toPath(), new byte[]{1, 2, 3});

        HandoffRequest request = new HandoffRequest()
                .setProjectName("demo-app")
                .setReadme("# Demo\n")
                .setAiInstructions("Build the latest version.")
                .setSourceCode("class Main {}")
                .setBuildLog("BUILD SUCCESSFUL")
                .setErrorLog("No errors")
                .setTestResults("Owner testing required")
                .setVersionInfo("version 1.0")
                .setCommitInfo("commit abcdef1")
                .setApkFile(apk);
        request.getSourceRoots().add(sourceDir);
        request.getAssets().add(assetsDir);

        File outDir = temp.newFolder("out");
        ZipServiceImpl service = new ZipServiceImpl();
        HandoffResult result = service.createHandoffZip(request, outDir);

        File zip = result.getFile();
        assertTrue("handoff zip should exist", zip.exists());
        assertTrue(zip.length() > 0);
        assertNotNull(result.manifestText());
        assertFalse(result.manifestText().isEmpty());
        assertEquals(1, result.getChecksums().size());
        assertEquals(64, result.getChecksums().get(0).length());

        List<String> names = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zip)) {
            zipFile.stream().forEach(entry -> names.add(entry.getName()));
        }
        assertTrue(names.contains("README.md"));
        assertTrue(names.contains("AI-INSTRUCTIONS.md"));
        assertTrue(names.contains("build-log.txt"));
        assertTrue(names.contains("commit-info.txt"));
        assertTrue(names.stream().anyMatch(n -> n.endsWith("app-debug.apk")));
        assertTrue(names.stream().anyMatch(n -> n.endsWith("App.java")));

        // No credential-shaped file may ever appear in a handoff package.
        assertFalse(names.stream().anyMatch(n -> n.toLowerCase().contains("credential")));
        assertFalse(names.stream().anyMatch(n -> n.toLowerCase().contains("token")));
    }

    @Test
    public void handoffFileNameIsSanitized() {
        ZipServiceImpl service = new ZipServiceImpl();
        String name = service.handoffFileName("my repo/../evil", 3);
        assertEquals("TouchDeveloper-my-repo-..-evil-handoff-v003.zip", name);
        assertFalse(name.contains("/"));
        assertEquals("TouchDeveloper-project-handoff-v001.zip",
                service.handoffFileName("   ", 1));
    }

    @Test
    public void jsonHelpersParseStringAndNumericFields() {
        String json = "{\"name\":\"Repo Name\",\"size\":2048,\"nested\":{\"login\":\"owner\"}}";
        assertEquals("Repo Name", Http.stringField(json, "name"));
        assertEquals("owner", Http.stringField(json, "login"));
        assertEquals(2048, Json.numericField(json, "size"));
        assertNull(Http.stringField(json, "missing"));
    }

    @Test
    public void jsonObjectsSplitsTopLevelArray() {
        String array = "[{\"name\":\"a\"},{\"name\":\"b\"}]";
        assertEquals(2, Json.objects(array).size());
        assertEquals("{\"name\":\"a\"}", Json.objects(array).get(0));
    }

    @Test
    public void jsonEscapeHandlesQuotesAndNewlines() {
        assertEquals("a\\\"b", Json.escape("a\"b"));
        assertEquals("a\\nb", Json.escape("a\nb"));
        assertEquals("a\\\\b", Json.escape("a\\b"));
    }

    @Test
    public void buildRecordTracksProgressAndErrorsWithoutClaimingSuccess() {
        BuildRecord record = new BuildRecord("id", "proj", "owner/repo", "main");
        assertEquals(StatusLabel.LOCAL, record.getStatus());
        record.appendProgress("started");
        record.appendGradle("> Task :app:assembleDebug");
        assertFalse(record.hasErrors());
        record.appendError("error: broken");
        assertTrue(record.hasErrors());
        assertTrue(record.progressText().contains("started"));
        assertEquals("not started", record.startText());
        assertEquals("in progress", record.endText());
    }

    @Test
    public void formatsSanitizeUnsafeCharacters() {
        assertEquals("project", Formats.sanitizeFileName(""));
        assertEquals("a-b-c", Formats.sanitizeFileName("a/b c"));
        assertFalse(Formats.sanitizeFileName("../../x").contains("/"));
    }

    @Test
    public void openHandsDemoNeverReportsSuccess() {
        OpenHandsDemoService service = new OpenHandsDemoService();
        assertFalse(service.isConfigured());
        BuildRequest request = new BuildRequest().setAiInstructions("do things");
        assertFalse(service.startBuild(request).ok);
        assertFalse(service.sendInstructions(request).ok);
        assertFalse(service.buildStatus("job").ok);
        assertTrue(service.startBuild(request).demo);
    }

    @Test
    public void repoFileDisplayAndFlags() {
        RepoFile file = new RepoFile("app/src", "src", true, 0, null);
        assertEquals("src/", file.displayName());
        assertTrue(file.isIncludedInBuild());
        file.setIncludedInBuild(false);
        assertFalse(file.isIncludedInBuild());
        file.setStagedLocally(true);
        assertTrue(file.isStagedLocally());
    }

    @Test
    public void repoShowsShortCommitAndStatus() {
        Repo repo = new Repo("o", "r", "main", false);
        assertEquals("none", repo.shortCommit());
        repo.setLatestCommitSha("abcdef1234567890");
        assertEquals("abcdef1", repo.shortCommit());
        assertEquals("o/r", repo.getFullName());
        repo.addBranch("main");
        assertEquals(1, repo.getBranches().size());
    }
}
