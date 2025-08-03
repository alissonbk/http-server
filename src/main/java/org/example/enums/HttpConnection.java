package org.example.enums;

public enum HttpConnection {
    CLOSE {
        @Override
        public String toString() {
            return "close";
        }
    },
    KEEP_ALIVE {
        @Override
        public String toString() {
            return "keep-alive";
        }
    }
}
