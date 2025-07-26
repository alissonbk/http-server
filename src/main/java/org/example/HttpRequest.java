package org.example;


import org.example.enums.HttpMethod;

import java.net.http.HttpHeaders;
import java.util.Arrays;

public class HttpRequest extends Http implements HttpParser {
    private HttpMethod method;
    private String target;

    public HttpRequest() {}

    @Override
    public void parseStartLine() {
        var firstLine = super.rawContent.split("\r\n", 2)[0];
        var spaceSeparated = firstLine.split(" ", 3);
        method = HttpMethod.valueOf(spaceSeparated[0]);
        target = spaceSeparated[1];
        super.protocol = spaceSeparated[2];
    }

    @Override
    public void parseHeaders() {

    }

    @Override
    public void parseBody() {

    }

    @Override
    public void parseAll() {

    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getTarget() {
        return target;
    }
}
