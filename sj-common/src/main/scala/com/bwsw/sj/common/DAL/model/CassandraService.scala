package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class CassandraService extends Service {
  @Reference var provider: Provider = null
  var keyspace: String = null
}
