package com.touchdeveloper.app.safety;

import java.util.List;

/**
 * Activity history of actions taken in the app.
 */
public interface ActivityLogService {

    void record(String category, String message, boolean confirmedByUser);

    List<ActivityEntry> entries();

    String asText();
}
