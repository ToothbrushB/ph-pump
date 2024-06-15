package com.calebli.phpump;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.fazecast.jSerialComm.SerialPort.*;

public class HelloController {
    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final NumberFormat pHMeterFormat = new DecimalFormat("#0.000");
    private final ObservableList<PhRecord> data = FXCollections.observableList(new LinkedList<>());
    @FXML
    private MenuItem copyMenu;
    @FXML
    private Button savePumpButton;
    @FXML
    private IntegerTextField pumpHigh;
    @FXML
    private IntegerTextField pumpLow;
    @FXML
    private IntegerTextField pumpTimeout;
    @FXML
    private DoubleTextField yAxisMax;
    @FXML
    private DoubleTextField yAxisMin;
    @FXML
    private DoubleTextField xAxisMax;
    @FXML
    private DoubleTextField xAxisMin;
    @FXML
    private MenuItem openMenu;
    @FXML
    private MenuItem saveAsMenu;
    @FXML
    private MenuItem saveMenu;
    @FXML
    private MenuBar menuBar;
    @FXML
    private RadioButton pumpOnRb;
    @FXML
    private RadioButton pumpAutoRb;
    @FXML
    private RadioButton pumpOffRb;
    @FXML
    private ToggleGroup pumpModeGroup;
    @FXML
    private IntegerTextField sliderMin;
    @FXML
    private IntegerTextField sliderMax;
    @FXML
    private Slider waterLevelSlider;
    @FXML
    private TextArea consoleText;
    @FXML
    private Label acidSlope;
    @FXML
    private Label baseSlope;
    @FXML
    private Label zeroOffset;
    @FXML
    private DoubleTextField calibratePh;
    @FXML
    private ComboBox<CalibrationPoint> calibrateComboBox;
    @FXML
    private Button openPortButton;
    @FXML
    private Button saveButton;
    @FXML
    private DoubleTextField enterPh;
    @FXML
    private DoubleTextField enterTime;
    @FXML
    private Button openButton;
    @FXML
    private Label digitalMeter;
    @FXML
    private TableView<PhRecord> tableView;
    @FXML
    private BorderPane chartView;
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
    private Status status = Status.NOT_CONNECTED;
    private LocalDateTime startTime;
    private File saveFile;
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
//        consoleText.textProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
//            System.out.println(autoScrollCheckBox.isSelected());
//            if (autoScrollCheckBox.isSelected())
//                consoleText.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
//            else
//                consoleText.setScrollTop(consoleText.getLength());
//        });
        sliderMax.textProperty().addListener(e -> waterLevelSlider.setMax(sliderMax.getValue()));
        sliderMin.textProperty().addListener(e -> waterLevelSlider.setMax(sliderMin.getValue()));

        pumpModeGroup.selectedToggleProperty().addListener(e -> {
            if (pumpOffRb.isSelected()) pumpOff();
            else if (pumpAutoRb.isSelected()) pumpAuto();
            else if (pumpOnRb.isSelected()) pumpOn();

        });
        pumpTimeout.valueProperty().addListener((obv, ov, nv) -> {
            sendString(".t" + nv);
            showPumpSave();
        });
        pumpHigh.valueProperty().addListener((obv, ov, nv) -> {
            sendString(".h" + nv);
            showPumpSave();
        });
        pumpLow.valueProperty().addListener((obv, ov, nv) -> {
            sendString(".l" + nv);
            showPumpSave();
        });
//        calibratePh.textProperty().addListener((observable, oldValue, newValue) -> {
//            if (!newValue.matches("\\d*")) {
//                sliderMax.setText(newValue.replaceAll("[^\\d]", ""));
//            }
//        });
//        enterTime.textProperty().addListener((observable, oldValue, newValue) -> {
//            if (!newValue.matches("\\d*")) {
//                sliderMax.setText(newValue.replaceAll("[^\\d]", ""));
//            }
//        });
//        enterPh.textProperty().addListener((observable, oldValue, newValue) -> {
//            if (!newValue.matches("\\d*")) {
//                sliderMax.setText(newValue.replaceAll("[^\\d]", ""));
//            }
//        });
        calibrateComboBox.setItems(FXCollections.observableArrayList(CalibrationPoint.values()));
        calibrateComboBox.getSelectionModel().select(0);
        baudRateComboBox.setItems(FXCollections.observableArrayList(300, 1200, 2400, 9600, 19200, 38400, 57600, 115200));
        baudRateComboBox.getSelectionModel().select(3);

        // menu
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Mac"))
            menuBar.useSystemMenuBarProperty().set(true);
        openMenu.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        saveMenu.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        saveAsMenu.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+S"));
        copyMenu.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        reloadPorts();

