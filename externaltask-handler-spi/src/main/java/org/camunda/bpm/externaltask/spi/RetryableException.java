package org.camunda.bpm.externaltask.spi;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to handle subsequent retries of an external tasks.
 * <p>
 * Provide:
 * <ul>
 * <li><i>maxRetries:</i> The total number of retries</li>
 * <li><i>retries:</i> The current &quot;retries&quot; value given by {@link ExternalTaskHandlerProcessor#apply(String, String, String, java.util.Map, Integer)}</li>
 * <li><i>retryTimeouts:</i> A sequence of timeouts used for each attempt. If more attempts are configured than retry values available then the last retry timeout will be used for those attempts.</li>
 * <li><i>retryTimeout:</i> A timeout used for each attempt</li>
 * </ul>
 * 
 * @see ExternalTaskHandlerProcessor
 */
public class RetryableException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public static final Long DEFAULT_TIMEOUT = Long.valueOf(5000); // 5 seconds
    
    private static final List<Long> defaultTimeouts;
    
    static {
        defaultTimeouts = new LinkedList<>();
        defaultTimeouts.add(DEFAULT_TIMEOUT);
    }
    
    private long nextTimeout;
    
    private int nextRetries;
 
    public RetryableException(int maxRetries, Integer retries, List<Long> retryTimeouts) {
        super();
        initialize(maxRetries, retries, retryTimeouts);
    }

    public RetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int maxRetries, Integer retries, List<Long> retryTimeouts) {
        super(message, cause, enableSuppression, writableStackTrace);
        initialize(maxRetries, retries, retryTimeouts);
    }

    public RetryableException(String message, Throwable cause, int maxRetries, Integer retries, List<Long> retryTimeouts) {
        super(message, cause);
        initialize(maxRetries, retries, retryTimeouts);
    }

    public RetryableException(String message, int maxRetries, Integer retries, List<Long> retryTimeouts) {
        super(message);
        initialize(maxRetries, retries, retryTimeouts);
    }

    public RetryableException(Throwable cause, int maxRetries, Integer retries, List<Long> retryTimeouts) {
        super(cause);
        initialize(maxRetries, retries, retryTimeouts);
    }

    public RetryableException(int maxRetries, Integer retries, Long retryTimeout) {
        super();
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxRetries, retries, timeouts);
    }

    public RetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int maxRetries, Integer retries, Long retryTimeout) {
        super(message, cause, enableSuppression, writableStackTrace);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxRetries, retries, timeouts);
    }

    public RetryableException(String message, Throwable cause, int maxRetries, Integer retries, Long retryTimeout) {
        super(message, cause);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxRetries, retries, timeouts);
    }

    public RetryableException(String message, int maxRetries, Integer retries, Long retryTimeout) {
        super(message);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxRetries, retries, timeouts);
    }

    public RetryableException(Throwable cause, int maxRetries, Integer retries, Long retryTimeout) {
        super(cause);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxRetries, retries, timeouts);
    }
    
    public long getRetryTimeout() {
        return nextTimeout;
    }
    
    public int getRetries() {
        return nextRetries;
    }

    private void initialize(int maxRetries, Integer retries, List<Long> retryTimeouts) {
        
        final List<Long> timeouts;
        if ((retryTimeouts == null)
                || retryTimeouts.isEmpty()) {
            timeouts = defaultTimeouts;
        } else {
            timeouts = retryTimeouts;
        }
        
        final int maxAttempts = maxRetries + 1;
        final int attempt;
        if (retries == null) {
            attempt = 1;
        } else {
            attempt = maxAttempts - retries + 1;
        }
        
        nextRetries = (maxAttempts - attempt);
        
        if (nextRetries == 0) {
            nextTimeout = 0;
        } else if (attempt >= timeouts.size()) {
            nextTimeout = timeouts.get(timeouts.size() - 1);
        } else {
            nextTimeout = timeouts.get(attempt - 1);
        }
        
    }
    
}
