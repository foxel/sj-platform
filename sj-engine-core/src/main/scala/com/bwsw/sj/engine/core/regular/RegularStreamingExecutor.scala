package com.bwsw.sj.engine.core.regular

import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager

/**
  * Class that contains an execution logic of regular module
 * Created: 11/04/2016
 *
 * @author Kseniya Mikhaleva
 */

class RegularStreamingExecutor(manager: ModuleEnvironmentManager) {
  /**
   * Will invoke only once at the beginning of launch of module
   */
  def onInit(): Unit = {}

  /**
   * Used for processing one envelope. Will invoke for every message from kafka stream or t-stream
   */
  def onMessage(envelope: Envelope): Unit = {}

  /**
   * Handler triggered before every checkpoint
   */
  def onBeforeCheckpoint(): Unit = {}

  /**
   * Handler triggered after every checkpoint
   */
  def onAfterCheckpoint(): Unit = {}

  /**
   * Will invoke every time when a set timer went out
   *
   * @param jitter Delay between a real response time and an invocation this handler
   */
  def onTimer(jitter: Long): Unit = {}

  /**
   * Handler triggered if idle timeout went out but new txns haven't appeared.
   * Nothing to execute
   */
  def onIdle(): Unit = {}

  /**
   * Handler triggered before save state
    *
    * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  def onBeforeStateSave(isFullState: Boolean): Unit = {}

  /**
   * Handler triggered after save state
    *
    * @param isFullState Flag denotes that there was save of full state (true) or partial changes of state(false)
   */
  def onAfterStateSave(isFullState: Boolean): Unit = {}

}