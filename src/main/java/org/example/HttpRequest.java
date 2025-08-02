package org.example;


import org.example.enums.HttpMethod;
import org.example.exceptions.FailedToParse;


public class HttpRequest extends Http {
    private HttpMethod method;
    private String target;

    public HttpRequest() {}

    public void parseRequestLine(byte[] buf) {
        var rawContent = new String(buf);
        var firstLine = rawContent.split("\r\n", 2)[0];
        var spaceSeparated = firstLine.split(" ", 3);
        try {
            method = HttpMethod.valueOf(spaceSeparated[0]);
        } catch (IllegalArgumentException e) {
            throw new FailedToParse("invalid request line, unkown http method: " + spaceSeparated[0]);
        }

        target = spaceSeparated[1];
        super.protocol = spaceSeparated[2];
    }

    public void parseHeaders(byte[] buf) {

    }

    public void parseBody(byte[] buf) {

    }

    public void parseAll(byte[] buf) {

    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getTarget() {
        return target;
    }
}
