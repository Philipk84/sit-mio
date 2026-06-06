package edu.icesi.mio.experiments.Monolitic;

import Mio.Position;
import edu.icesi.mio.common.Geo;
import edu.icesi.mio.experiments.RouteMonthKey;
import edu.icesi.mio.experiments.SpeedStats;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MonolithicAverageSpeedCalculator {

    public Map<RouteMonthKey, SpeedStats> calculate(Map<RouteMonthKey, Map<Integer, List<Position>>> dataset) {
        Map<RouteMonthKey, SpeedStats> results = new HashMap<>();

        for (Map.Entry<RouteMonthKey, Map<Integer, List<Position>>> routeMonthEntry : dataset.entrySet()) {
            SpeedStats routeMonthStats = new SpeedStats();

            for (Map.Entry<Integer, List<Position>> busEntry : routeMonthEntry.getValue().entrySet()) {
                List<Position> positions = busEntry.getValue();
                positions.sort(Comparator.comparing(position -> position.timestamp));

                for (int i = 1; i < positions.size(); i++) {
                    Position previous = positions.get(i - 1);
                    Position current = positions.get(i);

                    double speed = Geo.speedKmh(previous, current);
                    if (speed > 0.0 && speed <= 120.0) {
                        routeMonthStats.add(speed);
                    }
                }
            }

            results.put(routeMonthEntry.getKey(), routeMonthStats);
        }

        return results;
    }
}