package com.calebli.phpump;

public enum TimeUnits {
    SECOND(1), MINUTE(1 / 60.0), HOUR(1 / 3600.0);
    private final double factor;

    public double getFactor() {
        return factor;
    }

    TimeUnits(double factor) {
        this.factor = factor;
    }
}
