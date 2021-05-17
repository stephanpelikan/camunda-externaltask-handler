package org.camunda.bpm.externaltask.spi;

import java.util.Map;

/**
 * <p>
 * A service which uses Camunda's external tasks for simplified task
 * implementation and streamlined error handling.
 * </p>
 * <p>
 * Usage for synchronous processing:
 * </p>
 * 
 * <pre>
 *   &#64;PostConstruct
 *   private void init() {
 *      // variant 1:
 *      externalTaskHandler.registerExternalTaskProcessor("myprocess", "mytopic1", this::processServiceTask1); 
 *      // variant 2:
 *      externalTaskHandler.registerExternalTaskProcessor("myprocess", "mytopic2",
 *          ((processInstanceId, activityId, executionId, variables) -&gt; processServiceTask2(variables))); 
 *   }
 *   
 *   public Map&lt;String, Object&gt; processServiceTask1(String processInstanceId, String activityId, String executionId,
 *          Map&lt;String, Object&gt; variables) throws BpmnError, RetryableException, Exception {
 *      doSomething();
 *      return null; // do not change or set any process variable
 *   }
 *
 *   private Map&lt;String, Object&gt; processServiceTask2(variables) {
 *      String myOrderId = (String) variables.get("myOrderId");
 *      String result = doWhatever(myOrderId);
 *      return Map.of("whatever", result); // set process variables
 *   }
 * </pre>
 * <p>
 * See {@link ExternalTaskHandlerSyncProcessor} for details of how to treat
 * errors.
 * </p>
 * <p>
 * Usage for asynchronous processing:
 * </p>
 * 
 * <pre>
 *   &#64;Autowired
 *   private ExternalTaskHandler externalTaskHandler;
 *   
 *   &#64;Autowired
 *   private AsyncApiClient client;
 *   
 *   &#64;PostConstruct
 *   private void init() {
 *      externalTaskHandler.&lt;String, MyResponse&gt;registerExternalTaskProcessor("myprocess", "mytopic1", this::processRequest,
 *              (processInstanceId, activityId, executionId, retries, correlationId, response, variableToBeSet) -&gt;
 *                      processResponse(correlationId, response, variableToBeSet); 
 *   }
 *   
 *   private void processRequest(String correlationId, String processInstanceId, String activityId, String executionId,
 *          Map&lt;String, Object&gt; variables, Integer retries) throws BpmnError, RetryableException, Exception {
 *      try {
 *          client.doSomething(correlationId, variables.get("myOrderId"));
 *      } catch (Exception e) {
 *          throw new RetryableException("remote call failed", e, 4, retries, List.of(5000l, 60000l));
 *      }
 *   }
 *
 *   private String processResponse(String correlationId, MyResponse response) throws BpmnError, Exception {
 *     final String result = doWhatever(correlationId, myResponse);
 *     variableToBeSet.put("whatever", result);
 *     return "Thank you ;-)";
 *   }
 *   
 *   &#64;RequestMapping(value = "/my-async-response-rest-api/{correlationId}", method = RequestMethod.POST)
 *   public ResponseEntity&lt;String&gt; processAsyncRestResponse(@PathVariable("correlationId") correlationId,
 *           &#64;RequestBody MyResponse data) {
 *       final String resultMessage = externalTaskHandler.handleAsyncInput(correlationId, data);
 *       return ResponseEntity.ok(resultMessage);
 *   }
 * </pre>
 * <p>
 * There are various methods for processor registration having different
 * capabilities:
 * </p>
 * <ul>
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerSyncProcessor)}
 * <li>ExternalTaskHandler{@link #registerExternalTaskProcessor(String, String, ExternalTaskHandlerAsyncRequestProcessor, ExternalTaskHandlerAsyncResponseProcessor)}
 * </ul>
 * 
 * @author Stephan Pelikan
 */
public interface ExternalTaskHandler {

