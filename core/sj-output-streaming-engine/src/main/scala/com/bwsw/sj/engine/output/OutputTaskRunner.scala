package com.bwsw.sj.engine.output

import java.util.concurrent.{ExecutorCompletionService, Executors}

import com.bwsw.sj.common.ModuleConstants
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.engine.OutputTaskEngineFactory
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.{Logger, LoggerFactory}

/**
 * Runner object for engine of output-streaming module
 * Created: 26/05/2016
 *
 * @author Kseniya Tomskikh
 */
object OutputTaskRunner {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val threadPool = createThreadPool()
  private val executorService = new ExecutorCompletionService[Unit](threadPool)
  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(ModuleConstants.persistentBlockingQueue)

  private def createThreadPool() = {
    val countOfThreads = 3
    val threadFactory = createThreadFactory()

    Executors.newFixedThreadPool(countOfThreads, threadFactory)
  }

  private def createThreadFactory() = {
    new ThreadFactoryBuilder()
      .setNameFormat("OutputTaskRunner-%d")
      .build()
  }

  def main(args: Array[String]) {
    try {
      val manager = new OutputTaskManager()

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for output module\n")

      val performanceMetrics = new OutputStreamingPerformanceMetrics(manager)

      val outputTaskEngineFactory = new OutputTaskEngineFactory(manager, performanceMetrics, blockingQueue)

      val outputTaskEngine = outputTaskEngineFactory.createOutputTaskEngine()

      val outputTaskInputService: TaskInputService = outputTaskEngine.taskInputService

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(outputTaskInputService)
      executorService.submit(outputTaskEngine)
      executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case assertionError: Error => handleException(assertionError)
      case exception: Exception => handleException(exception)
    }
  }

  def handleException(exception: Throwable) = {
    logger.error("Runtime exception", exception)
    exception.printStackTrace()
    threadPool.shutdownNow()
    System.exit(-1)
  }
}