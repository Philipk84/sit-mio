package edu.icesi.mio.cco;

import Mio.AverageSpeed;
import Mio.HistoricalRepositoryPrx;
import Mio.Position;
import edu.icesi.mio.common.Geo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final class MetricsMaster {
    private final HistoricalRepositoryPrx historicalRepository;
    private final ExecutorService workers;

    MetricsMaster(HistoricalRepositoryPrx historicalRepository) {
        this.historicalRepository = historicalRepository;
        this.workers = Executors.newFixedThreadPool(4);
    }

    AverageSpeed averageSpeedByRouteAndMonth(int lineId, int month) {
        try {
            return workers.submit(() -> calculate(lineId, month)).get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            System.err.println("Consulta de metricas no disponible: " + ex.getMessage());
            return new AverageSpeed(lineId, month, 0.0, 0);
        }
    }

    void stop() {
        workers.shutdownNow();
    }

    private AverageSpeed calculate(int lineId, int month) {
        Map<Integer, java.util.List<Position>> byBus = Arrays.stream(
                        historicalRepository.positionsByRouteAndMonth(lineId, month))
                .collect(Collectors.groupingBy(position -> position.busId));
        double total = 0.0;
        long samples = 0;
        for (java.util.List<Position> positions : byBus.values()) {
            positions.sort(Comparator.comparing(position -> position.timestamp));
            for (int i = 1; i < positions.size(); i++) {
                double speed = Geo.speedKmh(positions.get(i - 1), positions.get(i));
                if (speed > 0.0 && speed <= 120.0) {
                    total += speed;
                    samples++;
                }
            }
        }
        return new AverageSpeed(lineId, month, samples == 0 ? 0.0 : total / samples, samples);
    }
}
