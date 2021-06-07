package org.camunda.bpm.externaltask.spring;

import static org.camunda.bpm.externaltask.spring.SpringExternalTaskSyncProcessingIT.variables;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithResult;
import org.camunda.bpm.externaltask.spi.BpmnErrorWithVariables;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.camunda.bpm.externaltask.spi.RetryableException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Import({ AsyncConfiguration.class })
public class SpringExternalTaskAsyncProcessingIT {

    private static String TESTPROCESS_DEFINITION_KEY = "ExternalTaskProcess";

    private static String TESTPROCESS_TESTTOPIC = "TestTopic";
    
    private static List<Long> TEST_TIMEOUTS;

    static {
        TEST_TIMEOUTS = new LinkedList<>();
        TEST_TIMEOUTS.add(1000l);
        TEST_TIMEOUTS.add(2000l);
    }

    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private HistoryService historyService;

    @Autowired
    private ExternalTaskHandler externalTaskHandler;
    
    @Autowired
    private ManagementService managementService;

    @Test
    public void testExternalTaskHandling() throws Exception {

        final String[] processorCalled = new String[] { null, null, null };
        
        final String asyncFeedback = "Thank you!";

        externalTaskHandler
                .<String, String>registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY,
                        TESTPROCESS_TESTTOPIC,
                        (correlationId, processInstanceId, businessKey, activityId, executionId, variables,
                                retries) -> processRequest(processorCalled, correlationId),
                        (processInstanceId, businessKey, activityId, executionId, retries, correlationId,
                                response, variablesToBeSet) -> setVariableProcessor(processorCalled,
                                        correlationId,
                                        response,
                                        variablesToBeSet,
                                        asyncFeedback));

        String processInstanceId = null;

        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("request processor not called!", processorCalled[0]);

        final String responseValue = "Yeah";
        
        synchronized (processorCalled) {
            final String feedback = externalTaskHandler.handleAsyncInput(processorCalled[0], responseValue);

            Assert.assertNotNull("feedback of handleAsyncResponse is null", feedback);
            Assert.assertTrue("feedback of handleAsyncResponse is unexpected", asyncFeedback.equals(feedback));

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertNotNull("response processor not called!", processorCalled[1]);
        Assert.assertNotNull("response processor didn't receive async response value!", processorCalled[2]);

        Assert
                .assertTrue("correlation of response does not match request",
                        processorCalled[1].equals(processorCalled[0]));
        Assert.assertTrue("value of response changed", processorCalled[2].equals(responseValue));
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }
        
