/**
 * Copyright (C) 2011, 2012 camunda services GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.container.impl.jboss.service;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.camunda.bpm.container.impl.jboss.metadata.ManagedProcessEngineMetadata;
import org.camunda.bpm.container.impl.jboss.util.Tccl;
import org.camunda.bpm.container.impl.jboss.util.Tccl.Operation;
import org.camunda.bpm.container.impl.jmx.services.JmxManagedProcessEngineController;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.JtaProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator;
import org.jboss.as.connector.subsystems.datasources.DataSourceReferenceFactoryService;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.Services;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;


/**
 * <p>Service responsible for starting / stopping a managed process engine inside the Msc</p>
 * 
 * <p>This service is used for managing process engines that are started / stopped 
 * through the application server management infrastructure.</p>
 * 
 * <p>This is the Msc counterpart of the {@link JmxManagedProcessEngineController}</p>
 * 
 * @author Daniel Meyer
 */
public class MscManagedProcessEngineController extends MscManagedProcessEngine {
  
  private final static Logger LOGGER = Logger.getLogger(MscManagedProcessEngineController.class.getName());
  
  protected InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();
    
  // Injecting these values makes the MSC aware of our dependencies on these resources.
  // This ensures that they are available when this service is started
  protected final InjectedValue<TransactionManager> transactionManagerInjector = new InjectedValue<TransactionManager>();
  protected final InjectedValue<DataSourceReferenceFactoryService> datasourceBinderServiceInjector = new InjectedValue<DataSourceReferenceFactoryService>();
  protected final InjectedValue<MscRuntimeContainerDelegate> containerPlatformServiceInjector = new InjectedValue<MscRuntimeContainerDelegate>();
  protected final InjectedValue<ContainerJobExecutorService> containerJobExecutorInjector = new InjectedValue<ContainerJobExecutorService>();
    
  protected ManagedProcessEngineMetadata processEngineMetadata;

  protected JtaProcessEngineConfiguration processEngineConfiguration;
  
  /**
   * Instantiate  the process engine controller for a process engine configuration. 
   * 
   */
  public MscManagedProcessEngineController(ManagedProcessEngineMetadata processEngineConfiguration) {
    this.processEngineMetadata = processEngineConfiguration;
  }
  
  public void start(final StartContext context) throws StartException {
    context.asynchronous();    
    executorInjector.getValue().submit(new Runnable() {
      public void run() {
        try {
          start();
          context.complete();          
        } catch (Throwable e) {
          context.failed(new StartException(e));
        }
      }
    });
  }
  
  public void stop(final StopContext context) {
    context.asynchronous();    
    executorInjector.getValue().submit(new Runnable() {
      public void run() {
        try {
          
          try {
            processEngine.close();      
          } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "exception while closing process engine", e);
          }      
          releaseJobExecutorDelegate();  
          
        } finally {
          context.complete();
        }
      }
    });
  }
    
  public void start() {        
    // setting the TCCL to the Classloader of this module.
    // this exploits a hack in MyBatis allowing it to use the TCCL to load the 
    // mapping files from the process engine module
    Tccl.runUnderClassloader(new Operation<Void>() {
      public Void run() {        
        startProcessEngine();
        return null;
      }

    }, ProcessEngine.class.getClassLoader());   
    
    setJobExecutorDelegate();    
  }
    
  protected void startProcessEngine() {
    
    processEngineConfiguration = new JtaProcessEngineConfiguration();
        
    // set the name for the process engine 
    processEngineConfiguration.setProcessEngineName(processEngineMetadata.getEngineName());
    
    // set UUid generator
    // TODO: move this to configuration and use as default?
    processEngineConfiguration.setIdGenerator(new StrongUuidGenerator());
    
    // use the injected datasource
    processEngineConfiguration.setDataSource((DataSource) datasourceBinderServiceInjector.getValue().getReference().getInstance());
    
    // use the injected transaction manager
    processEngineConfiguration.setTransactionManager(transactionManagerInjector.getValue());    
    
    // set the value for the history
    processEngineConfiguration.setHistory(processEngineMetadata.getHistoryLevel());
    
    // set auto schema update
    if(processEngineMetadata.isAutoSchemaUpdate()) {
      processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    }

    // set db table prefix
    if( processEngineMetadata.getDbTablePrefix() != null ) {
      processEngineConfiguration.setDatabaseTablePrefix(processEngineMetadata.getDbTablePrefix());
    }
    
    processEngine = processEngineConfiguration.buildProcessEngine();        
  }

  public Injector<TransactionManager> getTransactionManagerInjector() {
    return transactionManagerInjector;
  }

  public Injector<DataSourceReferenceFactoryService> getDatasourceBinderServiceInjector() {
    return datasourceBinderServiceInjector;
  }
    
  public InjectedValue<MscRuntimeContainerDelegate> getContainerPlatformServiceInjector() {
    return containerPlatformServiceInjector;
  }
  
  public InjectedValue<ContainerJobExecutorService> getContainerJobExecutorInjector() {
    return containerJobExecutorInjector;
  }

  public static void initializeServiceBuilder(ManagedProcessEngineMetadata processEngineConfiguration, MscManagedProcessEngineController service,
          ServiceBuilder<ProcessEngine> serviceBuilder) {
    
    ContextNames.BindInfo datasourceBindInfo = ContextNames.bindInfoFor(processEngineConfiguration.getDatasourceJndiName());
    serviceBuilder.addDependency(ServiceName.JBOSS.append("txn").append("TransactionManager"), TransactionManager.class, service.getTransactionManagerInjector())
      .addDependency(datasourceBindInfo.getBinderServiceName(), DataSourceReferenceFactoryService.class, service.getDatasourceBinderServiceInjector())
      .addDependency(ServiceNames.forMscRuntimeContainerDelegate(), MscRuntimeContainerDelegate.class, service.getContainerPlatformServiceInjector())
      .addDependency(ContainerJobExecutorService.getServiceName(), ContainerJobExecutorService.class, service.getContainerJobExecutorInjector())            
      .setInitialMode(Mode.ACTIVE);
    
    Services.addServerExecutorDependency(serviceBuilder, service.getExecutorInjector(), false);
    
  }

  protected void setJobExecutorDelegate() {

    String jobAcquisitionName = processEngineMetadata.getJobExecutorAcquisitionName();
    
    if (jobAcquisitionName != null) {
      // register the process engine
      JobExecutor jobExecutorDelegate = containerJobExecutorInjector.getValue().registerProcessEngine(processEngineConfiguration, jobAcquisitionName);
      processEngineConfiguration.setJobExecutor(jobExecutorDelegate);
    }

  }
  
  protected void releaseJobExecutorDelegate() {

    String jobAcquisitionName = processEngineMetadata.getJobExecutorAcquisitionName();
    
    if (jobAcquisitionName != null) {
      // register the process engine
      containerJobExecutorInjector.getValue().unregisterProcessEngine(processEngineConfiguration, jobAcquisitionName);
      processEngineConfiguration.setJobExecutor(null);
    }

  }

  public ProcessEngine getProcessEngine() {
    return processEngine;
  }
  
  public InjectedValue<ExecutorService> getExecutorInjector() {
    return executorInjector;
  }
  
}
