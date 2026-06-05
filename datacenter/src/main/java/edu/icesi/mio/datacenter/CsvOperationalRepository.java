package edu.icesi.mio.datacenter;

import Mio.OperationalRepository;
import Mio.Route;
import Mio.RouteMapData;
import Mio.RoutePoint;
import Mio.Station;
import edu.icesi.mio.common.CsvRouteParser;
import edu.icesi.mio.common.Paths;

import com.zeroc.Ice.Current;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

final class CsvOperationalRepository implements OperationalRepository {
    private final Path routesFile;

    CsvOperationalRepository(String routesFile) {
        this.routesFile = Paths.projectFile(routesFile);
    }

    @Override
    public Route[] routes(Current current) {
        return loadRoutes();
    }

    @Override
    public RouteMapData routeDetails(int lineId, Current current) {
        Route route = java.util.Arrays.stream(loadRoutes())
                .filter(candidate -> candidate.lineId == lineId)
                .findFirst()
                .orElse(new Route(lineId, String.valueOf(lineId), "Ruta " + lineId));
        return new RouteMapData(route, new Station[0], new RoutePoint[0]);
    }

    private Route[] loadRoutes() {
        try {
            List<Route> routes = Files.lines(routesFile)
                    .map(CsvRouteParser::parse)
                    .filter(Optional -> Optional.isPresent())
                    .map(Optional -> Optional.get())
                    .collect(Collectors.toList());
            return routes.toArray(new Route[0]);
        } catch (IOException ex) {
            System.err.println("No se pudo leer rutas " + routesFile + ": " + ex.getMessage());
            return new Route[0];
        }
    }
}
