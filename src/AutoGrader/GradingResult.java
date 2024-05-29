package AutoGrader;

public class GradingResult {
	private int id;
	private String studentId;
    private final String grade;
    private final double score;
    private final String errorMessage;

    public GradingResult(String grade, double score) {
        this.grade = grade;
        this.score = score;
        this.errorMessage = null;
    }

    public GradingResult(String errorMessage) {
        this.grade = null;
        this.score = -1;
        this.errorMessage = errorMessage;
    }
    
    public GradingResult(int id, String studentId, String grade, double score) {
        this.id = id;
        this.studentId = studentId;
        this.grade = grade;
        this.score = score;
        this.errorMessage = null;
    }
    
    public int getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
    	this.studentId = studentId;
    }

    public String getGrade() {
        return grade;
    }

    public double getScore() {
        return score;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isError() {
        return errorMessage != null;
    }
}
