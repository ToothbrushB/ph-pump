package com.calebli.phpump;

import javafx.scene.chart.XYChart;

public enum TimeUnits {
    SECOND(1), MINUTE(1 / 60.0), HOUR(1 / 3600.0);
    private final double factor;
    private XYChart.Series<Number, Number> series = new XYChart.Series<>();

    public XYChart.Series<Number, Number> getSeries() {
        return series;
    }

    public double getFactor() {
        return factor;
    }

    TimeUnits(double factor) {
        this.factor = factor;
        series.setName(this.name());
    }
}
