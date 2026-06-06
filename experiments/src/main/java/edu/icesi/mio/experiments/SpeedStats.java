package edu.icesi.mio.experiments;

public final class SpeedStats {
    private double totalSpeed;
    private long samples;

    public void add(double speed) {
        totalSpeed += speed;
        samples++;
    }

    public void merge(SpeedStats other) {
        totalSpeed += other.totalSpeed;
        samples += other.samples;
    }

    public double totalSpeed() {
        return totalSpeed;
    }

    public long samples() {
        return samples;
    }

    public double average() {
        return samples == 0 ? 0.0 : totalSpeed / samples;
    }
}