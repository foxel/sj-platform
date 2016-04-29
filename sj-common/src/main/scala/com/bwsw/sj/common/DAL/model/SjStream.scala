package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Entity, Id}

@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = null
  var partitions: Int = 0
  var service: Service = null
  var tags: String = null
  var generator: Array[String] = null
}