package org.example;

import org.example.exceptions.ConnectionTimeout;

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
    static String FILE_PATH_DIR = "";

    public static void main(String[] args) {
        readDirectoryPath(args);
        var cpus = Runtime.getRuntime().availableProcessors();
        System.out.println("files path: " + FILE_PATH_DIR);
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

    private static void readDirectoryPath(String[] args) {
        System.out.println(Arrays.toString(args));
        if (args.length > 1 && args[0].equals("--directory")) {
            var path = args[1];
            if (!path.isEmpty()) {
                FILE_PATH_DIR = path;
            }
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
        } catch (ConnectionTimeout e) {
            try {
                var response = new HttpResponse(null).buildRequestTimeout();
                clientSocket.getOutputStream().write(response);
                clientSocket.getOutputStream().flush();
                clientSocket.getOutputStream().close();
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    private static void waitForConnectionContent(Socket s) throws IOException {
        var startTime = Date.from(Instant.now());
        while(s.getInputStream().available() <= 0 && Date.from(Instant.now().minusSeconds(EMPTY_CONNECTION_TIMEOUT_SECONDS)).before(startTime)) {
            // wait for content or timeout
        }
        if (s.getInputStream().available() <= 0) {
            throw new ConnectionTimeout(
                "the client spent more than " + EMPTY_CONNECTION_TIMEOUT_SECONDS + " seconds without sending any content"
            );
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