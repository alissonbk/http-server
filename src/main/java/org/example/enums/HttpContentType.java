package org.example.enums;

public enum HttpContentType {
    TEXT {
        @Override
        public String toString() {
            return "text/plain";
        }
    },
    JSON {
        @Override
        public String toString() {
            return "application/json";
        }
    },
    XML {
        @Override
        public String toString() {
            return "application/xml";
        }
    },
    FILE {
        @Override
        public String toString() {
            return "application/octet-stream";
        }
    }
}


