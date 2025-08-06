package org.example;


import org.example.enums.HttpContentType;
import org.example.enums.HttpMethod;
import org.example.exceptions.FailedToParse;

import java.util.Arrays;



public class HttpRequest extends Http {
    private HttpMethod method;
    private String target;
    private String userAgent;
    private String host;
    private String accept;

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
        System.out.println(Arrays.toString(split));
        System.out.println(split[split.length - 2]);
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
                System.out.println("A");
                this.contentLength = Integer.parseInt(splitHeader[1]);
                System.out.println("b");
            }
            if (splitHeader[0].equals("Content-Type")) {
                try {
                    System.out.println("here");
                    this.contentType = HttpContentType.valueOf(splitHeader[1]);
                } catch (IllegalArgumentException e) {
                    throw new FailedToParse("invalid Content-Type: " + splitHeader[1]);
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
        System.out.printf("method: %s\ntarget: %s\nuserAgent: %s\nhost: %s\naccept: %s\n", method, target, userAgent, host, accept);
    }


}
