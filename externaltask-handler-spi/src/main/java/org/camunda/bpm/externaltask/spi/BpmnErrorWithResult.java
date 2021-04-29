package org.camunda.bpm.externaltask.spi;

import org.camunda.bpm.engine.delegate.BpmnError;

public class BpmnErrorWithResult extends BpmnError {

    private static final long serialVersionUID = 1L;

    private Object result;

    public BpmnErrorWithResult(String errorCode, String message, Object result, Throwable cause) {
        super(errorCode, message, cause);
        this.result = result;
    }

    public BpmnErrorWithResult(String errorCode, String message, Object result) {
        super(errorCode, message);
        this.result = result;
    }

    public BpmnErrorWithResult(String errorCode, Object result, Throwable cause) {
        super(errorCode, cause);
        this.result = result;
    }

    public BpmnErrorWithResult(String errorCode, Object result) {
        super(errorCode);
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

}
