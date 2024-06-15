package com.calebli.phpump;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class IntegerTextField extends TextField {
    private final Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?");

    private final UnaryOperator<TextFormatter.Change> filter = c -> {
        String text = c.getControlNewText();
        if (validEditingState.matcher(text).matches()) {
            return c;
        } else {
            return null;
        }
    };

    private final StringConverter<Integer> converter = new StringConverter<>() {

        @Override
        public Integer fromString(String s) {
            if (s.isEmpty() || "-".equals(s)) {
                return 0;
            } else {
                return Integer.valueOf(s);
            }
        }


        @Override
        public String toString(Integer i) {
            return i.toString();
        }
    };

    private final TextFormatter<Integer> textFormatter = new TextFormatter<>(converter, 0, filter);

    public IntegerTextField() {
        this(0);
    }

    public IntegerTextField(Integer num) {
        super(num.toString());
        setValue(num);
        setTextFormatter(textFormatter);
    }

//    public DoubleTextField(String text) {
//        super(text);
//        setTextFormatter(textFormatter);
//    }

    public Integer getValue() {
        return textFormatter.getValue();
    }

    public void setValue(Integer v) {
        textFormatter.setValue(v);
    }

    public ObjectProperty<Integer> valueProperty() {
        return textFormatter.valueProperty();
    }


}
