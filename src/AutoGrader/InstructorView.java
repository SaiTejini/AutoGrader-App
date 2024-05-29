package AutoGrader;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
//import java.nio.file.Path;

// The methods in this class are self explanatory.
public class InstructorView extends VBox {

    private final GraderController graderController;
    // These are required fields in Instructor view, we can change the text on buttons if required.
    private final FileChooser fileChooser = new FileChooser();
    private final Button uploadTestsButton = new Button("Upload Unit Tests");
    private final Button uploadCorrectImplButton = new Button("Upload Correct Implementation");
    private final Button showAllGradesButton = new Button("Show All Student Grades");
    private final Label successLabel = new Label();
   

    public InstructorView(GraderController graderController) {
    	// Initialize controller
        this.graderController = graderController;
        // Configure UI components
        configureFileChooser();
        configureButtons();
        configureSuccessLabel();
        buildLayout();
        // Initializes earlier uploaded instructor files if present in the directory.
        // It does nothing if no file present (first time)
        initializeFiles();
    }

    private void configureFileChooser() {
        fileChooser.setTitle("Select File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON and Java Files", "*.json", "*.java"));
    }

    private void configureButtons() {
    	// Handling of Instructor Unit test json file upload button.
        uploadTestsButton.setOnAction(event -> {
        	Path targetPath;
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            if (file != null && file.getName().endsWith(".json")) {
                try {
                    targetPath = Path.of("instructor_unit_tests", file.getName());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);                    
                } catch (IOException e) {
                	e.printStackTrace();
                    showAlert("Error", "Failed to save the file. Please try again.");
                    return;
                }                
                boolean result = graderController.setInstructorUnitTests(targetPath.toFile());
                if (result) {
                	successLabel.setStyle("-fx-text-fill: green;");
	                successLabel.setText("Unit tests uploaded successfully.");
	                successLabel.setVisible(true);
                } else {
                	try {
                        Files.delete(targetPath);
                    } catch (IOException e) {
                    	e.printStackTrace();
                    }
                	 successLabel.setStyle("-fx-text-fill: red;");
                	 successLabel.setText("Failed to parse the provided json file, upload correct one");
 	                 successLabel.setVisible(true);
                }
            } else {
            	successLabel.setStyle("-fx-text-fill: red;");
                successLabel.setText("Invalid file. Please upload a JSON file containing unit tests.");
                successLabel.setVisible(true);
            }
        });

        // Handling of Instructor Java file upload button.
        uploadCorrectImplButton.setOnAction(event -> {
        	Path targetPath;
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            if (file != null && file.getName().endsWith(".java")) {
                try {
                    targetPath = Path.of("instructor_implementation", file.getName());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);                    
                } catch (IOException e) {
                    showAlert("Error", "Failed to save the file. Please try again.");
                    return;
                }
                GradingResult result = graderController.setInstructorImplementation(targetPath.toFile());
                if(result.isError()) {
                	try {
                        Files.delete(targetPath);
                    } catch (IOException e) {
                    	e.printStackTrace();
                    }
                	successLabel.setStyle("-fx-text-fill: red;");
                	successLabel.setText(result.getErrorMessage());
                	successLabel.setVisible(true);
                } else {
                	successLabel.setStyle("-fx-text-fill: green;");
	                successLabel.setText("Correct implementation uploaded successfully.");
	                successLabel.setVisible(true);
                }
            } else {
            	successLabel.setStyle("-fx-text-fill: red;");
                successLabel.setText("Invalid file. Please upload a Java file containing the correct implementation.");
                successLabel.setVisible(true);
            }
        });
        
        // Upon user clicking on showallgrades button, we will read all the entries in DB from controller
        // Display them in a tabular form.
        showAllGradesButton.setOnAction(event -> {
            List<GradingResult> gradingResults = graderController.getAllGradingResults();
            if (gradingResults.isEmpty()) {
            	showAlert("Warning", "No Student grade results present in data base");
            } else {
            	showGradingResultsTable(gradingResults);
            }
        });
    }
    private void initializeFiles() {
        Path testsPath = Path.of("instructor_unit_tests");
        Path implPath = Path.of("instructor_implementation");

        try {
            Files.createDirectories(testsPath);
            Files.createDirectories(implPath);
        } catch (IOException e) {
            showAlert("Error", "Failed to create required directories. Please try again.");
            return;
        }

        File[] testFiles = testsPath.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (testFiles != null && testFiles.length > 0) {
            boolean result = graderController.setInstructorUnitTests(testFiles[0]);
            if (!result) {
            	showAlert("Error", "Earlier uploaded unit test file is not proper, you need to upload correct one");
            }
        }

        File[] implFiles = implPath.toFile().listFiles((dir, name) -> name.endsWith(".java"));
        if (implFiles != null && implFiles.length > 0) {
        	GradingResult result = graderController.setInstructorImplementation(implFiles[0]);
        	if (result.isError()) {
        		showAlert("Error", "Earlier uploaded instructor java file is not proper, you need to upload correct one");
        	}
        }
    }
    
    // This will display alert dialog box with error message
	private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
	
	public void clearLabels() {	    
	    successLabel.setText("");
	    successLabel.setVisible(false);	    
	}

    private void configureSuccessLabel() {
        successLabel.getStyleClass().add("success-label");
        successLabel.setVisible(false);
    }

    private void buildLayout() {
        setSpacing(20);
        setAlignment(Pos.CENTER);
        getChildren().addAll(uploadTestsButton, uploadCorrectImplButton, showAllGradesButton, successLabel);
    }
    
    /*
     * This will display the All students grades in a separate window in tabular form.
     * */
    @SuppressWarnings("unchecked")
	private void showGradingResultsTable(List<GradingResult> gradingResults) {
        Stage resultsStage = new Stage();
        resultsStage.setTitle("All Student Grades");

        TableView<GradingResult> tableView = new TableView<>();
        tableView.setItems(FXCollections.observableList(gradingResults));

        TableColumn<GradingResult, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<GradingResult, String> studentIdColumn = new TableColumn<>("Student ID");
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        TableColumn<GradingResult, String> gradeColumn = new TableColumn<>("Grade");
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));

        TableColumn<GradingResult, Double> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));

        tableView.getColumns().addAll(idColumn, studentIdColumn, gradeColumn, scoreColumn);

        VBox resultsLayout = new VBox(tableView);
        resultsLayout.setPadding(new Insets(20));
        resultsLayout.setSpacing(10);

        Scene resultsScene = new Scene(resultsLayout, 400, 300);
        resultsStage.setScene(resultsScene);
        resultsStage.show();
    }
}
