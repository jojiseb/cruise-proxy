package com.cruise.offshore_proxy.connection;

import com.cruise.offshore_proxy.http.HttpRequestData;
import com.cruise.offshore_proxy.http.HttpResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ConnectionAcceptor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${offshore.proxy.tcp.port:9999}")
    private int tcpPort;

    private ServerSocketChannel serverSocketChannel;

    private ExecutorService executorService;

    private final AtomicBoolean running = new AtomicBoolean(true);

    @PostConstruct
    public void start() {
        try {
            log.info("**HELLO** OffshoreProxy v1.2.3 on Java {}", System.getProperty("java.version"));
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(tcpPort));
            serverSocketChannel.configureBlocking(true);

            log.info("TCP server started on port {}", tcpPort);

            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(this::acceptConnections);
        } catch (IOException e) {
            log.error("Failed to start TCP server",e);
        }
    }

    private void acceptConnections() {
        log.info("Started accepting connections");

        while(running.get()) {
            try {
                log.info("Waiting for ship proxy to connect...");
                SocketChannel clientChannel = serverSocketChannel.accept();
                log.info("Connection accepted from : {}", clientChannel.getRemoteAddress());

                handleConnection(clientChannel);
            }
            catch (Exception ex) {
                if (running.get()) {
                    log.error("Error accepting connection", ex);
                }
            }
        }
    }

    /**
     * Handle a single client connection using length‑prefixed frames:
     *   [4‑byte big‑endian length][payload bytes]
     */
    private void handleConnection(SocketChannel clientChannel) {
        try {
            while (running.get() && clientChannel.isConnected()) {
                // 1) Read exactly 4 bytes for the length prefix
                ByteBuffer lenBuf = ByteBuffer.allocate(4);
                if (!readFully(clientChannel, lenBuf)) {
                    log.info("Client disconnected during length read");
                    break;
                }
                lenBuf.flip();
                int len = lenBuf.getInt();

                // 2) Read exactly the number of bytes of payload according to length prefix
                ByteBuffer dataBuf = ByteBuffer.allocate(len);
                if (!readFully(clientChannel, dataBuf)) {
                    log.info("Client disconnected during payload read");
                    break;
                }
                dataBuf.flip();
                byte[] data = new byte[len];
                dataBuf.get(data);

                String message = new String(data, StandardCharsets.UTF_8);
                log.info("Received {}‑byte message: {}", len, message);

                // 3) Parse HTTP request
                HttpRequestData reqData = HttpRequestData.parse(message);

                // 4) Execute via RestTemplate
                HttpMethod method = HttpMethod.valueOf(reqData.getMethod());
                HttpHeaders headers = new HttpHeaders();
                reqData.getHeaders().forEach(headers::add);
                HttpEntity<byte[]> entity = new HttpEntity<>(reqData.getBody(), headers);

                ResponseEntity<byte[]> respEntity;
                try {
                    respEntity = restTemplate.exchange(
                            reqData.getUrl(), method, entity, byte[].class);
                } catch (Exception e) {
                    log.error("Upstream HTTP error", e);
                    // Build a 502 response
                    respEntity = ResponseEntity
                            .status(HttpStatus.BAD_GATEWAY)
                            .body(("Proxy error: "+e.getMessage()).getBytes(StandardCharsets.UTF_8));
                }

                // 5) Build HttpResponseData
                Map<String,String> respHeaders = new LinkedHashMap<>();
                respEntity.getHeaders().forEach((k, vals) -> respHeaders.put(k, String.join(",", vals)));
                HttpResponseData respData = new HttpResponseData(
                        respEntity.getStatusCodeValue(),
                        respEntity.getStatusCode().getReasonPhrase(),
                        respHeaders,
                        respEntity.getBody() == null ? new byte[0] : respEntity.getBody()
                );

                String rawResponse = respData.serialize();
                byte[] respBytes = rawResponse.getBytes(StandardCharsets.UTF_8);
                log.info("Forwarding {}‑byte HTTP response", respBytes.length);

                // Build a 4‑byte length header
                ByteBuffer header = ByteBuffer.allocate(4);
                header.putInt(respBytes.length);
                header.flip();

// Send header, then payload
                clientChannel.write(header);
                clientChannel.write(ByteBuffer.wrap(respBytes));

                log.info("Sent framed {}‑byte HTTP response", respBytes.length);
            }
        } catch (IOException ex) {
            log.error("Error handling connection", ex);
        } finally {
            try {
                clientChannel.close();
            } catch (IOException ex) {
                log.error("Error closing client channel", ex);
            }
        }
    }

    /** Helper: read until buffer is full, or return false if EOF */
    private boolean readFully(SocketChannel ch, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int r = ch.read(buf);
            if (r < 0) return false;
        }
        return true;
    }

    @PreDestroy
    public void stop() {
        running.set(false);

        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        log.info("TCP server stopped");
    }


}
