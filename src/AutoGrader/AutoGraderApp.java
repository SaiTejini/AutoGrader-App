package AutoGrader;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AutoGraderApp extends Application {

    private final GraderController graderController = new GraderController();

    public static void main(String[] args) {
        launch(args);
    }

    /*
     * This will create the required window with Student View and Instructor View Tabs.
     * */
    @Override
    public void start(Stage primaryStage) {
        StudentView studentView = new StudentView(graderController);
        InstructorView instructorView = new InstructorView(graderController);

        TabPane tabPane = new TabPane();
        Tab studentTab = new Tab("Student", studentView);
        Tab instructorTab = new Tab("Instructor", instructorView);
        studentTab.setClosable(false);
        instructorTab.setClosable(false);

        tabPane.getTabs().addAll(studentTab, instructorTab);

        StackPane root = new StackPane(tabPane);
        // change width, height of the window accordingly.
        Scene scene = new Scene(root, 500, 400);        
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setTitle("Auto Grader");
        primaryStage.setScene(scene);
        primaryStage.show();
        
       // Listener to detect when the selected tab changes
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab == studentTab) {                
                studentView.clearLabels();
            } else {
            	instructorView.clearLabels();
            }
        });
    }
}
