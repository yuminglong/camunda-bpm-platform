package org.camunda.bpm.engine.impl;

import java.util.List;

import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.management.ProcessDefinitionStatistics;
import org.camunda.bpm.engine.management.ProcessDefinitionStatisticsQuery;

/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ProcessDefinitionStatisticsQueryImpl extends AbstractQuery<ProcessDefinitionStatisticsQuery, ProcessDefinitionStatistics>  
  implements ProcessDefinitionStatisticsQuery {

  protected static final long serialVersionUID = 1L;
  protected boolean includeFailedJobs = false;

  public ProcessDefinitionStatisticsQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return 
      commandContext
        .getStatisticsManager()
        .getStatisticsCountGroupedByProcessDefinitionVersion(this);
  }

  @Override
  public List<ProcessDefinitionStatistics> executeList(CommandContext commandContext,
      Page page) {
    checkQueryOk();
    return 
      commandContext
        .getStatisticsManager()
        .getStatisticsGroupedByProcessDefinitionVersion(this, page);
  }

  public ProcessDefinitionStatisticsQuery includeFailedJobs() {
    includeFailedJobs = true;
    return this;
  }

  public boolean isFailedJobsToInclude() {
    return includeFailedJobs;
  }
}
