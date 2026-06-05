package edu.icesi.mio.cco;

import Mio.Position;
import edu.icesi.mio.common.Geo;

import java.util.Comparator;

final class MetricsWorker {
    MetricPartialResult calculate(MetricTask task) {
        task.positions().sort(Comparator.comparing(position -> position.timestamp));
        double total = 0.0;
        long samples = 0;
        for (int i = 1; i < task.positions().size(); i++) {
            Position previous = task.positions().get(i - 1);
            Position current = task.positions().get(i);
            double speed = Geo.speedKmh(previous, current);
            if (speed > 0.0 && speed <= 120.0) {
                total += speed;
                samples++;
            }
        }
        return new MetricPartialResult(task.busId(), total, samples);
    }
}
