package com.bwsw.sj.common.DAL.model

import java.net.{InetSocketAddress, URI}
import java.nio.channels.ClosedChannelException
import java.util.Collections

import com.aerospike.client.{AerospikeClient, AerospikeException}
import com.bwsw.common.ElasticsearchClient
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.provider.ProviderData
import com.bwsw.sj.common.utils.{ConfigLiterals, ProviderLiterals}
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import kafka.javaapi.TopicMetadataRequest
import kafka.javaapi.consumer.SimpleConsumer
import org.apache.zookeeper.ZooKeeper
import org.mongodb.morphia.annotations.{Entity, Id, Property}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

@Entity("providers")
class Provider {
  @Id var name: String = null
  var description: String = null
  var hosts: Array[String] = Array()
  var login: String = null
  var password: String = null
  @Property("provider-type") var providerType: String = null

  def this(name: String, description: String, hosts: Array[String], login: String, password: String, providerType: String) = {
    this()
    this.name = name
    this.description = description
    this.hosts = hosts
    this.login = login
    this.password = password
    this.providerType = providerType
  }

  def getHosts() = {
    val inetSocketAddresses = this.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet

    inetSocketAddresses
  }

  def asProtocolProvider() = {
    val providerData = new ProviderData(
      this.name,
      this.login,
      this.password,
      this.providerType,
      this.hosts,
      this.description
    )

    providerData
  }

  def checkConnection() = {
    val errors = ArrayBuffer[String]()
    for (host <- this.hosts) {
      errors ++= checkProviderConnectionByType(host, this.providerType)
    }

    errors
  }

  private def checkProviderConnectionByType(host: String, providerType: String) = {
    providerType match {
      case ProviderLiterals.cassandraType =>
        checkCassandraConnection(host)
      case ProviderLiterals.aerospikeType =>
        checkAerospikeConnection(host)
      case ProviderLiterals.zookeeperType =>
        checkZookeeperConnection(host)
      case ProviderLiterals.kafkaType =>
        checkKafkaConnection(host)
      case ProviderLiterals.elasticsearchType =>
        checkESConnection(host)
      case ProviderLiterals.jdbcType =>
        checkJdbcConnection(host)
      case _ =>
        throw new Exception(s"Host checking for provider type '$providerType' is not implemented")
    }
  }

  private def checkCassandraConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    try {
      val (host, port) = getHostAndPort(address)

      val builder = Cluster.builder().addContactPointsWithPorts(new InetSocketAddress(host, port))

      val client = builder.build()
      client.getMetadata
      client.close()
    } catch {
      case ex: NoHostAvailableException =>
        errors += s"Cannot gain an access to Cassandra on '$address'"
      case _: Throwable =>
        errors += s"Wrong host '$address'"
    }

    errors
  }

  private def checkAerospikeConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)

    try {
      val client = new AerospikeClient(host, port)
      if (!client.isConnected) {
        errors += s"Cannot gain an access to Aerospike on '$address'"
      }
      client.close()
    } catch {
      case ex: AerospikeException =>
        errors += s"Cannot gain an access to Aerospike on '$address'"
    }

    errors
  }

  private def checkZookeeperConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
    var client: ZooKeeper = null
    try {
      client = new ZooKeeper(address, zkTimeout, null)
      val deadline = 1.seconds.fromNow
      var connected: Boolean = false
      while (!connected && deadline.hasTimeLeft) {
        connected = client.getState.isConnected
      }
      if (!connected) {
        errors += s"Can gain an access to Zookeeper on '$address'"
      }

    } catch {
      case ex: Throwable =>
        errors += s"Wrong host '$address'"
    }
    if (Option(client).isDefined) {
      client.close()
    }

    errors
  }

  private def checkKafkaConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)
    val consumer = new SimpleConsumer(host, port, 500, 64 * 1024, "connectionTest")
    val topics = Collections.singletonList("test_connection")
    val req = new TopicMetadataRequest(topics)
    try {
      consumer.send(req)
    } catch {
      case ex: ClosedChannelException =>
        errors += s"Can not establish connection to Kafka on '$address'"
      case ex: java.io.EOFException =>
        errors += s"Can not establish connection to Kafka on '$address'"
      case ex: Throwable =>
        errors += s"Some issues encountered while trying to establish connection to '$address'"
    }

    errors
  }

  private def checkESConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    val client = new ElasticsearchClient(Set(getHostAndPort(address)))
    if (client.isConnected()) {
      errors += s"Can not establish connection to ElasticSearch on '$address'"
    }
    client.close()

    errors
  }

  private def checkJdbcConnection(address: String) = {
    val errors = ArrayBuffer[String]()

    errors
  }

  private def getHostAndPort(address: String) = {
    val uri = new URI("dummy://" + address)
    val host = uri.getHost()
    val port = uri.getPort()

    (host, port)
  }
}
