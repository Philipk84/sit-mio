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
    private final Set<Integer> warnedUnknownRoutes = ConcurrentHashMap.newKeySet();

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
        if (!knownRoutes.isEmpty() && !knownRoutes.contains(datagram.lineId)
                && warnedUnknownRoutes.add(datagram.lineId)) {
            System.err.println("Ruta " + datagram.lineId
                    + " no esta en el catalogo operativo; se acepta por venir en datagrama activo.");
        }
        return true;
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
