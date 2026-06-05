package edu.icesi.mio.cco;

import Mio.Position;

import java.util.List;

final class MetricTask {
    private final int busId;
    private final List<Position> positions;

    MetricTask(int busId, List<Position> positions) {
        this.busId = busId;
        this.positions = positions;
    }

    int busId() {
        return busId;
    }

    List<Position> positions() {
        return positions;
    }
}
