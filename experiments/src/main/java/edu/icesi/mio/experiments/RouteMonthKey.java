package edu.icesi.mio.experiments;

import java.util.Objects;

public final class RouteMonthKey {
    private final int lineId;
    private final int month;

    public RouteMonthKey(int lineId, int month) {
        this.lineId = lineId;
        this.month = month;
    }

    public int lineId() {
        return lineId;
    }

    public int month() {
        return month;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteMonthKey)) return false;
        RouteMonthKey that = (RouteMonthKey) o;
        return lineId == that.lineId && month == that.month;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineId, month);
    }

    @Override
    public String toString() {
        return "RouteMonthKey{" +
                "lineId=" + lineId +
                ", month=" + month +
                '}';
    }
}