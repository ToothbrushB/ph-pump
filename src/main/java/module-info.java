module com.calebli.phpump {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;


    opens com.calebli.phpump to javafx.fxml;
    exports com.calebli.phpump;
}