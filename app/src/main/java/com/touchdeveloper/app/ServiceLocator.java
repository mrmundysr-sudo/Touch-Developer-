package com.touchdeveloper.app;

import android.content.Context;

import com.touchdeveloper.app.build.BuildService;
import com.touchdeveloper.app.build.DemoBuildService;
import com.touchdeveloper.app.build.OpenHandsBuildService;
import com.touchdeveloper.app.files.FileTransferService;
import com.touchdeveloper.app.files.ZipService;
import com.touchdeveloper.app.files.ZipServiceImpl;
import com.touchdeveloper.app.github.GitHubApiService;
import com.touchdeveloper.app.github.GitHubDemoService;
import com.touchdeveloper.app.github.GitHubService;
import com.touchdeveloper.app.openhands.OpenHandsApiService;
import com.touchdeveloper.app.openhands.OpenHandsDemoService;
import com.touchdeveloper.app.openhands.OpenHandsService;
import com.touchdeveloper.app.safety.ActivityLog;
import com.touchdeveloper.app.safety.ActivityLogService;
import com.touchdeveloper.app.safety.CredentialService;
import com.touchdeveloper.app.safety.SecureCredentialService;

/**
 * Builds the service graph and selects real or demo implementations.
 *
 * A real implementation is used only when the matching credential is present in
 * secure storage. Otherwise the clearly labelled demo implementation is used.
 */
public class ServiceLocator {

    public static final String KEY_GITHUB_TOKEN = "github_token";
    public static final String KEY_OPENHANDS_ENDPOINT = "openhands_endpoint";
    public static final String KEY_OPENHANDS_TOKEN = "openhands_token";

    private final CredentialService credentials;
    private final ActivityLogService activityLog;
    private final ZipService zipService;
    private final FileTransferService fileTransferService;

    private GitHubService gitHubService;
    private OpenHandsService openHandsService;
    private BuildService buildService;
    private DemoBuildService demoBuildService;

    public ServiceLocator(Context context) {
        Context app = context.getApplicationContext();
        this.credentials = new SecureCredentialService(app);
        this.activityLog = new ActivityLog(app);
        this.zipService = new ZipServiceImpl();
        this.fileTransferService = new FileTransferService(app);
        this.demoBuildService = new DemoBuildService(app);
        rebuild();
    }

    /** Re-reads credentials and re-selects real or demo integrations. */
    public void rebuild() {
        String githubToken = credentials.retrieve(KEY_GITHUB_TOKEN);
        gitHubService = (githubToken != null && !githubToken.trim().isEmpty())
                ? new GitHubApiService(githubToken)
                : new GitHubDemoService();

        String endpoint = credentials.retrieve(KEY_OPENHANDS_ENDPOINT);
        String openHandsToken = credentials.retrieve(KEY_OPENHANDS_TOKEN);
        OpenHandsService openHands = (endpoint != null && !endpoint.trim().isEmpty())
                ? new OpenHandsApiService(endpoint, openHandsToken)
                : new OpenHandsDemoService();
        openHandsService = openHands;

        if (openHands.isConfigured()) {
            buildService = new OpenHandsBuildService(openHands);
        } else {
            buildService = demoBuildService;
        }
    }

    public CredentialService credentials() {
        return credentials;
    }

    public ActivityLogService activityLog() {
        return activityLog;
    }

    public GitHubService gitHub() {
        return gitHubService;
    }

    public OpenHandsService openHands() {
        return openHandsService;
    }

    public BuildService build() {
        return buildService;
    }

    public DemoBuildService demoBuild() {
        return demoBuildService;
    }

    public ZipService zip() {
        return zipService;
    }

    public FileTransferService fileTransfer() {
        return fileTransferService;
    }
}
