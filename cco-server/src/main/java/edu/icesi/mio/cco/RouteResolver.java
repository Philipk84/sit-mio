package edu.icesi.mio.cco;

import Mio.Datagram;
import Mio.OperationalRepositoryPrx;
import Mio.Route;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class RouteResolver {
    private final OperationalRepositoryPrx operationalRepository;
    private final Set<Integer> knownRoutes = ConcurrentHashMap.newKeySet();

    RouteResolver(OperationalRepositoryPrx operationalRepository) {
        this.operationalRepository = operationalRepository;
        refreshRoutes();
    }

    boolean resolveRoute(Datagram datagram) {
        if (datagram.lineId <= 0) {
            return false;
        }
        if (knownRoutes.isEmpty() || knownRoutes.contains(datagram.lineId)) {
            return true;
        }
        refreshRoutes();
        return knownRoutes.isEmpty() || knownRoutes.contains(datagram.lineId);
    }

    private void refreshRoutes() {
        try {
            knownRoutes.clear();
            Arrays.stream(operationalRepository.routes()).map(route -> route.lineId).forEach(knownRoutes::add);
        } catch (RuntimeException ex) {
            System.err.println("No se pudo refrescar relacion bus-ruta: " + ex.getMessage());
        }
    }
}
