package com.calebli.phpump;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;

public abstract class BidirectionalBinding<S, T> {

    protected final Property<S> property1;
    protected final Property<T> property2;
    private final InvalidationListener listener;
    protected boolean calculating = false;

    protected BidirectionalBinding(Property<S> property1, Property<T> property2) {
        if (property2.isBound() || property1 == null) {
            throw new IllegalArgumentException();
        }

        this.property1 = property1;
        this.property2 = property2;

        listener = o -> {
            if (!calculating) {
                calculating = true;
                try {
                    if (o == property1) {
                        T value = convert(property1.getValue());
                        property2.setValue(value);
                    } else {
                        S value = inverseConvert(property2.getValue());
                        property1.setValue(value);
                    }
                } catch (Exception ex) {
                    dispose();
                }
                calculating = false;
            }
        };

        property1.addListener(listener);
        property2.addListener(listener);
    }

    /**
     * Convert value for property 1 to value for property 2
     *
     * @param value value with type of property 1
     * @return converted value with type of property 2
     */
    protected abstract T convert(S value);

    /**
     * Convert value for property 2 to value for property 1
     *
     * @param value value with type of property 2
     * @return converted value with type of property 1
     */
    protected abstract S inverseConvert(T value);

    public void dispose() {
        property1.removeListener(listener);
        property2.removeListener(listener);
    }
}