package edu.icesi.mio.experiments;

import Mio.Datagram;
import Mio.Position;
import Mio.Route;
import edu.icesi.mio.common.CsvDatagramParser;
import edu.icesi.mio.common.CsvRouteParser;
import edu.icesi.mio.common.Env;
import edu.icesi.mio.common.Paths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class PilotDatasetLoader {

    public Map<RouteMonthKey, Map<Integer, List<Position>>> load() throws IOException {
        Set<Integer> activeRoutes = loadActiveRouteIds();
        return loadGroupedPositions(activeRoutes);
    }

    private Set<Integer> loadActiveRouteIds() throws IOException {
        String routesFile = Env.value("MIO_EXPERIMENT_ROUTES_FILE", "lines-241-ActiveGT.csv");
        Path path = Paths.projectFile(routesFile);

        Set<Integer> activeRoutes = new HashSet<>();
        try (Stream<String> lines = Files.lines(path)) {
            lines.map(CsvRouteParser::parse)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(route -> route.lineId)
                    .forEach(activeRoutes::add);
        }
        return activeRoutes;
    }

    private Map<RouteMonthKey, Map<Integer, List<Position>>> loadGroupedPositions(Set<Integer> activeRoutes) throws IOException {
        String datagramsFile = Env.value("MIO_EXPERIMENT_DATAGRAMS_FILE", "chunck.csv");
        Path path = Paths.projectFile(datagramsFile);

        Map<RouteMonthKey, Map<Integer, List<Position>>> grouped = new HashMap<>();

        try (Stream<String> lines = Files.lines(path)) {
            lines.map(CsvDatagramParser::parse)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(datagram -> activeRoutes.contains(datagram.lineId))
                    .forEach(datagram -> addDatagram(grouped, datagram));
        }

        return grouped;
    }

    private void addDatagram(Map<RouteMonthKey, Map<Integer, List<Position>>> grouped, Datagram datagram) {
        int month = CsvDatagramParser.month(datagram);
        if (month < 1 || month > 12) {
            return;
        }

        RouteMonthKey key = new RouteMonthKey(datagram.lineId, month);
        Position position = CsvDatagramParser.toPosition(datagram);

        grouped.computeIfAbsent(key, ignored -> new HashMap<>())
               .computeIfAbsent(datagram.busId, ignored -> new ArrayList<>())
               .add(position);
    }
}