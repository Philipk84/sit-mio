package edu.icesi.mio.cco;

import Mio.AverageSpeed;
import Mio.Datagram;
import Mio.OperationalRepositoryPrx;
import Mio.Position;
import Mio.Route;

import com.zeroc.Ice.Current;

final class CcoFacade {
    private final PositionRegistry positionRegistry;
    private final OperationalRepositoryPrx operationalRepository;
    private final MetricsMaster metricsMaster;
    private final DatagramQueueProcessor datagramQueueProcessor;

    CcoFacade(PositionRegistry positionRegistry, OperationalRepositoryPrx operationalRepository,
              MetricsMaster metricsMaster, DatagramQueueProcessor datagramQueueProcessor) {
        this.positionRegistry = positionRegistry;
        this.operationalRepository = operationalRepository;
        this.metricsMaster = metricsMaster;
        this.datagramQueueProcessor = datagramQueueProcessor;
    }

    public boolean submit(Datagram datagram, Current current) {
        boolean accepted = datagramQueueProcessor.offer(datagram);
        ReliableMessagingAgent.ack(datagram, accepted);
        return accepted;
    }

    public Position[] latestPositions(int lineId, Current current) {
        return positionRegistry.latest(lineId);
    }

    public Route[] listRoutes(Current current) {
        return operationalRepository.routes();
    }

    public AverageSpeed averageSpeedByRouteAndMonth(int lineId, int month, Current current) {
        return metricsMaster.averageSpeedByRouteAndMonth(lineId, month);
    }
}
