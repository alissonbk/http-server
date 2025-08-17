package org.example;

import org.example.enums.HttpConnection;
import org.example.enums.HttpContentType;
import org.example.enums.HttpEncoding;
import org.example.enums.HttpMethod;
import org.example.exceptions.FailedToParseFile;
import org.example.exceptions.NotFoundException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import static org.example.Main.FILE_PATH_DIR;

public class HttpResponse extends Http {
    private int statusCode;
    private HttpConnection connection;
    private final HttpRequest request;
    private HttpEncoding contentEncoding;

    public HttpResponse(HttpRequest request) {
        this.request = request;
    }

    public byte[] parseResponseLine() {
        if (statusCode < 100 || statusCode > 599) {
            System.out.println("Not handled HTTP status code: " + statusCode);
            statusCode = 500;
        }
        return ("HTTP/1.1 " + statusCode + " " + this.getStatusMessage() + "\r\n").getBytes();
    }

    public byte[] parseHeaders() {
        return (
                parseContentType() +
                parseContentLength() +
                parseConnection() +
                parseContentEncoding() +
                "\r\n"
        ).getBytes();
    }

    /**
     * can change some parameters (statusCode, contentType...) depending on the request.
     */
    public byte[] parseBody() {
        try {
            final var body = compressIfSupported(getBodyFromPath());
            if (body == null) {
                return new byte[]{};
            }

            this.contentLength = body.length;
            return body;
        } catch (NotFoundException e) {
            statusCode = 404;
            System.out.println("not found");
        } catch (IOException e) {
            System.out.println("IOException " + e);
            throw new RuntimeException(e);
        }

        return new byte[]{};
    }

    public byte[] parseAll() {
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
            this.statusCode = 200;
            var str = path.split("/echo/", 2)[1];
            return str.trim().getBytes();
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
                if (!request.contentType.equals(HttpContentType.FILE)) {
                    this.statusCode = 415;
                    return null;
                }
                try {
                    saveFile(request.body.getBytes());
                    this.statusCode = 201;
                    return null;
                } catch (Exception e) {
                    System.out.println("failed to saveFile: " + e.getMessage());
                    this.statusCode = 500;
                    return null;
                }

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

    private String parseContentEncoding() {
        if (contentEncoding == null) { return ""; }
        return "Content-Encoding: " + this.contentEncoding + "\r\n";
    }

    // Saves file to { FILE_PATH_DIR }
    private void saveFile(byte[] buf) {
        if (buf.length != request.contentLength) {
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

    private byte[] compressIfSupported(byte[] body) throws IOException {
        if (request.getAcceptEncoding() == null || body == null) {
            return body;
        }

        switch (request.getAcceptEncoding()) {
            case gzip -> {
                this.contentEncoding = HttpEncoding.gzip;
                return gzip(body);
            }

            case compression -> {
                this.contentEncoding = HttpEncoding.compression;
                // TODO COMPRESS LZW
                throw new RuntimeException("LZW compression not implemented yet");
            }

            case deflate -> {
                this.contentEncoding = HttpEncoding.deflate;
                // TODO COMPRESS with deflate
                throw new RuntimeException("deflate compression not implemented yet");
            }
        }

        throw new RuntimeException("could not find the appropriated encoding");
    }

    private byte[] gzip(byte[] body) throws IOException {
        InputStream is = new ByteArrayInputStream(body);
        var os = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = new GZIPOutputStream(os);
        byte[] buf = new byte[body.length];

        int bytesRead;
        while((bytesRead = is.read(buf)) > -1) {
            gzipOut.write(buf, 0, bytesRead);
        }

        gzipOut.close();
        os.close();
        return os.toByteArray();
    }
}
