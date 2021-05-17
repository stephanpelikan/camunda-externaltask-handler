package org.camunda.bpm.externaltask;

import java.util.Date;

import org.camunda.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;

public class AsyncProcessorTimeoutJobHandlerConfiguration implements JobHandlerConfiguration {

    private String externalTaskId;

    private Date lockTimeout;

    public AsyncProcessorTimeoutJobHandlerConfiguration(final String canonicalString) {

        final int pos = canonicalString.indexOf('#');
        externalTaskId = canonicalString.substring(0, pos);
        lockTimeout = new Date(Long.parseLong(canonicalString.substring(pos + 1)));

    }

    public AsyncProcessorTimeoutJobHandlerConfiguration(final String externalTaskId,
            final Date lockTimeout) {

        this.externalTaskId = externalTaskId;
        this.lockTimeout = lockTimeout;

    }

    @Override
    public String toCanonicalString() {

        return externalTaskId + "#" + Long.toString(lockTimeout.getTime());

    }

    public String getExternalTaskId() {

        return externalTaskId;

    }

    public Date getLockTimeout() {

        return lockTimeout;

    }

}
