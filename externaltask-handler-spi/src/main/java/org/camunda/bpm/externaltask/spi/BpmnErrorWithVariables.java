package org.camunda.bpm.externaltask.spi;

import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;

public class BpmnErrorWithVariables extends BpmnError {

    private static final long serialVersionUID = 1L;
    
    private Map<String, Object> variables;
    
    public BpmnErrorWithVariables(String errorCode, String message, Map<String, Object> variables, Throwable cause) {
        super(errorCode, message, cause);
        this.variables = variables;
    }

    public BpmnErrorWithVariables(String errorCode, String message, Map<String, Object> variables) {
        super(errorCode, message);
        this.variables = variables;
    }

    public BpmnErrorWithVariables(String errorCode, Map<String, Object> variables, Throwable cause) {
        super(errorCode, cause);
        this.variables = variables;
    }

    public BpmnErrorWithVariables(String errorCode, Map<String, Object> variables) {
        super(errorCode);
        this.variables = variables;
    }
    
    public Map<String, Object> getVariables() {
        return variables;
    }

}
