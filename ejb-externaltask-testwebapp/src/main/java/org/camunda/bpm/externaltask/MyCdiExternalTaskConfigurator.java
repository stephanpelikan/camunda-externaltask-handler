package org.camunda.bpm.externaltask;

import javax.enterprise.context.ApplicationScoped;

import org.camunda.bpm.externaltask.cdi.ExternalTaskHandlerConfigurator;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;

@ApplicationScoped
public class MyCdiExternalTaskConfigurator implements ExternalTaskHandlerConfigurator {

    @Override
    public void configure(final ExternalTaskHandler handler) {
        
        handler.setWorkerId("testWorker");
        handler.setDefaultLockTimeout(60000);
        
    }
    
}
