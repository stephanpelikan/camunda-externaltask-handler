package org.camunda.bpm.externaltask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithResult;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithResultAndVariables;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithVariables;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncRequestProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncResponseProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerSyncProcessor;
import org.camunda.bpm.externaltask.spi.RetryableException;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

public abstract class ExternalTaskHandlerImpl implements ExternalTaskHandler {
    
    protected abstract long getDefaultLockTimeout();

    protected abstract ExternalTaskService getExternalTaskService();

    protected abstract String getWorkerId();

    protected abstract void doAfterTransaction(Runnable action);
    
    protected abstract void processAsynchronously(Runnable action);
    
    protected abstract void scheduleFetchAndLockExternalTasks(long timeout, String key);
    
    protected Map<String, Long> customTimeouts = new HashMap<>();

    protected Map<String, ExternalTaskHandlerProcessor> processors = new HashMap<>();

    protected Map<String, ExternalTaskHandlerAsyncResponseProcessor<Object, Object>> asyncResponseProcessors = new HashMap<>();

    protected Map<String, List<String>> variablesToBeFetch = new HashMap<>();

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerSyncProcessor processor) {

        registerExternalTaskProcessor(processDefinitionKey, topic, processor, (Long) null, false, null);

    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerSyncProcessor processor, final boolean fetchNoVariables) {

        registerExternalTaskProcessor(processDefinitionKey, topic, processor, (Long) null, fetchNoVariables, null);

    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerSyncProcessor processor, final Long lockTimeout) {

        registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout, false, null);

    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerSyncProcessor processor, final boolean fetchNoVariables, final Long lockTimeout) {

        registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout, fetchNoVariables, null);

    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerSyncProcessor processor, final String firstVariableToFetch,
            final String... variablesToFetch) {

        final List<String> variableNames = new LinkedList<>();
        variableNames.add(firstVariableToFetch);
        if (variablesToFetch != null) {
            Arrays.stream(variablesToFetch).forEach(variableNames::add);
        }

        registerExternalTaskProcessor(processDefinitionKey, topic, processor, null, false, variableNames);

    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerSyncProcessor processor, final Long lockTimeout,
            final String firstVariableToFetch, final String... variablesToFetch) {

        final List<String> variableNames = new LinkedList<>();
        variableNames.add(firstVariableToFetch);
        if (variablesToFetch != null) {
            Arrays.stream(variablesToFetch).forEach(variableNames::add);
        }

        registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout, false, variableNames);

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, I> void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor) {

        registerExternalTaskProcessor(processDefinitionKey, topic, requestProcessor, (Long) null, false, null);
        registerExternalTaskAsyncResponseProcessor(processDefinitionKey,
                topic,
                (ExternalTaskHandlerAsyncResponseProcessor<Object, Object>) responseProcessor);

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, I> void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, final boolean fetchNoVariables) {

        registerExternalTaskProcessor(processDefinitionKey,
                topic,
                requestProcessor,
                (Long) null,
                fetchNoVariables,
                null);
        registerExternalTaskAsyncResponseProcessor(processDefinitionKey,
                topic,
                (ExternalTaskHandlerAsyncResponseProcessor<Object, Object>) responseProcessor);

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, I> void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, final boolean fetchNoVariables,
            final Long lockTimeout) {

        registerExternalTaskProcessor(processDefinitionKey,
                topic,
                requestProcessor,
                lockTimeout,
                fetchNoVariables,
                null);
        registerExternalTaskAsyncResponseProcessor(processDefinitionKey,
                topic,
                (ExternalTaskHandlerAsyncResponseProcessor<Object, Object>) responseProcessor);

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, I> void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, final Long lockTimeout) {

        registerExternalTaskProcessor(processDefinitionKey, topic, requestProcessor, lockTimeout, false, null);
        registerExternalTaskAsyncResponseProcessor(processDefinitionKey,
                topic,
                (ExternalTaskHandlerAsyncResponseProcessor<Object, Object>) responseProcessor);

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, I> void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, final String firstVariableToFetch,
            final String... variablesToFetch) {

        final LinkedList<String> variableNames = new LinkedList<>();
        variableNames.add(firstVariableToFetch);
        if (variablesToFetch != null) {
            Arrays.stream(variablesToFetch).forEach(variableNames::add);
        }

        registerExternalTaskProcessor(processDefinitionKey, topic, requestProcessor, null, false, variableNames);
        registerExternalTaskAsyncResponseProcessor(processDefinitionKey,
                topic,
                (ExternalTaskHandlerAsyncResponseProcessor<Object, Object>) responseProcessor);

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, I> void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, final Long lockTimeout,
            final String firstVariableToFetch, final String... variablesToFetch) {

        final LinkedList<String> variableNames = new LinkedList<>();
        variableNames.add(firstVariableToFetch);
        if (variablesToFetch != null) {
            Arrays.stream(variablesToFetch).forEach(variableNames::add);
        }

        registerExternalTaskProcessor(processDefinitionKey, topic, requestProcessor, lockTimeout, false, variableNames);
        registerExternalTaskAsyncResponseProcessor(processDefinitionKey,
                topic,
                (ExternalTaskHandlerAsyncResponseProcessor<Object, Object>) responseProcessor);

    }

    private void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerProcessor processor, final Long lockTimeout, final boolean fetchNoVariables,
            final List<String> variablesToFetch) {

        final String key = getInternalKey(processDefinitionKey, topic);

        if (lockTimeout != null) {
            customTimeouts.put(key, lockTimeout);
        }

        if (fetchNoVariables) {
            this.variablesToBeFetch.put(key, new LinkedList<>());
        } else {
            this.variablesToBeFetch.put(key, variablesToFetch);
        }

        processors.put(key, processor);

    }
    
    private void registerExternalTaskAsyncResponseProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerAsyncResponseProcessor<Object, Object> processor) {

        final String key = getInternalKey(processDefinitionKey, topic);

        asyncResponseProcessors.put(key, processor);

    }

    /**
     * Listen for Camunda events "create a task" and check if configured external
     * task topic is registered. If is registered then fetch and lock the external
     * task and call the registered method.
     * 
     * @param execution Camunda's delegate execution
     */
    protected void onTaskEvent(final String processDefinitionKey, final FlowElement bpmnElement) {

        final String topic = getTopic(bpmnElement);
        if (topic == null) {
            return; // an activity which has not an external task implementation
        }

        final String key = getInternalKey(processDefinitionKey, topic);
        if (!processors.containsKey(key)) {
            return; // a topic not yet registered
        }

        /*
         * Fetch and lock needs to be done in a separate transaction. Additionally at
         * the moment of task creation the External Task is not yet visible to other
         * threads and we need wait for the current transaction to complete.
         */
        doAfterTransaction(() ->
                processAsynchronously(() ->
                        fetchAndLockExternalTasks(key)));

    }
    
    /*
     * Fetch and lock any external tasks available - usually only the current one
     * if called in the context of a task event processing.
     */
    protected void fetchAndLockExternalTasks(final String key) {
        
        final String topic = getTopicFromInternalKey(key);
        
        final List<LockedExternalTask> externalTasks = getExternalTaskService()
                .fetchAndLock(Integer.MAX_VALUE, getWorkerId())
                .topic(topic, getLockTimeout(key))
                .variables(variablesToBeFetch.get(key))
                .execute();
        
        if ((externalTasks == null) || externalTasks.isEmpty()) {
            return;
        }
        
        /*
         * Processing each task needs to be done in a separate transaction because
         * completing the external task might be faster than committing the transaction
         * of "fetchAndLock". Additionally at the moment of task locking this status
         * is not yet visible to other threads and we need wait for the current
         * transaction to complete. 
         */
        doAfterTransaction(() ->
                externalTasks.forEach(task ->
                        processAsynchronously(() -> 
                                runRegisteredProcessor(
                                        task.getProcessDefinitionKey(),
                                        task.getTopicName(),
                                        task.getId(),
                                        task.getProcessInstanceId(),
                                        task.getActivityId(),
                                        task.getExecutionId(),
                                        task.getVariables(),
                                        task.getRetries()))));

    }

    private void runRegisteredProcessor(final String processDefinitionKey, final String topic,
            final String externalTaskId, final String processInstanceId,
            final String activityId, final String executionId,
            final Map<String, Object> variables, final Integer retries) {

        final String key = getInternalKey(processDefinitionKey, topic);
        final String workerId = getWorkerId();
        
        try {
            final ExternalTaskHandlerProcessor processor = processors.get(key);
            if (processor instanceof ExternalTaskHandlerSyncProcessor) {
                final Map<String, Object> variablesToBeSet = ((ExternalTaskHandlerSyncProcessor) processor)
                        .apply(processInstanceId, activityId, executionId, variables, retries);
                getExternalTaskService().complete(externalTaskId, workerId, variablesToBeSet);
            } else {
                ((ExternalTaskHandlerAsyncRequestProcessor) processor)
                        .apply(externalTaskId, processInstanceId, activityId, executionId, variables, retries);
            }
        } catch (BpmnErrorWithVariables e) {
            getExternalTaskService().handleBpmnError(externalTaskId, workerId, e.getErrorCode(), e.getMessage(), e.getVariables());
        } catch (BpmnError e) {
            getExternalTaskService().handleBpmnError(externalTaskId, workerId, e.getErrorCode(), e.getMessage());
        } catch (RetryableException e) {
            getExternalTaskService().handleFailure(externalTaskId, workerId,
                    e.getMessage(), buildIncidentDetails(e), e.getRetries(), e.getRetryTimeout());
            if (e.getRetries() > 0) {
                doAfterTransaction(() ->
                        scheduleFetchAndLockExternalTasks(e.getRetryTimeout(), key));
            }
        } catch (Exception e) {
            getExternalTaskService().handleFailure(externalTaskId, workerId,
                    e.getMessage(), buildIncidentDetails(e), 0, 0);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, I> R handleAsyncInput(String correlationId, I input) throws Exception {

        final ExternalTask externalTask = getExternalTaskService()
                .createExternalTaskQuery()
                .externalTaskId(correlationId)
                .singleResult();

        final String key = getInternalKey(externalTask.getProcessDefinitionKey(), externalTask.getTopicName());
        final Map<String, Object> variablesToBeSet = new HashMap<>();
        R result = null;
        try {
            result = (R) asyncResponseProcessors
                    .get(key)
                    .apply(externalTask.getProcessInstanceId(),
                            externalTask.getActivityId(),
                            externalTask.getExecutionId(),
                            externalTask.getRetries(),
                            correlationId,
                            input,
                            variablesToBeSet);
        } catch (BpmnErrorWithResultAndVariables e) {
            getExternalTaskService()
                    .handleBpmnError(correlationId, getWorkerId(), e.getErrorCode(), e.getMessage(), e.getVariables());
            return (R) e.getResult();
        } catch (BpmnErrorWithResult e) {
            getExternalTaskService().handleBpmnError(correlationId, getWorkerId(), e.getErrorCode(), e.getMessage());
            return (R) e.getResult();
        } catch (BpmnErrorWithVariables e) {
            getExternalTaskService()
                    .handleBpmnError(correlationId, getWorkerId(), e.getErrorCode(), e.getMessage(), e.getVariables());
            return null;
        } catch (BpmnError e) {
            getExternalTaskService().handleBpmnError(correlationId, getWorkerId(), e.getErrorCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            throw e;
        }

        try {
            getExternalTaskService().complete(correlationId, getWorkerId(), variablesToBeSet);
        } catch (Exception e) {
            getExternalTaskService()
                    .handleFailure(correlationId, getWorkerId(), e.getMessage(), buildIncidentDetails(e), 0, 0);
        }

        return result;

    }

    @SuppressWarnings("boxing")
    private long getLockTimeout(final String key) {

        return customTimeouts.getOrDefault(key, getDefaultLockTimeout()).longValue();

    }
    
    private static String getInternalKey(final String processDefinitionKey, final String topic) {

        return processDefinitionKey + "#" + topic;

    }
    
    private static String getTopicFromInternalKey(final String key) {

        return key.substring(key.indexOf('#') + 1);

    }
    
    private static String buildIncidentDetails(final Exception e) {
        
        try (final StringWriter result = new StringWriter()) {
            try (final PrintWriter writer = new PrintWriter(result)) {
                e.printStackTrace(writer);
                return result.toString();
            }
        } catch (Exception ie) {
            throw new RuntimeException("Could not close writer used for building incident details", ie);
        }
        
    }

    static String getTopic(final FlowElement bpmnElement) {

        if (bpmnElement instanceof ServiceTask) {
            return ((ServiceTask) bpmnElement).getCamundaTopic();
        } else if (bpmnElement instanceof SendTask) {
            return ((SendTask) bpmnElement).getCamundaTopic();
        } else if (bpmnElement instanceof MessageEventDefinition) {
            return ((MessageEventDefinition) bpmnElement).getCamundaTopic();
        } else if (bpmnElement instanceof BusinessRuleTask) {
            return ((BusinessRuleTask) bpmnElement).getCamundaTopic();
        } else {
            return null;
        }

    }

}
