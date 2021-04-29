package org.camunda.bpm.externaltask.spring;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SpringExternalTaskHandler extends org.camunda.bpm.externaltask.ExternalTaskHandlerImpl {

    @Value("${camunda.bpm.externaltask-handler.default-locktimeout:60000}")
    private long defaultLockTimeout;
    
    @Autowired
    @Qualifier("workerId")
    private String workerId;
    
    @Autowired
    private ExternalTaskService externalTaskService;
    
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ExternalTaskHandlerHelper helper;
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Override
    protected long getDefaultLockTimeout() {
        return defaultLockTimeout;
    }
    
    @Override
    protected ExternalTaskService getExternalTaskService() {
        return externalTaskService;
    }
    
    @Override
    protected RuntimeService getRuntimeService() {
        return runtimeService;
    }

    @Override
    protected String getWorkerId() {
        return workerId;
    }
    
    /**
     * Pickup external task which might be "lost" due to system crashes.
     */
    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    @Transactional
    public void fetchAndLockExternalTasks() {
        
        processors.keySet()
                .forEach(super::fetchAndLockExternalTasks);
        
    }
    
    @Override
    protected void doAfterTransaction(Runnable action) {
        
        applicationEventPublisher
                .publishEvent(
                        new ExternalTaskHandlerEvent(this, action));
        
    }
    
    @SuppressWarnings("static-method")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void processAfterTransaction(ExternalTaskHandlerEvent event) {
        
        event.getAction().run();
        
    }
    
    @Override
    protected void processAsynchronously(Runnable action) {
        
        helper.processAsynchronously(action);
        
    }
    
    @Override
    protected void scheduleFetchAndLockExternalTasks(long timeout, String key) {
        
        helper.schedule(timeout, () -> super.fetchAndLockExternalTasks(key));
        
    }
    
    @EventListener(condition = "#execution.eventName == 'start'")
    protected void onTaskEvent(DelegateExecution execution) {
        
        final FlowElement bpmnElement = execution.getBpmnModelElementInstance();
        final String processDefinitionKey = ((ExecutionEntity) execution).getProcessDefinition().getKey();

        super.onTaskEvent(processDefinitionKey, bpmnElement);
        
    }

    @Override
    public void setDefaultLockTimeout(long lockTimeout) {
        this.defaultLockTimeout = lockTimeout;
    }
    
    @Override
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
}
