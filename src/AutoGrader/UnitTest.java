package AutoGrader;

public class UnitTest {
    private Object[] inputs;
    private Class<?>[] inputTypes;
    private Object expectedOutput;
    private String comment;
    private String functionName;

    public UnitTest(String functionName, Object[] inputs, Class<?>[] inputTypes, Object expectedOutput, String comment) {
        this.inputs = inputs;
        this.inputTypes = inputTypes;
        this.expectedOutput = expectedOutput;
        this.comment = comment;
        this.functionName = functionName;
    }

    public Object[] getInputs() {
        return inputs;
    }

    public Class<?>[] getInputTypes() {
        return inputTypes;
    }

    public Object getExpectedOutput() {
        return expectedOutput;
    }

    public String getComment() {
        return comment;
    }
    
    public String getFunctionName() {
        return functionName;
    }
}

