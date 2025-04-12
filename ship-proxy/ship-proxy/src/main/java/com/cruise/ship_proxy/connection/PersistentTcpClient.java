package com.cruise.ship_proxy.connection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class PersistentTcpClient {

    @Value("${offshore.proxy.host:localhost}")
    private String offshoreProxyHost;

    @Value("${offshore.proxy.tcp.port:9999}")
    private int offshoreProxyPort;

    private SocketChannel socketChannel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        // Background thread that will keep checking and (re)connecting.
        executor.submit(this::connectLoop);
    }


    private void connectLoop() {
        while (running) {
            if (socketChannel == null || !socketChannel.isConnected()) {
                try {
                    log.info("Attempting to connect to offshore proxy at {}:{}", offshoreProxyHost, offshoreProxyPort);
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true); // Blocking mode for simplicity
                    socketChannel.connect(new InetSocketAddress(offshoreProxyHost, offshoreProxyPort));
                    log.info("Connected to offshore proxy");
                } catch (IOException e) {
                    log.error("Failed to connect to offshore proxy, retrying in 5 seconds", e);
                    sleep(5000);
                    continue;
                }
            }

            // Optional: read data from the offshore proxy for debugging.
//            try {
//                ByteBuffer buffer = ByteBuffer.allocate(1024);
//                int bytesRead = socketChannel.read(buffer);
//                if (bytesRead == -1) {
//                    log.warn("Connection closed by offshore proxy");
//                    closeConnection();
//                } else if (bytesRead > 0) {
//                    buffer.flip();
//                    byte[] data = new byte[buffer.remaining()];
//                    buffer.get(data);
//                    log.info("Received from offshore proxy: {}", new String(data));
//                }
//            } catch (IOException ex) {
//                log.error("Error reading from offshore proxy, closing connection", ex);
//                closeConnection();
//            }
        }
    }

    public synchronized void send(byte[] data) throws IOException {
//        if (socketChannel != null && socketChannel.isConnected()) {
//            ByteBuffer buffer = ByteBuffer.wrap(data);
//            socketChannel.write(buffer);
//        } else {
//            throw new IOException("Not connected to offshore proxy");
//        }

        if (socketChannel == null || !socketChannel.isConnected()) {
            throw new IOException("Not Connected to offshore proxy");
        }
        ByteBuffer buf = ByteBuffer.allocate(4 + data.length);
        buf.putInt(data.length);    // length prefix framing to separate btw requests
        buf.put(data);              // payload
        buf.flip();
        while (buf.hasRemaining()) {
            socketChannel.write(buf);
        }
        log.debug("Sent framed {}‑byte payload", data.length);
    }

    /**
     * Reads a single framed message.
     * It first reads 4 bytes to determine the length, then reads that many bytes.
     * Returns the payload as a byte array.
     */
    public synchronized byte[] receive() throws IOException {
        if (socketChannel == null || !socketChannel.isConnected()) {
            throw new IOException("Not connected to offshore proxy");
        }
        // Read 4-byte length prefix
        ByteBuffer lenBuf = ByteBuffer.allocate(4);
        if (!readFully(socketChannel, lenBuf)) {
            throw new EOFException("Stream closed during length read");
        }
        lenBuf.flip();
        int len = lenBuf.getInt();

        // Read the payload
        ByteBuffer payloadBuf = ByteBuffer.allocate(len);
        if (!readFully(socketChannel, payloadBuf)) {
            throw new EOFException("Stream closed during payload read");
        }
        payloadBuf.flip();
        byte[] payload = new byte[len];
        payloadBuf.get(payload);
        log.debug("Received framed {}-byte payload", len);
        return payload;
    }

    // Helper to read fully into the buffer.
    private boolean readFully(SocketChannel ch, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int r = ch.read(buf);
            if (r < 0) return false;
        }
        return true;
    }


    private void closeConnection() {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                log.error("Error closing socket channel", e);
            }
        }
        socketChannel = null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendFrame(byte[] payload) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(payload.length);
        header.flip();
        socketChannel.write(header);
        socketChannel.write(ByteBuffer.wrap(payload));
    }

    public byte[] readFrame() throws IOException {
        ByteBuffer lenBuf = ByteBuffer.allocate(4);
        readFully(socketChannel, lenBuf);
        lenBuf.flip();
        int len = lenBuf.getInt();
        ByteBuffer dataBuf = ByteBuffer.allocate(len);
        readFully(socketChannel, dataBuf);
        return dataBuf.array();
    }


    @PreDestroy
    public void shutdown() {
        running = false;
        closeConnection();
        executor.shutdown();
        log.info("Persistent TCP client stopped");
    }

}
