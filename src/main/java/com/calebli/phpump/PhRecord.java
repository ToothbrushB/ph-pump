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

    public DoubleProperty timeProperty() {
        return time;
    }

    public void setTime(double time) {
        this.time.set(time);
    }

    public double getpH() {
        return pH.get();
    }

    public DoubleProperty pHProperty() {
        return pH;
    }

    public void setpH(double pH) {
        this.pH.set(pH);
    }

    public boolean equalsData(XYChart.Data<Number, Number> data) {
        return data.getXValue().equals(this.getTime()) && data.getYValue().equals(this.getpH());
    }

//    @Override
//    public String toString() {
//        return "PhRecord[" +
//                "time=" + time + ", " +
//                "pH=" + pH + ']';
//    }

}
