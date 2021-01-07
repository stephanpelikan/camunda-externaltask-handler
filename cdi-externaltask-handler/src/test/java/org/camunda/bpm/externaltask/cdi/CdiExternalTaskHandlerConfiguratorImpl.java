package org.camunda.bpm.externaltask.cdi;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CdiExternalTaskHandlerConfiguratorImpl implements CdiExternalTaskHandlerConfigrator {

    @Override
    public void configure(CdiExternalTaskHandler handler) {
        
        handler.setWorkerId("testWorker");
        handler.setDefaultLockTimeout(60000);
       
    }
    
}
