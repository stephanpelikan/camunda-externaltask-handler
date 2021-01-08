package org.camunda.bpm.externaltask.cdi;

import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;

public interface ExternalTaskHandlerConfigurator {

    void configure(ExternalTaskHandler handler);
    
}
