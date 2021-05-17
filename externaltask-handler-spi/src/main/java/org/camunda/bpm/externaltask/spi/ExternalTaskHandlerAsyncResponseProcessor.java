package org.camunda.bpm.externaltask.spi;

import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;

/**
 * Used as a method signature for processing the response of asynchronous
 * communication or processing as part of an external task.
 * 
 * @param <R> The type of the response of processing the asynchronous input
 * @param <I> The type of the input given by an asynchronous processor
 */
public interface ExternalTaskHandlerAsyncResponseProcessor<R, I> {

    /**
     * @param processInstanceId The task's processInstanceId
     * @param activityId        The task's activityId
     * @param executionId       The task's executionId
     * @param retries           How many attempts left (null if first attempt)
     * @param input             The input given by an asynchronous processor
     * @param variablesToBeSet  Which variables should be set as part of completing
     *                          the task
     * @throws BpmnError If a error has to be treated as BPMN error and therefore
     *                   processed by the workflow
     * @throws Exception Any other error which will cause a Camunda incident
     * @return Any result given to the caller of
     *         {@link ExternalTaskHandler#handleAsyncInput(String, Object)}
     * @see BpmnError Can be used to indicate a BPMN error. The result to
     *      {@link ExternalTaskHandler#handleAsyncInput(String, Object)} will be
     *      null.
     * @see BpmnErrorWithVariables Can be used to indicate a BPMN error and set
     *      variables. The result to
     *      {@link ExternalTaskHandler#handleAsyncInput(String, Object)} will be
     *      null.
     * @see BpmnErrorWithResult Can be used to indicate an BPMN error and provide a
     *      result for {@link ExternalTaskHandler#handleAsyncInput(String, Object)}.
     * @see BpmnErrorWithResultAndVariables Can be used to indicate an BPMN error,
     *      set variables and provide a result for
     *      {@link ExternalTaskHandler#handleAsyncInput(String, Object)}.
     */
    R apply(String processInstanceId, String activityId, String executionId, Integer retries, String correlationId,
            I input, Map<String, Object> variablesToBeSet) throws BpmnError, Exception;

}
