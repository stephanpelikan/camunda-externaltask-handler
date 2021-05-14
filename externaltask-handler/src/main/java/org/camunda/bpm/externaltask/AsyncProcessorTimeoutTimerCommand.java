package org.camunda.bpm.externaltask;

import java.util.Date;

import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.MessageEntity;

/**
 * @see https://medium.com/holisticon-consultants/implementing-camunda-custom-job-handler-f574a7b08ee1
 */
public class AsyncProcessorTimeoutTimerCommand implements Command<String> {

    private Date duedate;

    private AsyncProcessorTimeoutJobHandlerConfiguration jobHandlerConfiguration;

    public AsyncProcessorTimeoutTimerCommand(
            final Date duedate, final String externalTaskId, final Date lockTimeout) {

        this.duedate = duedate;
        this.jobHandlerConfiguration = new AsyncProcessorTimeoutJobHandlerConfiguration(externalTaskId, lockTimeout);

    }

    @Override
    public String execute(final CommandContext commandContext) {

        final MessageEntity entity = new MessageEntity();

        entity.init(commandContext);
        entity.setJobHandlerType(ExternalTaskHandlerImpl.ASYNC_TIMEOUT_HANDLER_TYPE);
        entity.setJobHandlerConfiguration(jobHandlerConfiguration);
        entity.setDuedate(duedate);

        commandContext.getJobManager().send(entity);

        return entity.getId();

    }

}
