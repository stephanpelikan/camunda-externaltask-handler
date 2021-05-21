package org.camunda.bpm.externaltask.spring;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ExternalTaskHandlerHelper {

    @Autowired
    private TaskScheduler taskScheduler;
    
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Async
    void processAsynchronously(final Runnable task) {
        processTransactional(task);
    }
    
    void schedule(final long timeout, final Runnable task) {
        taskScheduler.schedule(
                () -> processTransactional(task),
                new Date(System.currentTimeMillis() + timeout));
    }
    
    <T> T processTransactional(final Runnable task) {
        return transactionTemplate.execute(txStatus -> {
            task.run();
            return null;
        });
    }

}
