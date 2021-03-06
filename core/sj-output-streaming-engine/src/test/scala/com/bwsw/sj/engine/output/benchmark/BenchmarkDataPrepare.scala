package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
 *
 *
 * @author Kseniya Tomskikh
 */
object BenchmarkDataPrepare extends App {

  val instanceName: String = "test-bench-instance"
  val checkpointInterval = 3
  val checkpointMode = EngineLiterals.everyNthCheckpointMode
  val partitions = 4

  val module = new File(getClass.getClassLoader.getResource("sj-stub-output-bench-test.jar").getPath)
  println("module upload")
  uploadModule(module)
  open()
  println("cassandra prepare")
  prepareCassandra("bench")
  println("create providers")
  createProviders()
  println("create services")
  createServices()
  println("create streams")
  createStreams(partitions)
  println("create instance")
  createInstance(instanceName, checkpointMode, checkpointInterval)

  println("create test data")
  createData(50, 20)

  println("close connections")
  close()
  ConnectionRepository.close()

  println("DONE")

}