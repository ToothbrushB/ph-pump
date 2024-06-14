package com.calebli.phpump;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import java.util.Arrays;

import static com.fazecast.jSerialComm.SerialPort.*;

public class HelloController {
    public Label digitalMeter;
    public TableView<PhRecord> tableView;
    public LineChart<Number, Number> lineChart;
    public AnchorPane chartView;
    @FXML
    private ComboBox<Integer> baudRateComboBox;

    @FXML
    private ComboBox<SerialPort> portComboBox;



    @FXML
    protected void initialize() {
        baudRateComboBox.setItems(FXCollections.observableArrayList(300 ,1200 ,2400 ,9600, 19200, 38400, 57600, 115200));
        baudRateComboBox.getSelectionModel().select(3);
        reloadPorts();
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Stock Monitoring, 2010");
        //defining a series
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        series.setName("My portfolio");
        //populating the series with data

        series.getData().add(new XYChart.Data<>(1, 23));
        series.getData().add(new XYChart.Data<>(3, 15));
        series.getData().add(new XYChart.Data<>(4, 24));
        series.getData().add(new XYChart.Data<>(2, 14));
        series.getData().add(new XYChart.Data<>(5, 34));
        series.getData().add(new XYChart.Data<>(6, 36));
        series.getData().add(new XYChart.Data<>(7, 22));
        series.getData().add(new XYChart.Data<>(8, 45));
        series.getData().add(new XYChart.Data<>(9, 43));
        series.getData().add(new XYChart.Data<>(10, 17));
        series.getData().add(new XYChart.Data<>(11, 29));
        series.getData().add(new XYChart.Data<>(12, 25));
        lineChart.getData().add(series);
        chartView.getChildren().add(lineChart);

        TableColumn<PhRecord, Double> timeColumn = new TableColumn<>("Time");
        TableColumn<PhRecord, Double> pHColumn = new TableColumn<>("pH");
        tableView.getColumns().addAll(timeColumn, pHColumn);

        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        pHColumn.setCellValueFactory(new PropertyValueFactory<>("ph"));
        final ObservableList<PhRecord> data = FXCollections.observableArrayList(
                new PhRecord(34, 5),
        new PhRecord(324, 5),
        new PhRecord(56, 10),
        new PhRecord(3, 4.3),
        new PhRecord(6, 1),
        new PhRecord(68, 7)
        );
        tableView.setItems(data);


    }

    @FXML
    protected void openPort() {
        SerialPort port = portComboBox.getValue();
        if (port == null)
            return;
        port.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING | TIMEOUT_WRITE_BLOCKING, TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT_WRITE_BLOCKING);
        port.setBaudRate(baudRateComboBox.getValue());
        port.openPort();

        port.addDataListener(new SerialPortMessageListener() {
            @Override
            public byte[] getMessageDelimiter() {
                return new byte[]{(byte) 0xFF, 0x00, (byte) 0xFF, 0x00, (byte) 0xFF};
            }

            @Override
            public boolean delimiterIndicatesEndOfMessage() {
                return true;
            }

            @Override
            public int getListeningEvents() {
                return LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] originalMessage = event.getReceivedData();
//                Register register = Register.values()[originalMessage[0]];
                byte[] payload = Arrays.copyOfRange(originalMessage, 1, originalMessage.length);
                String smsg = new String(payload);
                char command = smsg.charAt(0);
                System.out.println(smsg);
                if (originalMessage[0] == 0x00) { // from the pump/water sensor side
                    switch (command) {
                        case 't' -> {} // delay
                        case 'h' -> {} //high level
                        case 'l' -> {} // low level
                        default -> { // water level
                        }
                    }
                } else { // from the pH probe
                    try {double pH = Double.parseDouble(smsg);}

                    catch (NumberFormatException e) {System.out.println("Invalid response from probe");}
                }

            }




        });
    }
    @FXML
    protected void reloadPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (int i = 0; i < ports.length; i++) {
            SerialPort port = ports[i];
            System.out.println("("+i+")  "+port.getPortDescription());
        }
        ObservableList<SerialPort> availablePorts = FXCollections.observableList(Arrays.stream(ports).toList());
        portComboBox.setItems(availablePorts);

    }
}