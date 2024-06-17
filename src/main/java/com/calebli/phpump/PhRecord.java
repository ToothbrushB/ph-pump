package com.calebli.phpump;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.chart.XYChart;


public final class PhRecord {
    private final DoubleProperty time = new SimpleDoubleProperty();
    private final DoubleProperty pH = new SimpleDoubleProperty();

    public PhRecord(double time, double pH) {
        this.time.set(time);
        this.pH.set(pH);
    }

    public double getTime() {
        return time.get();
    }

    public void setTime(double time) {
        this.time.set(time);
    }

    public DoubleProperty timeProperty() {
        return time;
    }

    public double getpH() {
        return pH.get();
    }

    public void setpH(double pH) {
        this.pH.set(pH);
    }

    public DoubleProperty pHProperty() {
        return pH;
    }

    public boolean equalsData(XYChart.Data<Number, Number> data, TimeUnits units) {
        return Math.abs(data.getXValue().doubleValue() - this.getTime() * units.getFactor()) < 1.0E-20 &&
            Math.abs(data.getYValue().doubleValue() - this.getpH()) < 1.0E-20;
    }

//    @Override
//    public String toString() {
//        return "PhRecord[" +
//                "time=" + time + ", " +
//                "pH=" + pH + ']';
//    }

}
