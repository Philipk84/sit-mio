package edu.icesi.mio.gateway;

import Mio.AverageSpeed;
import Mio.MetricsServicePrx;
import Mio.Position;
import Mio.PositionServicePrx;
import Mio.Route;
import Mio.RouteMapData;
import Mio.RoutePoint;
import Mio.RouteServicePrx;
import Mio.Station;
import edu.icesi.mio.common.Env;
import edu.icesi.mio.common.IceSupport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zeroc.Ice.Communicator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class WebGatewayApplication {
    private WebGatewayApplication() {
    }

    public static void main(String[] args) throws Exception {
        String ccoEndpoint = Env.value("MIO_CCO_PROXY_ENDPOINT", "tcp -h 127.0.0.1 -p 10010");
        int port = Env.intValue("MIO_GATEWAY_PORT", 8080);

        Communicator communicator = IceSupport.communicator(args);
        RouteServicePrx routeService = RouteServicePrx.checkedCast(
                communicator.stringToProxy("RouteService:" + ccoEndpoint));
        PositionServicePrx positionService = PositionServicePrx.checkedCast(
                communicator.stringToProxy("PositionService:" + ccoEndpoint));
        MetricsServicePrx metricsService = MetricsServicePrx.checkedCast(
                communicator.stringToProxy("MetricsService:" + ccoEndpoint));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/routes", exchange -> respond(exchange, routesJson(routeService.listRoutes())));
        server.createContext("/api/route-details", exchange -> {
            int lineId = intQuery(exchange.getRequestURI(), "lineId", 0);
            respond(exchange, routeDetailsJson(routeService.routeDetails(lineId)));
        });
        server.createContext("/api/positions", exchange -> {
            int lineId = intQuery(exchange.getRequestURI(), "lineId", 0);
            respond(exchange, positionsJson(positionService.latestPositions(lineId)));
        });
        server.createContext("/api/metrics", exchange -> {
            int lineId = intQuery(exchange.getRequestURI(), "lineId", 0);
            int month = intQuery(exchange.getRequestURI(), "month", 5);
            AverageSpeed average = metricsService.averageSpeedByRouteAndMonth(lineId, month);
            respond(exchange, metricJson(average));
        });
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            communicator.close();
        }));
        System.out.println("Web gateway listo en http://127.0.0.1:" + port);
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static int intQuery(URI uri, String name, int fallback) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getRawQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        try {
            return Integer.parseInt(params.getOrDefault(name, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String routesJson(Route[] routes) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < routes.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"lineId\":").append(routes[i].lineId)
                    .append(",\"shortName\":\"").append(escape(routes[i].shortName))
                    .append("\",\"description\":\"").append(escape(routes[i].description)).append("\"}");
        }
        return json.append(']').toString();
    }

    private static String routeDetailsJson(RouteMapData details) {
        StringBuilder json = new StringBuilder("{\"route\":");
        json.append(routeJson(details.route));
        json.append(",\"stations\":[");
        for (int i = 0; i < details.stations.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            Station station = details.stations[i];
            json.append("{\"id\":").append(station.id)
                    .append(",\"lineId\":").append(station.lineId)
                    .append(",\"name\":\"").append(escape(station.name))
                    .append("\",\"latitude\":").append(station.latitude)
                    .append(",\"longitude\":").append(station.longitude)
                    .append(",\"kind\":\"").append(escape(station.kind)).append("\"}");
        }
        json.append("],\"baseGeometry\":[");
        for (int i = 0; i < details.baseGeometry.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            RoutePoint point = details.baseGeometry[i];
            json.append("{\"lineId\":").append(point.lineId)
                    .append(",\"latitude\":").append(point.latitude)
                    .append(",\"longitude\":").append(point.longitude)
                    .append(",\"order\":").append(point.order).append("}");
        }
        return json.append("]}").toString();
    }

    private static String routeJson(Route route) {
        return "{\"lineId\":" + route.lineId
                + ",\"shortName\":\"" + escape(route.shortName)
                + "\",\"description\":\"" + escape(route.description) + "\"}";
    }

    private static String positionsJson(Position[] positions) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < positions.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            Position position = positions[i];
            json.append("{\"busId\":").append(position.busId)
                    .append(",\"lineId\":").append(position.lineId)
                    .append(",\"stopId\":").append(position.stopId)
                    .append(",\"odometer\":").append(position.odometer)
                    .append(",\"latitude\":").append(position.latitude)
                    .append(",\"longitude\":").append(position.longitude)
                    .append(",\"speedKmh\":").append(position.speedKmh)
                    .append(",\"timestamp\":\"").append(escape(position.timestamp)).append("\"}");
        }
        return json.append(']').toString();
    }

    private static String metricJson(AverageSpeed average) {
        return "{\"lineId\":" + average.lineId
                + ",\"month\":" + average.month
                + ",\"speedKmh\":" + average.speedKmh
                + ",\"samples\":" + average.samples + "}";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
