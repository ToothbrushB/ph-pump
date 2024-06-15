package com.calebli.phpump;

import javafx.scene.control.TableCell;
import javafx.scene.input.KeyCode;

class EditingCell extends TableCell<PhRecord, Double> {

    private DoubleTextField textField;
 
        public EditingCell() {
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
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
            textField.focusedProperty().addListener((observableValue, oldValue, newValue) -> {
                    if (!newValue) {
                        commitEdit(textField.getValue());
                    }
            });
            textField.setOnKeyPressed(e -> {
                if (e.getCode().equals(KeyCode.ENTER)) {
                    commitEdit(Double.valueOf(textField.getText()));
                }
            });

        }

    private Double getDouble() {
        return getItem();
    }
        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }