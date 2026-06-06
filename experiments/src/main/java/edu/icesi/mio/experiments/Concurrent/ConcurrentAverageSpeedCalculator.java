package edu.icesi.mio.experiments.Concurrent;

import Mio.Position;
import edu.icesi.mio.common.Env;
import edu.icesi.mio.common.Geo;
import edu.icesi.mio.experiments.RouteMonthKey;
import edu.icesi.mio.experiments.SpeedStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public final class ConcurrentAverageSpeedCalculator {

    public Map<RouteMonthKey, SpeedStats> calculate(Map<RouteMonthKey, Map<Integer, List<Position>>> dataset) throws Exception {
        int workers = Env.intValue("MIO_EXPERIMENT_WORKERS", 4);
        ExecutorService pool = Executors.newFixedThreadPool(workers);

        try {
            Map<RouteMonthKey, SpeedStats> results = new HashMap<>();

            for (Map.Entry<RouteMonthKey, Map<Integer, List<Position>>> routeMonthEntry : dataset.entrySet()) {
                List<Callable<SpeedStats>> tasks = new ArrayList<>();

                for (Map.Entry<Integer, List<Position>> busEntry : routeMonthEntry.getValue().entrySet()) {
                    tasks.add(() -> calculateBus(busEntry.getValue()));
                }

                SpeedStats combined = new SpeedStats();
                List<Future<SpeedStats>> futures = pool.invokeAll(tasks);

                for (Future<SpeedStats> future : futures) {
                    combined.merge(future.get());
                }

                results.put(routeMonthEntry.getKey(), combined);
            }

            return results;
        } finally {
            pool.shutdown();
        }
    }

    private SpeedStats calculateBus(List<Position> positions) {
        positions.sort(Comparator.comparing(position -> position.timestamp));
        SpeedStats stats = new SpeedStats();

        for (int i = 1; i < positions.size(); i++) {
            Position previous = positions.get(i - 1);
            Position current = positions.get(i);

            double speed = Geo.speedKmh(previous, current);
            if (speed > 0.0 && speed <= 120.0) {
                stats.add(speed);
            }
        }

        return stats;
    }
}