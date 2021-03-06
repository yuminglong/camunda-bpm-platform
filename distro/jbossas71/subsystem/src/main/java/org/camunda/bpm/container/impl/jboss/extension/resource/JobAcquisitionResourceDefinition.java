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
package org.camunda.bpm.container.impl.jboss.extension.resource;

import org.camunda.bpm.container.impl.jboss.extension.BpmPlatformExtension;
import org.camunda.bpm.container.impl.jboss.extension.ModelConstants;
import org.camunda.bpm.container.impl.jboss.extension.handler.JobAcquisitionAdd;
import org.camunda.bpm.container.impl.jboss.extension.handler.JobAcquisitionRemove;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;



public class JobAcquisitionResourceDefinition extends SimpleResourceDefinition {

  private static final PathElement JOB_ACQUISTIONS_PATH  = PathElement.pathElement(ModelConstants.JOB_ACQUISITIONS);
  
  public JobAcquisitionResourceDefinition() {
    super(JOB_ACQUISTIONS_PATH, BpmPlatformExtension.getResourceDescriptionResolver(ModelConstants.JOB_ACQUISITIONS),
            JobAcquisitionAdd.INSTANCE, JobAcquisitionRemove.INSTANCE);
  }
  
}
