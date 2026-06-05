package edu.icesi.mio.cco;

import Mio.AverageSpeed;
import Mio.MetricsService;

import com.zeroc.Ice.Current;

final class MetricsServiceServant implements MetricsService {
    private final CcoFacade facade;

    MetricsServiceServant(CcoFacade facade) {
        this.facade = facade;
    }

    @Override
    public AverageSpeed averageSpeedByRouteAndMonth(int lineId, int month, Current current) {
        return facade.averageSpeedByRouteAndMonth(lineId, month, current);
    }
}
