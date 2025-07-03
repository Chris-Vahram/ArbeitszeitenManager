package org.example.arbeitszeitenmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        HelloController controller = fxmlLoader.getController(); // <-- Referenz holen
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Arbeitszeiten Manager");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.stop();
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}