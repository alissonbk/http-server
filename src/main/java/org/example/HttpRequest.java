package org.example;


import org.example.enums.HttpConnection;
import org.example.enums.HttpContentType;
import org.example.enums.HttpEncoding;
import org.example.enums.HttpMethod;
import org.example.exceptions.FailedToParse;

import java.util.Arrays;



public class HttpRequest extends Http {
    private HttpMethod method;
    private String target;
    private String userAgent;
    private String host;
    private String accept;
    private HttpEncoding acceptEncoding;

    public HttpRequest() {
    }

    // Will set method, target and protocol
    private void parseRequestLine(String requestLine) {
        var spaceSeparated = requestLine.split(" ", 3);
        try {
            method = HttpMethod.valueOf(spaceSeparated[0].trim());
        } catch (IllegalArgumentException e) {
            throw new FailedToParse("invalid request line, unkown http method: " + spaceSeparated[0]);
        }

        target = spaceSeparated[1];
        super.protocol = spaceSeparated[2];
    }

    private void parseHeaders(String[] split) {
        if (split.length < 3) {
            var reduce = Arrays.stream(split).reduce((a, b) -> a + "\r\n" + b);
            if (reduce.isPresent()) {
                throw new FailedToParse("invalid request: " + reduce.get());
            }
            throw new FailedToParse("invalid request!");
        }
        for (var i = 1; i < split.length - 1; i++) {
            var str = split[i];
            if (str.trim().isEmpty()) { continue; }
            var splitHeader = str.split(": ", 2);

            if (splitHeader[0].equals("User-Agent")) {
                this.userAgent = splitHeader[1];
            }
            if (splitHeader[0].equals("Host")) {
                this.host = splitHeader[1];
            }
            if (splitHeader[0].equals("Accept")) {
                this.accept = splitHeader[1];
            }
            if (splitHeader[0].equals("Content-Length")) {
                this.contentLength = Integer.parseInt(splitHeader[1]);
            }
            if (splitHeader[0].equals("Connection")) {
                this.connection = HttpConnection.valueOf(splitHeader[1].replaceAll("-", "_").toUpperCase());
            }
            if (splitHeader[0].equals("Content-Type")) {
                try {
                    this.contentType = super.contentTypeFromMimeType(splitHeader[1]);
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    throw new FailedToParse("invalid Content-Type: " + splitHeader[1]);
                }

            }
            if (splitHeader[0].equals("Accept-Encoding")) {
                try {
                    final var encodings = splitHeader[1].split(",");
                    // FIXME: find a better way to define which encoding to use
                    this.acceptEncoding = HttpEncoding.valueOf(encodings[0]);
                } catch (IllegalArgumentException e) {
                    System.out.println("The request encoding is not supported: " + e.getMessage());
                }
            }
        }
    }

    private void parseBody(String body) {
        super.body = body;
    }

    // FIXME: should split the byte[] without transforming to string for better performance (specially if the body is a file...)
    public void parseAll(byte[] buf) {
        var rawContent = new String(buf);
        var split = rawContent.split("\r\n", -1);
        parseRequestLine(split[0]);
        parseHeaders(split);
        parseBody(split[split.length - 1]);
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getTarget() {
        return target;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void printRequest() {
        System.out.printf("printed request: \n\tmethod: %s\n\ttarget: %s\n\tuserAgent: %s\n\thost: %s\n\taccept: %s\n", method, target, userAgent, host, accept);
    }


    public HttpEncoding getAcceptEncoding() {
        return acceptEncoding;
    }
}
