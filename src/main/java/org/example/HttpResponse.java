package org.example;

import org.example.enums.HttpContentType;
import org.example.exceptions.NotFoundException;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class HttpResponse extends Http {
    private int statusCode;
    private HttpContentType contentType;
    private final HttpRequest request;

    public HttpResponse(HttpRequest request) {
        this.request = request;
    }

    public ByteBuffer parseRequestLine(ByteBuffer buf) {
        byte[] requestLine = ("HTTP/1.1 " + statusCode + " " + this.getStatusMessage() + "\r\n\r\n").getBytes();
        return prependBytes(requestLine, buf.array());
    }

    public void parseHeaders(ByteBuffer buf) {

    }

    /**
     * can change some parameters (statusCode, contentType...) depending on the request.
     */
    public ByteBuffer parseBody(ByteBuffer buf) {
        try {
            var content = getBodyFromPath();
            System.out.println("content: " + Arrays.toString(content));
            // FIXME: this dont increase the buffer size dynamically
            buf.put(content);
            return buf;
        } catch (NotFoundException e) {
            statusCode = 404;
            System.out.println("not found");
        }

        return buf;
    }

    public byte[] parseAll() {
        var byteBuffer = ByteBuffer.allocate(0);
        System.out.println(Arrays.toString(byteBuffer.array()));
        parseHeaders(byteBuffer);
        System.out.println(Arrays.toString(byteBuffer.array()));
        byteBuffer = parseBody(byteBuffer);
        System.out.println(Arrays.toString(byteBuffer.array()));
        // parse later as status code can change case the status code has changed
        var finalBuffer = parseRequestLine(byteBuffer);
        System.out.println("finalBuffer:" + Arrays.toString(finalBuffer.array()));
        return finalBuffer.array();
    }

    private String getStatusMessage()  {
        return switch (statusCode) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            default -> "";
        };
    }

    // changes contentType
    private byte[] getBodyFromPath() throws NotFoundException {
        var path = request.getTarget();
        if (path.startsWith("/echo/")) {
            this.contentType = HttpContentType.TEXT;
            var str = path.split("/echo/", 2)[1];
            return str.getBytes();
        }

        throw new NotFoundException("could not find this path");
    }

    private ByteBuffer prependBytes(byte[] prefix, byte[] original) {
        ByteBuffer buffer = ByteBuffer.allocate(prefix.length + original.length);
        buffer.put(prefix);
        buffer.put(original);
        buffer.flip();
        return buffer;
    }
}
