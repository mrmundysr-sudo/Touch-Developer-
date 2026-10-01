# Setup restore update

Debug 5 is the user-confirmed installed reference. APK comparison identified changes in RepoActionScreen and its upload callback; other differences were synthetic compiler metadata. The working source preserves Debug 5 path-first Pick file behavior.

Save Setup creates a separate password-encrypted connection file through Android document storage. Restore Setup reads that file after reinstall; it does not depend on the removed Keystore key. Tokens must still be valid. Credentials are never put in source archives or logs. The user explicitly requested this portable setup backup; it is an exception to the old blanket credential-export prohibition.

Validation required: CI unit tests, lint, APK build, and phone save/uninstall/reinstall/restore check. No phone test has been performed yet. Existing APK and uploaded backup were not modified.
