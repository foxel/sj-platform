package com.bwsw.sj.engine.input

import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.ExecutorService

import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}
import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCountUtil
import org.slf4j.LoggerFactory

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 * Created: 10/07/2016
 *
 * @author Kseniya Mikhaleva
 */
abstract class InputTaskEngine(manager: InputTaskManager) {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected var txnsByStreamPartitions = createTxnsStorage()
  protected val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] = manager.createOutputProducers
  protected val checkpointGroup = new CheckpointGroup()
  protected val moduleEnvironmentManager = new InputEnvironmentManager()
  protected val executor: InputStreamingExecutor = manager.getExecutor(moduleEnvironmentManager)
  protected val isNotOnlyCustomCheckpoint: Boolean

  addProducersToCheckpointGroup()

  protected def txnOpen(txn: UUID) = {
    println("txnOpen: UUID = " + txn) //todo
  }

  protected def txnClose(txn: UUID) = {
    println("txnClose: UUID = " + txn) //todo
  }

  protected def txnCancel(txn: UUID) = {
    println("txnCancel: UUID = " + txn) //todo
  }

  protected def envelopeProcessed(envelope: Option[InputEnvelope], isNotDuplicateOrEmpty: Boolean) = {
    if (isNotDuplicateOrEmpty) { //todo
      println("Envelope has been sent")
    } else if (envelope.isDefined) {
      println("Envelope is duplicate")
    } else {
      println("Envelope is empty")
    }
  }

  protected def processEnvelope(envelope: Option[InputEnvelope]): Boolean = {
    if (envelope.isDefined) {
      logger.info(s"Task name: ${manager.taskName}. Envelope is defined. Process it\n")
      val inputEnvelope = envelope.get
      if (checkForDuplication(inputEnvelope.key, inputEnvelope.duplicateCheck, inputEnvelope.data)) {
        inputEnvelope.outputMetadata.foreach(x => sendEnvelope(x._1, x._2, inputEnvelope.data))
        return true
      }
    }
    false
  }

  protected def sendEnvelope(stream: String, partition: Int, data: Array[Byte]) = {
    logger.info(s"Task name: ${manager.taskName}. Send envelope to each output stream.\n")
    val maybeTxn = getTxn(stream, partition)
    if (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      txn.send(data)
    } else {
      val txn = producers(stream).newTransaction(ProducerPolicies.errorIfOpen, partition)
      txn.send(data)
      txnOpen(txn.getTxnUUID)
    }
  }

  protected def checkForDuplication(key: String, duplicateCheck: Boolean, value: Array[Byte]): Boolean = {
    logger.info(s"Task name: ${manager.taskName}. " +
        s"Try to check key: $key for duplication with a setting duplicateCheck = $duplicateCheck\n")
    val uniqueEnvelopes = manager.getUniqueEnvelopes
    if (duplicateCheck) {
      logger.info(s"Task name: ${manager.taskName}. " +
        s"Check key: $key for duplication\n")
      if (!uniqueEnvelopes.containsKey(key)) {
        uniqueEnvelopes.put(key, value)
        return false
      }
      else uniqueEnvelopes.replace(key, value)
    }
    true
  }

  def runModule(executorService: ExecutorService, buffer: ByteBuf) = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run input task engine in a separate thread of execution service\n")
    executorService.execute(new Runnable {
      override def run(): Unit = try {
        while (true) {
          val maybeInterval = executor.tokenize(buffer)
          if (maybeInterval.isDefined) {
            val (beginIndex, endIndex) = maybeInterval.get
            if (buffer.isReadable(endIndex)) {
              println("before reading: " + buffer.toString(Charset.forName("UTF-8")) + "_") //todo: only for testing
              val inputEnvelope = executor.parse(buffer, beginIndex, endIndex)
              clearBufferAfterParsing(buffer, endIndex)
              println("after reading: " + buffer.toString(Charset.forName("UTF-8")) + "_") //todo: only for testing
              val isNotDuplicateOrEmpty = processEnvelope(inputEnvelope)
              envelopeProcessed(inputEnvelope, isNotDuplicateOrEmpty)
              doCheckpoint(moduleEnvironmentManager.isCheckpointInitiated)
            } else {
              logger.error(s"Task name: ${manager.taskName}. " +
                s"Method tokenize() returned end index that an input stream is not defined at\n")
              throw new IndexOutOfBoundsException("Method tokenize() returned end index that an input stream is not defined at")
            }
          } else Thread.sleep(2000) //todo: only for testing
        }
      } finally {
        ReferenceCountUtil.release(buffer)
      }
    })
  }

  protected def doCheckpoint(isCheckpointInitiated: Boolean): Unit

  protected def clearBufferAfterParsing(buffer: ByteBuf, endIndex: Int) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Remove a message, which have just been parsed, from input buffer\n")
    buffer.readerIndex(endIndex)
    buffer.discardReadBytes()
  }

  private def getTxn(stream: String, partition: Int) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Try to get txn by stream: $stream, partition: $partition\n")
    if (txnsByStreamPartitions(stream).isDefinedAt(partition)) {
      Some(txnsByStreamPartitions(stream)(partition))
    } else None
  }

  protected def createTxnsStorage() = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Create storage for keeping txns for each partition of output streams\n")
    producers.map(x => (x._1, Map[Int, BasicProducerTransaction[Array[Byte], Array[Byte]]]()))
  }

  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    producers.foreach(x => checkpointGroup.add(x._2.name, x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
  }
}