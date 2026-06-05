package edu.icesi.mio.cco;

import Mio.Datagram;
import Mio.DatagramReceiver;

import com.zeroc.Ice.Current;

final class DatagramReceiverServant implements DatagramReceiver {
    private final CcoFacade facade;

    DatagramReceiverServant(CcoFacade facade) {
        this.facade = facade;
    }

    @Override
    public boolean submit(Datagram datagram, Current current) {
        return facade.submit(datagram, current);
    }
}
