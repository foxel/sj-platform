package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.{ServiceData, TstrQServiceData}
import com.bwsw.sj.common.utils.{ProviderLiterals, CassandraFactory, ServiceLiterals}
import org.mongodb.morphia.annotations.{Property, Reference}

class TStreamService() extends Service {
  serviceType = ServiceLiterals.tstreamsType
  @Reference(value = "metadata_provider") var metadataProvider: Provider = null
  @Property("metadata_namespace") var metadataNamespace: String = null
  @Reference(value = "data_provider") var dataProvider: Provider = null
  @Property("data_namespace") var dataNamespace: String = null
  @Reference(value = "lock_provider") var lockProvider: Provider = null
  @Property("lock_namespace") var lockNamespace: String = null

  def this(name: String,
           serviceType: String,
           description: String,
           metadataProvider: Provider,
           metadataNamespace: String,
           dataProvider: Provider,
           dataNamespace: String,
           lockProvider: Provider,
           lockNamespace: String) = {
    this()
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.metadataProvider = metadataProvider
    this.metadataNamespace = metadataNamespace
    this.dataProvider = dataProvider
    this.dataNamespace = dataNamespace
    this.lockProvider = lockProvider
    this.lockNamespace = lockNamespace
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new TstrQServiceData()
    super.fillProtocolService(protocolService)

    protocolService.metadataProvider = this.metadataProvider.name
    protocolService.metadataNamespace = this.metadataNamespace
    protocolService.dataProvider = this.dataProvider.name
    protocolService.dataNamespace = this.dataNamespace
    protocolService.lockProvider = this.lockProvider.name
    protocolService.lockNamespace = this.lockNamespace

    protocolService
  }

  override def prepare() = {
    val cassandraFactory = new CassandraFactory
    cassandraFactory.open(this.metadataProvider.getHosts())
    cassandraFactory.createKeyspace(this.metadataNamespace)
    cassandraFactory.createMetadataTables(this.metadataNamespace)

    if (this.dataProvider.providerType.equals(ProviderLiterals.cassandraType)) {
      val cassandraFactory = new CassandraFactory
      cassandraFactory.open(this.dataProvider.getHosts())
      cassandraFactory.createKeyspace(this.dataNamespace)
      cassandraFactory.createDataTable(this.dataNamespace)
      cassandraFactory.close()
    }
    cassandraFactory.close()
  }
}
