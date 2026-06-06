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
        String datagramsFile = Env.value("MIO_DATAGRAMS_FILE", "/mnt/mio-datos/datagrams-MiniPilot.csv");
        String routesFile = Env.value("MIO_ROUTES_FILE", "/opt/mio/lines-241-ActiveGT.csv");
        String endpoints = Env.value("MIO_DATACENTER_ENDPOINTS", "tcp -h mio-datacenter -p 10001");

        try (Communicator communicator = IceSupport.communicator(args)) {
            ObjectAdapter adapter = IceSupport.adapter(communicator, "DataCenterAdapter", endpoints);
            adapter.add(new PersistentHistoricalRepository(datagramsFile), Util.stringToIdentity("HistoricalRepository"));
            adapter.add(new CsvOperationalRepository(routesFile), Util.stringToIdentity("OperationalRepository"));
            System.out.println("DataCenter listo en " + endpoints);
            communicator.waitForShutdown();
        }
    }
}
