package com.calebli.phpump;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.TableCell;
import javafx.scene.input.KeyCode;

class EditingCell extends TableCell<PhRecord, Double> {

    private static final DoubleProperty scaleFactor = new SimpleDoubleProperty(1);
    private DoubleTextField textField;
    private final BooleanProperty useScaleFactor = new SimpleBooleanProperty();

    public EditingCell() {
    }

    public EditingCell(boolean useScaleFactor) {
        this.useScaleFactor.set(useScaleFactor);
    }

    public static double getScaleFactor() {
        return scaleFactor.get();
    }

    public static void setScaleFactor(double scaleFactor) {
        EditingCell.scaleFactor.set(scaleFactor);
    }

    public static DoubleProperty scaleFactorProperty() {
        return scaleFactor;
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText(getItem().toString());
        setGraphic(null);
    }

    @Override
    public void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setValue(getDouble());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getString());
                setGraphic(null);
            }
        }
    }

    private void createTextField() {
        textField = new DoubleTextField(getDouble());
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        textField.focusedProperty().addListener((observableValue, oldValue, newValue) -> {
            if (!newValue) {
                commitEdit(textField.getValue() / (useScaleFactor.get() ? getScaleFactor() : 1));
            }
        });
        textField.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                commitEdit(Double.parseDouble(textField.getText()) / (useScaleFactor.get() ? getScaleFactor() : 1));
            }
        });

    }

    private Double getDouble() {
        return getItem() * (useScaleFactor.get() ? getScaleFactor() : 1);
    }

    private String getString() {
        return getItem() == null ? "" : getDouble().toString();
    }
}