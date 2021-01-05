package org.camunda.bpm.externaltask.spring;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
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
public class ExternalTaskHandler extends org.camunda.bpm.externaltask.ExternalTaskHandler {

    @Value("${camunda.bpm.externaltask-handler.default-locktimeout:60000}")
    private long defaultLockTimeout;
    
    @Autowired
    @Qualifier("workerId")
    private String workerId;
    
    @Autowired
    private ExternalTaskService externalTaskService;
    
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
    protected void schedule(long timeout, Runnable action) {
        
        helper.schedule(timeout, action);
        
    }
    
    @EventListener(condition = "#execution.eventName == 'start'")
    @Override
    protected void onTaskEvent(DelegateExecution execution) {
        
        super.onTaskEvent(execution);
        
    }
    
}
