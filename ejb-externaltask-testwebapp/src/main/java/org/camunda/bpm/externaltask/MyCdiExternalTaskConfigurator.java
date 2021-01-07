package org.camunda.bpm.externaltask;

import javax.enterprise.context.ApplicationScoped;

import org.camunda.bpm.externaltask.cdi.ExternalTaskHandlerConfigrator;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;

@ApplicationScoped
public class MyCdiExternalTaskConfigurator implements ExternalTaskHandlerConfigrator {

    @Override
    public void configure(final ExternalTaskHandler handler) {
        
        handler.setWorkerId("testWorker");
        handler.setDefaultLockTimeout(60000);
        
    }
    
}
