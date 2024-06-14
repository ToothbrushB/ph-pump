package com.calebli.phpump;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

class EditingCell extends TableCell<PhRecord, Double> {
 
        private TextField textField;
 
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
                        textField.setText(getString());
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
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
            textField.focusedProperty().addListener((observableValue, oldValue, newValue) -> {
                    if (!newValue) {
                        commitEdit(Double.valueOf(textField.getText()));
                    }
            });
            textField.setOnKeyPressed(e -> {
                if (e.getCode().equals(KeyCode.ENTER)) {
                    commitEdit(Double.valueOf(textField.getText()));
                }
            });

        }
 
        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }