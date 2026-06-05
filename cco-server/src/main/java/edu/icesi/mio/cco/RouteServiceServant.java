package edu.icesi.mio.cco;

import Mio.Route;
import Mio.RouteMapData;
import Mio.RouteService;

import com.zeroc.Ice.Current;

final class RouteServiceServant implements RouteService {
    private final CcoFacade facade;

    RouteServiceServant(CcoFacade facade) {
        this.facade = facade;
    }

    @Override
    public Route[] listRoutes(Current current) {
        return facade.listRoutes(current);
    }

    @Override
    public RouteMapData routeDetails(int lineId, Current current) {
        return facade.routeDetails(lineId, current);
    }
}
