package com.cruise.ship_proxy;

import com.cruise.ship_proxy.connection.PersistentTcpClient;
import com.cruise.ship_proxy.http.HttpRequestData;
import com.cruise.ship_proxy.http.HttpResponseData;
import com.cruise.ship_proxy.service.RequestExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
public class ShipProxyController {

    private final HttpClient httpClient;

    private final PersistentTcpClient persistentTcpClient;

    private final RequestExecutorService executorService;

    @Autowired
    public ShipProxyController(HttpClient httpClient, PersistentTcpClient persistentTcpClient, RequestExecutorService executorService) {
        this.httpClient = httpClient;
        this.persistentTcpClient = persistentTcpClient;
        this.executorService = executorService;
    }

//    @RequestMapping("/**")
//    public ResponseEntity<String> proxyRequest(HttpServletRequest request) {
//        try {
//            log.info("Started processing on thread: {}", Thread.currentThread().getName());
//            String targetUrl = extractTargetUrl(request);
//            log.info("Proxying request to : {}", targetUrl);
//
//            byte[] bodyContent = new byte[0];
//            if(request.getContentLength() > 0) {
//                try {
//                    if(request instanceof ContentCachingRequestWrapper) {
//                        bodyContent = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
//                    }
//                    else {
//                        InputStream bodyInputStream = request.getInputStream();
//                        bodyContent = StreamUtils.copyToByteArray(bodyInputStream);
//                    }
//                    log.info("Request body size: {} bytes", bodyContent.length);
//                }
//                catch (Exception ex) {
//                    log.warn("Could not read body from request: {}", ex.getMessage());
//                }
//            }
//
//            String method = request.getMethod();
//
//            if((method.equals("PUT") || method.equals("PATCH") || method.equals("POST")) && bodyContent.length == 0) {
//                String contentType = request.getHeader("Content-Type");
//                log.info("Logging parameter map to verify that form data exists...");
//
//                Map<String, String[]> parameterMap = request.getParameterMap();
//                log.info("Detected empty body for {} request. Parameter map: {}", method, parameterMap);
//
//                if(contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
//                    StringBuilder stringBuilder = new StringBuilder();
//                    for (Map.Entry<String, String[]> entry: parameterMap.entrySet()) {
//                        for (String value: entry.getValue()) {
//                            if(stringBuilder.length() > 0) {
//                                stringBuilder.append("&");
//                            }
//                            stringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
//                                    .append("=")
//                                    .append(URLEncoder.encode(value, "UTF-8"));
//                        }
//                    }
//
//                    String formData = stringBuilder.toString();
//
//                    if(!formData.isEmpty()) {
//                        bodyContent = formData.getBytes(StandardCharsets.UTF_8);
//                        log.info("Reconstructed for data from parameters : {}", formData);
//                    }
//                }
//                else {
//                    log.info("No form data reconstruction needed. Content-Type: {}", contentType);
//                }
//            }
//
//            HttpUriRequest proxyRequest;
//
//            // Test if the input stream can be read
//            try {
//                int available = request.getInputStream().available();
//                log.info("Available bytes in input stream before processing: {}", available);
//
//                // Try to read a small sample
//                byte[] sampleBuffer = new byte[Math.min(available, 10)];
//                int bytesRead = request.getInputStream().read(sampleBuffer);
//                log.info("Bytes read from input stream: {}", bytesRead);
//
//                if (bytesRead > 0) {
//                    log.info("Sample of input stream: {}", new String(sampleBuffer, 0, bytesRead));
//                }
//            } catch (Exception e) {
//                log.error("Error testing input stream: {}", e.getMessage());
//            }
//
//            log.info("Body content length: {}", bodyContent.length);
//            if (bodyContent.length > 0) {
//                log.info("Body content: {}", new String(bodyContent));
//            }
//
//            switch(method) {
//                case "GET":
//                    proxyRequest = new HttpGet(targetUrl);
//                    break;
//                case "POST":
//                    HttpPost postRequest = new HttpPost(targetUrl);
//                    if(bodyContent.length > 0) {
//                        postRequest.setEntity(new ByteArrayEntity(bodyContent));
//                    }
////                    if(request.getInputStream() != null) {
////                        postRequest.setEntity(new InputStreamEntity(request.getInputStream()));
////                    }
//                    proxyRequest = postRequest;
//                    break;
//                case "PUT":
//                    HttpPut putRequest = new HttpPut(targetUrl);
//                    if(bodyContent.length > 0) {
//                        putRequest.setEntity(new ByteArrayEntity(bodyContent));
//                    } else {
//                        // For testing - add hardcoded data for PUT to see if it works
//                        log.info("Adding test data for PUT request");
//                        String testData = "data=updated";
//                        putRequest.setEntity(new StringEntity(testData));
//                        putRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
//                    }
////                    if(request.getInputStream() != null) {
////                        putRequest.setEntity(new InputStreamEntity(request.getInputStream()));
////                    }
//                    proxyRequest = putRequest;
//                    break;
//                case "PATCH":
//                    HttpPatch patchRequest = new HttpPatch(targetUrl);
//                    if(bodyContent.length > 0) {
//                        patchRequest.setEntity(new ByteArrayEntity(bodyContent));
//                    }
//                    proxyRequest = patchRequest;
//                    break;
//                case "DELETE":
//                    proxyRequest = new HttpDelete(targetUrl);
//                    break;
//
//                default:
//                    return ResponseEntity.status(HttpServletResponse.SC_NOT_IMPLEMENTED).body("HTTP Method not Supported : "+method);
//            }
//
//            copyRequestHeaders(request, proxyRequest);
//
//            /*      Added to test the type of processing i.e., sequential or concurrent
//
//            long start = System.currentTimeMillis();
//            log.info("Direct outbound call starting on thread: {} at {}", Thread.currentThread().getName(), start);
//
//            HttpResponse proxyResponse = httpClient.execute(proxyRequest);// concurrent execution
//
//            long finish = System.currentTimeMillis();
//            log.info("Direct outbound call completed on thread: {} at {} (duration {} ms)", Thread.currentThread().getName(), finish, finish - start);
//
//            */
//
//            //Replacing with sequential execution using executor
//
//            String outboundMessage = method + " " + targetUrl;
//            log.info("Sending outbound message via persistent TCP client: {}", outboundMessage);
//            try {
//                persistentTcpClient.send(outboundMessage.getBytes(StandardCharsets.UTF_8));
//                // For testing, immediately read the echo response:
//                byte[] echoed = persistentTcpClient.receive();
//                String echoStr = new String(echoed, StandardCharsets.UTF_8);
//                log.info("Received echo from offshore proxy: {}", echoStr);
//                return ResponseEntity.ok("Request forwarded. Echo: " + echoStr);
//
//            }
//            catch (IOException e) {
//                log.error("Error sending message over persistent TCP connection", e);
//                return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
//                        .body("Error sending message over persistent TCP connection: " + e.getMessage());
//            }
//
//            // For testing, return a fixed response indicating that the message was sent.
////            return ResponseEntity.ok("Request forwarded to offshore proxy: " + outboundMessage);
//
//
//            /* Commenting this part to simply testing whether integration works over persistent tcp client
//
////            Future<HttpResponse> future = executorService.executeSequentially(() -> httpClient.execute(proxyRequest));
////
////            HttpResponse proxyResponse;
////
////            try {
////                proxyResponse = future.get(60, TimeUnit.SECONDS);
////            }
////            catch (InterruptedException ex) {
////                Thread.currentThread().interrupt();
////                return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
////                        .body("Request Processing Interrupted");
////            }
////            catch (ExecutionException ex) {
////                log.error("Error executing outbound request", ex.getCause());
////                return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
////                        .body("Proxy Error: "+ex.getCause().getMessage());
////            }
////            catch (TimeoutException ex) {
////                return ResponseEntity.status(HttpServletResponse.SC_GATEWAY_TIMEOUT)
////                        .body("Request times out after 60 seconds");
////            }
////
////            HttpEntity entity = proxyResponse.getEntity();
////            String responseBody = entity != null ? EntityUtils.toString(entity) : "";
////
////            return ResponseEntity.status(proxyResponse.getStatusLine().getStatusCode())
////                    .body(responseBody);
//
//*/
//
//        }
//        catch (IOException ex) {
//            log.error("Error Proxying Request", ex);
//            return ResponseEntity.status(500).body("Proxy Error : "+ex.getMessage());
//        }
//    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxyAll(HttpServletRequest request) {
        try {
            log.info("**HELLO** ShipProxyController v1.2.3 running on Java {}", System.getProperty("java.version"));
            // 1) Extract method, URL, headers, body
            String method = request.getMethod();
            String targetUrl = extractTargetUrl(request);

            byte[] bodyBytes;
            try (InputStream is = request.getInputStream()) {
                bodyBytes = StreamUtils.copyToByteArray(is);
            }

            String contentType = request.getContentType();
            if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)
                    && bodyBytes.length == 0) {
                StringBuilder sb = new StringBuilder();
                request.getParameterMap().forEach((k, vals) -> {
                    for (String v : vals) {
                        if (sb.length() > 0) sb.append("&");
                        try {
                            sb.append(URLEncoder.encode(k, "UTF-8"))
                                    .append("=")
                                    .append(URLEncoder.encode(v, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                bodyBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                log.info("Rebuilt form body: {}", sb);
            }


            // Collect headers
            HttpHeaders incoming = new HttpHeaders();
            Enumeration<String> names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Enumeration<String> vals = request.getHeaders(name);
                while (vals.hasMoreElements()) {
                    incoming.add(name, vals.nextElement());
                }
            }

            Map<String,String> headerMap = new LinkedHashMap<>();
            incoming.forEach((k, v) -> headerMap.put(k, String.join(",", v)));

            // 2) Build raw HTTP request text
            HttpRequestData reqData = new HttpRequestData(
                    method,
                    targetUrl,
                    headerMap,
                    bodyBytes
            );
            String rawRequest = reqData.serialize();
            log.info("Framing HTTP request:\n{}", rawRequest);

            // 3) Frame & send
            byte[] reqBytes = rawRequest.getBytes(StandardCharsets.UTF_8);
            persistentTcpClient.send(reqBytes);

            // 4) Read framed HTTP response
            byte[] respFrame = persistentTcpClient.readFrame();
            String rawResponse = new String(respFrame, StandardCharsets.UTF_8);
            log.info("Received framed HTTP response:\n{}", rawResponse);

            // 5) Parse HTTP response
            HttpResponseData respData = HttpResponseData.parse(rawResponse);

            // 6) Build and return ResponseEntity
            HttpHeaders out = new HttpHeaders();
            respData.getHeaders().forEach(out::add);
            return new ResponseEntity<>(
                    respData.getBody(),
                    out,
                    HttpStatus.valueOf(respData.getStatusCode())
            );
        } catch (Exception e) {
            log.error("Proxy error", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Proxy error: " + e.getMessage())
                            .getBytes(StandardCharsets.UTF_8));
        }
    }


    private void copyRequestHeaders(HttpServletRequest request, HttpUriRequest proxyRequest) {
        Enumeration<String> headerNames = request.getHeaderNames();

        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            if(headerName.equalsIgnoreCase("host") ||
            headerName.equalsIgnoreCase("connection") ||
            headerName.equalsIgnoreCase("content-length")) {
                continue;
            }

            String headerValue = request.getHeader(headerName);
            proxyRequest.setHeader(headerName, headerValue);
        }
    }

    private String extractTargetUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if(uri != null && uri.startsWith("http")) {
            String query = request.getQueryString();
            return query != null ? uri + "?" + query : uri;
        }

        String host = request.getHeader("Host");
        if(host != null) {
            String scheme = request.isSecure() ? "https" : "http";

            StringBuilder url = new StringBuilder();
            url.append(scheme).append("://").append(host);

            if(uri != null && !uri.equals("/")) {
                url.append(uri);
            }

            String query = request.getQueryString();
            if(query != null) {
                url.append("?").append(query);
            }
            return url.toString();
        }

        return "http://httpforever.com";
    }

}
