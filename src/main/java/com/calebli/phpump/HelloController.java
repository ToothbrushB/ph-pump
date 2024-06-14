package com.calebli.phpump;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

import static com.fazecast.jSerialComm.SerialPort.*;

public class HelloController {
    @FXML
    private Label acidSlope;
    @FXML
    private Label baseSlope;
    @FXML
    private Label zeroOffset;
    @FXML
    private TextField calibratePh;
    @FXML
    private ComboBox<CalibrationPoint> calibrateComboBox;
    @FXML
    private Button clearDataButton;
    @FXML
    private Button openPortButton;
    @FXML
    private Button saveButton;
    @FXML
    private TextField enterPh;
    @FXML
    private TextField enterTime;
    @FXML
    private Button openButton;
    @FXML
    private Label digitalMeter;
    @FXML
    private TableView<PhRecord> tableView;
    private LineChart<Number, Number> lineChart;
    @FXML
    private AnchorPane chartView;
    @FXML
    private TextField cmdField;
    @FXML
    private Label runTimeLabel;
    @FXML
    private Button runButton;
    @FXML
    private ComboBox<Integer> baudRateComboBox;

    @FXML
    private ComboBox<SerialPort> portComboBox;

    private SerialPort currentPort;

    private final XYChart.Series<Number,Number> series = new XYChart.Series<>();

    private Status status = Status.NOT_CONNECTED;
    private LocalDateTime startTime;
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final NumberFormat pHMeterFormat = new DecimalFormat("#0.000");

    private final ObservableList<PhRecord> data = FXCollections.observableList(new LinkedList<>());

