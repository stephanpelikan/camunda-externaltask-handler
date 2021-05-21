package org.camunda.bpm.externaltask.cdi;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@Lock(LockType.READ)
public class CdiExternalTaskHandlerHelper {

    @SuppressWarnings("static-method")
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processAsynchronously(final Runnable task) {
        task.run();
    }
    
}
