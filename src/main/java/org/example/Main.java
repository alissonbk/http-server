package org.example;

import org.example.exceptions.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class Main {
    static final int PORT = 8080;
    // Timeout that the client has to send content after a tcp connection is created
    static final int EMPTY_CONNECTION_TIMEOUT_SECONDS = 5;

    public static void main(String[] args) {
        var cpus = Runtime.getRuntime().availableProcessors();
        System.out.println("CPU cores available: " + cpus);
        try (var serverSocket = new ServerSocket(PORT); var executor = Executors.newFixedThreadPool(cpus)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                var clientSocket = serverSocket.accept();
                System.out.printf("Accepted connection from %s, thread pool size: %d, active threads: %d\n",
                        clientSocket.getInetAddress().getHostName(),
                        ((ThreadPoolExecutor) executor).getPoolSize(),
                        ((ThreadPoolExecutor) executor).getActiveCount());
                executor.submit(() -> {
                    handleNewConnection(clientSocket);
                });
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleNewConnection(Socket clientSocket) {
        try {
            waitForConnectionContent(clientSocket);
            var httpRequest = readRequest(clientSocket.getInputStream());
            var httpResponse = new HttpResponse(httpRequest);
            var response = httpResponse.parseAll();
            System.out.println("response: " + Arrays.toString(response));
            System.out.println("response: " + new String(response));
            clientSocket.getOutputStream().write(response);
            clientSocket.getOutputStream().flush();
            clientSocket.getOutputStream().close();
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void waitForConnectionContent(Socket s) throws IOException {
        var startTime = Date.from(Instant.now());
        var inTimeoutRange = Date.from(Instant.now().minusSeconds(EMPTY_CONNECTION_TIMEOUT_SECONDS)).before(startTime);
        while(s.getInputStream().available() <= 0 && inTimeoutRange) {
            // wait for content or timeout
        }
    }

    private static HttpRequest readRequest(InputStream is) throws IOException {
        byte[] buffer = new byte[is.available()];
        var ignore = is.read(buffer);
        var req = new HttpRequest();
        System.out.println("request buffer: " + new String(buffer));
        req.parseAll(buffer);
        req.printRequest();
        return req;
    }
}