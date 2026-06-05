package edu.icesi.mio.cco;

import Mio.Datagram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class ReliableMessagingAgent {
    private final Map<String, String> trace = new ConcurrentHashMap<>();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    boolean receive(Datagram datagram) {
        boolean valid = datagram != null && datagram.busId > 0 && datagram.lineId > 0
                && datagram.latitude > 0 && datagram.longitude < 0;
        trace.put(key(datagram), valid ? "received" : "invalid");
        return valid;
    }

    boolean acknowledge(Datagram datagram, boolean accepted) {
        trace.put(key(datagram), accepted ? "accepted" : "rejected");
        if (accepted) {
            this.accepted.incrementAndGet();
        } else {
            this.rejected.incrementAndGet();
        }
        if (!accepted) {
            System.err.println("Datagrama rechazado por cola llena para bus " + datagram.busId);
        }
        return accepted;
    }

    void markRejected(Datagram datagram, String reason) {
        trace.put(key(datagram), "rejected:" + reason);
        System.err.println("Datagrama rechazado bus=" + (datagram == null ? "null" : datagram.busId)
                + " reason=" + reason);
    }

    long acceptedCount() {
        return accepted.get();
    }

    long rejectedCount() {
        return rejected.get();
    }

    private String key(Datagram datagram) {
        if (datagram == null) {
            return "null";
        }
        return datagram.busId + ":" + datagram.lineId + ":" + datagram.datagramDate;
    }
}
