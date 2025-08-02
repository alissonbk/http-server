package org.example;

import org.example.enums.HttpContentType;
import org.example.exceptions.NotFoundException;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class HttpResponse extends Http {
    private int statusCode;
    private HttpContentType contentType;
    private int contentLength;
    private final HttpRequest request;

    public HttpResponse(HttpRequest request) {
        this.request = request;
    }

    public byte[] parseRequestLine() {
        if (statusCode < 100 || statusCode > 599) {
            statusCode = 500;
        }
        return ("HTTP/1.1 " + statusCode + " " + this.getStatusMessage() + "\r\n").getBytes();
    }

    public byte[] parseHeaders() {
        return (
                parseContentType() +
                parseContentLength() +
                "\r\n"
        ).getBytes();
    }

    /**
     * can change some parameters (statusCode, contentType...) depending on the request.
     */
    public byte[] parseBody() {
        try {
            final var body = getBodyFromPath();
            statusCode = 200;
            contentType = HttpContentType.TEXT;
            contentLength = body.length;
            return body;
        } catch (NotFoundException e) {
            statusCode = 404;
            System.out.println("not found");
        }

        return new byte[]{};
    }

    public byte[] parseAll() {
        var body = parseBody();
        System.out.println(Arrays.toString(body));

        var headers = parseHeaders();
        System.out.println(Arrays.toString(headers));

        var requestLine = parseRequestLine();
        System.out.println(Arrays.toString(requestLine));

        byte[] finalBuffer = new byte[requestLine.length + body.length + headers.length];
        System.arraycopy(requestLine, 0, finalBuffer, 0, requestLine.length);
        System.arraycopy(headers, 0, finalBuffer, requestLine.length, headers.length);
        System.arraycopy(body, 0, finalBuffer, requestLine.length + headers.length , body.length);
        return finalBuffer;
    }

    private String getStatusMessage()  {
        return switch (statusCode) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }

    // changes contentType
    private byte[] getBodyFromPath() throws NotFoundException {
        var path = request.getTarget();
        if (path.startsWith("/echo")) {
            this.contentType = HttpContentType.TEXT;
            var str = path.split("/echo", 2)[1];
            return str.getBytes();
        }

        if (path.startsWith("/user-agent")) {
            this.contentType = HttpContentType.TEXT;
            return this.request.getUserAgent().getBytes();
        }

        throw new NotFoundException("could not find this path");
    }

    private String parseContentType() {
        if (contentType == null) { return ""; }
        return "Content-Type: " + this.contentType + "\r\n";
    }

    private String parseContentLength() {
        return "Content-Length: " + this.contentLength + "\r\n";
    }
}
