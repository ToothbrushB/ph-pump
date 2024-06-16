package com.calebli.phpump;

import javafx.beans.property.Property;

public class BidirectionalTimeBinding extends BidirectionalBinding<Number, Number> {
    private final TimeUnits units;

    @Override
    protected Number convert(Number value) {
        return value.doubleValue() * units.getFactor();
    }

    @Override
    protected Number inverseConvert(Number value) {
        return value.doubleValue() / units.getFactor();
    }

    public BidirectionalTimeBinding(Property<Number> property1, Property<Number> property2, TimeUnits unit) {
        super(property1, property2);
        this.units = unit;
        property2.setValue(convert(property1.getValue()));
    }
}
