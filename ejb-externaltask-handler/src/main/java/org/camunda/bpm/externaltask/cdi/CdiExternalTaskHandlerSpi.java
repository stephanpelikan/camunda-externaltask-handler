package org.camunda.bpm.externaltask.cdi;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerProcessor;

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
    public void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor) {
        delegate.registerExternalTaskProcessor(processDefinitionKey, topic, processor);
    }

    @Override
    public void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, boolean fetchNoVariables) {
        delegate.registerExternalTaskProcessor(processDefinitionKey, topic, processor, fetchNoVariables);
    }

    @Override
    public void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, Long lockTimeout) {
        delegate.registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout);
    }

    @Override
    public void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, boolean fetchNoVariables, Long lockTimeout) {
        delegate.registerExternalTaskProcessor(processDefinitionKey, topic, processor, fetchNoVariables, lockTimeout);
    }

    @Override
    public void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, String firstVariableToFetch, String... variablesToFetch) {
        delegate
                .registerExternalTaskProcessor(processDefinitionKey, topic, processor, firstVariableToFetch,
                        variablesToFetch);
    }

    @Override
    public void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, Long lockTimeout, String firstVariableToFetch,
            String... variablesToFetch) {
        delegate
                .registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout,
                        firstVariableToFetch, variablesToFetch);
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
