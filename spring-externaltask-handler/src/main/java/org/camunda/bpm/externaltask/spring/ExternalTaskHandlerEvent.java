package org.camunda.bpm.externaltask.spring;

import org.springframework.context.ApplicationEvent;

public class ExternalTaskHandlerEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private Runnable action;

    public ExternalTaskHandlerEvent(Object source, Runnable action) {
        super(source);
        this.action = action;
    }

    public Runnable getAction() {
        return action;
    }

}
