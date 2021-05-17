package org.camunda.bpm.externaltask.spi;

public interface ExternalTaskAsyncProcessingRegistration
        extends ExternalTaskSyncProcessingRegistration<ExternalTaskAsyncProcessingRegistration> {

    /**
     * Use this timeout for asynchronous processing. If this period passes without
     * completing the task by triggering
     * {@link ExternalTaskHandler#handleAsyncInput(String, Object)} then an incident
     * is created.
     * 
     * @param responseTimeout The period for receiving end processing asynchronous
     *                        response
     * @return the current registration for fluent API
     */
    ExternalTaskAsyncProcessingRegistration responseTimeout(Long responseTimeout);

    /**
     * A message which will be used for incidents created once a response timeout
     * occurs.
     * 
     * @see {@link ExternalTaskAsyncProcessingRegistration#responseTimeout(Long)}
     * @param responseTimeoutExpiredMessage
     * @return the current registration for fluent API
     */
    ExternalTaskAsyncProcessingRegistration responseTimeoutExpiredMessage(String responseTimeoutExpiredMessage);
    
}
