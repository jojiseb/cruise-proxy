package com.cruise.ship_proxy.http;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpResponseData {
    private final int statusCode;
    private final String statusText;
    private final Map<String,String> headers;
    private final byte[] body;

    public HttpResponseData(int statusCode, String statusText,
                            Map<String,String> headers, byte[] body) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
    }

    public int getStatusCode() { return statusCode; }
    public String getStatusText() { return statusText; }
    public Map<String,String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }

    /**
     * Parse a raw HTTP response (status‑line + headers + blank line + body).
     */
    public static HttpResponseData parse(String raw) {
        String[] parts = raw.split("\\r?\\n\\r?\\n", 2);
        String headerBlock = parts[0];
        String bodyPart = parts.length > 1 ? parts[1] : "";

        String[] lines = headerBlock.split("\\r?\\n");
        String[] status = lines[0].split(" ", 3);
        int statusCode = Integer.parseInt(status[1]);
        String statusText = status[2];

        Map<String,String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int idx = line.indexOf(':');
            if (idx > 0) {
                String name = line.substring(0, idx).trim();
                String value = line.substring(idx+1).trim();
                headers.put(name, value);
            }
        }

        byte[] body = bodyPart.getBytes(StandardCharsets.UTF_8);
        return new HttpResponseData(statusCode, statusText, headers, body);
    }

    /**
     * Serialize back to raw HTTP response string (for testing).
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ")
                .append(statusCode)
                .append(' ')
                .append(statusText)
                .append("\r\n");
        headers.forEach((k,v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");
        return sb.toString() + new String(body, StandardCharsets.UTF_8);
    }
}
