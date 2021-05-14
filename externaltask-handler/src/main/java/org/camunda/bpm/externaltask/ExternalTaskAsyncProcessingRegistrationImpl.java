package org.camunda.bpm.externaltask;

import org.camunda.bpm.externaltask.spi.ExternalTaskAsyncProcessingRegistration;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncRequestProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncResponseProcessor;

public class ExternalTaskAsyncProcessingRegistrationImpl<R, I>
        extends ExternalTaskSyncProcessingRegistrationImpl<ExternalTaskAsyncProcessingRegistration>
        implements ExternalTaskAsyncProcessingRegistration {

    private final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor;

    private Long responseTimeout;

    private String responseTimeoutExpiredMessage;

    private String incidentResolutionVariableName;

    ExternalTaskAsyncProcessingRegistrationImpl(
            final ExternalTaskHandlerAsyncRequestProcessor processor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor) {

        super(processor);
        this.responseProcessor = responseProcessor;

    }

    ExternalTaskHandlerAsyncResponseProcessor<R, I> getResponseProcessor() {
        return responseProcessor;
    }

    public Long getResponseTimeout() {
        return responseTimeout;
    }

    @Override
    public ExternalTaskAsyncProcessingRegistrationImpl<R, I> responseTimeout(Long responseTimeout) {
        this.responseTimeout = responseTimeout;
        return this;
    }

    public String getResponseTimeoutExpiredMessage() {
        return responseTimeoutExpiredMessage;
    }

    @Override
    public ExternalTaskAsyncProcessingRegistrationImpl<R, I> responseTimeoutExpiredMessage(
            String responseTimeoutExpiredMessage) {
        this.responseTimeoutExpiredMessage = responseTimeoutExpiredMessage;
        return this;
    }

    public String getIncidentResolutionVariableName() {
        return incidentResolutionVariableName;
    }

    @Override
    public ExternalTaskAsyncProcessingRegistrationImpl<R, I> incidentResolutionVariableName(
            String incidentResolutionVariableName) {
        this.incidentResolutionVariableName = incidentResolutionVariableName;
        return this;
    }

}
