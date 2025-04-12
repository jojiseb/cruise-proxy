package com.cruise.ship_proxy.http;

public class HttpParserSmokeTest {
    public static void main(String[] args) {
        String rawReq = "POST /foo HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "X-Test: yes\r\n" +
                "\r\n" +
                "body-data";
        HttpRequestData req = HttpRequestData.parse(rawReq);
        System.out.println("Method: " + req.getMethod());
        System.out.println("URL: " + req.getUrl());
        System.out.println("Headers: " + req.getHeaders());
        System.out.println("Body: " + new String(req.getBody()));

        String round = req.serialize();
        System.out.println("Reserialized request:\n" + round);

        String rawResp = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";
        HttpResponseData resp = HttpResponseData.parse(rawResp);
        System.out.println("Status: " + resp.getStatusCode() + " " + resp.getStatusText());
        System.out.println("Resp headers: " + resp.getHeaders());
        System.out.println("Resp body length: " + resp.getBody().length);
    }
}
