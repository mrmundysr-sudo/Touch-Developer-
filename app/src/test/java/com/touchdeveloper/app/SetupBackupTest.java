package com.touchdeveloper.app;
import com.touchdeveloper.app.safety.SetupBackup;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;
public class SetupBackupTest {
 @Test public void portableRestoreAndAuthentication() throws Exception {
  Properties p=new Properties(); p.setProperty("github_token", "fake-test-token"); p.setProperty("openhands_endpoint", "https://example.invalid");
  char[] pass="test-password".toCharArray(); byte[] encrypted=SetupBackup.encrypt(p,pass);
  assertEquals(p,SetupBackup.decrypt(encrypted,pass));
  try { SetupBackup.decrypt(encrypted,"wrong-password".toCharArray()); fail("Wrong password accepted"); } catch(javax.crypto.AEADBadTagException expected) {}
  encrypted[encrypted.length-1]^=1;
  try { SetupBackup.decrypt(encrypted,pass); fail("Modified file accepted"); } catch(javax.crypto.AEADBadTagException expected) {}
 }
 @Test public void malformedAndEmptyFilesRejected() throws Exception {
  try { SetupBackup.decrypt(new byte[10],"test-password".toCharArray()); fail("Short file accepted"); } catch(java.io.IOException expected) {}
  byte[] empty=SetupBackup.encrypt(new Properties(),"test-password".toCharArray());
  try { SetupBackup.decrypt(empty,"test-password".toCharArray()); fail("Empty settings accepted"); } catch(java.io.IOException expected) {}
 }
}
