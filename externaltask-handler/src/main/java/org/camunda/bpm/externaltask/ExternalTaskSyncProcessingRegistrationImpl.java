package org.camunda.bpm.externaltask;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskSyncProcessingRegistration;

public class ExternalTaskSyncProcessingRegistrationImpl<T extends ExternalTaskSyncProcessingRegistration<?>>
        implements ExternalTaskSyncProcessingRegistration<T> {

    private Long lockTimeout;

    private ExternalTaskHandlerProcessor processor;

    private List<String> variablesToFetch;

    ExternalTaskSyncProcessingRegistrationImpl(final ExternalTaskHandlerProcessor processor) {
        this.processor = processor;
    }

    ExternalTaskHandlerProcessor getProcessor() {
        return processor;
    }

    public Long getLockTimeout() {
        return lockTimeout;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T lockTimeout(Long lockTimeout) {
        this.lockTimeout = lockTimeout;
        return (T) this;
    }

    public List<String> getVariablesToFetch() {
        return variablesToFetch;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T variablesToFetch(List<String> variables) {
        this.variablesToFetch = variables;
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T variablesToFetch(String... variables) {
        this.variablesToFetch = new LinkedList<>();
        Arrays.stream(variables).forEach(variablesToFetch::add);
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T fetchNoVariables() {
        this.variablesToFetch = new LinkedList<>();
        return (T) this;
    }

}
