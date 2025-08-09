package org.example;

import org.example.enums.HttpContentType;

public abstract class Http {
    protected String protocol;
    protected String body;
    protected HttpContentType contentType;
    protected int contentLength;


    // parse content type to enum
    protected HttpContentType contentTypeFromMimeType(String mimeType) {
        for (HttpContentType type : HttpContentType.values()) {
            if (type.toString().trim().equalsIgnoreCase(mimeType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown content type: " + mimeType);
    }
}
