package org.example;


import org.example.enums.HttpMethod;


public class HttpRequest extends Http {
    private HttpMethod method;
    private String target;

    public HttpRequest() {}

    public void parseRequestLine(byte[] buf) {
        var rawContent = new String(buf);
        var firstLine = rawContent.split("\r\n", 2)[0];
        var spaceSeparated = firstLine.split(" ", 3);
        method = HttpMethod.valueOf(spaceSeparated[0]);
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
