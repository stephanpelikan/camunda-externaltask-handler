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
import org.camunda.bpm.externaltask.spi.BpmnErrorWithVariables;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerProcessor;
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

    protected Map<String, List<String>> variablesToBeFetch = new HashMap<>();

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerProcessor processor) {
        registerExternalTaskProcessor(processDefinitionKey, topic, processor, (Long) null, false, null);
    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerProcessor processor, final boolean fetchNoVariables) {
        registerExternalTaskProcessor(processDefinitionKey, topic, processor, (Long) null, fetchNoVariables, null);
    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerProcessor processor, final Long lockTimeout) {
        registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout, false, null);
    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerProcessor processor, final boolean fetchNoVariables, final Long lockTimeout) {
        registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout, fetchNoVariables, null);
    }

    @Override
    public void registerExternalTaskProcessor(final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerProcessor processor, final String firstVariableToFetch,
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
            final ExternalTaskHandlerProcessor processor, final Long lockTimeout,
            final String firstVariableToFetch, final String... variablesToFetch) {
        final List<String> variableNames = new LinkedList<>();
        variableNames.add(firstVariableToFetch);
        if (variablesToFetch != null) {
            Arrays.stream(variablesToFetch).forEach(variableNames::add);
        }
        registerExternalTaskProcessor(processDefinitionKey, topic, processor, lockTimeout, false, variableNames);
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
         * threads and we need wait for the current transaction to complete. This is
         * done by sending an event which is handled by a transaction listener.
         */
        doAfterTransaction(() -> processAsynchronously(() -> fetchAndLockExternalTasks(key)));

    }
    
    /*
     * Fetch and lock any external tasks available - usually only the current one
     * if called in the context of a task event processing.
     */
    protected void fetchAndLockExternalTasks(final String key) {
        
        final String topic = getTopicFromInternalKey(key);
        
        getExternalTaskService()
                .fetchAndLock(Integer.MAX_VALUE, getWorkerId())
                .topic(topic, getLockTimeout(key))
                .variables(variablesToBeFetch.get(key))
                .execute()
                .forEach(task ->
                    processAsynchronously(() -> {
                        runRegisteredProcessor(task.getProcessDefinitionKey(), task.getTopicName(), task.getId(),
                                task.getProcessInstanceId(), task.getActivityId(), task.getExecutionId(),
                                task.getVariables(), task.getRetries());
                    }));

    }

    private void runRegisteredProcessor(final String processDefinitionKey, final String topic,
            final String externalTaskId, final String processInstanceId,
            final String activityId, final String executionId,
            final Map<String, Object> variables, final Integer retries) {

        final String key = getInternalKey(processDefinitionKey, topic);
        final String workerId = getWorkerId();
        
        try {
            final Map<String, Object> variablesToBeSet = processors
                    .get(key)
                    .apply(processInstanceId, activityId, executionId, variables, retries);
            getExternalTaskService().complete(externalTaskId, workerId, variablesToBeSet);
        } catch (BpmnErrorWithVariables e) {
            getExternalTaskService().handleBpmnError(externalTaskId, workerId, e.getErrorCode(), e.getMessage(), e.getVariables());
        } catch (BpmnError e) {
            getExternalTaskService().handleBpmnError(externalTaskId, workerId, e.getErrorCode(), e.getMessage());
        } catch (RetryableException e) {
            getExternalTaskService().handleFailure(externalTaskId, workerId,
                    e.getMessage(), buildIncidentDetails(e), e.getRetries(), e.getRetryTimeout());
            if (e.getRetries() > 0) {
                doAfterTransaction(() -> scheduleFetchAndLockExternalTasks(e.getRetryTimeout(), key));
            }
        } catch (Exception e) {
            getExternalTaskService().handleFailure(externalTaskId, workerId,
                    e.getMessage(), buildIncidentDetails(e), 0, 0);
        }

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
