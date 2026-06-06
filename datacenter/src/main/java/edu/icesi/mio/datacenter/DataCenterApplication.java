package edu.icesi.mio.datacenter;

import Mio.HistoricalRepository;
import Mio.OperationalRepository;
import edu.icesi.mio.common.Env;
import edu.icesi.mio.common.IceSupport;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public final class DataCenterApplication {
    private DataCenterApplication() {
    }

    public static void main(String[] args) {
        String datagramsFile = Env.value("MIO_DATAGRAMS_FILE",
                "/opt/swarch/datacenter/datagrams-MiniPilot.csv");
        String routesFile = Env.value("MIO_ROUTES_FILE",
                "/opt/swarch/datacenter/lines-241-ActiveGT.csv");
        String endpoints = Env.value("MIO_DATACENTER_ENDPOINTS", "tcp -h 0.0.0.0 -p 10001");
        int datagramsHttpPort = Env.intValue("MIO_DATAGRAMS_HTTP_PORT", 10002);

        try (Communicator communicator = IceSupport.communicator(args)) {
            ObjectAdapter adapter = IceSupport.adapter(communicator, "DataCenterAdapter", endpoints);
            adapter.add(new PersistentHistoricalRepository(datagramsFile), Util.stringToIdentity("HistoricalRepository"));
            adapter.add(new CsvOperationalRepository(routesFile), Util.stringToIdentity("OperationalRepository"));
            DatagramFileServer datagramFileServer = new DatagramFileServer(datagramsFile, datagramsHttpPort);
            datagramFileServer.start();
            System.out.println("DataCenter listo en " + endpoints);
            communicator.waitForShutdown();
            datagramFileServer.stop();
        }
    }
}