    /**
     * Register processor for a certain process definition and a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution</li>
     * <li>The default lock timeout will be uses (1 minute or Spring property
     * &quot;application.external-task-handler.lock-timeout&quot;)</li>
     * </ul>
     * 
     * @param processDefinitionKey
     * @param topic
     * @param processor
     * 
     * @see ExternalTaskHandlerSyncProcessor#apply(String, String, String, Map,
     *      Integer)
     */
    ExternalTaskSyncProcessingRegistration<ExternalTaskSyncProcessingRegistration<?>> registerExternalTaskProcessor(
            String processDefinitionKey, String topic, ExternalTaskHandlerSyncProcessor processor);
    
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
     * @see ExternalTaskHandlerSyncProcessor#apply(String, String, String, Map)
     */
//    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerSyncProcessor processor, boolean fetchNoVariables);
    
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
     * @see ExternalTaskHandlerSyncProcessor#apply(String, String, String, Map)
     */
//    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerSyncProcessor processor, Long lockTimeout);

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
     * @see ExternalTaskHandlerSyncProcessor#apply(String, String, String, Map)
     */
//    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerSyncProcessor processor, boolean fetchNoVariables, Long lockTimeout);

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
     * @see ExternalTaskHandlerSyncProcessor#apply(String, String, String, Map)
     */
//    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerSyncProcessor processor, String firstVariableToFetch,
//            String... variablesToFetch);
    
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
     * @see ExternalTaskHandlerSyncProcessor#apply(String, String, String, Map)
     */
//    void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerSyncProcessor processor, Long lockTimeout,
//            String firstVariableToFetch, String... variablesToFetch);

    /**
     * Register processors for asynchronous processing for a certain process
     * definition and a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution</li>
     * <li>The default lock timeout will be uses (1 minute or Spring property
     * &quot;application.external-task-handler.lock-timeout&quot;)</li>
     * </ul>
     * 
     * @param <I>                  The type of the input given by an asynchronous
     *                             processor
     * @param <R>                  The type of the response of processing the
     *                             asynchronous input
     * @param processDefinitionKey
     * @param topic
     * @param requestProcessor
     * @param responseProcessor
     * 
     * @see ExternalTaskHandlerAsyncRequestProcessor#apply(String, String, String,
     *      String, Map, Integer)
     * @see ExternalTaskHandlerAsyncResponseProcessor#apply(String, String, String,
     *      Integer, String, Object, Map)
     */
    <R, I> ExternalTaskAsyncProcessingRegistration registerExternalTaskProcessor(String processDefinitionKey,
            String topic,
            ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
            ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor);

    /**
     * Register processors for async processing for a certain process definition and
     * a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution according
     * to the param &quot;fetchNoVariables&quot;</li>
     * <li>The default lock timeout will be uses (1 minute or Spring property
     * &quot;application.external-task-handler.lock-timeout&quot;)</li>
     * </ul>
     * 
     * @param <I>                  The type of the input given by an asynchronous
     *                             processor
     * @param <R>                  The type of the response of processing the
     *                             asynchronous input
     * @param processDefinitionKey
     * @param topic
     * @param requestProcessor
     * @param responseProcessor
     * @param fetchNoVariables
     * 
     * @see ExternalTaskHandlerAsyncRequestProcessor#apply(String, String, String,
     *      String, Map, Integer)
     * @see ExternalTaskHandlerAsyncResponseProcessor#apply(String, String, String,
     *      Integer, String, Object)
     */
//    <R, I> void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
//            ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, boolean fetchNoVariables);

    /**
     * Register processors for async processing for a certain process definition and
     * a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution according
     * to the param &quot;fetchNoVariables&quot;</li>
     * <li>The lock timeout according to the param &quot;lockTimeout&quot; will be
     * used</li>
     * </ul>
     * 
     * @param <I>                  The type of the input given by an asynchronous
     *                             processor
     * @param <R>                  The type of the response of processing the
     *                             asynchronous input
     * @param processDefinitionKey
     * @param topic
     * @param requestProcessor
     * @param responseProcessor
     * @param fetchNoVariables
     * @param lockTimeout
     * 
     * @see ExternalTaskHandlerAsyncRequestProcessor#apply(String, String, String,
     *      String, Map, Integer)
     * @see ExternalTaskHandlerAsyncResponseProcessor#apply(String, String, String,
     *      Integer, String, Object)
     */
//    <R, I> void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
//            ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, boolean fetchNoVariables,
//            Long lockTimeout);

