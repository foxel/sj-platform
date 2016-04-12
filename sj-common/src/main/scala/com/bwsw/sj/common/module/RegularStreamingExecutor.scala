package com.bwsw.sj.common.module

import com.bwsw.sj.common.module.entities.Transaction

/**
 * Class that contains an execution logic of regular module
 * Created: 11/04/2016
 * @author Kseniya Mikhaleva
 */

abstract class RegularStreamingExecutor(moduleEnvironmentManager: ModuleEnvironmentManager) {

  def init(): Unit

  def run(transaction: Transaction): Unit

  def finish(): Unit

  def onCheckpoint(): Unit

  def onTimer(): Unit

}

/**
 * Class that contains an execution logic of windowed module
 * Created: 11/04/2016
 * @author Kseniya Mikhaleva
 */

abstract class WindowedStreamingExecutor() {

}

