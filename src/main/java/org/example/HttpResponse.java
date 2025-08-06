package org.example;

import org.example.enums.HttpConnection;
import org.example.enums.HttpContentType;
import org.example.enums.HttpMethod;
import org.example.exceptions.FailedToParseFile;
import org.example.exceptions.NotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.example.Main.FILE_PATH_DIR;

public class HttpResponse extends Http {
    private int statusCode;
    private HttpConnection connection;
    private final HttpRequest request;

    public HttpResponse(HttpRequest request) {
        this.request = request;
    }

    public byte[] parseResponseLine() {
        if (statusCode < 100 || statusCode > 599) {
            statusCode = 500;
        }
        return ("HTTP/1.1 " + statusCode + " " + this.getStatusMessage() + "\r\n").getBytes();
    }

    public byte[] parseHeaders() {
        return (
                parseContentType() +
                parseContentLength() +
                parseConnection() +
                "\r\n"
        ).getBytes();
    }

    /**
     * can change some parameters (statusCode, contentType...) depending on the request.
     */
    public byte[] parseBody() {
        try {
            final var body = getBodyFromPath();
            if (body == null) {
                return new byte[]{};
            }

            this.contentLength = body.length;
            return body;
        } catch (NotFoundException e) {
            statusCode = 404;
            System.out.println("not found");
        }

        return new byte[]{};
    }

    public byte[] parseAll() {
        System.out.println("parse all");
        var body = parseBody();
        var headers = parseHeaders();
        var responseLine = parseResponseLine();

        byte[] finalBuffer = new byte[responseLine.length + body.length + headers.length];
        System.arraycopy(responseLine, 0, finalBuffer, 0, responseLine.length);
        System.arraycopy(headers, 0, finalBuffer, responseLine.length, headers.length);
        System.arraycopy(body, 0, finalBuffer, responseLine.length + headers.length , body.length);
        return finalBuffer;
    }

    // Creates a response for request timeout
    public byte[] buildRequestTimeout() {
        this.connection = HttpConnection.CLOSE;
        this.statusCode = 408;
        var headers = parseHeaders();
        var responseLine = parseResponseLine();

        byte[] finalBuffer = new byte[responseLine.length + headers.length];
        System.arraycopy(responseLine, 0, finalBuffer, 0, responseLine.length);
        System.arraycopy(headers, 0, finalBuffer, responseLine.length, headers.length);
        return finalBuffer;
    }

    private String getStatusMessage()  {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "CREATED";
            case 202 -> "ACCEPTED";
            case 204 -> "NO_CONTENT";
            case 205 -> "MOVED_PERMANENTLY";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 410 -> "Not Acceptable";
            case 415 -> "Unsupported Media Type";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }

    // changes contentType
    private byte[] getBodyFromPath() throws NotFoundException {
        var path = request.getTarget();
        if (path.equals("/") || path.isEmpty()) {
            this.statusCode = 200;
            return null;
        }
        if (path.startsWith("/echo")) {
            this.contentType = HttpContentType.TEXT;
            var str = path.split("/echo", 2)[1];
            return str.getBytes();
        }

        if (path.startsWith("/user-agent")) {
            this.contentType = HttpContentType.TEXT;
            return this.request.getUserAgent().getBytes();
        }

        if (path.startsWith("/files")) {
            if (request.getMethod().equals(HttpMethod.GET)) {
                this.contentType = HttpContentType.FILE;
                var str = path.split("/files", 2)[1];
                try {
                    return Files.readAllBytes(Paths.get(FILE_PATH_DIR + "/" + str));
                } catch (IOException e) {
                    System.out.println("failed to read file: " + e.getMessage());
                    throw new NotFoundException("file not found");
                }
            }
            if (request.getMethod().equals(HttpMethod.POST)) {
                if (!contentType.equals(HttpContentType.FILE)) {
                    this.statusCode = 415;
                    return null;
                }
                saveFile(request.body.getBytes());
                this.statusCode = 201;
                return null;
            }
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

    private String parseConnection() {
        if (connection == null) { return ""; }
        return "Connection: " + this.connection + "\r\n";
    }

    // Saves file to { FILE_PATH_DIR }
    private void saveFile(byte[] buf) {
        System.out.println("saving file to: " + request.getTarget());
        if (buf.length != this.contentLength) {
            throw new FailedToParseFile(
                    String.format("the content length doesn't match with file length. " +
                                    "body length: %d, contentLength: %d\n",
                            buf.length, this.contentLength)
            );
        }
        var fileName = request.getTarget().split("/files", 2)[1];
        try {
            Path file = Paths.get(FILE_PATH_DIR + "/" + fileName);
            Files.write(file, buf);
        } catch (IOException e) {
            System.out.println("failed to write file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
