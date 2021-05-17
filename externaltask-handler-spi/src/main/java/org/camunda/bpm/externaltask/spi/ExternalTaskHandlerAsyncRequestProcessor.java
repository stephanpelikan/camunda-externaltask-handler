package org.camunda.bpm.externaltask.spi;

import java.util.Date;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;

/**
 * Used as a method signature for processing the external tasks which do
 * requests of asynchronous communication or processing.
 */
@FunctionalInterface
public interface ExternalTaskHandlerAsyncRequestProcessor extends ExternalTaskHandlerProcessor {

    /**
     * @param processInstanceId The task's processInstanceId
     * @param activityId        The task's activityId
     * @param executionId       The task's executionId
     * @param variables         The variables fetched for processing
     * @param retries           How many attempts left (null if first attempt)
     * @throws BpmnError          If a error has to be treated as BPMN error and
     *                            therefore processed by the workflow
     * @throws RetryableException Any error which should cause a retry.
     * @throws Exception          Any other error which will cause a Camunda
     *                            incident
     * @return Can be used to override
     *         {@link ExternalTaskAsyncProcessingRegistration#responseTimeout(Long)}
     *         per request otherwise null
     * @see BpmnError
     * @see BpmnErrorWithVariables
     */
    Date apply(String correlationId, String processInstanceId, String activityId, String executionId,
            Map<String, Object> variables, Integer retries) throws BpmnError, RetryableException, Exception;

}
