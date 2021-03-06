package org.camunda.bpm.container.impl.jobexecutor.ejb;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.resource.ResourceException;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.camunda.bpm.engine.impl.jobexecutor.tobemerged.JobExecutorService;
import org.camunda.bpm.engine.impl.jobexecutor.tobemerged.ra.outbound.PlatformJobExecutorConnection;
import org.camunda.bpm.engine.impl.jobexecutor.tobemerged.ra.outbound.PlatformJobExecutorConnectionFactory;
import org.camunda.bpm.engine.impl.jobexecutor.tobemerged.spi.JobAcquisitionConfiguration;

/**
 * Bean exposing the JCA implementation of the {@link JobExecutorService} as
 * as stateless bean.
 * 
 * @author Daniel Meyer
 *
 */
@Stateless
@Local(JobExecutorService.class)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class PlatformJobExecutorBean implements JobExecutorService {
  
  @Resource(mappedName="eis/PlatformJobExecutorConnectionFactory")
  protected PlatformJobExecutorConnectionFactory platformJobExecutorConnectionFactory;
  
  protected PlatformJobExecutorConnection platformJobExecutorConnection;

  @PostConstruct
  protected void openConnection() {
    try {
      platformJobExecutorConnection = platformJobExecutorConnectionFactory.getConnection();
    } catch (ResourceException e) {
      throw new ProcessEngineException("Could not open connection to platform job executor resource ", e);
    } 
  }
  
  @PreDestroy
  protected void closeConnection() {
    if(platformJobExecutorConnection != null) {
      platformJobExecutorConnection.closeConnection();
    }
  }

  public JobExecutor startJobAcquisition(JobAcquisitionConfiguration configuration) {
    return platformJobExecutorConnection.startJobAcquisition(configuration);
  }

  public void stopJobAcquisition(String jobAcquisitionName) {
    platformJobExecutorConnection.stopJobAcquisition(jobAcquisitionName);
  }

  public JobExecutor registerProcessEngine(ProcessEngineConfigurationImpl processEngineConfiguration, String acquisitionName) {
    return platformJobExecutorConnection.registerProcessEngine(processEngineConfiguration, acquisitionName);
  }

  public void unregisterProcessEngine(ProcessEngineConfigurationImpl processEngineConfiguration, String acquisitionName) {
    platformJobExecutorConnection.unregisterProcessEngine(processEngineConfiguration, acquisitionName);
  }

  public JobExecutor getJobAcquisitionByName(String name) {
    return platformJobExecutorConnection.getJobAcquisitionByName(name);
  }

  public List<JobExecutor> getJobAcquisitions() {
    return platformJobExecutorConnection.getJobAcquisitions();
  }
  
}
