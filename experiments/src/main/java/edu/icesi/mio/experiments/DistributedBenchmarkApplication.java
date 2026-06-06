package edu.icesi.mio.experiments;

import edu.icesi.mio.common.Env;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DistributedBenchmarkApplication {

    public static void main(String[] args) throws Exception {
        String baseUrl = Env.value("MIO_DISTRIBUTED_BASE_URL", "http://127.0.0.1:8080");

        PilotDatasetLoader loader = new PilotDatasetLoader();
        Map<RouteMonthKey, Map<Integer, List<Mio.Position>>> dataset = loader.load();

        List<RouteMonthKey> keys = new ArrayList<>(dataset.keySet());
        keys.sort(Comparator.comparing(RouteMonthKey::lineId).thenComparing(RouteMonthKey::month));

        Path outputDir = Path.of("experiments-output");
        Files.createDirectories(outputDir);
        Path csv = outputDir.resolve("distributed-results.csv");

        int successful = 0;
        long totalStart = System.nanoTime();

        try (BufferedWriter writer = Files.newBufferedWriter(csv)) {
            writer.write("line_id,month,speed_kmh,samples,query_time_ms");
            writer.newLine();

            for (RouteMonthKey key : keys) {
                long queryStart = System.nanoTime();
                MetricResponse response = queryMetric(baseUrl, key);
                long queryMs = (System.nanoTime() - queryStart) / 1_000_000;

                if (response != null) {
                    successful++;
                    writer.write(response.lineId + "," +
                            response.month + "," +
                            response.speedKmh + "," +
                            response.samples + "," +
                            queryMs);
                    writer.newLine();
                }
            }
        }

        long totalMs = (System.nanoTime() - totalStart) / 1_000_000;

        System.out.println("Version distribuida completada.");
        System.out.println("Consultas esperadas: " + keys.size());
        System.out.println("Consultas exitosas: " + successful);
        System.out.println("Tiempo total (ms): " + totalMs);
        System.out.println("Promedio por consulta (ms): " +
                (successful == 0 ? 0.0 : ((double) totalMs / successful)));
        System.out.println("Archivo generado: experiments-output/distributed-results.csv");
    }

    private static MetricResponse queryMetric(String baseUrl, RouteMonthKey key) throws IOException {
        String urlValue = baseUrl + "/api/metrics?lineId=" + key.lineId() + "&month=" + key.month();
        HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int status = connection.getResponseCode();
        if (status != 200) {
            return null;
        }

        try (InputStream input = connection.getInputStream()) {
            String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return parseMetric(body);
        } finally {
            connection.disconnect();
        }
    }

    private static MetricResponse parseMetric(String json) {
        int lineId = extractInt(json, "lineId");
        int month = extractInt(json, "month");
        double speedKmh = extractDouble(json, "speedKmh");
        long samples = extractLong(json, "samples");
        return new MetricResponse(lineId, month, speedKmh, samples);
    }

    private static int extractInt(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\":(-?\\d+)").matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static long extractLong(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\":(-?\\d+)").matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
    }

    private static double extractDouble(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\":(-?\\d+(?:\\.\\d+)?)").matcher(json);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0.0;
    }

    private static final class MetricResponse {
        private final int lineId;
        private final int month;
        private final double speedKmh;
        private final long samples;

        private MetricResponse(int lineId, int month, double speedKmh, long samples) {
            this.lineId = lineId;
            this.month = month;
            this.speedKmh = speedKmh;
            this.samples = samples;
        }
    }
}