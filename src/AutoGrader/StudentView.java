package AutoGrader;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class StudentView extends VBox {

    private final GraderController graderController;

 // These are required fields in Student view, we can change the text on buttons, Label if required.
    private final FileChooser fileChooser = new FileChooser();
    private final Button uploadButton = new Button("Upload Implementation");
    private final Button submitButton = new Button("Submit");
    private final Label gradeLabel = new Label("Grade: ");
    private final Label scoreLabel = new Label("Score: ");
    private final Label errorLabel = new Label();
    private final TextField studentIdTextField = new TextField();
    private final Label studentIdLabel = new Label("Student ID:");

    public StudentView(GraderController graderController) {
        this.graderController = graderController;
        configureFileChooser();
        configureButtons();
        configureLabels();
        configureErrorLabel();
        configureStudentIdTextField();
        buildLayout();
    }

    // Textbox for the student to enter student id
    private void configureStudentIdTextField() {
        studentIdTextField.setPromptText("Enter your student ID");
        studentIdTextField.setPrefWidth(200);
    }

    // File chooser to allow user to browse and student implementation java file.
    private void configureFileChooser() {
        fileChooser.setTitle("Select Implementation");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
    }

    // This will configure the buttons and actions to be taken on clicking on the button.
    private void configureButtons() {
        uploadButton.setOnAction(event -> {
        	errorLabel.setVisible(false);
        	String studentId = studentIdTextField.getText();
            if (studentId.isEmpty()) {
                showAlert("Error", "Please enter your student ID.");
                return;
            }
            
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                try {
                    Path targetPath = Path.of("student_implementation", studentId, file.getName());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    graderController.setStudentImplementation(targetPath.toFile());
                    gradeLabel.setText("Grade: ");
                    scoreLabel.setText("Score: ");                    
                } catch (IOException e) {
                    showAlert("Error", "Failed to save the file. Please try again.");
                }
            }
        });

        submitButton.setOnAction(event -> {
            GradingResult result = graderController.gradeStudentImplementation();
            if (result.isError()) {
                errorLabel.setText(result.getErrorMessage());
                errorLabel.setVisible(true);
                gradeLabel.setVisible(false);
                scoreLabel.setVisible(false);
            } else {
            	// Save the result to the database
                String studentId = studentIdTextField.getText();
                result.setStudentId(studentId);
                graderController.saveResultToDatabase(result);
                
                gradeLabel.setText("Grade: " + result.getGrade());
                scoreLabel.setText("Score: " + result.getScore() + "%");
                errorLabel.setVisible(false);
                gradeLabel.setVisible(true);
                scoreLabel.setVisible(true);
            }
        });
    }

    private void configureLabels() {
        gradeLabel.getStyleClass().add("result-label");
        scoreLabel.getStyleClass().add("result-label");
    }

    private void configureErrorLabel() {
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
    }
	
    // This displays a small dialog box if any critical error is observed.
	private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

	public void clearLabels() {
	    gradeLabel.setText("Grade: ");
	    scoreLabel.setText("Score: ");
	    errorLabel.setVisible(false);
	    studentIdTextField.clear();
	}
	
    private void buildLayout() {
    	HBox studentIdBox = new HBox(10, studentIdLabel, studentIdTextField); // Create a new HBox for the label and text field
        studentIdBox.setAlignment(Pos.CENTER);
        
        setSpacing(20);
        setAlignment(Pos.CENTER);
        getChildren().addAll(studentIdBox, uploadButton, submitButton, gradeLabel, scoreLabel, errorLabel);
    }
}