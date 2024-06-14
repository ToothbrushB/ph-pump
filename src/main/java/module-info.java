module com.calebli.phpump {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;
    requires org.kordamp.ikonli.javafx;
    requires org.apache.commons.csv;

    opens com.calebli.phpump to javafx.fxml;
    exports com.calebli.phpump;
}