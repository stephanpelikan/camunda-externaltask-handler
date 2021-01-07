package org.camunda.bpm.externaltask.cdi;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;

@Alternative
public class TestProducer {

    @Produces
    @ApplicationScoped
    public ExternalTaskService produceExternalTaskService() {
        
        return CdiExternalTaskHandlerIT.processEngineRule.getExternalTaskService();
        
    }
    
    @Produces
    @ApplicationScoped
    public RuntimeService produceRuntimeService() {
        
        return CdiExternalTaskHandlerIT.processEngineRule.getRuntimeService();
        
    }
    
    @Produces
    @ApplicationScoped
    public RepositoryService produceRepositoryService() {
        
        return CdiExternalTaskHandlerIT.processEngineRule.getRepositoryService();
        
    }
    
    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Produces
    @Default
    @RequestScoped
    public EntityManager create()
    {
        return this.entityManagerFactory.createEntityManager();
    }

    public void dispose(@Disposes @Default EntityManager entityManager)
    {
        if (entityManager.isOpen())
        {
            entityManager.close();
        }
    }    
    
    @Produces
    public TimerService produceTimersService(InjectionPoint injectionPoint) {
        
        return new TimerService() {
            
            @Override
            public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createTimer(long initialDuration, long intervalDuration, Serializable info)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createTimer(Date expiration, Serializable info)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createTimer(long duration, Serializable info)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(duration);
                        } catch (InterruptedException e) {
                            return;
                        }
                        
                        Arrays.stream(injectionPoint.getClass().getDeclaredMethods())
                                .filter(m -> m.isAnnotationPresent(Timeout.class))
                                .findFirst()
                                .ifPresent(m -> {
                                    try {
                                        m.invoke(injectionPoint.getBean(), info);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    }
                }, "timer").start();

                return null;
                
            }
            
            @Override
            public Timer createSingleActionTimer(long duration, TimerConfig timerConfig)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Timer createCalendarTimer(ScheduleExpression schedule)
                    throws IllegalArgumentException, IllegalStateException, EJBException {
                throw new UnsupportedOperationException();
            }
            
        };
        
    }
    
}