    @FXML
    protected void initialize() {
        // bind enter for command bar
        cmdField.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) submitCommand();
        });
        enterPh.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) addEntry();
        });
        enterTime.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) addEntry();
        });

        calibrateComboBox.setItems(FXCollections.observableArrayList(CalibrationPoint.values()));
        calibrateComboBox.getSelectionModel().select(0);
        baudRateComboBox.setItems(FXCollections.observableArrayList(300 ,1200 ,2400 ,9600, 19200, 38400, 57600, 115200));
        baudRateComboBox.getSelectionModel().select(3);
        reloadPorts();

        // set up the line
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("pH");
        series.setName("pH");
        loadDataChart();
        data.addListener((ListChangeListener<? super PhRecord>) c -> {
            while (c.next()) {
                if (c.wasPermutated()) {
                    // ignore because order doesn't matter in a chart
//                        for (int i = c.getFrom(); i < c.getTo(); ++i) {
//                            //permutate
//                            c.getPermutation(i);
//                        }
                } else if (c.wasUpdated()) {
                    //update item
                    // since we have no idea which element changed bc we don't keep track of indices, we have no choice but to rebuild the entire list
                    loadDataChart();
                } else {
                    c.getRemoved().forEach(remitem -> series.getData().removeIf(remitem::equalsData));
                    c.getAddedSubList().forEach(additem -> {
                        XYChart.Data<Number,Number> d = new XYChart.Data<>();
                        d.XValueProperty().bindBidirectional(additem.timeProperty());
                        d.YValueProperty().bindBidirectional(additem.pHProperty());
                        series.getData().add(d);
                    });
                }
            }
        });
        lineChart.getData().add(series);
        chartView.getChildren().add(lineChart);

        // set up the table
        tableView.setEditable(true);

        TableColumn<PhRecord, Double> timeColumn = new TableColumn<>("Time");
        timeColumn.setMinWidth(100);
        TableColumn<PhRecord, Double> pHColumn = new TableColumn<>("pH");
        pHColumn.setMinWidth(100);
        tableView.getColumns().addAll(timeColumn, pHColumn);

        Callback<TableColumn<PhRecord, Double>, TableCell<PhRecord, Double>> cellFactory =
            p -> new EditingCell();

        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeColumn.setCellFactory(cellFactory);
        timeColumn.setOnEditCommit(
            t -> t.getTableView().getItems().get(
                t.getTablePosition().getRow()).setTime(t.getNewValue())
        );
        pHColumn.setCellValueFactory(new PropertyValueFactory<>("pH"));
        pHColumn.setCellFactory(cellFactory);
        pHColumn.setOnEditCommit(
            t -> t.getTableView().getItems().get(
                t.getTablePosition().getRow()).setpH(t.getNewValue())
        );
        tableView.setItems(data);


        // update the clock
        Timeline updateRunTime = new Timeline(
            new KeyFrame(Duration.millis(1),
                event -> {
                    if (status == Status.RUN) {
                        runTimeLabel.setText(
                            LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(java.time.Duration.between(startTime, LocalDateTime.now()))
                                .format(timeFormat));
                    }
                }));
        updateRunTime.setCycleCount(Timeline.INDEFINITE);
        updateRunTime.play();


    }
    private double getTimeElapsed() {
        java.time.Duration d = java.time.Duration.between(startTime, LocalDateTime.now());
        return d.getSeconds() + d.getNano()*1E-9;
    }

    private void loadDataChart() {
        series.getData().clear();
        data.forEach(r -> {
            XYChart.Data<Number,Number> d = new XYChart.Data<>();
            d.XValueProperty().bindBidirectional(r.timeProperty());
            d.YValueProperty().bindBidirectional(r.pHProperty());
            series.getData().add(d);
        });
    }

    private void sendString(String s) {
        if (currentPort == null || !currentPort.isOpen()) {
            new Alert(Alert.AlertType.ERROR, "Device is not connected. Cannot send command:\n"+s).show();
            return;
        }
        currentPort.writeBytes(s.getBytes(), s.getBytes().length);
    }

    @FXML
    protected void openPort() {
        SerialPort port = portComboBox.getValue();
        if (port == null) {
            new Alert(Alert.AlertType.WARNING, "No COM port selected").show();
            return;
        }
        else {
            if (currentPort != null && currentPort.isOpen()) {
                if (status == Status.RUN) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Closing the COM port will stop any data collection").showAndWait()
                        .filter(response -> response == ButtonType.OK)
                        .ifPresent(response -> {
                            status = Status.STOP_READY;
                            currentPort.closePort();
                            openPortButton.setText("Open Port");
                            status = Status.NOT_CONNECTED;
                        });
                } else {
                    currentPort.closePort();
                    openPortButton.setText("Open Port");
                    status = Status.NOT_CONNECTED;
                }

                return;
            }
        }

        currentPort = port;
        port.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING | TIMEOUT_WRITE_BLOCKING, TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT_WRITE_BLOCKING);
        port.setBaudRate(baudRateComboBox.getValue());
        port.openPort();

        port.addDataListener(new SerialPortMessageListener() {
            @Override
            public byte[] getMessageDelimiter() {
                return new byte[]{'\r'};
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
                if (originalMessage[0] == '?') { // from the pump/water sensor side
                    byte[] payload = Arrays.copyOfRange(originalMessage, 1, originalMessage.length);
                    String smsg = new String(payload);
                    char command = smsg.charAt(0);
                    switch (command) {
                        case 't' -> {} // delay
                        case 'h' -> {} //high level
                        case 'l' -> {} // low level
                        default -> { // water level
                        }
                    }
                } else { // from the pH probe
                    double pH = Double.MIN_VALUE;
                    try {
                        pH = Double.parseDouble(new String(originalMessage));
                        double finalPH = pH;
                        Platform.runLater(() -> digitalMeter.setText(pHMeterFormat.format(finalPH)));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid response from probe");
                    }
                    if (status == Status.RUN) {
                        if (pH != Double.MIN_VALUE) {
                            double finalPH1 = pH;
                            double elapsedTime = getTimeElapsed();
                            Platform.runLater(() -> data.add(new PhRecord(elapsedTime, finalPH1)));
                        }
                    }
                }

            }
        });
        openPortButton.setText("Close Port");
        status = Status.STOP_READY;
    }
    @FXML
    protected void reloadPorts() {
        if (currentPort != null && currentPort.isOpen()) {
            currentPort.closePort();
            openPortButton.setText("Open Port");
        }
        SerialPort[] ports = SerialPort.getCommPorts();
        ObservableList<SerialPort> availablePorts = FXCollections.observableList(Arrays.stream(ports).toList());
        portComboBox.setItems(availablePorts);

    }

    @FXML
    protected void submitCommand() {
        String cmd = cmdField.getText();
        System.out.println(cmd);
        cmdField.clear();
        sendString(cmd);
    }

    @FXML
    protected void runButton() {
        if (status == Status.STOP_READY) {
            startTime = LocalDateTime.now();
            runButton.setGraphic(new FontIcon("mdi2s-stop"));
            status = Status.RUN;
        }
        else if (status == Status.RUN){
            runButton.setGraphic(new FontIcon("mdi2p-play"));
            status = Status.STOP_READY;
        } else if (status == Status.NOT_CONNECTED) {
            new Alert(Alert.AlertType.ERROR, "Device is not connected.").show();
        }
    }

    @FXML
    protected void open() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV (Comma delimited)", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        chooser.setTitle("Open a CSV");
        File file = chooser.showOpenDialog(openButton.getScene().getWindow());

        if (file == null) return;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader("Time", "pH")
            .build();
        try (final CSVParser parse = new CSVParser(new BufferedReader(new FileReader(file)), csvFormat)) {
            data.clear();
            data.addAll(parse.stream().skip(1).map(r -> new PhRecord(Double.parseDouble(r.get(0)), Double.parseDouble(r.get(1)))).toList());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "File read error\n"+e).show();
        }

    }
    @FXML
    protected void save() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("Run " + (startTime!=null ? startTime : LocalDateTime.now()).format(dateTimeFormat) + ".csv");

        chooser.setTitle("Save as CSV");
        File file = chooser.showSaveDialog(saveButton.getScene().getWindow());
        if (file == null) return;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader("Time", "pH")
            .build();

        try (final CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter(file)), csvFormat)) {
            data.forEach(r -> {
                try {
                    printer.printRecord(r.getTime(), r.getpH());
                } catch (IOException e) {
                    new Alert(Alert.AlertType.ERROR, "File save error\n"+e).show();
                }

            });
        }catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "File save error\n"+e).show();
        }
    }


    @FXML
    protected void addEntry() {
        try {
            double t = Double.parseDouble(enterTime.getText());
            double ph = Double.parseDouble(enterPh.getText());
            data.add(new PhRecord(t, ph));
            enterTime.clear();
            enterPh.clear();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "pH or time is not a number").show();
        }
    }

    @FXML
    protected void clearData() {
        if (status == Status.RUN)
            new Alert(Alert.AlertType.CONFIRMATION, "Clearing data will stop data collection. OK?").showAndWait()
                .filter(r -> r.equals(ButtonType.OK))
                .ifPresent(r -> {
//                    series.getData().clear();
                    data.clear();
                    status = Status.STOP_READY;
                });
        else {
//            series.getData().clear();
            data.clear();
        }

    }

    @FXML
    protected void calibrate() {
        try {
            Double.parseDouble(calibratePh.getText());
            sendString("Cal,"+calibrateComboBox.getSelectionModel().getSelectedItem().toString()+","+calibratePh.getText());
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "pH "+calibratePh.getText()+" is not a number").show();
        }
    }
}