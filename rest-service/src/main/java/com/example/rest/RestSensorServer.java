package com.example.rest;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.h2.tools.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class RestSensorServer {

    private static final Gson GSON = new Gson();
    private static final int PORT = Integer.getInteger("rest.port", 8082);

    public static void main(String[] args) throws Exception {
        try {
            DatabaseHelper.ensureTable();
            System.out.println("H2 table ready.");
        } catch (Exception e) {
            System.err.println("WARNING: DB connection issue – " + e.getMessage());
        }

        
        try {
            Server.createWebServer("-webPort", "8083").start();
            System.out.println("H2 Console: http://localhost:8083  (JDBC URL: " + DatabaseHelper.getJdbcUrl() + ", user: sa, no password)");
        } catch (Exception e) {
            System.err.println("WARNING: Could not start H2 console – " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/sensors", new SensorHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println("  REST Sensor Service is running");
        System.out.println("  POST http://localhost:" + PORT + "/api/sensors");
        System.out.println("  GET  http://localhost:" + PORT + "/api/sensors/{deviceId}");
        System.out.println("=================================================");
    }

    static class SensorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            try {
                // Handle CORS preflight
                if ("OPTIONS".equalsIgnoreCase(method)) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(method) && path.equals("/api/sensors")) {
                    handlePost(exchange);
                } else if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/sensors")) {
                    String deviceId = path.substring("/api/sensors".length());
                    if (deviceId.startsWith("/")) {
                        deviceId = deviceId.substring(1);
                    }
                    handleGet(exchange, deviceId);
                } else {
                    send(exchange, 404, "{\"error\":\"Not found\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private void handlePost(HttpExchange exchange) throws Exception {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            SensorData data = GSON.fromJson(body, SensorData.class);

            System.out.println("REST POST received: " + body);
            if (data == null || data.getDeviceId() == null || data.getDeviceId().isBlank()) {
                send(exchange, 400, "{\"error\":\"deviceId is required\"}");
                return;
            }
            DatabaseHelper.save(data, "REST");

            String response = "{\"status\":\"OK\",\"deviceId\":\"" + data.getDeviceId() + "\"}";
            send(exchange, 201, response);
        }

        private void handleGet(HttpExchange exchange, String deviceId) throws Exception {
            // No device id  ->  latest readings of all devices; otherwise that device only
            List<SensorData> list = deviceId.isBlank()
                    ? DatabaseHelper.findLatest()
                    : DatabaseHelper.findByDevice(deviceId);
            System.out.println("REST GET " + (deviceId.isBlank() ? "(all devices)" : deviceId)
                    + " -> " + list.size() + " rows read from H2");
            String json = GSON.toJson(list);
            send(exchange, 200, json);
        }

        private void send(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
