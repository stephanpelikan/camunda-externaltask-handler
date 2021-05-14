package org.camunda.bpm.externaltask.spi;

import java.util.List;

public interface ExternalTaskSyncProcessingRegistration<T extends ExternalTaskSyncProcessingRegistration<?>> {

    T lockTimeout(Long lockTimeout);

    T variablesToFetch(List<String> variables);

    T variablesToFetch(String... variables);

    T fetchNoVariables();

}
