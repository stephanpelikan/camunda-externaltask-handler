package org.camunda.bpm.externaltask.spi;

public interface ExternalTaskAsyncProcessingRegistration
        extends ExternalTaskSyncProcessingRegistration<ExternalTaskAsyncProcessingRegistration> {

    ExternalTaskAsyncProcessingRegistration responseTimeout(Long responseTimeout);
    
    ExternalTaskAsyncProcessingRegistration responseTimeoutExpiredMessage(String responseTimeoutExpiredMessage);
    
    ExternalTaskAsyncProcessingRegistration incidentResolutionVariableName(String incidentResolutionVariableName);
    
}
