# camunda-externaltask-handler

Provides the ExternalTaskHandler bean which can be used to process external tasks easily:
```
  @Autowired
  private ExternalTaskHandler externalTaskHandler;
  
  @PostConstruct
  private void init() {
     // variant 1:
     externalTaskHandler.registerExternalTaskProcessor("myprocess", "mytopic1", this::processServiceTask1); 
     // variant 2:
     externalTaskHandler.registerExternalTaskProcessor("myprocess", "mytopic2",
         ((processInstanceId, activityId, executionId, variables, retries) -> processServiceTask2(variables))); 
  }
  
  public Map<String, Object> processServiceTask1(String processInstanceId, String activityId, String executionId,
         Map<String, Object> variables, Integer retries) throws BpmnError, RetryableException, Exception {
    // do something or throw exception for incident
  }

  private Map<String, Object> processServiceTask2(variables) {
    final var somOrderId = variables.get("somOrderId");
    final var result = doWhatever(somOrderId);
    return Map.of("whatever", result);
  }
```
A processor method for a certain process definition and a specific topic has to be registered. The method receives process information and variables according to the registration method used. A processor method can
* return a map of process variables to be set
* return null if no process variables should be set
* throw a BpmnError if an error should be handled by Camunda
* throw a BpmnErrorWithVariables if an error should be handled by Camunda and additionally variables should be set
* throw a RetryableException if an incident should be created after a defined number of further attempts
* throw an Exception if an incident should be created immediately

### Lock timeout

External tasks need to be locked. So processing should not take more time than the lock timeout. The default timeout is a minute. If the lock timeout expires (for example due to system failures) then the task will be retried in an one minute interval. A non standard lock timeout can be defined on method regristration.

### Backkoff retry handling

The retry counter is provided which has to be passed to the RetryableException to use the retry mechanism. The exception's constructor takes the configuration values for backoff retry behavior (see Javadoc of RetryableException).

```
  public Map<String, Object> processServiceTask1(String processInstanceId, String activityId, String executionId,
         Map<String, Object> variables, Integer retries) throws BpmnError, RetryableException, Exception {
    throw new RetryableException("failed", 5, retries, List.of(5000l, 60000l)); // retry after 5 seconds and afterwards after 1 minute
  }
```

## Spring

### Preconditions

Asynchronous and scheduled task processing musted be configured properly. 

Find the class `org.camunda.bpm.externaltask.spring.AsyncConfiguration` in `src/test/java` as an example how this can be achieved.

### Lock timeout

The default lock timeout can be configured using the property `camunda.bpm.externaltask-handler.default-locktimeout`.

## CDI

