package com.touchdeveloper.app.safety;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Default protection rules for artifacts that must not be removed by an
 * accidental file or repository action.
 */
public final class Protection {

    /** File name fragments that are treated as protected artifacts. */
    private static final List<String> PROTECTED_FRAGMENTS = Arrays.asList(
            "-handoff-", "handoff", ".apk", "source.zip", "-source-", "readme.md",
            "touch-developer-v1-build-prompt", "ai-instructions");

    private Protection() {
    }

    public static boolean isProtectedName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.US);
        for (String fragment : PROTECTED_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    public static String reasonFor(String name) {
        return "Protected by default: " + name
                + " looks like a source, APK, handoff, or handoff-instruction artifact.";
    }
}
