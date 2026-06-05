package edu.icesi.mio.cco;

import Mio.Datagram;

final class ReliableMessagingAgent {
    private ReliableMessagingAgent() {
    }

    static void ack(Datagram datagram, boolean accepted) {
        if (!accepted) {
            System.err.println("Datagrama rechazado por cola llena para bus " + datagram.busId);
        } else if (datagram.busId <= 0) {
            System.err.println("Datagrama recibido sin bus valido");
        }
    }
}
