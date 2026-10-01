package com.touchdeveloper.app.openhands;

import com.touchdeveloper.app.build.BuildRequest;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.safety.DemoMode;
import com.touchdeveloper.app.util.Result;

/**
 * Labelled demo OpenHands behaviour used when no endpoint is configured.
 *
 * Demo calls never report success and never send anything over the network.
 */
public class OpenHandsDemoService implements OpenHandsService {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String modeDescription() {
        return DemoMode.banner("OpenHands");
    }

    @Override
    public Result<String> openRepositoryLink(Repo repo) {
        return Result.demo("Demo mode: a real OpenHands link would open " + repo.getFullName()
                + ". Nothing was opened.", "https://app.all-hands.dev/");
    }

    @Override
    public Result<String> sendInstructions(BuildRequest request) {
        return Result.demo("Demo mode: " + request.getAiInstructions().length()
                + " characters of instructions would be sent. Nothing was sent.", null);
    }

    @Override
    public Result<String> startBuild(BuildRequest request) {
        return Result.demo("Demo mode: no remote build was started. Use the local demo build to preview "
                + "the progress and status screens.", null);
    }

    @Override
    public Result<String> buildStatus(String jobReference) {
        return Result.demo("Demo mode: no remote job exists to poll.", null);
    }
}
