package com.bwsw.sj.engine.regular

import java.util.concurrent.{ExecutorCompletionService, Executors}

import com.bwsw.sj.common.ModuleConstants
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.engine.input.RegularTaskInputService
import com.bwsw.sj.engine.regular.task.engine.{RegularTaskEngine, RegularTaskEngineFactory}
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory

/**
 * Object is responsible for running a task of job that launches regular module
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)
  val countOfThreads = 3
  private val threadFactory = createThreadFactory()
  private val threadPool = Executors.newFixedThreadPool(countOfThreads, threadFactory)
  private val executorService = new ExecutorCompletionService[Unit](threadPool)

  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(ModuleConstants.persistentBlockingQueue)

  def main(args: Array[String]) {
    try {
      val manager = new RegularTaskManager()

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for regular module\n")

      val performanceMetrics: RegularStreamingPerformanceMetrics = new RegularStreamingPerformanceMetrics(manager)

      val regularTaskEngineFactory = new RegularTaskEngineFactory(manager, performanceMetrics, blockingQueue)

      val regularTaskEngine: RegularTaskEngine = regularTaskEngineFactory.createInputTaskEngine()

      val regularTaskInputService: RegularTaskInputService = regularTaskEngine.regularTaskInputService

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(regularTaskInputService)
      executorService.submit(regularTaskEngine)
      executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case assertionError: Error => handleException(assertionError)
      case exception: Exception => handleException(exception)
    }
  }

  def createThreadFactory() = {
    new ThreadFactoryBuilder()
      .setNameFormat("RegularTaskRunner-%d")
      .build()
  }

  def handleException(exception: Throwable) = {
    exception.printStackTrace()
    threadPool.shutdownNow()
    System.exit(-1)
  }
}