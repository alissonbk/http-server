package org.example.exceptions;

public class FailedToParseFile extends RuntimeException {
    public FailedToParseFile(String message) {
        super(message);
    }
}
