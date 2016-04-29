package com.bwsw.sj.common.DAL.model

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{Entity, Id}

@Entity("fs.files")
class FileMetadata() {
  @Id val _id: ObjectId = null
  val name: String = null
  val filename: String = null
  val filetype: String = null
  val specification: Specification = null
}