package com.bwsw.sj.engine.regular.task.engine.state

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics

import scala.collection.Map

/**
 * Class is in charge of creating a ModuleEnvironmentManager (and executor)
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
class StatelessRegularTaskEngineService(manager: RegularTaskManager, performanceMetrics: RegularStreamingPerformanceMetrics)
  extends RegularTaskEngineService(manager, performanceMetrics) {

  val moduleEnvironmentManager = new ModuleEnvironmentManager(
    serializer.deserialize[Map[String, Any]](regularInstance.options),
    outputProducers,
    regularInstance.outputs
      .map(ConnectionRepository.getStreamService.get)
      .filter(_.tags != null),
    outputTags,
    moduleTimer,
    performanceMetrics
  )

  val executor = manager.getExecutor(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]
}