    /**
     * Register processors for async processing for a certain process definition and
     * a specific topic.
     * <ul>
     * <li>All process variables will be fetched and supplied on execution</li>
     * <li>The lock timeout according to the param &quot;lockTimeout&quot; will be
     * used</li>
     * </ul>
     * 
     * @param <I>                  The type of the input given by an asynchronous
     *                             processor
     * @param <R>                  The type of the response of processing the
     *                             asynchronous input
     * @param processDefinitionKey
     * @param topic
     * @param requestProcessor
     * @param responseProcessor
     * @param lockTimeout
     * 
     * @see ExternalTaskHandlerAsyncRequestProcessor#apply(String, String, String,
     *      String, Map, Integer)
     * @see ExternalTaskHandlerAsyncResponseProcessor#apply(String, String, String,
     *      Integer, String, Object)
     */
//    <R, I> void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
//            ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, Long lockTimeout);

    /**
     * Register processors for async processing for a certain process definition and
     * a specific topic.
     * <ul>
     * <li>The process variables according to the params
     * &quot;firstVariableToFetch&quot; and &quot; variablesToFetch&quot; will be
     * fetched and supplied on execution</li>
     * <li>The lock timeout according to the param &quot;lockTimeout&quot; will be
     * used</li>
     * </ul>
     * 
     * @param <I>                  The type of the input given by an asynchronous
     *                             processor
     * @param <R>                  The type of the response of processing the
     *                             asynchronous input
     * @param processDefinitionKey
     * @param topic
     * @param requestProcessor
     * @param responseProcessor
     * @param lockTimeout
     * @param firstVariableToFetch
     * @param variablesToFetch
     * 
     * @see ExternalTaskHandlerAsyncRequestProcessor#apply(String, String, String,
     *      String, Map, Integer)
     * @see ExternalTaskHandlerAsyncResponseProcessor#apply(String, String, String,
     *      Integer, String, Object)
     */
//    <R, I> void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
//            ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, Long lockTimeout,
//            String firstVariableToFetch, String... variablesToFetch);

    /**
     * Register processors for async processing for a certain process definition and
     * a specific topic.
     * <ul>
     * <li>The process variables according to the params
     * &quot;firstVariableToFetch&quot; and &quot; variablesToFetch&quot; will be
     * fetched and supplied on execution</li>
     * <li>The default lock timeout will be uses (1 minute or Spring property
     * &quot;application.external-task-handler.lock-timeout&quot;)</li>
     * </ul>
     * 
     * @param <I>                  The type of the input given by an asynchronous
     *                             processor
     * @param <R>                  The type of the response of processing the
     *                             asynchronous input
     * @param processDefinitionKey
     * @param topic
     * @param requestProcessor
     * @param responseProcessor
     * @param firstVariableToFetch
     * @param variablesToFetch
     * 
     * @see ExternalTaskHandlerAsyncRequestProcessor#apply(String, String, String,
     *      String, Map, Integer)
     * @see ExternalTaskHandlerAsyncResponseProcessor#apply(String, String, String,
     *      Integer, String, Object)
     */
//    <R, I> void registerExternalTaskProcessor(String processDefinitionKey, String topic,
//            ExternalTaskHandlerAsyncRequestProcessor requestProcessor,
//            ExternalTaskHandlerAsyncResponseProcessor<R, I> responseProcessor, String firstVariableToFetch,
//            String... variablesToFetch);

    /**
     * Feed asynchronous input to the external task handler which will call the
     * response handler registered before.
     * <p>
     * Hint: Exceptions thrown by the response will be passed to the caller.
     * Exceptions on completing the external task, which typically are caused by
     * subsequent workflow activities in the same transaction, will raise an
     * incident.
     * </p>
     * 
     * @param <I>           The type of the asynchronous input passed to the
     *                      response handler
     * @param <R>           The result of the response handler which might be passed
     *                      to the source of this input
     * @param correlationId
     * @param input
     * @return Any result of the response handler
     * @throws Exception Any exception thrown by the response handler.
     */
    <R, I> R handleAsyncInput(String correlationId, I input) throws Exception;

    /**
     * @param workerId Used to lock external tasks. e.g. the machine's name or IP
     *                 address
     * 
     * @see https://docs.camunda.org/manual/7.13/user-guide/process-engine/external-tasks/
     */
    void setWorkerId(String workerId);
    
    /**
     * @param lockTimeout Use as a lock timeout for external tasks, if no specific timeout was used.
     * 
     * @see https://docs.camunda.org/manual/7.13/user-guide/process-engine/external-tasks/
     */
    void setDefaultLockTimeout(long lockTimeout);
    
}
