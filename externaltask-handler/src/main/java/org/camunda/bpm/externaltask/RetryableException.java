package org.camunda.bpm.externaltask;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to handle subsequent retries of an external tasks.
 * <p>
 * Provide:
 * <ul>
 * <li><i>maxAttempts:</i> The total number of attempts</li>
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
 
    public RetryableException(int maxAttempts, Integer retries, List<Long> retryTimeouts) {
        super();
        initialize(maxAttempts, retries, retryTimeouts);
    }

    public RetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int maxAttempts, Integer retries, List<Long> retryTimeouts) {
        super(message, cause, enableSuppression, writableStackTrace);
        initialize(maxAttempts, retries, retryTimeouts);
    }

    public RetryableException(String message, Throwable cause, int maxAttempts, Integer retries, List<Long> retryTimeouts) {
        super(message, cause);
        initialize(maxAttempts, retries, retryTimeouts);
    }

    public RetryableException(String message, int maxAttempts, Integer retries, List<Long> retryTimeouts) {
        super(message);
        initialize(maxAttempts, retries, retryTimeouts);
    }

    public RetryableException(Throwable cause, int maxAttempts, Integer retries, List<Long> retryTimeouts) {
        super(cause);
        initialize(maxAttempts, retries, retryTimeouts);
    }

    public RetryableException(int maxAttempts, Integer retries, Long retryTimeout) {
        super();
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxAttempts, retries, timeouts);
    }

    public RetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int maxAttempts, Integer retries, Long retryTimeout) {
        super(message, cause, enableSuppression, writableStackTrace);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxAttempts, retries, timeouts);
    }

    public RetryableException(String message, Throwable cause, int maxAttempts, Integer retries, Long retryTimeout) {
        super(message, cause);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxAttempts, retries, timeouts);
    }

    public RetryableException(String message, int maxAttempts, Integer retries, Long retryTimeout) {
        super(message);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxAttempts, retries, timeouts);
    }

    public RetryableException(Throwable cause, int maxAttempts, Integer retries, Long retryTimeout) {
        super(cause);
        final List<Long> timeouts = new LinkedList<>();
        timeouts.add(retryTimeout);
        initialize(maxAttempts, retries, timeouts);
    }
    
    public long getRetryTimeout() {
        return nextTimeout;
    }
    
    public int getRetries() {
        return nextRetries;
    }

    private void initialize(int maxAttempts, Integer retries, List<Long> retryTimeouts) {
        
        final List<Long> timeouts;
        if ((retryTimeouts == null)
                || retryTimeouts.isEmpty()) {
            timeouts = defaultTimeouts;
        } else {
            timeouts = retryTimeouts;
        }
        
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
