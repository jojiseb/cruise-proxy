package com.cruise.offshore_proxy.http;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequestData {
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final byte[] body;

    public HttpRequestData(String method, String url, Map<String,String> headers, byte[] body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }

    /**
     * Parse a raw HTTP request (start‑line + headers + blank line + optional body).
     */
    public static HttpRequestData parse(String raw) {
        String[] parts = raw.split("\\r?\\n\\r?\\n", 2);
        String headerBlock = parts[0];
        String bodyPart = parts.length > 1 ? parts[1] : "";

        String[] lines = headerBlock.split("\\r?\\n");
        String[] start = lines[0].split(" ", 3);
        String method = start[0];
        String url = start[1];

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
        return new HttpRequestData(method, url, headers, body);
    }

    /**
     * Serialize back to raw HTTP request string (for testing).
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(' ').append(url).append(" HTTP/1.1\r\n");
        headers.forEach((k,v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");
        return sb.toString() + new String(body, StandardCharsets.UTF_8);
    }
}
