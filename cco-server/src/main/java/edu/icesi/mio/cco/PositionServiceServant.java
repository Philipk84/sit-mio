package edu.icesi.mio.cco;

import Mio.Position;
import Mio.PositionService;

import com.zeroc.Ice.Current;

final class PositionServiceServant implements PositionService {
    private final CcoFacade facade;

    PositionServiceServant(CcoFacade facade) {
        this.facade = facade;
    }

    @Override
    public Position[] latestPositions(int lineId, Current current) {
        return facade.latestPositions(lineId, current);
    }
}
