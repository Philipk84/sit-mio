package edu.icesi.mio.cco;

final class MetricPartialResult {
    private final int busId;
    private final double totalSpeed;
    private final long samples;

    MetricPartialResult(int busId, double totalSpeed, long samples) {
        this.busId = busId;
        this.totalSpeed = totalSpeed;
        this.samples = samples;
    }

    int busId() {
        return busId;
    }

    double totalSpeed() {
        return totalSpeed;
    }

    long samples() {
        return samples;
    }
}
