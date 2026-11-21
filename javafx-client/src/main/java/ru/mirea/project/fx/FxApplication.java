package ru.mirea.project.fx;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import ru.mirea.project.AppContext;

public class FxApplication extends Application {
    private static final String TITLE = "Парковочная система";

    private AppContext context;

    @Override
    public void init() {
        context = new AppContext();
    }

    @Override
    public void start(Stage stage) {
        Parent root = Views.load("MainWindow.fxml", context).getRoot();
        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(Views.stylesheet());

        stage.setTitle(TITLE);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
