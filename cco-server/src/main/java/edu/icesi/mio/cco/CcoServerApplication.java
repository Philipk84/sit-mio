package edu.icesi.mio.cco;

import Mio.HistoricalRepositoryPrx;
import Mio.OperationalRepositoryPrx;
import edu.icesi.mio.common.Env;
import edu.icesi.mio.common.IceSupport;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public final class CcoServerApplication {
    private CcoServerApplication() {
    }

    public static void main(String[] args) {
        String endpoints = Env.value("MIO_CCO_ENDPOINTS", "tcp -h 127.0.0.1 -p 10010");
        String historicalProxy = Env.value("MIO_HISTORICAL_PROXY",
                "HistoricalRepository:tcp -h 127.0.0.1 -p 10001");
        String operationalProxy = Env.value("MIO_OPERATIONAL_PROXY",
                "OperationalRepository:tcp -h 127.0.0.1 -p 10001");

        try (Communicator communicator = IceSupport.communicator(args)) {
            HistoricalRepositoryPrx historicalRepository = HistoricalRepositoryPrx.checkedCast(
                    communicator.stringToProxy(historicalProxy));
            OperationalRepositoryPrx operationalRepository = OperationalRepositoryPrx.checkedCast(
                    communicator.stringToProxy(operationalProxy));

            PositionRegistry positionRegistry = new PositionRegistry();
            MetricsMaster metricsMaster = new MetricsMaster(historicalRepository);
            DatagramQueue datagramQueue = new DatagramQueue(Env.intValue("MIO_DATAGRAM_QUEUE_CAPACITY", 100_000));
            ThreadPoolDispatcher dispatcher = new ThreadPoolDispatcher(Env.intValue("MIO_CCO_WORKERS", 4));
            ReliableMessagingAgent reliableMessagingAgent = new ReliableMessagingAgent();
            RouteResolver routeResolver = new RouteResolver(operationalRepository);
            ReceptorDatagramas receptorDatagramas = new ReceptorDatagramas(
                    reliableMessagingAgent, routeResolver, datagramQueue);
            DatagramQueueProcessor processor = new DatagramQueueProcessor(
                    datagramQueue, dispatcher, positionRegistry, historicalRepository);
            CcoFacade facade = new CcoFacade(positionRegistry, operationalRepository, metricsMaster, receptorDatagramas);
            processor.start();

            ObjectAdapter adapter = IceSupport.adapter(communicator, "CcoAdapter", endpoints);
            adapter.add(new DatagramReceiverServant(facade), Util.stringToIdentity("DatagramReceiver"));
            adapter.add(new PositionServiceServant(facade), Util.stringToIdentity("PositionService"));
            adapter.add(new RouteServiceServant(facade), Util.stringToIdentity("RouteService"));
            adapter.add(new MetricsServiceServant(facade), Util.stringToIdentity("MetricsService"));
            System.out.println("Servidor CCO listo en " + endpoints);
            communicator.waitForShutdown();
            processor.stop();
            metricsMaster.stop();
        }
    }
}
