package org.camunda.bpm.externaltask.cdi;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.cdi.BusinessProcessEvent;
import org.camunda.bpm.engine.cdi.BusinessProcessEventType;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.model.bpmn.instance.FlowElement;

@Singleton
@Lock(LockType.READ)
public class CdiExternalTaskHandler extends org.camunda.bpm.externaltask.ExternalTaskHandlerImpl {

    private long defaultLockTimeout;
    
    private String workerId;
    
    @Inject
    private ExternalTaskService externalTaskService;

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private RepositoryService repositoryService;
    
    @Resource
    private TimerService timerService;
    
    @Resource
    private TransactionSynchronizationRegistry txSyncRegistry;
    
    @EJB
    private CdiExternalTaskHandlerHelper helper;
    
    @Inject
    private ExternalTaskHandlerConfigurator configurator;
    
    @Inject
    private ProcessEngine processEngine;

    @PostConstruct
    private void configure() {
        
        configurator.configure(this);

        getProcessEngineConfiguration()
                .getJobHandlers()
                .put(this.getType(), this);

    }
    
    @Override
    protected ProcessEngineConfigurationImpl getProcessEngineConfiguration() {

        return (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

    }

    @Override
    protected long getDefaultLockTimeout() {
        return defaultLockTimeout;
    }
    
    @Override
    public void setDefaultLockTimeout(long defaultLockTimeout) {
        this.defaultLockTimeout = defaultLockTimeout;
    }
    
    @Override
    protected String getWorkerId() {
        return workerId;
    }
    
    @Override
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
    @Override
    protected ExternalTaskService getExternalTaskService() {
        return externalTaskService;
    }
    
    @Override
    protected RuntimeService getRuntimeService() {
        return runtimeService;
    }

    @Schedule(second = "0", minute = "*", hour = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void fetchAndLockExternalTasks() {
        
        registrations.keySet()
                .forEach(super::fetchAndLockExternalTasks);
        
    }
    
    @Override
    protected void doAfterTransaction(Runnable action) {
        
        txSyncRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                // ignore this event
            }

            @Override
            public void afterCompletion(int status) {
                if (status == Status.STATUS_COMMITTED) {
                    helper.processAsynchronously(action);
                }
            }
        });
        
    }
    
    @Override
    protected void processAsynchronously(Runnable action) {
        
        helper.processAsynchronously(action);
        
    }

    @Timeout
    public void fetchAndLockExternalTasks(Timer timer) {
        
        super.fetchAndLockExternalTasks((String) timer.getInfo());
        
    }
    
    @Override
    protected void scheduleFetchAndLockExternalTasks(long timeout, String key) {
        
        timerService.createTimer(timeout, key);
        
    }
    
    public void onTaskEvent(@Observes BusinessProcessEvent businessProcessEvent) {
        
        if (! businessProcessEvent.getType().equals(BusinessProcessEventType.START_ACTIVITY)) {
            return;
        }
        
        final FlowElement bpmnElement = repositoryService
                .getBpmnModelInstance(businessProcessEvent.getProcessDefinition().getId())
                .getModelElementById(businessProcessEvent.getActivityId());
        
        super.onTaskEvent(businessProcessEvent.getProcessDefinition().getKey(), bpmnElement);
        
    }
    
}
