package com.touchdeveloper.app.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import android.util.Base64OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal HTTP client built on HttpURLConnection.
 *
 * Authorization headers are only attached when a caller explicitly supplies a
 * token, and no request or response headers are ever written to logs, so tokens
 * cannot leak into build logs or handoff packages.
 */
public final class Http {

    public static class Response {
        public final int code;
        public final String body;
        /** True when the request never reached the server (DNS, connect, TLS, or timeout). */
        public final boolean transportError;

        Response(int code, String body) {
            this(code, body, code == -1);
        }

        Response(int code, String body, boolean transportError) {
            this.code = code;
            this.body = body;
            this.transportError = transportError;
        }

        public boolean ok() {
            return code >= 200 && code < 300;
        }
    }

    /**
     * A clear status description for an error: {@code "connection failed"} when the
     * request never reached the server, otherwise {@code "HTTP <code>"}. Avoids the
     * confusing "HTTP -1" for transport failures.
     */
    public static String statusText(Response response) {
        return response.transportError ? "connection failed" : "HTTP " + response.code;
    }

    private Http() {
    }

    public static Response request(String method, String url, String token, String body,
                                   String contentType) {
        return request(method, url, token, body, contentType, 15000, 30000);
    }

    /** Variant with explicit timeouts, used so tests can exercise the timeout path quickly. */
    public static Response request(String method, String url, String token, String body,
                                   String contentType, int connectTimeoutMs, int readTimeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            conn.setRequestProperty("User-Agent", "TouchDeveloper/1.0");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (body != null) {
                conn.setDoOutput(true);
                if (contentType != null) {
                    conn.setRequestProperty("Content-Type", contentType);
                }
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : null;
            if (stream == null) {
                stream = conn.getInputStream();
            }
            String responseBody = readFully(stream);
            return new Response(code, responseBody);
        } catch (Exception e) {
            // A transport failure has code -1. The body carries a human-readable reason
            // (unknown host, connection refused/timed out, TLS failure) so the UI can
            // explain what actually went wrong instead of showing an opaque status.
            return new Response(-1, transportMessage(e), true);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** Turns a transport exception into a clear, actionable reason for the user. */
    /** Prevents Base64OutputStream.close() from closing the network stream. */
    private static final class NonClosingOutputStream extends java.io.FilterOutputStream {
        NonClosingOutputStream(OutputStream out) { super(out); }
        @Override public void close() throws java.io.IOException { flush(); }
    }

    public static String transportMessage(Exception e) {
        String cause = e == null ? "" : e.getClass().getSimpleName();
        String detail = e == null || e.getMessage() == null ? cause : e.getMessage();
        String lower = detail.toLowerCase(java.util.Locale.US);
        String reason;
        if (lower.contains("timed out") || lower.contains("timeout")) {
            reason = "The connection timed out. Check your network and try again.";
        } else if (lower.contains("unable to resolve host") || lower.contains("unknownhost")
                || lower.contains("nodename nor servname")) {
            reason = "The server name could not be resolved. Check your internet connection or DNS.";
        } else if (lower.contains("econnrefused") || lower.contains("connection refused")) {
            reason = "The server refused the connection.";
        } else if (lower.contains("network is unreachable") || lower.contains("no route to host")) {
            reason = "The network is unreachable. Check your Wi-Fi or mobile data.";
        } else if (lower.contains("ssl") || lower.contains("certificate") || lower.contains("tls")) {
            reason = "A secure connection could not be established (TLS/certificate error).";
        } else if (cause.contains("SocketTimeout")) {
            reason = "The connection timed out. Check your network and try again.";
        } else if (cause.contains("UnknownHost")) {
            reason = "The server name could not be resolved. Check your internet connection or DNS.";
        } else if (cause.contains("Connect")) {
            reason = "Could not connect to the server. Check your network and try again.";
        } else {
            reason = "The request could not reach the server.";
        }
        return reason + (detail.isEmpty() || lower.startsWith(reason.toLowerCase(java.util.Locale.US))
                ? "" : " (" + detail + ")");
    }

    /** Sends a JSON object whose content field is streamed as Base64 from bytes. */
    public static Response postJsonWithBase64Bytes(String url, String token, byte[] bytes) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            conn.setRequestProperty("User-Agent", "TouchDeveloper/1.0");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            byte[] prefix = "{\"content\":\"".getBytes(StandardCharsets.UTF_8);
            byte[] suffix = "\",\"encoding\":\"base64\"}".getBytes(StandardCharsets.UTF_8);
            long byteCount = bytes == null ? 0 : bytes.length;
            long encodedLength = 4L * ((byteCount + 2L) / 3L);
            conn.setFixedLengthStreamingMode(prefix.length + encodedLength + suffix.length);
            try (OutputStream raw = conn.getOutputStream()) {
                raw.write(prefix);
                try (Base64OutputStream encoded = new Base64OutputStream(new NonClosingOutputStream(raw), android.util.Base64.NO_WRAP)) {
                    if (bytes != null) encoded.write(bytes);
                }
                raw.write(suffix);
            }
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            return new Response(code, readFully(stream));
        } catch (Exception e) {
            return new Response(-1, transportMessage(e), true);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Streams raw bytes using the exact headers returned by a Git LFS upload action. */
    public static Response putBytes(String url, Map<String, String> headers, byte[] bytes) {
        byte[] payload = bytes == null ? new byte[0] : bytes;
        Response last = new Response(-1, "The upload did not start.", true);
        for (int attempt = 1; attempt <= 3; attempt++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("PUT");
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(600000);
                conn.setRequestProperty("User-Agent", "TouchDeveloper/1.0");
                conn.setRequestProperty("Connection", "close");
                if (headers != null) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        if (header.getKey() != null && header.getValue() != null) {
                            conn.setRequestProperty(header.getKey(), header.getValue());
                        }
                    }
                }
                conn.setFixedLengthStreamingMode(payload.length);
                conn.setDoOutput(true);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload);
                    out.flush();
                }
                int code = conn.getResponseCode();
                InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                Response response = new Response(code, readFully(stream));
                if (response.ok() || !response.transportError) return response;
                last = response;
            } catch (Exception e) {
                last = new Response(-1, transportMessage(e), true);
            } finally {
                if (conn != null) conn.disconnect();
            }
            try {
                Thread.sleep(1000L * attempt);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return last;
    }

    /** Streams raw bytes to a pre-authorized upload URL. */
    public static Response putBytes(String url, String authorization, byte[] bytes) {
        return putBytes(url, authorization, "application/octet-stream", bytes);
    }

    /** Streams raw bytes while honoring the content type returned by Git LFS. */
    public static Response putBytes(String url, String authorization, String contentType, byte[] bytes) {
        byte[] payload = bytes == null ? new byte[0] : bytes;
        Response last = new Response(-1, "The upload did not start.", true);
        for (int attempt = 1; attempt <= 3; attempt++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("PUT");
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(600000);
                conn.setRequestProperty("User-Agent", "TouchDeveloper/1.0");
                conn.setRequestProperty("Connection", "close");
                if (authorization != null && !authorization.isEmpty()) {
                    conn.setRequestProperty("Authorization", authorization);
                }
                conn.setRequestProperty("Content-Type", contentType == null || contentType.isEmpty()
                        ? "application/octet-stream" : contentType);
                conn.setFixedLengthStreamingMode(payload.length);
                conn.setDoOutput(true);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload);
                    out.flush();
                }
                int code = conn.getResponseCode();
                InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                Response response = new Response(code, readFully(stream));
                if (response.ok() || !response.transportError) return response;
                last = response;
            } catch (Exception e) {
                last = new Response(-1, transportMessage(e), true);
            } finally {
                if (conn != null) conn.disconnect();
            }
            try {
                Thread.sleep(1000L * attempt);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return last;
    }

    public static Response get(String url, String token) {
        return request("GET", url, token, null, null);
    }

    public static Response put(String url, String token, String jsonBody) {
        return request("PUT", url, token, jsonBody, "application/json");
    }

    public static Response post(String url, String token, String jsonBody) {
        return request("POST", url, token, jsonBody, "application/json");
    }

    public static Response delete(String url, String token, String jsonBody) {
        return request("DELETE", url, token, jsonBody, "application/json");
    }

    /** POST with an already-formed Authorization header, used by Git LFS. */
    public static Response postWithAuthorization(String url, String authorization, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Accept", "application/vnd.git-lfs+json");
            conn.setRequestProperty("Content-Type", "application/vnd.git-lfs+json");
            conn.setRequestProperty("User-Agent", "TouchDeveloper/1.0");
            conn.setRequestProperty("Authorization", authorization);
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) { out.write(jsonBody.getBytes(StandardCharsets.UTF_8)); }
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            return new Response(code, readFully(stream));
        } catch (Exception e) { return new Response(-1, transportMessage(e), true); }
        finally { if (conn != null) conn.disconnect(); }
    }

    private static String readFully(InputStream in) {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /** Extracts a JSON string field from a flat object body. */
    public static String stringField(String json, String field) {
        if (json == null) {
            return null;
        }
        String needle = "\"" + field + "\"";
        int key = json.indexOf(needle);
        if (key < 0) {
            return null;
        }
        int colon = json.indexOf(':', key + needle.length());
        if (colon < 0) {
            return null;
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(next); break;
                }
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    public static Map<String, String> emptyQuery() {
        return new LinkedHashMap<>();
    }
}