        // set up the line
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setUpperBound(xAxisMax.getValue());
        xAxis.setLowerBound(xAxisMin.getValue());
        yAxis.setUpperBound(yAxisMax.getValue());
        yAxis.setLowerBound(yAxisMin.getValue());
        xAxisMax.valueProperty().addListener(e -> xAxis.setUpperBound(xAxisMax.getValue()));
        xAxisMin.valueProperty().addListener(e -> xAxis.setLowerBound(xAxisMin.getValue()));
        yAxisMax.valueProperty().addListener(e -> yAxis.setUpperBound(yAxisMax.getValue()));
        yAxisMin.valueProperty().addListener(e -> yAxis.setLowerBound(yAxisMin.getValue()));

        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);

        xAxis.setLabel("Time");
        yAxis.setLabel("pH");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
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
                        XYChart.Data<Number, Number> d = new XYChart.Data<>();
                        d.XValueProperty().bindBidirectional(additem.timeProperty());
                        d.YValueProperty().bindBidirectional(additem.pHProperty());
                        series.getData().add(d);
                    });
                }
            }
        });
        lineChart.getData().add(series);
        chartView.setCenter(lineChart);

        // set up the table
        tableView.setEditable(true);
        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        TableColumn<PhRecord, Double> timeColumn = new TableColumn<>("Time");
        timeColumn.setMinWidth(100);
        TableColumn<PhRecord, Double> pHColumn = new TableColumn<>("pH");
        pHColumn.setMinWidth(100);

        TableColumn<PhRecord, PhRecord> deleteCol = new TableColumn<>("Delete");
        deleteCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        deleteCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("", new FontIcon("mdi2d-delete"));

            @Override
            protected void updateItem(PhRecord r, boolean empty) {
                super.updateItem(r, empty);

                if (r == null) {
                    setGraphic(null);
                    return;
                }

                setGraphic(deleteButton);
                deleteButton.setOnAction(event -> data.remove(r));
            }
        });
