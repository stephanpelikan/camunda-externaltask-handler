package org.camunda.bpm.externaltask.spi;

import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;

/**
 * Used as a method signature for processing external tasks.
 */
@FunctionalInterface
public interface ExternalTaskHandlerSyncProcessor extends ExternalTaskHandlerProcessor {

    /**
     * @param processInstanceId The task's processInstanceId
     * @param businessKey       The process' businessKey
     * @param activityId        The task's activityId
     * @param executionId       The task's executionId
     * @param variables         The variables fetched for processing
     * @param retries           How many attempts left (null if first attempt)
     * @return The variables as a result of successful processing (null for none)
     * @throws BpmnError          If a error has to be treated as BPMN error and
     *                            therefore processed by the workflow
     * @throws RetryableException Any error which should cause a retry.
     * @throws Exception          Any other error which will cause a Camunda
     *                            incident
     * @see BpmnError
     * @see BpmnErrorWithVariables
     * @see RetryableException
     */
    Map<String, Object> apply(String processInstanceId, String businessKey, String activityId, String executionId,
            Map<String, Object> variables, Integer retries) throws BpmnError, RetryableException, Exception;

}
