package org.camunda.bpm.externaltask.spi;

import java.util.Map;

public class BpmnErrorWithResultAndVariables extends BpmnErrorWithVariables {

    private static final long serialVersionUID = 1L;
    
    private Object result;
    
    public BpmnErrorWithResultAndVariables(String errorCode, String message, Object result,
            Map<String, Object> variables, Throwable cause) {
        super(errorCode, message, variables, cause);
        this.result = result;
    }

    public BpmnErrorWithResultAndVariables(String errorCode, String message, Object result,
            Map<String, Object> variables) {
        super(errorCode, message, variables);
        this.result = result;
    }

    public BpmnErrorWithResultAndVariables(String errorCode, Object result, Map<String, Object> variables,
            Throwable cause) {
        super(errorCode, variables, cause);
        this.result = result;
    }

    public BpmnErrorWithResultAndVariables(String errorCode, Object result, Map<String, Object> variables) {
        super(errorCode, variables);
        this.result = result;
    }
    
    public Object getResult() {
        return result;
    }

}
