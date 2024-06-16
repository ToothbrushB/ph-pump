package com.calebli.phpump;

import javafx.collections.FXCollections;
import javafx.scene.chart.XYChart;

import java.util.LinkedList;

public enum TimeUnits {
    SECOND(1), MINUTE(1 / 60.0), HOUR(1 / 3600.0);
    private final double factor;
    private final XYChart.Series<Number, Number> series = new XYChart.Series<>(FXCollections.synchronizedObservableList(FXCollections.observableList(new LinkedList<>())));

    TimeUnits(double factor) {
        this.factor = factor;
        series.setName(this.name());
    }

    public XYChart.Series<Number, Number> getSeries() {
        return series;
    }

    public double getFactor() {
        return factor;
    }
}
