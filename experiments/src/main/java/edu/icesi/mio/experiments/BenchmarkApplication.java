package edu.icesi.mio.experiments;

import java.util.Map;

import edu.icesi.mio.experiments.Concurrent.ConcurrentAverageSpeedCalculator;
import edu.icesi.mio.experiments.Monolitic.MonolithicAverageSpeedCalculator;

public final class BenchmarkApplication {

    public static void main(String[] args) throws Exception {
        PilotDatasetLoader loader = new PilotDatasetLoader();
        Map<RouteMonthKey, Map<Integer, java.util.List<Mio.Position>>> dataset = loader.load();

        MonolithicAverageSpeedCalculator mono = new MonolithicAverageSpeedCalculator();
        ConcurrentAverageSpeedCalculator concurrent = new ConcurrentAverageSpeedCalculator();

        long monoStart = System.nanoTime();
        Map<RouteMonthKey, SpeedStats> monoResults = mono.calculate(dataset);
        long monoEnd = System.nanoTime();

        long concStart = System.nanoTime();
        Map<RouteMonthKey, SpeedStats> concResults = concurrent.calculate(dataset);
        long concEnd = System.nanoTime();

        long monoMs = (monoEnd - monoStart) / 1_000_000;
        long concMs = (concEnd - concStart) / 1_000_000;

        System.out.println("=== Benchmark ===");
        System.out.println("Monolitica (ms): " + monoMs);
        System.out.println("Concurrente (ms): " + concMs);
        System.out.println("Resultados monolitica: " + monoResults.size());
        System.out.println("Resultados concurrente: " + concResults.size());

        ResultsCsvWriter writer = new ResultsCsvWriter();
        writer.write(monoResults, "monolithic-results.csv", "monolithic");
        writer.write(concResults, "concurrent-results.csv", "concurrent");
    }
}