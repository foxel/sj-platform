package com.bwsw.sj.common.module

import java.util.{Timer, TimerTask}

/**
 * Class representing a wrapper for java.util.Timer
 * Created: 14/04/2016
 * @author Kseniya Mikhaleva
 */

class SjTimer {

  /**
   * Flag defines the timer time went out or not
   */
  private var isTimerWentOut = false

  private var timer: Timer = null

  /**
   * Sets a timer handler that changes flag on true value when time is went out
   * @param delay
   */
  def set(delay: Long) = {
    timer = new Timer()
    timer.schedule(new TimerTask {
      def run() {
        isTimerWentOut = true
      }
    }, delay)
  }

  /**
   * Allows checking a timer has went out or not
   * @return The result of checking
   */
  def isTime: Boolean = {
    isTimerWentOut
  }

  /**
   * Allows resetting a timer 
   */
  def reset() = {
    timer.cancel()
    isTimerWentOut = false
  }
}
