package com.touchdeveloper.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.touchdeveloper.app.util.Http;

import org.junit.Test;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Tests for the HTTP layer's failure reporting, which is what the app shows when a
 * GitHub call cannot complete.
 *
 * These use a loopback server so the real code paths (timeout and error-body
 * handling) run without a device or network.
 */
public class HttpTransportTest {

    /** Accepts one connection, waits, then closes, forcing a read timeout. */
    @Test
    public void timeoutIsReportedAsATransportErrorWithAUsefulMessage() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread acceptor = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    Thread.sleep(3000);
                    socket.getOutputStream().write("late".getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                    // The client is expected to give up first.
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            Http.Response response = Http.request("GET",
                    "http://127.0.0.1:" + server.getLocalPort() + "/user/repos",
                    null, null, null, 1000, 400);

            assertEquals("a transport failure uses code -1", -1, response.code);
            assertTrue("transport failures are flagged", response.transportError);
            assertFalse(response.ok());
            assertNotNull(response.body);
            assertFalse("the message must be useful, not empty", response.body.trim().isEmpty());
            assertFalse("the opaque 'no message' text must not appear",
                    response.body.contains("no message"));
            assertTrue("a timeout must say so",
                    response.body.toLowerCase(java.util.Locale.US).contains("timed out"));
        }
    }

    /** Returns a GitHub-shaped error body with a non-2xx status. */
    @Test
    public void errorBodyMessageIsParsedForNonSuccessStatus() throws Exception {
        String json = "{\"message\":\"Bad credentials\",\"documentation_url\":\"https://docs.github.com\"}";
        try (ServerSocket server = new ServerSocket(0)) {
            Thread responder = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    socket.getInputStream().read(new byte[1024]);
                    byte[] body = json.getBytes(StandardCharsets.UTF_8);
                    String headers = "HTTP/1.1 401 Unauthorized\r\n"
                            + "Content-Type: application/json\r\n"
                            + "Content-Length: " + body.length + "\r\n"
                            + "Connection: close\r\n\r\n";
                    OutputStream out = socket.getOutputStream();
                    out.write(headers.getBytes(StandardCharsets.UTF_8));
                    out.write(body);
                    out.flush();
                } catch (Exception ignored) {
                    // Best effort; the assertions below fail loudly if nothing arrived.
                }
            });
            responder.setDaemon(true);
            responder.start();

            Http.Response response = Http.request("GET",
                    "http://127.0.0.1:" + server.getLocalPort() + "/user/repos",
                    null, null, null, 2000, 2000);

            assertEquals(401, response.code);
            assertFalse(response.ok());
            assertFalse("a 401 reached the server, so it is not a transport error",
                    response.transportError);
            assertEquals("Bad credentials", Http.stringField(response.body, "message"));
        }
    }

    /** An unreachable port fails fast and stays a transport error. */
    @Test
    public void connectionRefusedIsATransportError() throws Exception {
        int port;
        try (ServerSocket probe = new ServerSocket(0)) {
            port = probe.getLocalPort();
        }
        Http.Response response = Http.request("GET",
                "http://127.0.0.1:" + port + "/user/repos", null, null, null, 1500, 1500);
        assertEquals(-1, response.code);
        assertTrue(response.transportError);
        assertFalse(response.body.trim().isEmpty());
    }
}
