package com.calebli.phpump;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class DoubleTextField extends TextField {
    private final Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");

    private final UnaryOperator<TextFormatter.Change> filter = c -> {
        String text = c.getControlNewText();
        if (validEditingState.matcher(text).matches()) {
            return c;
        } else {
            return null;
        }
    };

    private final StringConverter<Double> converter = new StringConverter<>() {

        @Override
        public Double fromString(String s) {
            if (s.isEmpty() || "-".equals(s) || ".".equals(s) || "-.".equals(s)) {
                return 0.0;
            } else {
                return Double.valueOf(s);
            }
        }


        @Override
        public String toString(Double d) {
            return d.toString();
        }
    };

    private TextFormatter<Double> textFormatter = new TextFormatter<>(converter, 0.0, filter);

    public DoubleTextField() {
        this(0.0);
    }

    public DoubleTextField(Double num) {
        super(num.toString());
        setValue(num);
        setTextFormatter(textFormatter);
    }

//    public DoubleTextField(String text) {
//        super(text);
//        setTextFormatter(textFormatter);
//    }

    public Double getValue() {
        return textFormatter.getValue();
    }

    public void setValue(Double v) {
        textFormatter.setValue(v);
    }

    public ObjectProperty<Double> valueProperty() {
        return textFormatter.valueProperty();
    }


}
