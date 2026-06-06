
package edu.icesi.mio.experiments.Monolitic;
import java.util.Map;

import edu.icesi.mio.experiments.PilotDatasetLoader;
import edu.icesi.mio.experiments.ResultsCsvWriter;
import edu.icesi.mio.experiments.RouteMonthKey;
import edu.icesi.mio.experiments.SpeedStats;



public final class MonolithicApplication {

    public static void main(String[] args) throws Exception {
        PilotDatasetLoader loader = new PilotDatasetLoader();
        MonolithicAverageSpeedCalculator calculator = new MonolithicAverageSpeedCalculator();
        ResultsCsvWriter writer = new ResultsCsvWriter();

        long start = System.nanoTime();
        Map<RouteMonthKey, Map<Integer, java.util.List<Mio.Position>>> dataset = loader.load();
        Map<RouteMonthKey, SpeedStats> results = calculator.calculate(dataset);
        long end = System.nanoTime();

        long elapsedMs = (end - start) / 1_000_000;

        writer.write(results, "monolithic-results.csv", "monolithic");

        System.out.println("Version monolitica completada.");
        System.out.println("Resultados: " + results.size());
        System.out.println("Tiempo total (ms): " + elapsedMs);
    }
}