package org.camunda.bpm.externaltask.spring;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExternalTaskHandlerHelper {

    @Autowired
    private TaskScheduler taskScheduler;
    
    @SuppressWarnings("static-method")
    @Async
    @Transactional
    void processAsynchronously(final Runnable task) {
        task.run();
    }
    
    void schedule(final long timeout, final Runnable task) {
        taskScheduler.schedule(task, new Date(System.currentTimeMillis() + timeout));
    }

}
