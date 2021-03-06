package org.camunda.bpm.container.impl.ejb;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.camunda.bpm.ProcessApplicationService;
import org.camunda.bpm.ProcessEngineService;
import org.camunda.bpm.container.RuntimeContainerDelegate;
import org.camunda.bpm.container.impl.ejb.deployment.EjbJarAttachments;
import org.camunda.bpm.container.impl.ejb.deployment.EjbJarParsePlatformXmlStep;
import org.camunda.bpm.container.impl.jmx.JmxRuntimeContainerDelegate;
import org.camunda.bpm.container.impl.jmx.deployment.PlatformXmlStartProcessEnginesStep;
import org.camunda.bpm.container.impl.jmx.deployment.StopProcessApplicationsStep;
import org.camunda.bpm.container.impl.jmx.deployment.StopProcessEnginesStep;


/**
 * <p>Bootstrap for the camunda BPM platform using a singleton EJB</p>
 *   
 * @author Daniel Meyer
 */
@Startup
@LocalBean
@Singleton(name="BpmPlatformBootstrap")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class EjbBpmPlatformBootstrap {

  final private static Logger LOGGER = Logger.getLogger(EjbBpmPlatformBootstrap.class.getName());
  
  @Resource(description="The location of the bpm-platform.xml file.")
  private String bpmPlatformXmlLocation = "META-INF/bpm-platform.xml";

  protected ProcessEngineService processEngineService;
  protected ProcessApplicationService processApplicationService; 

  @PostConstruct
  public void start() {

    final JmxRuntimeContainerDelegate containerDelegate = getContainerDelegate();
    
    containerDelegate.getServiceContainer().createDeploymentOperation("deploying camunda BPM platform")
      .addAttachment(EjbJarAttachments.BPM_PLATFORM_RESOURCE, bpmPlatformXmlLocation)
      .addStep(new EjbJarParsePlatformXmlStep())
      .addStep(new PlatformXmlStartProcessEnginesStep())
      .execute();
    
    processEngineService = containerDelegate.getProcessEngineService();
    processApplicationService = containerDelegate.getProcessApplicationService();
    
    LOGGER.log(Level.INFO, "camunda BPM platform started successfully.");
  }
  
  @PreDestroy
  public void stop() {
    
    final JmxRuntimeContainerDelegate containerDelegate = getContainerDelegate();
    
    containerDelegate.getServiceContainer().createUndeploymentOperation("undeploying camunda BPM platform")
      .addStep(new StopProcessApplicationsStep())
      .addStep(new StopProcessEnginesStep())
      .execute();
    
    LOGGER.log(Level.INFO, "camunda BPM platform stopped.");

  }
  
  protected JmxRuntimeContainerDelegate getContainerDelegate() {
    return (JmxRuntimeContainerDelegate) RuntimeContainerDelegate.INSTANCE.get();
  }
  
  // getters //////////////////////////////////////////////
  
  public ProcessEngineService getProcessEngineService() {
    return processEngineService;
  }
  
  public ProcessApplicationService getProcessApplicationService() {
    return processApplicationService;
  }

}
