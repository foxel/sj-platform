package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.utils.EngineConstants
import com.fasterxml.jackson.annotation.JsonProperty

class RegularInstanceMetadata extends InstanceMetadata {
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  @JsonProperty("execution-plan") var executionPlan: Map[String, Any] = null //todo используется только для того, чтобы показывать пользователям, мб стоит его заполнять,
  // а потом только конвертировать в модельный инстанс
  @JsonProperty("start-from") var startFrom: String = EngineConstants.newestStartMode
  @JsonProperty("state-management") var stateManagement: String = EngineConstants.noneStateMode
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 1000

  override def asModelInstance() = {
    val modelInstance = new RegularInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.stateManagement = this.stateManagement
    modelInstance.stateFullCheckpoint = this.stateFullCheckpoint
    modelInstance.eventWaitTime = this.eventWaitTime
    modelInstance.inputs = this.inputs
    modelInstance.outputs = this.outputs
    modelInstance.startFrom = this.startFrom

    modelInstance
  }
}
