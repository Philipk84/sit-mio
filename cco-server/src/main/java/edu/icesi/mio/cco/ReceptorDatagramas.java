package edu.icesi.mio.cco;

import Mio.Datagram;

final class ReceptorDatagramas {
    private final ReliableMessagingAgent reliableMessagingAgent;
    private final RouteResolver routeResolver;
    private final DatagramQueue datagramQueue;

    ReceptorDatagramas(ReliableMessagingAgent reliableMessagingAgent, RouteResolver routeResolver,
                       DatagramQueue datagramQueue) {
        this.reliableMessagingAgent = reliableMessagingAgent;
        this.routeResolver = routeResolver;
        this.datagramQueue = datagramQueue;
    }

    boolean receive(Datagram datagram) {
        if (!reliableMessagingAgent.receive(datagram)) {
            reliableMessagingAgent.markRejected(datagram, "datagrama invalido");
            return reliableMessagingAgent.acknowledge(datagram, false);
        }
        if (!routeResolver.resolveRoute(datagram)) {
            reliableMessagingAgent.markRejected(datagram, "ruta no resuelta");
            return reliableMessagingAgent.acknowledge(datagram, false);
        }
        boolean accepted = datagramQueue.enqueue(datagram);
        if (!accepted) {
            reliableMessagingAgent.markRejected(datagram, "cola llena");
        }
        return reliableMessagingAgent.acknowledge(datagram, accepted);
    }
}
