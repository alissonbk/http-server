package org.example;

import org.example.enums.HttpContentType;

public abstract class Http {
    protected String protocol;
    protected String body;
    protected HttpContentType contentType;
    protected int contentLength;
}
