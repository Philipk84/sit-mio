package edu.icesi.mio.common;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public final class IceSupport {
    private IceSupport() {
    }

    public static Communicator communicator(String[] args) {
        return Util.initialize(args);
    }

    public static ObjectAdapter adapter(Communicator communicator, String name, String endpoints) {
        ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(name, endpoints);
        adapter.activate();
        return adapter;
    }
}
