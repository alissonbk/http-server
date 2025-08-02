package org.example;

import org.example.exceptions.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Arrays;

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

    public static void main(String[] args) {
        try (var serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                var clientSocket = serverSocket.accept();
                var httpRequest = readRequest(clientSocket.getInputStream());
                var httpResponse = new HttpResponse(httpRequest);
                var response = httpResponse.parseAll();
                System.out.println("response: " + Arrays.toString(response));
                System.out.println("response: " + new String(response));
                clientSocket.getOutputStream().write(response);
                clientSocket.getOutputStream().flush();
                clientSocket.getOutputStream().close();
                clientSocket.close();
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static HttpRequest readRequest(InputStream is) throws IOException {
        byte[] buffer = new byte[is.available()];
        var ignore = is.read(buffer);
        var req = new HttpRequest();
        req.parseRequestLine(buffer);
        System.out.printf("method: %s\ntarget: %s\nprotocol: %s\n", req.getMethod(), req.getTarget(), req.getProtocol());
        return req;
    }
}