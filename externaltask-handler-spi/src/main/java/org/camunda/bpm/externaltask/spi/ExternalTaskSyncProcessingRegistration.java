package org.camunda.bpm.externaltask.spi;

import java.util.List;

import org.camunda.feel.syntaxtree.If;

public interface ExternalTaskSyncProcessingRegistration<T extends ExternalTaskSyncProcessingRegistration<?>> {

    /**
     * Use this lock timeout for external tasks.
     * 
     * @see If a task will not be processed within this period then it is considered
     *      as not processed and restarted.
     * @param lockTimeout The external task's lock timeout
     * @return the current registration for fluent API
     */
    T lockTimeout(Long lockTimeout);

    /**
     * Fetch only the variables here given on task execution.
     * 
     * @param variables Variables to be fetched
     * @return the current registration for fluent API
     */
    T variablesToFetch(List<String> variables);

    /**
     * Fetch only the variables here given on task execution.
     * 
     * @param variables Variables to be fetched
     * @return the current registration for fluent API
     */
    T variablesToFetch(String... variables);

    /**
     * Do not fetch any variables. If neither this method nor
     * {@link ExternalTaskSyncProcessingRegistration#variablesToFetch(List)} or
     * {@link ExternalTaskSyncProcessingRegistration#variablesToFetch(String...))}
     * is used then all variables will be fetch.
     * 
     * @return the current registration for fluent API
     */
    T fetchNoVariables();

}
