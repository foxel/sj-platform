package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Entity for windowed instance-json
  * Created:  13/04/2016
  * @author Kseniya Tomskikh
  */
case class WindowedInstanceMetadata(var uuid: String,
                                    @JsonProperty("module-name") var moduleName: String,
                                    @JsonProperty("module-version") var moduleVersion: String,
                                    var name: String,
                                    var description: String,
                                    var inputs: List[String],
                                    var outputs: List[String],
                                    @JsonProperty("checkpoint-mode") var checkpointMode: String,
                                    @JsonProperty("checkpoint-interval") var checkpointInterval: Int,
                                    @JsonProperty("state-management") var stateManagement: String,
                                    @JsonProperty("checkpoint-full-interval") var checkpointFullInterval: Int,
                                    var parallelism: Int,
                                    var options: Map[String, Any],
                                    @JsonProperty("start-from") var startFrom: Any,
                                    @JsonProperty("per-executor-cores") var perExecutorCores: Int,
                                    @JsonProperty("per-executor-ram") var perExecutorRam: Int,
                                    @JsonProperty("jvm-options") var jvmOptions: Map[String, Any],
                                    @JsonProperty("time-windowed") var timeWindowed: Int,
                                    @JsonProperty("window-full-max") var windowFullMax: Int) extends InstanceMetadata
