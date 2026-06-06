package edu.icesi.mio.experiments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ResultsCsvWriter {

    public void write(Map<RouteMonthKey, SpeedStats> results, String fileName, String implementation) throws IOException {
        Path outputDir = Path.of("experiments-output");
        Files.createDirectories(outputDir);

        Path file = outputDir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("line_id,month,average_speed,samples,implementation");
            writer.newLine();

            for (Map.Entry<RouteMonthKey, SpeedStats> entry : results.entrySet()) {
                RouteMonthKey key = entry.getKey();
                SpeedStats stats = entry.getValue();

                writer.write(key.lineId() + "," +
                        key.month() + "," +
                        stats.average() + "," +
                        stats.samples() + "," +
                        implementation);
                writer.newLine();
            }
        }
    }
}