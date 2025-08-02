package org.example.exceptions;

public class FailedToParse extends RuntimeException {
    public FailedToParse(String message) {
        super(message);
    }
}
