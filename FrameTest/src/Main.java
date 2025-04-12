import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        // 1) Connect to offshore proxy
        SocketChannel ch = SocketChannel.open(new InetSocketAddress("offshore-proxy", 9999));

        // 2) Send one HTTP GET request frame
        String httpGet = ""
                + "GET http://httpbin.org/get HTTP/1.1\r\n"
                + "Host: httpbin.org\r\n"
                + "User-Agent: FrameTest/1.0\r\n"
                + "Accept: */*\r\n"
                + "\r\n";
        sendFrame(ch, httpGet);
        System.out.println("Sent HTTP GET request");

        // 3) Read framed HTTP response
        String rawResponse = readFrame(ch);
        System.out.println("Received framed HTTP response:\n" + rawResponse);

        // 4) Close connection
        ch.close();
    }

    // Sends one length‑prefixed frame
    static void sendFrame(SocketChannel ch, String msg) throws IOException {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + data.length);
        buf.putInt(data.length).put(data).flip();
        while (buf.hasRemaining()) {
            ch.write(buf);
        }
    }

    // Reads one length‑prefixed frame and returns its payload string
    static String readFrame(SocketChannel ch) throws IOException {
        // Read 4‑byte length
        ByteBuffer lenBuf = ByteBuffer.allocate(4);
        if (!readFully(ch, lenBuf)) {
            throw new EOFException("Stream closed during length read");
        }
        lenBuf.flip();
        int len = lenBuf.getInt();

        // Read payload
        ByteBuffer dataBuf = ByteBuffer.allocate(len);
        if (!readFully(ch, dataBuf)) {
            throw new EOFException("Stream closed during payload read");
        }
        dataBuf.flip();

        byte[] data = new byte[len];
        dataBuf.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    // Helper: fill buffer or return false if EOF
    static boolean readFully(SocketChannel ch, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int r = ch.read(buf);
            if (r < 0) return false;
        }
        return true;
    }
}
