package com.escuelaing.edu.app;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpServer {

    private static final int THREAD_POOL_SIZE = 10;
    private static boolean running = false;
    private static ExecutorService threadPool;
    private static ServerSocket serverSocket;

    public static void start(int port) {
        running = true;
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor HTTP Concurrente iniciado en el puerto: " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (SocketException e) {
                    if (!running) {
                        System.out.println("ServerSocket cerrado debido a Graceful Shutdown.");
                    } else {
                        System.err.println("Error en la conexión del cliente: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor en el puerto " + port + ": " + e.getMessage());
        } finally {
            stopThreadPool();
        }

        System.out.println("Servidor detenido de manera concurrente (Graceful Shutdown completado).");
    }

    public static void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando ServerSocket: " + e.getMessage());
        }
    }

    private static void stopThreadPool() {
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream inStream = clientSocket.getInputStream();
             OutputStream outStream = new BufferedOutputStream(clientSocket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }


            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {

            }

            handleRequest(requestLine, outStream);

        } catch (IOException e) {
            System.err.println("Error procesando petición en hilo de trabajo: " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error cerrando socket del cliente: " + e.getMessage());
            }
        }
    }

    private static void handleRequest(String requestLine, OutputStream out) throws IOException {
        Request req = new Request(requestLine);
        Response resp = new Response();

        if (!req.getMethod().equals("GET")) {
            sendErrorResponse(out, "405 Method Not Allowed", "Método no soportado en esta versión.");
            return;
        }

        String path = req.getPath();


        if (Router.hasRoute(path)) {
            try {
                Route route = Router.getRoute(path);
                Object result = route.handle(req, resp);
                String body = result != null ? result.toString() : "";

                String contentType = resp.getContentType();
                if (body.startsWith("{") || body.startsWith("[")) {
                    contentType = "application/json; charset=UTF-8";
                }

                sendStringResponse(out, "200 OK", contentType, body);
            } catch (Exception e) {
                sendErrorResponse(out, "500 Internal Server Error", "Error ejecutando lambda: " + e.getMessage());
            }
            return;
        }


        byte[] fileData = StaticFileService.getFileAsBytes(path);

        if (fileData != null) {
            String contentType = StaticFileService.getContentType(path);
            sendOkBytesResponse(out, contentType, fileData);
            return;
        }


        sendErrorResponse(out, "404 Not Found", "Recurso no encontrado: " + path);
    }

    private static void sendStringResponse(OutputStream out, String status, String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private static void sendOkBytesResponse(OutputStream out, String contentType, byte[] body) throws IOException {
        String headers = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static void sendErrorResponse(OutputStream out, String status, String message) throws IOException {
        String htmlBody = "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + status + "</title></head>"
                + "<body><h1>" + status + "</h1><p>" + message + "</p></body></html>";
        byte[] bodyBytes = htmlBody.getBytes(StandardCharsets.UTF_8);

        String headers = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }
}