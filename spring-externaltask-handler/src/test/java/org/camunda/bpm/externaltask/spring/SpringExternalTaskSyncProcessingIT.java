package org.camunda.bpm.externaltask.spring;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.Incident;
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
public class SpringExternalTaskSyncProcessingIT {

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
    private ExternalTaskService externalTaskService;

    @Autowired
    private ExternalTaskHandler externalTaskHandler;
    
    @Test
    public void testExternalTaskHandling() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> setVariableProcessor(processorCalled, processInstanceId, variables));

        String processInstanceId = null;
        
        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(50000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
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
        Assert.assertEquals("Variable not set", variables("test", "success"), variablesSetByProcessor);

    }

    private Map<String, Object> setVariableProcessor(boolean[] result, String processInstanceId, Map<String, Object> variables)
            throws BpmnError {
        
        result[0] = true;
        
        synchronized (result) {
            result.notify();
        }
        
        return variables("test", "success");
        
    }

    @Test
    public void testExternalTaskBpmnError() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> throwBpmnErrorProcessor(processorCalled, processInstanceId, variables));

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
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
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

    private Map<String, Object> throwBpmnErrorProcessor(boolean[] result, String processInstanceId, Map<String, Object> variables)
            throws BpmnError {
        
        result[0] = true;
        
        synchronized (result) {
            result.notify();
        }
        
        throw new BpmnError("error", "failed");
        
    }

    @Test
    public void testExternalTaskBpmnErrorWithVariables() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> throwBpmnErrorWithVariablesProcessor(processorCalled, processInstanceId, variables));

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
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
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
        Assert.assertEquals("BPMN Error not processed", variables("error", "failed", "test", "success"), variablesSetByProcessor);

    }

    private Map<String, Object> throwBpmnErrorWithVariablesProcessor(boolean[] result, String processInstanceId, Map<String, Object> variables)
            throws BpmnError {
        
        result[0] = true;
        
        synchronized (result) {
            result.notify();
        }
        
        throw new BpmnErrorWithVariables("error", "failed", variables("test", "success"));
        
    }

    @Test
    public void testExternalTaskIncident() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> throwExceptionProcessor(processorCalled, processInstanceId, variables, retries));

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
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }
        
        final HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process ended", processInstance.getEndTime() == null);
        
        final Map<String, Object> variablesSetByProcessor = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("BPMN Error not processed", variables(), variablesSetByProcessor);
        
        final Incident incident = runtimeService.createIncidentQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertNotNull(incident);
        Assert.assertEquals("failed with incident", incident.getIncidentMessage());
        
        final String details = externalTaskService.getExternalTaskErrorDetails(incident.getConfiguration());
        Assert.assertNotNull(details);
        Assert.assertTrue(details.indexOf(incident.getIncidentMessage()) != -1);

    }

    private Map<String, Object> throwExceptionProcessor(boolean[] result, String processInstanceId, Map<String, Object> variables, Integer retries)
            throws BpmnError, Exception {
        
        result[0] = true;
        synchronized (result) {
            result.notify();
        }
        
        throw new Exception("failed with incident");
        
    }

    @Test
    public void testExternalTaskIncidentWithRetries() {

        final int[] processorCalled = new int[] { 3 };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> throwRetryableExceptionProcessor(processorCalled, processInstanceId, variables, retries));

        String processInstanceId = null;
        
        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                    .getProcessInstanceId();

            try {
                processorCalled.wait(500);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertTrue("processor not called!", processorCalled[0] == 2);

        synchronized (processorCalled) {
            try {
                processorCalled.wait(1500);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertTrue("processor not called!", processorCalled[0] == 1);

        synchronized (processorCalled) {
            try {
                processorCalled.wait(2500);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertTrue("processor not called!", processorCalled[0] == 0);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }
        
        final HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process ended", processInstance.getEndTime() == null);
        
        final Incident incident = runtimeService.createIncidentQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertNotNull(incident);
        Assert.assertEquals("failed with incident", incident.getIncidentMessage());
        
    }
    
    private Map<String, Object> throwRetryableExceptionProcessor(int[] result, String processInstanceId, Map<String, Object> variables, Integer retries)
            throws BpmnError, RetryableException {
        
        result[0] -= 1;
        synchronized (result) {
            result.notify();
        }
        
        throw new RetryableException("failed with incident", 2, retries, TEST_TIMEOUTS);
        
    }
    
    @Test
    public void testExternalTaskHandlingFetchAllVariables() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> fetchVariableProcessor(processorCalled, processInstanceId, variables));

        String processInstanceId = null;
        
        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY, variables("initA", "A", "initB", "B"))
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
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
        Assert.assertEquals("BPMN Error not processed", variables("initA", "A1", "initB", "B1"), variablesSetByProcessor);
                
    }

    @Test
    public void testExternalTaskHandlingFetchNoVariables() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> fetchVariableProcessor(processorCalled, processInstanceId, variables),
                        true);

        String processInstanceId = null;
        
        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY, variables("initA", "A", "initB", "B"))
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
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
        Assert.assertEquals("BPMN Error not processed", variables("initA", "A", "initB", "B"), variablesSetByProcessor);
                
    }

    @Test
    public void testExternalTaskHandlingFetchDefinedVariables() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> fetchVariableProcessor(processorCalled, processInstanceId, variables),
                        "initA");

        String processInstanceId = null;
        
        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY, variables("initA", "A", "initB", "B"))
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
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
        Assert.assertEquals("BPMN Error not processed", variables("initA", "A1", "initB", "B"), variablesSetByProcessor);
                
    }

    @Test
    public void testExternalTaskHandlingFetchManyDefinedVariables() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> fetchVariableProcessor(processorCalled, processInstanceId, variables),
                        "initA", "initB");

        String processInstanceId = null;
        
        synchronized (processorCalled) {
            processInstanceId = runtimeService
                    .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY, variables("initA", "A", "initB", "B"))
                    .getProcessInstanceId();

            try {
                processorCalled.wait(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertTrue("processor not called!", processorCalled[0]);
        
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
        Assert.assertEquals("BPMN Error not processed", variables("initA", "A1", "initB", "B1"), variablesSetByProcessor);
                
    }

    private Map<String, Object> fetchVariableProcessor(boolean[] result, String processInstanceId, Map<String, Object> variables)
            throws BpmnError {
        
        result[0] = true;
        
        synchronized (result) {
            result.notify();
        }
        
        return variables.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), entry.getValue() + "1"))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        
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
    
    public static Map<String, Object> variables(String... variables) {
        
        final Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < variables.length; i += 2) {
            result.put(variables[i], variables[i + 1]);
        }
        return result;
        
    }
    
}
