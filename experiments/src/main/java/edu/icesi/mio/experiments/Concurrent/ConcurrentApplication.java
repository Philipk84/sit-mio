package edu.icesi.mio.experiments.Concurrent;

import java.util.Map;

import edu.icesi.mio.experiments.PilotDatasetLoader;
import edu.icesi.mio.experiments.ResultsCsvWriter;
import edu.icesi.mio.experiments.RouteMonthKey;
import edu.icesi.mio.experiments.SpeedStats;

public final class ConcurrentApplication {

    public static void main(String[] args) throws Exception {
        PilotDatasetLoader loader = new PilotDatasetLoader();
        ConcurrentAverageSpeedCalculator calculator = new ConcurrentAverageSpeedCalculator();
        ResultsCsvWriter writer = new ResultsCsvWriter();

        long start = System.nanoTime();
        Map<RouteMonthKey, Map<Integer, java.util.List<Mio.Position>>> dataset = loader.load();
        Map<RouteMonthKey, SpeedStats> results = calculator.calculate(dataset);
        long end = System.nanoTime();

        long elapsedMs = (end - start) / 1_000_000;

        writer.write(results, "concurrent-results.csv", "concurrent");

        System.out.println("Version concurrente completada.");
        System.out.println("Resultados: " + results.size());
        System.out.println("Tiempo total (ms): " + elapsedMs);
    }
}