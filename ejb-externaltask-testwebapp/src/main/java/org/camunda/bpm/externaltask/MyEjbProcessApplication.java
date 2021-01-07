package org.camunda.bpm.externaltask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.ProcessApplicationInterface;
import org.camunda.bpm.application.impl.EjbProcessApplication;
import org.camunda.bpm.engine.cdi.impl.event.CdiEventListener;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@ProcessApplication
@Local(ProcessApplicationInterface.class)
public class MyEjbProcessApplication extends EjbProcessApplication {

    protected CdiEventListener cdiEventListener = new CdiEventListener();

    @PostConstruct
    public void start() {
        deploy();
    }

    @PreDestroy
    public void stop() {
        undeploy();
    }

    @Override
    public ExecutionListener getExecutionListener() {
        return cdiEventListener;
    }

    @Override
    public TaskListener getTaskListener() {
        return cdiEventListener;
    }
    
}
