package org.example;

public interface HttpParser {
    void parseStartLine();
    void parseHeaders();
    void parseBody();
    void parseAll();
}
