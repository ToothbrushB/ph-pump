package com.calebli.phpump;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.Objects;

public final class PhRecord {
    private final DoubleProperty time = new SimpleDoubleProperty();
    private final DoubleProperty ph = new SimpleDoubleProperty();

    public PhRecord(double time, double pH) {
        this.time.set(time);
        this.ph.set(pH);
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

    public double getph() {
        return ph.get();
    }

    public DoubleProperty phProperty() {
        return ph;
    }

    public void setph(double ph) {
        this.ph.set(ph);
    }

//    @Override
//    public String toString() {
//        return "PhRecord[" +
//                "time=" + time + ", " +
//                "pH=" + pH + ']';
//    }

}
