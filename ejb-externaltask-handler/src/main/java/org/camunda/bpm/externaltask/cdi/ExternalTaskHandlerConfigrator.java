package org.camunda.bpm.externaltask.cdi;

import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;

public interface ExternalTaskHandlerConfigrator {

    void configure(ExternalTaskHandler handler);
    
}
