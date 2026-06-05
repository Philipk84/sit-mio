package edu.icesi.mio.cco;

import Mio.AverageSpeed;
import Mio.HistoricalRepositoryPrx;
import Mio.Position;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
            return calculate(lineId, month);
        } catch (Exception ex) {
            System.err.println("Consulta de metricas no disponible: " + ex.getMessage());
            return new AverageSpeed(lineId, month, 0.0, 0);
        }
    }

    void stop() {
        workers.shutdownNow();
    }

    private AverageSpeed calculate(int lineId, int month) throws Exception {
        Map<Integer, List<Position>> byBus = Arrays.stream(
                        historicalRepository.positionsByRouteAndMonth(lineId, month))
                .collect(Collectors.groupingBy(position -> position.busId));
        List<Callable<MetricPartialResult>> tasks = byBus.entrySet().stream()
                .map(entry -> (Callable<MetricPartialResult>) () ->
                        new MetricsWorker().calculate(new MetricTask(entry.getKey(), entry.getValue())))
                .collect(Collectors.toList());

        double total = 0.0;
        long samples = 0;
        for (java.util.concurrent.Future<MetricPartialResult> future : workers.invokeAll(tasks, 2500, TimeUnit.MILLISECONDS)) {
            if (!future.isCancelled()) {
                MetricPartialResult partial = future.get();
                total += partial.totalSpeed();
                samples += partial.samples();
            }
        }
        return new AverageSpeed(lineId, month, samples == 0 ? 0.0 : total / samples, samples);
    }
}
