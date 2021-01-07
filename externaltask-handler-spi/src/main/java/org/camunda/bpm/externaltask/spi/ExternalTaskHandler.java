package org.camunda.bpm.externaltask.spi;

import java.util.Map;

/**
 * <p>
 * A service which uses Camunda's external task implementation for
 * proper error handling.
 * <p>
 * Usage:
 * <pre>
 *   @PostConstruct
 *   private void init() {
 *      // variant 1:
 *      externalTaskHandler.registerExternalTaskProcessor("myprocess", "mytopic1", this::processServiceTask1); 
 *      // variant 2:
 *      externalTaskHandler.registerExternalTaskProcessor("myprocess", "mytopic2",
 *          ((processInstanceId, activityId, executionId, variables) -> processServiceTask2(variables))); 
 *   }
 *   
 *   public Map<String, Object> processServiceTask1(String processInstanceId, String activityId, String executionId,
 *          Map<String, Object> variables) throws BpmnError, RetryableException, Exception{
 *   }
 *
 *   private Map<String, Object> processServiceTask2(variables) {
 *     var somOrderId = variables.get("somOrderId");
 *     var result = doWhatever(somOrderId);
 *     return Map.of("whatever", result);
 *   }
 * </pre>
 * See {@link ExternalTaskHandlerProcessor} for details of how to treat errors.
 * <p>
 * There are various methods for processor registration having different capabilities:
 * <ul>
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor)}
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, Long)
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, boolean)
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, boolean, Long)
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, String, String...)
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, Long, String, String...)
 * </ul>
 * 
 * @author Stephan Pelikan
 */
public interface ExternalTaskHandler {

    /**
     * Register processor for a certain process definition and a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution</li>
     * <li>The default lock timeout will be uses (1 minute or Spring
     * property &quot;application.external-task-handler.lock-timeout&quot;)</li>
     * </ul>
     * 
     * @param processDefinitionKey
     * @param topic
     * @param processor
     * 
     * @see ExternalTaskHandlerProcessor#apply(String, String, String, Map)
     */
    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor);
    
    /**
     * Register processor for a certain process definition and a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution according to the param &quot;fetchNoVariables&quot;</li>
     * <li>The default lock timeout will be uses (1 minute or Spring
     * property &quot;application.external-task-handler.lock-timeout&quot;)</li>
     * </ul>
     * 
     * @param processDefinitionKey
     * @param topic
     * @param processor
     * @param fetchNoVariables
     * 
     * @see ExternalTaskHandlerProcessor#apply(String, String, String, Map)
     */
    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, boolean fetchNoVariables);
    
    /**
     * Register processor for a certain process definition and a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution</li>
     * <li>The lock timeout according to the param &quot;lockTimeout&quot; will be used</li>
     * </ul>
     * 
     * @param processDefinitionKey
     * @param topic
     * @param processor
     * @param lockTimeout
     * 
     * @see ExternalTaskHandlerProcessor#apply(String, String, String, Map)
     */
    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, Long lockTimeout);

    /**
     * Register processor for a certain process definition and a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution according to the param &quot;fetchNoVariables&quot;</li>
     * <li>The lock timeout according to the param &quot;lockTimeout&quot; will be used</li>
     * </ul>
     * 
     * @param processDefinitionKey
     * @param topic
     * @param processor
     * @param fetchNoVariables
     * @param lockTimeout
     * 
     * @see ExternalTaskHandlerProcessor#apply(String, String, String, Map)
     */
    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, boolean fetchNoVariables, Long lockTimeout);

    /**
     * Register processor for a certain process definition and a specific topic.
     * <ul>
     * <li>The process variables according to the params &quot;firstVariableToFetch&quot; and
     * &quot; variablesToFetch&quot; will be fetched and supplied on execution</li>
     * <li>The default lock timeout will be uses (1 minute or Spring
     * property &quot;application.external-task-handler.lock-timeout&quot;)</li>
     * </ul>
     * 
     * @param processDefinitionKey
     * @param topic
     * @param processor
     * @param firstVariableToFetch
     * @param variablesToFetch
     * 
     * @see ExternalTaskHandlerProcessor#apply(String, String, String, Map)
     */
    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, String firstVariableToFetch,
            String... variablesToFetch);
    
    /**
     * Register processor for a certain process definition and a specific topic.
     * <ul>
     * <li>The process variables according to the params &quot;firstVariableToFetch&quot; and
     * &quot; variablesToFetch&quot; will be fetched and supplied on execution</li>
     * <li>The lock timeout according to the param &quot;lockTimeout&quot; will be used</li>
     * </ul>
     * 
     * @param processDefinitionKey
     * @param topic
     * @param processor
     * @param lockTimeout
     * @param firstVariableToFetch
     * @param variablesToFetch
     * 
     * @see ExternalTaskHandlerProcessor#apply(String, String, String, Map)
     */
    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
            ExternalTaskHandlerProcessor processor, Long lockTimeout,
            String firstVariableToFetch, String... variablesToFetch);

    /**
     * @param workerId Used to lock external tasks. e.g. the machine's name or IP address
     * 
     * @see https://docs.camunda.org/manual/7.13/user-guide/process-engine/external-tasks/
     */
    void setWorkerId(String workerId);
    
    /**
     * @param lockTimeout Use as a lock timeout for external tasks, if no specific timeout was used.
     * 
     * @see https://docs.camunda.org/manual/7.13/user-guide/process-engine/external-tasks/
     * @see ExternalTaskHandler#registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, Long)
     * @see ExternalTaskHandler#registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, boolean, Long)
     * @see ExternalTaskHandler#registerExternalTaskProcessor(String, String, ExternalTaskHandlerProcessor, Long, String, String...)
     */
    void setDefaultLockTimeout(long lockTimeout);
    
}