        final HistoricProcessInstance processInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);
        
        final Map<String, Object> variablesSetByProcessor = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("Variable not set", variables("test", "success"), variablesSetByProcessor);

    }

    private Date processRequest(String[] result, String correlationId) throws BpmnError, RetryableException, Exception {

        result[0] = correlationId;

        synchronized (result) {
            result.notify();
        }

        return null;

    }

    private String setVariableProcessor(String[] result, String correlationId, String response,
            Map<String, Object> variablesToBeSet, String feedback)
            throws BpmnError, RetryableException, Exception {
        
        result[1] = correlationId;
        result[2] = response;
        
        synchronized (result) {
            result.notify();
        }
        
        variablesToBeSet.put("test", "success");

        return feedback;
        
    }

    @Test
    public void testExternalTaskBpmnError() throws Exception {

        final String[] processorCalled = new String[] { null, null, null };
        
        final String asyncFeedback = "Thank you!";

        externalTaskHandler
                .<String, String>registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY,
                        TESTPROCESS_TESTTOPIC,
                        (correlationId, processInstanceId, businessKey, activityId, executionId, variables,
                                retries) -> processRequest(processorCalled, correlationId),
                        (processInstanceId, businessKey, activityId, executionId, retries, correlationId,
                                response, variablesToBeSet) -> throwBpmnErrorProcessor(processorCalled,
                                        correlationId, response, asyncFeedback));

        String processInstanceId = null;

        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("request processor not called!", processorCalled[0]);

        final String responseValue = "Yeah";
        
        synchronized (processorCalled) {
            final String feedback = externalTaskHandler.handleAsyncInput(processorCalled[0], responseValue);

            Assert.assertNotNull("feedback of handleAsyncResponse is null", feedback);
            Assert.assertTrue("feedback of handleAsyncResponse is unexpected", asyncFeedback.equals(feedback));
            
            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertNotNull("response processor not called!", processorCalled[1]);
        Assert.assertNotNull("response processor didn't receive async response value!", processorCalled[2]);

        Assert
                .assertTrue("correlation of response does not match request",
                        processorCalled[1].equals(processorCalled[0]));
        Assert.assertTrue("value of response changed", processorCalled[2].equals(responseValue));
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }
        
        final HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);
        
        final Map<String, Object> variablesSetByProcessor = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("BPMN Error not processed", variables("error", "failed"), variablesSetByProcessor);

    }

    private String throwBpmnErrorProcessor(String[] result, String correlationId, String response, String feedback)
            throws BpmnError, RetryableException, Exception {
        
        result[1] = correlationId;
        result[2] = response;
        
        synchronized (result) {
            result.notify();
        }
        
        throw new BpmnErrorWithResult("error", "failed", feedback);
        
    }

    @Test
    public void testExternalTaskBpmnErrorWithVariables() throws Exception {

        final String[] processorCalled = new String[] { null, null, null };
        
        externalTaskHandler
                .<Void, String>registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY,
                        TESTPROCESS_TESTTOPIC,
                        (correlationId, processInstanceId, businessKey, activityId, executionId, variables,
                                retries) -> processRequest(processorCalled, correlationId),
                        (processInstanceId, businessKey, activityId, executionId, retries, correlationId,
                                response, variableToBeSet) -> throwBpmnErrorWithVariablesProcessor(processorCalled,
                                        correlationId, response, variableToBeSet));

        String processInstanceId = null;

        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("request processor not called!", processorCalled[0]);

        final String responseValue = "Yeah";
        
        synchronized (processorCalled) {
            externalTaskHandler.handleAsyncInput(processorCalled[0], responseValue);

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertNotNull("response processor not called!", processorCalled[1]);
        Assert.assertNotNull("response processor didn't receive async response value!", processorCalled[2]);

        Assert
                .assertTrue("correlation of response does not match request",
                        processorCalled[1].equals(processorCalled[0]));
        Assert.assertTrue("value of response changed", processorCalled[2].equals(responseValue));
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }
        
        final HistoricProcessInstance processInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);
        
        final Map<String, Object> variablesSetByProcessor = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert
                .assertEquals("BPMN Error not processed",
                        variables("error", "failed", "test", "success"),
                        variablesSetByProcessor);

    }

    private Void throwBpmnErrorWithVariablesProcessor(String[] result, String correlationId, String response,
            Map<String, Object> variableToBeSet) throws BpmnError, RetryableException, Exception {
        
        result[1] = correlationId;
        result[2] = response;
        
        synchronized (result) {
            result.notify();
        }
        
        throw new BpmnErrorWithVariables("error", "failed", variables("test", "success"));
        
    }

    @Test
    public void testExternalTaskAsyncResponseFailedToProcess() throws Exception {

        final String[] processorCalled = new String[] { null, null, null };
        
        externalTaskHandler
                .<Void, String>registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY,
                        TESTPROCESS_TESTTOPIC,
                        (correlationId, processInstanceId, businessKey, activityId, executionId, variables,
                                retries) -> processRequest(processorCalled, correlationId),
                        (processInstanceId, businessKey, activityId, executionId, retries, correlationId,
                                response,
                                variablesToBeSet) -> throwExceptionProcessor(processorCalled, correlationId, response));

        String processInstanceId = null;

        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("request processor not called!", processorCalled[0]);

        final String responseValue = "Yeah";
        
        synchronized (processorCalled) {
            try {
                externalTaskHandler.handleAsyncInput(processorCalled[0], responseValue);
                Assert.fail("Expected to catch exception");
            } catch (Exception e) {
                Assert.assertTrue(e.getMessage().equals("failed"));
            }

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final HistoricProcessInstance processInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process ended but shouldn't", processInstance.getEndTime() == null);

        final Map<String, Object> variablesSetByProcessor = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("BPMN Error not processed", variables(), variablesSetByProcessor);

    }

    @Test
    public void testExternalTaskAsyncResponseTimedOut() throws Exception {

        final String[] processorCalled = new String[] { null, null, null };

        final String asyncFeedback = "Thank you!";

        externalTaskHandler
                .<String, String>registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY,
                        TESTPROCESS_TESTTOPIC,
                        (correlationId, processInstanceId, businessKey, activityId, executionId, variables,
                                retries) -> processRequest(processorCalled, correlationId),
                        (processInstanceId, businessKey, activityId, executionId, retries, correlationId, response,
                                variablesToBeSet) -> setVariableProcessor(processorCalled,
                                        correlationId,
                                        response,
                                        variablesToBeSet,
                                        asyncFeedback))
                .responseTimeout(3000l)
                .responseTimeoutExpiredMessage("TIMEOUT");

        String processInstanceId = null;

        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("request processor not called!", processorCalled[0]);

        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final Job job1 = managementService.createJobQuery()
                .active()
                .executable()
                .messages() // external task timeout is implemented as a message job
                .singleResult();
        Assert.assertNull("Got executable timer job, but unexpected!", job1);
        final Incident incident1 = runtimeService
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertNull("Got incident, but unexpected!", incident1);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final Job job2 = managementService.createJobQuery()
                .active()
                .executable()
                .messages() // external task timeout is implemented as a message job
                .singleResult();
        Assert.assertNotNull("Got no executable timer job!", job2);
        managementService.executeJob(job2.getId());

        final Incident incident2 = runtimeService
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertNotNull("Expected incident, but got none!", incident2);
        Assert.assertEquals("Unexpected incident message", "TIMEOUT", incident2.getIncidentMessage());

        final String responseValue = "Yeah";

        synchronized (processorCalled) {
            final String feedback = externalTaskHandler.handleAsyncInput(processorCalled[0], responseValue);

            Assert.assertNotNull("feedback of handleAsyncResponse is null", feedback);
            Assert.assertTrue("feedback of handleAsyncResponse is unexpected", asyncFeedback.equals(feedback));

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("response processor not called!", processorCalled[1]);
        Assert.assertNotNull("response processor didn't receive async response value!", processorCalled[2]);

        Assert
                .assertTrue("correlation of response does not match request",
                        processorCalled[1].equals(processorCalled[0]));
        Assert.assertTrue("value of response changed", processorCalled[2].equals(responseValue));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final HistoricProcessInstance processInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);

        final Map<String, Object> variablesSetByProcessor = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("Variable not set", variables("test", "success"), variablesSetByProcessor);

    }

    @Test
    public void testExternalTaskAsyncResponseTimedOutCustomTimeout() throws Exception {

        final String[] processorCalled = new String[] { null, null, null };

        final String asyncFeedback = "Thank you!";

        externalTaskHandler
                .<String, String>registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY,
                        TESTPROCESS_TESTTOPIC,
                        (correlationId, processInstanceId, businessKey, activityId, executionId, variables,
                                retries) -> {
                            processRequest(processorCalled, correlationId);
                            return new Date(System.currentTimeMillis() + 6000);
                        },
                        (processInstanceId, businessKey, activityId, executionId, retries, correlationId, response,
                                variablesToBeSet) -> setVariableProcessor(processorCalled,
                                        correlationId,
                                        response,
                                        variablesToBeSet,
                                        asyncFeedback))
                .responseTimeout(3000l)
                .responseTimeoutExpiredMessage("TIMEOUT");

        String processInstanceId = null;

        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("request processor not called!", processorCalled[0]);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final Job job1 = managementService
                .createJobQuery()
                .active()
                .executable()
                .messages() // external task timeout is implemented as a message job
                .singleResult();
        Assert.assertNull("Got executable timer job, but unexpected!", job1);
        final Incident incident1 = runtimeService
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertNull("Got incident, but unexpected!", incident1);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final Job job2 = managementService
                .createJobQuery()
                .active()
                .executable()
                .messages() // external task timeout is implemented as a message job
                .singleResult();
        Assert.assertNull("Got executable timer job, but unexpected!", job2);
        final Incident incident2 = runtimeService
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertNull("Got incident, but unexpected!", incident2);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final Job job3 = managementService
                .createJobQuery()
                .active()
                .executable()
                .messages() // external task timeout is implemented as a message job
                .singleResult();
        Assert.assertNotNull("Got no executable timer job!", job3);
        managementService.executeJob(job3.getId());

        final Incident incident3 = runtimeService
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertNotNull("Expected incident, but got none!", incident3);
        Assert.assertEquals("Unexpected incident message", "TIMEOUT", incident3.getIncidentMessage());

        final String responseValue = "Yeah";

        synchronized (processorCalled) {
            final String feedback = externalTaskHandler.handleAsyncInput(processorCalled[0], responseValue);

            Assert.assertNotNull("feedback of handleAsyncResponse is null", feedback);
            Assert.assertTrue("feedback of handleAsyncResponse is unexpected", asyncFeedback.equals(feedback));

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertNotNull("response processor not called!", processorCalled[1]);
        Assert.assertNotNull("response processor didn't receive async response value!", processorCalled[2]);

        Assert
                .assertTrue("correlation of response does not match request",
                        processorCalled[1].equals(processorCalled[0]));
        Assert.assertTrue("value of response changed", processorCalled[2].equals(responseValue));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }

        final HistoricProcessInstance processInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);

        final Map<String, Object> variablesSetByProcessor = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("Variable not set", variables("test", "success"), variablesSetByProcessor);

    }

    private Void throwExceptionProcessor(String[] result, String correlationId, String response)
            throws BpmnError, RetryableException, Exception {
        
        result[1] = correlationId;
        result[2] = response;
        
        throw new Exception("failed");
        
    }

    @SpringBootApplication
    static class TestCamundaApplication {
    }
    
    @TestConfiguration
    @Order(0)
    static class TestCamundaApplicationConfiguration {
        
        @Bean(name = "workerId")
        public String getWorkerId() {
            return "testWorker";
        }
        
    }
    
}
