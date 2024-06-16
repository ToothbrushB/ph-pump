package com.calebli.phpump;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {
    public static final String MAIN_TITLE = "pH Probe and Pump Interface";
    public static StringProperty title = new SimpleStringProperty(MAIN_TITLE);

    public static String getTitle() {
        return title.get();
    }

    public static void setTitle(String title) {
        HelloApplication.title.set(title);
    }

    public static StringProperty titleProperty() {
        return title;
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1200, 700);
        HelloController appCtrl = fxmlLoader.getController();

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/CSS/style.css")).toExternalForm());
        stage.titleProperty().bind(title);
        stage.setScene(scene);
        stage.show();

        scene.getWindow().setOnCloseRequest(ev -> {
            if (!appCtrl.shutdown()) {
                ev.consume();
            }
        });
    }
}