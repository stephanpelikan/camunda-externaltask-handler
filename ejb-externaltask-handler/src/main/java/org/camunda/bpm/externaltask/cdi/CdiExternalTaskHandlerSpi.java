package org.camunda.bpm.externaltask.cdi;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

import org.camunda.bpm.externaltask.spi.ExternalTaskAsyncProcessingRegistration;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncRequestProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncResponseProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerSyncProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskSyncProcessingRegistration;

/**
 * We need a separate SPI bean because otherwise we could not hide all internal
 * methods: e.g. methods having @Observe must be part of the beans public API
 */
@Singleton
@Lock(LockType.READ)
@Local(ExternalTaskHandler.class)
public class CdiExternalTaskHandlerSpi implements ExternalTaskHandler {

    @EJB
    private CdiExternalTaskHandler delegate;

    @Override
    public <R, I> ExternalTaskAsyncProcessingRegistration registerExternalTaskProcessor(String processDefinitionKey,
            String topic, ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor) {
        return delegate.registerExternalTaskProcessor(processDefinitionKey, topic, requestProcessor, responseProcessor);
    }

    @Override
    public ExternalTaskSyncProcessingRegistration<ExternalTaskSyncProcessingRegistration<?>> registerExternalTaskProcessor(
            String processDefinitionKey, String topic, ExternalTaskHandlerSyncProcessor processor) {
        return delegate.registerExternalTaskProcessor(processDefinitionKey, topic, processor);
    }

    @Override
    public <R, I> R handleAsyncInput(String correlationId, I input) throws Exception {
        return delegate.handleAsyncInput(correlationId, input);
    }

    @Override
    public void setDefaultLockTimeout(long lockTimeout) {
        delegate.setDefaultLockTimeout(lockTimeout);
    }

    @Override
    public void setWorkerId(String workerId) {
        delegate.setWorkerId(workerId);
    }

}
