package AutoGrader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.tools.ToolProvider;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.tools.JavaCompiler;

import java.util.ArrayList;
import java.net.*;

//DB Related imports
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/*
 * Main controller which provides methods 
 * 1) To parse the unit test json file
 * 2) To compile Instructor/Student implementation
 * 3) To load and execute the required classes/methods
 * 5) provides the Grade based on score
 * 6) Provides Database API's to insert/update student records
 *  
 * */
public class GraderController {

    private File studentImplementation;
    private File instructorImplementation;
    Class<?> InstructorClass;
    private File instructorUnitTests;
    private List<UnitTest> unitTests;
	

	public GraderController() {
        this.unitTests = new ArrayList<>();
    }


	// Data base API's
	private void createResultsTableIfNotExists() {
		String url = "jdbc:sqlite:grading_results/grading_results.db";

	    // Create the grading_results folder if it doesn't exist
	    File gradingResultsFolder = new File("grading_results");
	    if (!gradingResultsFolder.exists()) {
	        gradingResultsFolder.mkdir();
	    }

	    String sql = "CREATE TABLE IF NOT EXISTS results (\n"
	            + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
	            + " student_id TEXT NOT NULL UNIQUE,\n"
	            + " grade TEXT NOT NULL,\n"
	            + " score REAL NOT NULL\n"
	            + ");";

	    try (Connection conn = DriverManager.getConnection(url);
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.execute();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	public void saveResultToDatabase(GradingResult result) {
	    createResultsTableIfNotExists();

	    String url = "jdbc:sqlite:grading_results/grading_results.db";

	 // Check if the student_id already exists
	    String checkSql = "SELECT id FROM results WHERE student_id = ?";
	    Integer id = null;
	    try (Connection conn = DriverManager.getConnection(url);
	         PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
	        pstmt.setString(1, result.getStudentId());
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            id = rs.getInt("id");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    if (id == null) {
	        // Insert new record
	        String insertSql = "INSERT INTO results(student_id, grade, score) VALUES(?, ?, ?)";
	        try (Connection conn = DriverManager.getConnection(url);
	             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
	            pstmt.setString(1, result.getStudentId());
	            pstmt.setString(2, result.getGrade());
	            pstmt.setDouble(3, result.getScore());
	            pstmt.executeUpdate();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    } else {
	        // Update existing record
	        String updateSql = "UPDATE results SET grade = ?, score = ? WHERE id = ?";
	        try (Connection conn = DriverManager.getConnection(url);
	             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
	            pstmt.setString(1, result.getGrade());
	            pstmt.setDouble(2, result.getScore());
	            pstmt.setInt(3, id);
	            pstmt.executeUpdate();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	public List<GradingResult> getAllGradingResults() {
	    createResultsTableIfNotExists();

	    String url = "jdbc:sqlite:grading_results/grading_results.db";
	    String sql = "SELECT id, student_id, grade, score FROM results";
	    List<GradingResult> gradingResults = new ArrayList<>();

	    try (Connection conn = DriverManager.getConnection(url);
	         Statement stmt = conn.createStatement();
	         ResultSet rs = stmt.executeQuery(sql)) {

	        while (rs.next()) {
	            int id = rs.getInt("id");
	            String studentId = rs.getString("student_id");
	            String grade = rs.getString("grade");
	            double score = rs.getDouble("score");
	            gradingResults.add(new GradingResult(id, studentId, grade, score));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return gradingResults;
	}
	
	
	// This will just update the new file provided by student
    public void setStudentImplementation(File studentImplementation) {
        this.studentImplementation = studentImplementation;
    }

    // This will compile the new implementation file uploaded by instructor
    public GradingResult setInstructorImplementation(File instructorImplementation) {
        this.instructorImplementation = instructorImplementation;
        InstructorClass = compileAndLoadClass(instructorImplementation);
        if (InstructorClass == null) {
    		return new GradingResult("Compilation failed");
    	}
        // Dummy return, ignore the grade and score's here.
        return new GradingResult("A", 100);
    }

    // This will load and parse the json file containing unit tests provided by Instructor.
    public boolean setInstructorUnitTests(File instructorUnitTests) {
    	this.unitTests = new ArrayList<>();
        this.instructorUnitTests = instructorUnitTests;
        //loadUnitTests();
        return loadMultipleMethodsUnitTests();
    }
    
    // This parses the provided json and fills internal unit test structure
    // TODO: Handle null checks for each key in json to show appropriate error message to user if required key is not present in file.
    @SuppressWarnings("resource")
	private boolean loadMultipleMethodsUnitTests() {
    	try {
    	    JsonObject jsonObject = new Gson().fromJson(new FileReader(instructorUnitTests), JsonObject.class);
    	    JsonArray functions = jsonObject.get("functions").getAsJsonArray();
    	    for (JsonElement functionElement : functions) {
    	        JsonObject functionObject = functionElement.getAsJsonObject();
    	        String functionName = functionObject.get("functionName").getAsString();
    	        JsonArray tests = functionObject.get("tests").getAsJsonArray();
    	        for (JsonElement testElement : tests) {
    	            JsonObject testObject = testElement.getAsJsonObject();
    	            JsonArray inputsArray = testObject.get("inputs").getAsJsonArray();
    	            JsonArray inputTypesArray = testObject.get("inputTypes").getAsJsonArray();
    	            String comment = testObject.get("comment").getAsString();
    	            Object[] inputs = new Object[inputsArray.size()];
    	            Class<?>[] inputTypes = new Class<?>[inputTypesArray.size()];
    	            for (int i = 0; i < inputTypesArray.size(); i++) {
    	                String className = inputTypesArray.get(i).getAsString();
    	                switch (className) {
    	                    case "int":
    	                        inputTypes[i] = int.class;
    	                        inputs[i] = new Gson().fromJson(inputsArray.get(i), int.class);
    	                        break;
    	                    case "long":
    	                        inputTypes[i] = long.class;
    	                        inputs[i] = new Gson().fromJson(inputsArray.get(i), long.class);
    	                        break;
    	                    case "double":
    	                        inputTypes[i] = double.class;
    	                        inputs[i] = new Gson().fromJson(inputsArray.get(i), double.class);
    	                        break;
    	                    case "str":
    	                    	inputTypes[i] = String.class;
    	                        inputs[i] = inputsArray.get(i).getAsString();
    	                        break;
    	                    default:
    	                        try {
    	                            inputTypes[i] = Class.forName(className);
    	                            inputs[i] = new Gson().fromJson(inputsArray.get(i), inputTypes[i]);
    	                        } catch (ClassNotFoundException e) {
    	                            e.printStackTrace();
    	                            return false;
    	                        }
    	                }
    	            }
    	            //Object expectedOutput = testObject.get("expectedOutput").getAsJsonPrimitive().getAsString();
    	            unitTests.add(new UnitTest(functionName, inputs, inputTypes, null, comment));
    	        }
    	    }
    	} catch (FileNotFoundException e) {
    	    e.printStackTrace();
    	    return false;
    	} catch (JsonSyntaxException e) {
            e.printStackTrace();
            System.err.println("Invalid JSON file format");
            return false;
        }
    	return true;
    }    
    
    // This will find the method in loaded class and invoke it with the given inputs.
    public Object findAndInvokeMethod(Class<?> loadedClass, String methodName, Class<?>[] inputTypes, Object[] inputs) throws Exception {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Object> task = () -> {
            try {                
                Method method = loadedClass.getDeclaredMethod(methodName, inputTypes);
                method.setAccessible(true);
                
                Object instance = null;
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    instance = loadedClass.getDeclaredConstructor().newInstance();
                }
                Object result = method.invoke(instance, inputs);
                return result;
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
                throw e;
            }
        };

        Object result = null;
        try {
            Future<Object> future = executorService.submit(task);
            result = future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            executorService.shutdownNow();
        }

        return result;
    }
    
//    public Object findAndInvokeMethod(Class<?> loadedClass, String methodName, Class<?>[] inputTypes, Object[] inputs) throws ReflectiveOperationException {
//    	
//    	try {
//    	
//        Method method = loadedClass.getDeclaredMethod(methodName, inputTypes);
//        method.setAccessible(true);
//
//        
//        Object instance = null;
//        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
//            instance = loadedClass.getDeclaredConstructor().newInstance();
//        }
//        Object result = method.invoke(instance, inputs);        
//        return result;
//    	} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
//            e.printStackTrace();
//            throw e;
//        }    	
//    }
    
    // This compiles the given java file
    // You need to use JDK for getSystemJavaCompiler to work
    public static Class<?> compileAndLoadClass(File javaFile) {
        // Compile the Java file
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int compilationResult = compiler.run(null, null, null, javaFile.getPath());

        if (compilationResult != 0) {
            //System.out.println("Compilation failed.");
            return null;
        }

        //File javaFile = new File(javaFilePath);
        File parentDir = javaFile.getParentFile();

        try {
            URL[] urls = new URL[]{parentDir.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls);
            String className = javaFile.getName().replace(".java", "");

            Class<?> loadedClass = classLoader.loadClass(className);
            
            return loadedClass;           

        } catch (MalformedURLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
		return null;
    }
    
    private String calculateGrade(double percentScore) {
        if (percentScore >= 90) {
            return "A";
        } else if (percentScore >= 80) {
            return "B";
        } else if (percentScore >= 70) {
            return "C";
        } else if (percentScore >= 60) {
            return "D";
        } else {
            return "F";
        }
    }
    
	// Main method to get the student grade on user clicking submit button in UI.
    // This will invoke both student and instructor methods and compare the results to calculate grade
	public GradingResult gradeStudentImplementation() {
		if (studentImplementation == null || instructorImplementation == null || unitTests == null || InstructorClass == null) {
            return new GradingResult("Missing required files.");
        }
        try {
            // Compile the student implementation
        	Class<?> studentClass = compileAndLoadClass(studentImplementation);
        	if (studentClass == null) {
        		return new GradingResult("Compilation failed");
        	}        	
            // Evaluate the student implementation against the unit tests            
            int totalTests = unitTests.size();
            int totalPassed = 0;
            for (UnitTest test : unitTests) {
                Object[] inputs = test.getInputs();
                Class<?>[] inputTypes = test.getInputTypes();
                String functionName = test.getFunctionName();
                try {
	                Object expectedOutput = findAndInvokeMethod(InstructorClass, functionName, inputTypes, inputs);
	                Object actualOutput = findAndInvokeMethod(studentClass, functionName, inputTypes, inputs);
	                boolean testPassed = expectedOutput.toString().equals(actualOutput.toString());
	                if (testPassed) {
	                    totalPassed++;
	                }
                } catch (Exception e) {
                	return new GradingResult("Exception occured while invoking the methods");
                }
            }
            // Calculate the final score and grade
            double percentScore = ((double) totalPassed / totalTests) * 100.0;
            String grade = calculateGrade(percentScore);

            return new GradingResult(grade, percentScore);
        } catch (Exception e) {
            return new GradingResult(e.getMessage());
        }
    }
}
