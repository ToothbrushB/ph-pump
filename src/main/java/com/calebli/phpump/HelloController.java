package com.calebli.phpump;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.fazecast.jSerialComm.SerialPort.*;

public class HelloController {
    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final NumberFormat pHMeterFormat = new DecimalFormat("#0.000");
    private final ObservableList<PhRecord> data = FXCollections.observableList(new LinkedList<>());
    @FXML
    private ComboBox<TimeUnits> timeUnitComboBox;
    @FXML
    private Label statusReadout;
    @FXML
    private CheckBox showPointsCheck;
    //    @FXML
//    private CheckBox zeroRangeCheckY;
    @FXML
    private CheckBox autoRangeCheckY;
    @FXML
    private CheckBox autoRangeCheckX;
    //    @FXML
//    private CheckBox zeroRangeCheckX;
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
    private SimpleObjectProperty<Status> status = new SimpleObjectProperty<>(Status.DISCONNECTED);
    private LocalDateTime startTime;
    private File saveFile;
    private final Watchdog connectedDog = new Watchdog(3000, () -> {
//        System.out.println("TRIGGERED");
        Platform.runLater(() -> {
//            runButton.setGraphic(new FontIcon("mdi2p-play"));
            openPortButton.setText("Open Port");
        });
        currentPort.closePort();
        if (status.getValue() == Status.RUN)
            Platform.runLater(() -> status.set(Status.INTERRUPTED));
        else
            Platform.runLater(() -> status.set(Status.DISCONNECTED));

        Platform.runLater(() -> {
            reloadPorts();
            createPopup("Device disconnected!", "error-popup");
        });
    });
    private LineChart<Number, Number> lineChart;
    private OpenFileService openFileService = new OpenFileService();
    private SimpleBooleanProperty unsavedChanges = new SimpleBooleanProperty(false);
    private ScheduledService<Boolean> autoSaveService;
    @FXML
    protected void initialize() {
        status.addListener(e -> statusReadout.setText(status.getValue().toString()));
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
        timeUnitComboBox.setItems(FXCollections.observableArrayList(TimeUnits.values()));
        timeUnitComboBox.getSelectionModel().select(TimeUnits.SECOND);
        timeUnitComboBox.getSelectionModel().selectedItemProperty().addListener((obv, ov, nv) -> {
            EditingCell.setScaleFactor(nv.getFactor());
            tableView.refresh();
            lineChart.getData().clear();
            lineChart.getData().add(nv.getSeries());
        });
        calibrateComboBox.setItems(FXCollections.observableArrayList(CalibrationPoint.values()));
        calibrateComboBox.getSelectionModel().select(0);
        baudRateComboBox.setItems(FXCollections.observableArrayList(300, 1200, 2400, 9600, 19200, 38400, 57600, 115200));
        baudRateComboBox.getSelectionModel().select(3);
        portComboBox.setCellFactory(new Callback<>() {
            @Override
            public ListCell<SerialPort> call(ListView<SerialPort> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(SerialPort item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            Label l = new Label(item.getPortDescription());
                            l.setTooltip(new Tooltip(item.getSystemPortName() + "\n" + item.getManufacturer()));
                            setGraphic(l);
                        }
                    }
                };
            }
        });

        openFileService.setOnSucceeded(v -> {
            if (openFileService.getValue())
                Platform.runLater(() -> createPopup("File opened successfully", "good-popup"));

            else Platform.runLater(() -> createPopup("File read error", "error-popup"));

        });
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

        xAxis.autoRangingProperty().bindBidirectional(autoRangeCheckX.selectedProperty());
        yAxis.autoRangingProperty().bindBidirectional(autoRangeCheckY.selectedProperty());


        xAxis.setLabel("Time");
        yAxis.setLabel("pH");
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.createSymbolsProperty().bind(showPointsCheck.selectedProperty());
//        lineChart.setTitle("pH");
        loadDataChart();
        data.addListener((ListChangeListener<? super PhRecord>) c -> {
            unsavedChanges.set(true);
            while (c.next()) {
                // don't care about permute bc order doesn't matter in a chart
                if (c.wasUpdated()) {
                    //update item
                    // since we have no idea which element changed bc we don't keep track of indices, we have no choice but to rebuild the entire list
                    loadDataChart();
                } else {
                    c.getRemoved().forEach(remitem -> {
                        for (TimeUnits unit : TimeUnits.values())
                            unit.getSeries().getData().removeIf(d -> remitem.equalsData(d, unit));
                    });
                    c.getAddedSubList().forEach(additem -> {
                        for (TimeUnits unit : TimeUnits.values()) {
                            XYChart.Data<Number, Number> d = new XYChart.Data<>();
                            new BidirectionalTimeBinding(additem.timeProperty(), d.XValueProperty(), unit);
                            d.YValueProperty().bindBidirectional(additem.pHProperty());
                            unit.getSeries().getData().add(d);
                        }
                    });
                }
            }
        });
        lineChart.getData().add(TimeUnits.SECOND.getSeries());
        lineChart.setAnimated(false);
        chartView.setCenter(lineChart);

        // set up the table
        tableView.setEditable(true);
        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        TableColumn<PhRecord, Double> timeColumn = new TableColumn<>("Time");
        timeColumn.setMinWidth(100);
        TableColumn<PhRecord, Double> pHColumn = new TableColumn<>("pH");
        pHColumn.setMinWidth(100);

        TableColumn<PhRecord, PhRecord> deleteCol = getPhRecordPhRecordTableColumn();

        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeColumn.setCellFactory(p -> new EditingCell(true));
        timeColumn.setOnEditCommit(t -> {
            t.getTableView().getItems().get(t.getTablePosition().getRow()).setTime(t.getNewValue());
            unsavedChanges.set(true);
        });
        pHColumn.setCellValueFactory(new PropertyValueFactory<>("pH"));
        pHColumn.setCellFactory(p -> new EditingCell());
        pHColumn.setOnEditCommit(t -> {
            t.getTableView().getItems().get(t.getTablePosition().getRow()).setpH(t.getNewValue());
            unsavedChanges.set(true);
        });

        tableView.getColumns().add(timeColumn);
        tableView.getColumns().add(pHColumn);
        tableView.getColumns().add(deleteCol);

        tableView.setItems(data);


        // update the clock
        Timeline updateRunTime = new Timeline(new KeyFrame(Duration.millis(5), event -> {
            if (status.get() == Status.RUN || status.getValue() == Status.INTERRUPTED) {
                runTimeLabel.setText(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(java.time.Duration.between(startTime, LocalDateTime.now())).format(timeFormat));
            }
        }));
        updateRunTime.setCycleCount(Timeline.INDEFINITE);
        updateRunTime.play();

        // ask for autosaves
        File[] autosaves = new File(System.getProperty("user.home")).listFiles((dir, name) -> name.startsWith("phpump_autosave_"));
        if (autosaves != null && autosaves.length > 0) {
            ChoiceDialog<File> fileChoiceDialog = new ChoiceDialog<>(autosaves[0], autosaves);
            fileChoiceDialog.setTitle("Load Auto-Save");
            fileChoiceDialog.setHeaderText("Auto-Save Files Found");
            fileChoiceDialog.setContentText("Select file to load: ");
            fileChoiceDialog.showAndWait()
                .ifPresent(openFileService::openIt);
        }

        // autosave
        autoSaveService = new AutoSaveService();
        autoSaveService.setPeriod(Duration.seconds(60));
        autoSaveService.setOnSucceeded(e -> {
            if (autoSaveService.getValue()) {
                createPopup("Auto-save success", "good-popup");
            } else {
                createPopup("Auto-save failed", "error-popup");
            }
        });

    }

    private TableColumn<PhRecord, PhRecord> getPhRecordPhRecordTableColumn() {
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
        return deleteCol;
    }

    private void createPopup(final String message, String css) {
        createPopup(message, css, 2);
    }

    private void createPopup(final String message, String css, int duration) {
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
        new Timeline(new KeyFrame(Duration.seconds(duration), e -> ft.play())).play();

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
        for (TimeUnits unit : TimeUnits.values()) {
            unit.getSeries().getData().clear();

            data.forEach(r -> {
                XYChart.Data<Number, Number> d = new XYChart.Data<>();
                new BidirectionalTimeBinding(r.timeProperty(), d.XValueProperty(), unit);
                d.YValueProperty().bindBidirectional(r.pHProperty());
                unit.getSeries().getData().add(d);
            });
        }


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
            createPopup("No COM port selected", "warn-popup");
//            new Alert(Alert.AlertType.WARNING, "No COM port selected").show();
            return;
        } else {
            if (currentPort != null && currentPort.isOpen()) {
                if (status.getValue() == Status.RUN) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Closing the COM port will stop any data collection").showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
                        status.set(Status.STOP_READY);
                        currentPort.closePort();
                        openPortButton.setText("Open Port");
                        status.set(Status.DISCONNECTED);
                        connectedDog.stop();
                    });
                } else {
                    currentPort.closePort();
                    openPortButton.setText("Open Port");
                    status.set(Status.DISCONNECTED);
                    connectedDog.stop();
                }

                return;
            }
        }

        currentPort = port;
        port.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING | TIMEOUT_WRITE_BLOCKING, TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT_WRITE_BLOCKING);
        port.setBaudRate(baudRateComboBox.getValue());
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
                connectedDog.feed();
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
                            System.out.println("Invalid response from probe: " + omsg);
                        }
                        if (status.getValue() == Status.RUN) {
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

        new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                if (port.openPort(100)) {
                    Platform.runLater(() -> {
                        openPortButton.setText("Close Port");
                        status.set((status.getValue() == Status.INTERRUPTED) ? Status.RUN : Status.STOP_READY);
                    });
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        connectedDog.start();
                    } catch (InterruptedException ignored) {
                    }
                    break;
                } else {
                    int finalI = i;
                    Platform.runLater(() -> createPopup("Failed to open port, attempt " + finalI + "/5.", "error-popup"));
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();

    }

    @FXML
    protected void reloadPorts() {
        if (currentPort != null && currentPort.isOpen()) {
            Platform.runLater(() -> {
                status.set(Status.DISCONNECTED);
                openPortButton.setText("Open Port");
            });
            currentPort.closePort();
            connectedDog.stop();
        }
        SerialPort[] ports = SerialPort.getCommPorts();
        ObservableList<SerialPort> availablePorts = FXCollections.observableList(Arrays.stream(ports).toList());
        Platform.runLater(() -> {
            portComboBox.setItems(availablePorts);
            availablePorts.stream().filter(p -> p.getManufacturer().contains("Arduino"))
                .findFirst()
                .ifPresent(p -> portComboBox.getSelectionModel().select(p));
            if (currentPort != null)
                availablePorts.stream().filter(p -> p.getPortDescription().equals(currentPort.getPortDescription()))
                    .findFirst()
                    .ifPresent(p -> portComboBox.getSelectionModel().select(p));
        });
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
        if (status.getValue() == Status.STOP_READY) {
            startTime = LocalDateTime.now();
            runButton.setGraphic(new FontIcon("mdi2s-stop"));
            if (autoSaveService.getState().equals(Worker.State.READY))
                autoSaveService.start();
            status.set(Status.RUN);

        } else if (status.getValue() == Status.RUN) {
            runButton.setGraphic(new FontIcon("mdi2p-play"));
            status.set(Status.STOP_READY);
        } else if (status.getValue() == Status.DISCONNECTED) {
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
        openFileService.openIt(file);
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
        saveToFile();

    }

    private void saveToFile() {
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
        File[] toDelete = new File(System.getProperty("user.home")).listFiles((dir, name) -> name.startsWith("phpump_autosave_"));
        if (toDelete != null) {
            for (File f : toDelete) {
                f.delete();
            }
        }
        createPopup("Saved", "good-popup");
        unsavedChanges.set(false);
    }

    @FXML
    protected void addEntry() {
        data.add(new PhRecord(enterTime.getValue(), enterPh.getValue()));
        enterTime.clear();
        enterPh.clear();
    }

    @FXML
    protected void clearData() {
        if (status.getValue() == Status.RUN)
            new Alert(Alert.AlertType.CONFIRMATION, "Clearing data will stop data collection. OK?").showAndWait().filter(r -> r.equals(ButtonType.OK)).ifPresent(r -> {
//                    series.getData().clear();
                data.clear();
                status.set(Status.STOP_READY);
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

        saveToFile();
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

    public boolean shutdown() {
        if (unsavedChanges.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes");
            alert.setContentText("What do you want to do?");

            ButtonType btSave = new ButtonType("Save");
            ButtonType btSaveAs = new ButtonType("Save As");
            ButtonType btDiscard = new ButtonType("Discard and Close");
            ButtonType btCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(btSave, btSaveAs, btDiscard, btCancel);
            AtomicBoolean doIt = new AtomicBoolean(true);
            alert.showAndWait().ifPresent(bt -> {
                if (bt.equals(btSave)) {
                    save();
                } else if (bt.equals(btSaveAs)) {
                    saveAs();
                } else if (bt.equals(btDiscard)) {

                } else {
                    doIt.set(false);
                }
            });
            return doIt.get();
        }
        return true;

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

                    if (saveFile != null) {
                        saveToFile();
                    } else {
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
                    }

                    return success.get();


                }
            };
        }
    }

    private class OpenFileService extends Service<Boolean> {
        private File file;

        public void openIt(File f) {
            file = f;
            if (!this.getState().equals(State.READY))
                this.reset();
            this.start();
        }

        @Override
        protected Task<Boolean> createTask() {
            return new Task<>() {
                @Override
                protected Boolean call() {
                    boolean success = true;
                    Platform.runLater(() -> createPopup("Opening file", "warn-popup"));
                    CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("Time", "pH").build();
                    try (FileReader fr = new FileReader(file); final CSVParser parse = new CSVParser(new BufferedReader(fr), csvFormat)) {
                        data.clear();
                        data.addAll(parse.stream().skip(1).map(r -> new PhRecord(Double.parseDouble(r.get(0)), Double.parseDouble(r.get(1)))).toList());
                        loadDataChart();
                    } catch (IOException e) {
                        e.printStackTrace();
                        success = false;
                    }
                    return success;
                }
            };
        }
    }
}