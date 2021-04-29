package org.camunda.bpm.externaltask;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.externaltask.spi.ExternalTaskHandler;
import org.camunda.bpm.externaltask.spi.RetryableException;
import org.junit.Assert;

@Path("/async-test")
public class TestAsyncApi {

    @Inject
    private RuntimeService runtimeService;
    
    @Inject
    private HistoryService historyService;
    
    @EJB
    private ExternalTaskHandler externalTaskHandler;
    
    private static int[] called = new int[] { 0 };
    
    private static String[] testCorrelationIds = new String[] { null };

    @PostConstruct
    public void init() {
        
        externalTaskHandler
                .<String, String>registerExternalTaskProcessor("AsyncExternalTaskProcess",
                        "AsyncTestTopic",
                (correlationId, processInstanceId, activityId, executionId, variables, retries) -> {
                    
                    called[0]++;
                    testCorrelationIds[0] = correlationId;
                    synchronized (called) {
                        called.notify();
                    }
                    
                    // testRetry
                    if (variables.get("retry") != null) {
                        final List<Long> retryTimeouts = new LinkedList<>();
                        retryTimeouts.add(1000l);
                        retryTimeouts.add(2000l);
                        throw new RetryableException("failed with incident", 2, retries, retryTimeouts);
                    }
                    
                },
                (processInstanceId, activityId, executionId, retries, correlationId, input, variablesToBeSet) -> {

                            Assert.assertTrue("mismatching correlationId", testCorrelationIds[0].equals(correlationId));

                    // testHandle
                    variablesToBeSet.put("status", "success");

                    return input + " -> processed";

                });
        
    }
    
    @GET
    @Path("/handle")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String testHandle() throws Exception {
        
        called[0] = 0;
        
        final ProcessInstance instance = runtimeService.startProcessInstanceByKey("AsyncExternalTaskProcess");
        
        synchronized (called) {
            try {
                called.wait(5000);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // 
            }
        }

        final String result = externalTaskHandler.handleAsyncInput(testCorrelationIds[0], "OK");
        Assert.assertTrue("Unexpected result", "OK -> processed".equals(result));

        final HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        Assert.assertTrue("Process not ended", processInstance.getEndTime() != null);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //
        }
        
        final Map<String, Object> variablesSetByProcessor = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("Variable not set", variables("status", "success"), variablesSetByProcessor);
        
        return "passed";
        
    }
    
    @GET
    @Path("/retry")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String testRetry() {
        
        final Map<String, Object> variables = new HashMap<>();
        variables.put("retry", Boolean.TRUE);

        called[0] = 0;
        
        final ProcessInstance instance = runtimeService
                .startProcessInstanceByKey("AsyncExternalTaskProcess", variables);

        synchronized (called) {
            try {
                called.wait(5000);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }
        
        Assert.assertTrue("processor not called!", called[0] == 1);

        synchronized (called) {
            try {
                called.wait(1000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertTrue("processor not called!", called[0] == 2);

        synchronized (called) {
            try {
                called.wait(2500);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted");
            }
        }

        Assert.assertTrue("processor not called!", called[0] == 3);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted");
        }
        
        final HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        Assert.assertTrue("Process ended", processInstance.getEndTime() == null);

        final Map<String, Object> variablesSetByProcessor = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));
        Assert.assertEquals("Variable not set", variables, variablesSetByProcessor);

        final Incident incident = runtimeService.createIncidentQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        Assert.assertNotNull(incident);
        Assert.assertEquals("failed with incident", incident.getIncidentMessage());
        
        return "passed";
        
    }

    private static Map<String, Object> variables(String... variables) {
        
        final Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < variables.length; i += 2) {
            result.put(variables[i], variables[i + 1]);
        }
        return result;
        
    }
    
}
