package synth.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * The main application class for the synthesiser's GUI.
 * This class is responsible for loading the FXML layout and showing the main window.
 */
public class SynthApplication extends Application {

    /**
     * The main entry point for all JavaFX applications.
     * @param primaryStage The primary stage for this application, onto which the scene can be set.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/synth/ui/synth_ui.fxml")));
        Parent root = loader.load();

        // Get the controller and set up a shutdown hook
        SynthUIController controller = loader.getController();
        primaryStage.setOnCloseRequest(event -> controller.shutdown());

        primaryStage.setTitle("MIDI Synthesiser");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
    }

    /**
     * The main method, used to launch the JavaFX application.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}