//        tableView.getSelectionModel().getSelectedCells().addListener((ListChangeListener<? super TablePosition>) c -> {
//
//            for (int i = 0; i < tableView.getItems().size(); i++) {
//                tableView.getSelectionModel().clearSelection(i,deleteCol);
//
//            }
//        });

        Callback<TableColumn<PhRecord, Double>, TableCell<PhRecord, Double>> cellFactory = p -> new EditingCell();

        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeColumn.setCellFactory(cellFactory);
        timeColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setTime(t.getNewValue()));
        pHColumn.setCellValueFactory(new PropertyValueFactory<>("pH"));
        pHColumn.setCellFactory(cellFactory);
        pHColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setpH(t.getNewValue()));

        tableView.getColumns().add(timeColumn);
        tableView.getColumns().add(pHColumn);
        tableView.getColumns().add(deleteCol);

        tableView.setItems(data);


        // update the clock
        Timeline updateRunTime = new Timeline(new KeyFrame(Duration.millis(1), event -> {
            if (status == Status.RUN) {
                runTimeLabel.setText(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(java.time.Duration.between(startTime, LocalDateTime.now())).format(timeFormat));
            }
        }));
        updateRunTime.setCycleCount(Timeline.INDEFINITE);
        updateRunTime.play();

        // autosave
        ScheduledService<Boolean> autoSaveService = new AutoSaveService();
        autoSaveService.setPeriod(Duration.seconds(60));
        autoSaveService.setOnSucceeded(e -> {
            if (autoSaveService.getValue()) {
                createPopup("Auto-save success", "good-popup");
            } else {
                createPopup("Auto-save failed", "error-popup");
            }
        });
        autoSaveService.start();
    }

    private void createPopup(final String message, String css) {
        final Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        Label label = new Label(message);
        label.getStyleClass().add(css);
        label.setOnMouseReleased(e -> popup.hide());
        FadeTransition ft = new FadeTransition(Duration.millis(300), label);
        ft.setFromValue(1);
        ft.setToValue(0);
        new Timeline(new KeyFrame(Duration.seconds(2), e -> ft.play())).play();

        label.setAlignment(Pos.BASELINE_CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        popup.getContent().add(label);
        popup.setOnShown(e -> {
            popup.setX(saveButton.getScene().getWindow().getX() + saveButton.getScene().getWindow().getWidth() * 0.9 - popup.getWidth() / 2);
            popup.setY(saveButton.getScene().getWindow().getY() + saveButton.getScene().getWindow().getHeight() * 0.9 - popup.getHeight() / 2);
        });
        popup.show(saveButton.getScene().getWindow());
    }

    private double getTimeElapsed() {
        java.time.Duration d = java.time.Duration.between(startTime, LocalDateTime.now());
        return d.getSeconds() + d.getNano() * 1E-9;
    }

    private void loadDataChart() {
        series.getData().clear();
        data.forEach(r -> {
            XYChart.Data<Number, Number> d = new XYChart.Data<>();
            d.XValueProperty().bindBidirectional(r.timeProperty());
            d.YValueProperty().bindBidirectional(r.pHProperty());
            series.getData().add(d);
        });
    }

    private void sendString(String s) {
        if (currentPort == null || !currentPort.isOpen()) {
            createPopup("Device is not connected. Cannot send command:\n" + s, "error-popup");
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
        } else {
            if (currentPort != null && currentPort.isOpen()) {
                if (status == Status.RUN) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Closing the COM port will stop any data collection").showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
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
            boolean firstTime = true;

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
                if (firstTime) {
                    loadSlope();
                    firstTime = false;
                }
                byte[] originalMessage = event.getReceivedData();
                String omsg = new String(originalMessage);
//                Register register = Register.values()[originalMessage[0]];
                if (originalMessage[0] == '.') { // from the pump/water sensor side
                    byte[] payload = Arrays.copyOfRange(originalMessage, 1, originalMessage.length);
                    String smsg = new String(payload);
                    char command = smsg.charAt(0);
                    switch (command) {
                        case 't' -> {
                            Platform.runLater(() -> consoleText.appendText(omsg + "\n"));
                            pumpTimeout.setValue(Integer.parseInt(smsg.substring(1)));
                        } // delay
                        case 'h' -> {
                            Platform.runLater(() -> consoleText.appendText(omsg + "\n"));
                            pumpHigh.setValue(Integer.parseInt(smsg.substring(1)));
                        } //high level
                        case 'l' -> {
                            Platform.runLater(() -> consoleText.appendText(omsg + "\n"));
                            pumpLow.setValue(Integer.parseInt(smsg.substring(1)));
                        } // low level
                        case 'w' -> { // water level
                            try {
                                waterLevelSlider.setValue(Double.parseDouble(smsg.substring(1)));
                            } catch (NumberFormatException e) {
                                System.out.println("malformed water level: " + smsg);
                            }
                        }
                    }
                } else { // from the pH probe
                    if (omsg.charAt(0) == '?') { // response from a command
                        Platform.runLater(() -> consoleText.appendText(omsg + "\n"));
                        if (omsg.startsWith("?Slope,")) { // slope
                            Platform.runLater(() -> {
                                String[] parsed = omsg.split(",");
                                acidSlope.setText(parsed[1] + "%");
                                baseSlope.setText(parsed[2] + "%");
                                zeroOffset.setText(parsed[3] + " mV");
                            });
                        }
                    } else { // new ph data
                        double pH = Double.MIN_VALUE;
                        try {
                            pH = Double.parseDouble(omsg);
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
        } else if (status == Status.RUN) {
            runButton.setGraphic(new FontIcon("mdi2p-play"));
            status = Status.STOP_READY;
        } else if (status == Status.NOT_CONNECTED) {
            new Alert(Alert.AlertType.ERROR, "Device is not connected.").show();
        }
    }

    @FXML
    protected void open() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV (Comma delimited)", "*.csv"), new FileChooser.ExtensionFilter("All Files", "*.*"));
        chooser.setTitle("Open a CSV");
        File file = chooser.showOpenDialog(openButton.getScene().getWindow());

        if (file == null) return;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("Time", "pH").build();
        try (final CSVParser parse = new CSVParser(new BufferedReader(new FileReader(file)), csvFormat)) {
            data.clear();
            data.addAll(parse.stream().skip(1).map(r -> new PhRecord(Double.parseDouble(r.get(0)), Double.parseDouble(r.get(1)))).toList());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "File read error\n" + e).show();
        }

    }

    @FXML
    protected void save() {
        if (saveFile == null) {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("Run " + (startTime != null ? startTime : LocalDateTime.now()).format(dateTimeFormat) + ".csv");

            chooser.setTitle("Save as CSV");
            File file = chooser.showSaveDialog(saveButton.getScene().getWindow());
            if (file == null) return;
            saveFile = file;
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("Time", "pH").build();

        try (final CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter(saveFile)), csvFormat)) {
            data.forEach(r -> {
                try {
                    printer.printRecord(r.getTime(), r.getpH());
                } catch (IOException e) {
                    new Alert(Alert.AlertType.ERROR, "File save error\n" + e).show();
                }

            });
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "File save error\n" + e).show();
        }
    }

    @FXML
    protected void addEntry() {
        data.add(new PhRecord(enterTime.getValue(), enterPh.getValue()));
        enterTime.clear();
        enterPh.clear();
    }

    @FXML
    protected void clearData() {
        if (status == Status.RUN)
            new Alert(Alert.AlertType.CONFIRMATION, "Clearing data will stop data collection. OK?").showAndWait().filter(r -> r.equals(ButtonType.OK)).ifPresent(r -> {
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
        sendString("Cal," + calibrateComboBox.getSelectionModel().getSelectedItem().toString() + "," + calibratePh.getValue());
        loadSlope();
    }

    private void loadSlope() {
        sendString("Slope,?");
    }

    @FXML
    protected void clearConsole() {
        consoleText.clear();
        consoleText.setScrollTop(Double.MIN_VALUE); //this will scroll to the top
    }

    @FXML
    protected void pumpOn() {
        sendString(".1");
    }

    @FXML
    protected void pumpOff() {
        sendString(".0");
    }

    @FXML
    protected void pumpAuto() {
        sendString(".a");
    }

    @FXML
    protected void saveAs() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("Run " + (startTime != null ? startTime : LocalDateTime.now()).format(dateTimeFormat) + ".csv");

        chooser.setTitle("Save as CSV");
        File file = chooser.showSaveDialog(saveButton.getScene().getWindow());
        if (file == null) return;
        saveFile = file;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("Time", "pH").build();

        try (final CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter(saveFile)), csvFormat)) {
            data.forEach(r -> {
                try {
                    printer.printRecord(r.getTime(), r.getpH());
                } catch (IOException e) {
                    new Alert(Alert.AlertType.ERROR, "File save error\n" + e).show();
                }

            });
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "File save error\n" + e).show();
        }
    }

    @FXML
    protected void savePumpSettings() {
        sendString(".s");
        savePumpButton.setDisable(true);
        savePumpButton.setVisible(false);
    }

    private void showPumpSave() {
        savePumpButton.setVisible(true);
        savePumpButton.setDisable(false);
    }

    @FXML
    protected void copy() {
        ObservableList<TablePosition> posList = tableView.getSelectionModel().getSelectedCells();
        int old_r = -1;
        StringBuilder clipboardString = new StringBuilder();
        for (TablePosition p : posList) {
            int r = p.getRow();
            int c = p.getColumn();
            Object cell = tableView.getColumns().get(c).getCellData(r);
            if (cell == null)
                cell = "";
            if (old_r == r)
                clipboardString.append('\t');
            else if (old_r != -1)
                clipboardString.append('\n');
            if (cell instanceof Number || cell instanceof String)
                clipboardString.append(cell);
            old_r = r;
        }
        final ClipboardContent content = new ClipboardContent();
        content.putString(clipboardString.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private class AutoSaveService extends ScheduledService<Boolean> {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<>() {
                @Override
                protected Boolean call() {
                    Platform.runLater(() -> createPopup("Attempting auto-save", "warn-popup"));
                    AtomicBoolean success = new AtomicBoolean(true);

                    File[] toDelete = new File(System.getProperty("user.home")).listFiles((dir, name) -> name.startsWith("phpump_autosave_"));
                    if (toDelete != null) {
                        for (File f : toDelete) {
                            if (!f.delete())
                                success.set(false);
                        }
                    }

                    File file = new File(System.getProperty("user.home") + "/phpump_autosave_" + (LocalDateTime.now().format(dateTimeFormat)) + ".csv");
                    CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("Time", "pH").build();

                    try (final CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter(file)), csvFormat)) {
                        data.forEach(r -> {
                            try {
                                printer.printRecord(r.getTime(), r.getpH());
                            } catch (IOException e) {
//                                e.printStackTrace();
//                                new Alert(Alert.AlertType.ERROR, "File save error\n" + e).show();
                                success.set(false);
                            }

                        });
                    } catch (IOException e) {
//                        e.printStackTrace();
//                        new Alert(Alert.AlertType.ERROR, "File save error\n" + e).show();
                        success.set(false);
                    }

                    if (saveFile != null) {
                        try (final CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter(saveFile)), csvFormat)) {
                            data.forEach(r -> {
                                try {
                                    printer.printRecord(r.getTime(), r.getpH());
                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                new Alert(Alert.AlertType.ERROR, "File save error\n" + e).show();
                                    success.set(false);
                                }

                            });
                        } catch (IOException e) {
//                            e.printStackTrace();
                            success.set(false);
                        }
                    }

                    return success.get();


                }
            };
        }
    }
}