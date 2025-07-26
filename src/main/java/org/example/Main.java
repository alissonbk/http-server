package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;

// curl -v -X GET http://localhost:8080
// Request line
// GET                          // HTTP method
// index.html                  // Request target
// HTTP/1.1                     // HTTP version
//        \r\n                         // CRLF that marks the end of the request line

// Headers
//Host: localhost:4221\r\n     // Header that specifies the server's host and port
//User-Agent: curl/7.64.1\r\n  // Header that describes the client's user agent
//Accept: */*\r\n              // Header that specifies which media types the client can accept
//\r\n                         // CRLF that marks the end of the headers

// Request body (empty)
public class Main {
    static final int PORT = 8080;
    static final String HTTP200 =  "HTTP/1.1 200 OK\r\n\r\n";

    public static void main(String[] args) {
        try (var serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            var clientSocket = serverSocket.accept();
            System.out.println("new connection");
            var content = clientSocket.getInputStream();
            var req = readRequest(content.readAllBytes());
            clientSocket.getOutputStream().write(HTTP200.getBytes());
            clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static HttpRequest readRequest(byte[] buf) throws IOException {
        var req = new HttpRequest();
        req.rawContent = new String(buf);
        req.parseStartLine();
        System.out.printf("method: %s\ntarget: %s\nprotocol: %s\n", req.getMethod(), req.getTarget(), req.getProtocol());
        return req;
    }
}