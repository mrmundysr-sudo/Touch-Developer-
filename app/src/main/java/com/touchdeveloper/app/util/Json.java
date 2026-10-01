package com.touchdeveloper.app.util;

/** Minimal helpers for reading and writing the JSON shapes used by the GitHub API. */
public final class Json {

    private Json() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Reads a numeric field value from a flat JSON object body. */
    public static long numericField(String json, String field) {
        if (json == null) {
            return 0;
        }
        String needle = "\"" + field + "\"";
        int key = json.indexOf(needle);
        if (key < 0) {
            return 0;
        }
        int colon = json.indexOf(':', key + needle.length());
        if (colon < 0) {
            return 0;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '-')) {
            i++;
        }
        if (i == start) {
            return 0;
        }
        try {
            return Long.parseLong(json.substring(start, i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Splits a top-level JSON array body into its object element substrings. */
    public static java.util.List<String> objects(String arrayBody) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (arrayBody == null) {
            return out;
        }
        int depth = 0;
        int start = -1;
        boolean inString = false;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    out.add(arrayBody.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return out;
    }

    /** Returns true when the body is a JSON object that contains an error message. */
    public static String errorMessage(String body) {
        String message = Http.stringField(body, "message");
        return message == null ? null : message;
    }
}
