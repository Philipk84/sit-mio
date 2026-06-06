package edu.icesi.mio.datacenter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.icesi.mio.common.Paths;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

final class DatagramFileServer {
    private final Path datagramsFile;
    private final HttpServer server;

    DatagramFileServer(String datagramsFile, int port) {
        try {
            this.datagramsFile = Paths.projectFile(datagramsFile);
            this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            this.server.createContext("/datagrams-MiniPilot.csv", this::serveDatagrams);
            this.server.setExecutor(Executors.newFixedThreadPool(2));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo iniciar servidor HTTP de datagramas", ex);
        }
    }

    void start() {
        server.start();
        System.out.println("CSV de datagramas disponible en /datagrams-MiniPilot.csv desde " + datagramsFile);
    }

    void stop() {
        server.stop(0);
    }

    private void serveDatagrams(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        if (!Files.exists(datagramsFile)) {
            byte[] body = ("No existe archivo de datagramas: " + datagramsFile).getBytes();
            exchange.sendResponseHeaders(404, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "text/csv; charset=utf-8");
        exchange.sendResponseHeaders(200, Files.size(datagramsFile));
        try (OutputStream output = exchange.getResponseBody()) {
            Files.copy(datagramsFile, output);
        }
    }
}
