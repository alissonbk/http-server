package org.example.exceptions;

public class ConnectionTimeout extends RuntimeException {
    public ConnectionTimeout(String message) {
        super(message);
    }
}
