package com.touchdeveloper.app.safety;

/**
 * Secure storage for integration credentials.
 *
 * Implementations must never persist credentials in plain text and must never
 * log or export credential values.
 */
public interface CredentialService {

    /** Stores a credential under {@code key}. Returns true only if persistence succeeded. */
    boolean store(String key, String value);

    /** Returns the stored credential, or null when absent. */
    String retrieve(String key);

    /** Returns the stored credential, or {@code fallback} when absent. */
    String retrieveOrDefault(String key, String fallback);

    /** Returns true when a non-empty credential exists for {@code key}. */
    boolean has(String key);

    /** Removes a stored credential. */
    void remove(String key);

    /** Removes every stored credential. */
    void clearAll();

    /**
     * Names of the credentials this app knows about, paired with the last four
     * characters of the stored value (or "not set"). Never exposes full values.
     */
    String maskedSummary(String key);
}
