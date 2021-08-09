package org.camunda.bpm.externaltask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandler;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithResult;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithResultAndVariables;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithVariables;
import org.camunda.bpm.externaltask.spi.ExternalTaskAsyncProcessingRegistration;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncRequestProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerAsyncResponseProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandlerSyncProcessor;
import org.camunda.bpm.externaltask.spi.ExternalTaskSyncProcessingRegistration;
import org.camunda.bpm.externaltask.spi.RetryableException;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExternalTaskHandlerImpl
        implements ExternalTaskHandler, JobHandler<AsyncProcessorTimeoutJobHandlerConfiguration> {

    private static Logger logger = LoggerFactory.getLogger(ExternalTaskHandlerImpl.class);

    static final String ASYNC_TIMEOUT_HANDLER_TYPE = ExternalTaskHandlerImpl.class.getName();

    protected abstract long getDefaultLockTimeout();

    protected abstract ExternalTaskService getExternalTaskService();
    
    protected abstract RuntimeService getRuntimeService();

    protected abstract ProcessEngineConfigurationImpl getProcessEngineConfiguration();

    protected abstract String getWorkerId();

    protected abstract void doAfterTransaction(Runnable action);
    
    protected abstract void processAsynchronously(Runnable action);
    
    protected abstract void scheduleFetchAndLockExternalTasks(long timeout, String key);
    
    protected Map<String, ExternalTaskSyncProcessingRegistrationImpl<?>> registrations = new HashMap<>();

    @Override
    public ExternalTaskSyncProcessingRegistration<ExternalTaskSyncProcessingRegistration<?>> registerExternalTaskProcessor(
            final String processDefinitionKey, final String topic,
            final ExternalTaskHandlerSyncProcessor processor) {

        final ExternalTaskSyncProcessingRegistrationImpl<ExternalTaskSyncProcessingRegistration<?>> registration
                = new ExternalTaskSyncProcessingRegistrationImpl<>(processor);
        registration.lockTimeout(getDefaultLockTimeout());

        final String key = getInternalKey(processDefinitionKey, topic);

        registrations.put(key, registration);

        return registration;

    }

    @Override
    public <R, I> ExternalTaskAsyncProcessingRegistration registerExternalTaskProcessor(
            final String processDefinitionKey,
            final String topic,
            final ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            final ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor) {

        final ExternalTaskAsyncProcessingRegistrationImpl<R, I> registration
                = new ExternalTaskAsyncProcessingRegistrationImpl<>(requestProcessor, responseProcessor);
        registration.lockTimeout(getDefaultLockTimeout());

        final String key = getInternalKey(processDefinitionKey, topic);

        registrations.put(key, registration);

        return registration;

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
        if (!registrations.containsKey(key)) {
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
        final String processDefinitionKey = getProcessDefinitionKeyFromInternalKey(key);
        final ExternalTaskSyncProcessingRegistrationImpl<?> registration = registrations.get(key);
        
        final List<LockedExternalTask> externalTasks = getExternalTaskService()
                .fetchAndLock(Integer.MAX_VALUE, getWorkerId())
                .topic(topic, registration.getLockTimeout())
                .processDefinitionKey(processDefinitionKey)
                .variables(registration.getVariablesToFetch())
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
                                        task.getBusinessKey(),
                                        task.getProcessInstanceId(),
                                        task.getActivityId(),
                                        task.getExecutionId(),
                                        task.getLockExpirationTime(),
                                        task.getVariables(),
                                        task.getRetries()))));

    }

    private void runRegisteredProcessor(final String processDefinitionKey, final String topic,
            final String externalTaskId, final String businessKey, final String processInstanceId,
            final String activityId, final String executionId,
            final Date lockExpirationTime, final Map<String, Object> variables, final Integer retries) {

        final String key = getInternalKey(processDefinitionKey, topic);
        final ExternalTaskSyncProcessingRegistrationImpl<?> registration = registrations.get(key);

        final String workerId = getWorkerId();
        try {
            final ExternalTaskHandlerProcessor processor = registration.getProcessor();
            if (processor instanceof ExternalTaskHandlerSyncProcessor) {
                final Map<String, Object> variablesToBeSet = ((ExternalTaskHandlerSyncProcessor) processor)
                        .apply(processInstanceId, businessKey, activityId, executionId, variables, retries);
                getExternalTaskService().complete(externalTaskId, workerId, variablesToBeSet);
            } else {
                final Date responseTimeout = ((ExternalTaskHandlerAsyncRequestProcessor) processor)
                        .apply(externalTaskId, processInstanceId, businessKey, activityId, executionId, variables, retries);
                
                setAsyncResponseTimeout(externalTaskId, lockExpirationTime, responseTimeout,
                        (ExternalTaskAsyncProcessingRegistrationImpl<?, ?>) registration);
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
        final ExternalTaskSyncProcessingRegistrationImpl<?> registration = registrations.get(key);
        if (!(registration instanceof ExternalTaskAsyncProcessingRegistration)) {
            throw new Exception("Topic '"
                    + externalTask.getTopicName()
                    + "' of process definition '"
                    + externalTask.getProcessDefinitionKey()
                    + "' was registered for synchonous processing!");
        }
        final ExternalTaskAsyncProcessingRegistrationImpl<R, I> asyncRegistration
                = (ExternalTaskAsyncProcessingRegistrationImpl<R, I>) registration;
        
        final ExternalTaskHandlerAsyncResponseProcessor<R, I> asyncResponseProcessor
                = asyncRegistration.getResponseProcessor();

        final Map<String, Object> variablesToBeSet = new HashMap<>();
        R result = null;
        try {
            result = asyncResponseProcessor
                    .apply(externalTask.getProcessInstanceId(),
                            externalTask.getBusinessKey(),
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
            logger.warn("Could not complete external task '{}' of process '{}'! Will raise an incident.",
                    correlationId, externalTask.getProcessDefinitionKey());
            processAsynchronously(() -> {
                if (! variablesToBeSet.isEmpty()) {
                    try {
                        getRuntimeService()
                                .setVariables(externalTask.getExecutionId(), variablesToBeSet);
                    } catch (Exception ie) {
                        logger.warn("Could not set variables and their values might be lost! {}", variablesToBeSet, ie);
                    }
                }
                try {
                    getExternalTaskService()
                            .handleFailure(correlationId, getWorkerId(), e.getMessage(), buildIncidentDetails(e), 0, 0);
                } catch (Exception ie) {
                    logger.warn("Could not build incident", ie);
                }
            });
        }

        return result;

    }
    
    private static String getInternalKey(final String processDefinitionKey, final String topic) {

        return processDefinitionKey + "#" + topic;

    }
    
    private static String getTopicFromInternalKey(final String key) {

        return key.substring(key.indexOf('#') + 1);

    }
    
    private static String getProcessDefinitionKeyFromInternalKey(final String key) {

        return key.substring(0, key.indexOf('#'));

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

    private void setAsyncResponseTimeout(final String externalTaskId, final Date lockTimeout,
            final Date overridingResponseTimeout,
            final ExternalTaskAsyncProcessingRegistrationImpl<?, ?> registration) {

        final Long responseTimeout = registration.getResponseTimeout();
        if ((responseTimeout == null)
                && (overridingResponseTimeout == null)) {
            return;
        }
        
        final Date duedate = overridingResponseTimeout != null
                ? overridingResponseTimeout
                : new Date(System.currentTimeMillis() + responseTimeout);
        final CommandExecutor executor = getProcessEngineConfiguration()
                .getCommandExecutorTxRequired();
        executor.execute(
                new AsyncProcessorTimeoutTimerCommand(duedate, externalTaskId, lockTimeout));
        
    }

    /**
     * Call by Camunda's Job Executor once a async processor times out
     */
    @Override
    public void execute(final AsyncProcessorTimeoutJobHandlerConfiguration configuration,
            final ExecutionEntity execution, final CommandContext commandContext, final String tenantId) {

        final String externalTaskId = configuration.getExternalTaskId();
        final ExternalTask externalTask = getExternalTaskService()
                .createExternalTaskQuery()
                .externalTaskId(externalTaskId)
                .locked()
                .singleResult();
        // already completed or not yet locked
        if (externalTask == null) {
            return;
        }
        // timeout timer belongs to expired external task execution
        if (!externalTask.getLockExpirationTime().equals(configuration.getLockTimeout())) {
            return;
        }
        // external task lock already expired - ignore timer
        if (externalTask.getLockExpirationTime().before(new Date())) {
            return;
        }
        
        final String key = getInternalKey(externalTask.getProcessDefinitionKey(), externalTask.getTopicName());
        final ExternalTaskAsyncProcessingRegistrationImpl<?, ?> registration = (ExternalTaskAsyncProcessingRegistrationImpl<?, ?>) registrations
                .get(key);
        
        getExternalTaskService()
                .handleFailure(externalTaskId, getWorkerId(), registration.getResponseTimeoutExpiredMessage(), 0, 0l);

    }

    @Override
    public String getType() {

        return ExternalTaskHandlerImpl.ASYNC_TIMEOUT_HANDLER_TYPE;

    }

    @Override
    public org.camunda.bpm.externaltask.AsyncProcessorTimeoutJobHandlerConfiguration newConfiguration(
            String canonicalString) {

        return new AsyncProcessorTimeoutJobHandlerConfiguration(canonicalString);

    }

    @Override
    public void onDelete(org.camunda.bpm.externaltask.AsyncProcessorTimeoutJobHandlerConfiguration configuration,
            JobEntity jobEntity) {

        // no action required

    }

}
