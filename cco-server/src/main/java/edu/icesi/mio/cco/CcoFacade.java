package edu.icesi.mio.cco;

import Mio.AverageSpeed;
import Mio.Datagram;
import Mio.OperationalRepositoryPrx;
import Mio.Position;
import Mio.Route;
import Mio.RouteMapData;

import com.zeroc.Ice.Current;

final class CcoFacade {
    private final PositionRegistry positionRegistry;
    private final OperationalRepositoryPrx operationalRepository;
    private final MetricsMaster metricsMaster;
    private final ReceptorDatagramas receptorDatagramas;

    CcoFacade(PositionRegistry positionRegistry, OperationalRepositoryPrx operationalRepository,
              MetricsMaster metricsMaster, ReceptorDatagramas receptorDatagramas) {
        this.positionRegistry = positionRegistry;
        this.operationalRepository = operationalRepository;
        this.metricsMaster = metricsMaster;
        this.receptorDatagramas = receptorDatagramas;
    }

    public boolean submit(Datagram datagram, Current current) {
        return receptorDatagramas.receive(datagram);
    }

    public Position[] latestPositions(int lineId, Current current) {
        return positionRegistry.latest(lineId);
    }

    public Route[] listRoutes(Current current) {
        return operationalRepository.routes();
    }

    public RouteMapData routeDetails(int lineId, Current current) {
        RouteMapData repositoryDetails = operationalRepository.routeDetails(lineId);
        return positionRegistry.routeDetails(repositoryDetails);
    }

    public AverageSpeed averageSpeedByRouteAndMonth(int lineId, int month, Current current) {
        return metricsMaster.averageSpeedByRouteAndMonth(lineId, month);
    }
}
