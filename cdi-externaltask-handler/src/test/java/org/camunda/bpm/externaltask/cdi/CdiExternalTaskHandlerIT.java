package org.camunda.bpm.externaltask.cdi;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.testcontrol.api.TestControl;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@Ignore // to much is not supported by deltaspike: @Asynchronous, TimerService, etc.
@RunWith(CdiTestRunner.class)
@TestControl(startScopes = { ApplicationScoped.class, RequestScoped.class })
@Deployment(resources = {"test-external-task.bpmn"})
public class CdiExternalTaskHandlerIT {

    private static String TESTPROCESS_DEFINITION_KEY = "ExternalTaskProcess";

    private static String TESTPROCESS_TESTTOPIC = "TestTopic";
    
    private static List<Long> TEST_TIMEOUTS;
    
    static {
        TEST_TIMEOUTS = new LinkedList<>();
        TEST_TIMEOUTS.add(1000l);
        TEST_TIMEOUTS.add(2000l);
    }
    
    @ClassRule
    public static ProcessEngineRule processEngineRule = new ProcessEngineRule();
    
    @Inject
    private EntityManager entityManager;
    
    @Inject
    private ExternalTaskHandler externalTaskHandler;
    
    @Test
    public void testExternalTaskHandling() {

        final boolean[] processorCalled = new boolean[] { false };
        
        externalTaskHandler
                .registerExternalTaskProcessor(TESTPROCESS_DEFINITION_KEY, TESTPROCESS_TESTTOPIC, 
                        (processInstanceId, activityId, executionId, variables, retries) -> setVariableProcessor(processorCalled, processInstanceId, variables));

        final String processInstanceId = processEngineRule
                .getRuntimeService()
                .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                .getProcessInstanceId();
        
        synchronized (processorCalled) {
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
        
        final HistoricProcessInstance processInstance = processEngineRule
                .getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);
        
        final Map<String, Object> variablesSetByProcessor = processEngineRule
                .getHistoryService()
                .createHistoricVariableInstanceQuery()
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

        final String processInstanceId = processEngineRule
                .getRuntimeService()
                .startProcessInstanceByKey(TESTPROCESS_DEFINITION_KEY)
                .getProcessInstanceId();
        
        synchronized (processorCalled) {
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
        
        final HistoricProcessInstance processInstance = processEngineRule
                .getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);
        
        final Map<String, Object> variablesSetByProcessor = processEngineRule
                .getHistoryService()
                .createHistoricVariableInstanceQuery()
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

    private Map<String, Object> variables(String... variables) {
        
        final Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < variables.length; i += 2) {
            result.put(variables[i], variables[i + 1]);
        }
        return result;
        
    }

}
